// blockfiles is a wrapper around bazil.org/fuse that exposes a byte array as a File in
// userspace at [mountpoint]/blocks that can be interacted with over a socket serving at [port]
// through a very simple protocol.
package main

import (
	"context"
	"encoding/binary"
	"flag"
	"fmt"
	"log"
	"net"
	"os"
	"sync"
	"syscall"

	"bazil.org/fuse"
	"bazil.org/fuse/fs"
	_ "bazil.org/fuse/fs/fstestutil"
	"bazil.org/fuse/fuseutil"
)

type RWBuf struct {
	buffer []byte
	lock   sync.RWMutex
}

func (b *RWBuf) Read(offset, length int) []byte {
	b.lock.RLock()
	defer b.lock.RUnlock()
	if (offset + length) > len(b.buffer) {
		length = len(b.buffer) - offset
	}
	data := make([]byte, length)
	for i := 0; i <= int(length); i++ {
		data[i] = b.buffer[offset+i]
	}
	return data
}

func (b *RWBuf) Write(offset, length int, data []byte) {
	b.lock.Lock()
	defer b.lock.Unlock()

}

func usage() {
	fmt.Fprintf(os.Stderr, "Usage of %s:\n", os.Args[0])
	fmt.Fprintf(os.Stderr, "  %s [mountpoint] [port]\n", os.Args[0])
	flag.PrintDefaults()
}

func main() {
	flag.Usage = usage
	flag.Parse()

	if flag.NArg() != 2 {
		usage()
		os.Exit(2)
	}
	mountpoint := flag.Arg(0)
	port := flag.Arg(1)
	buffer := make([]byte, 1024*1024)
	New(mountpoint, port, &buffer)
}

func New(mountpoint, port string, buffer *[]byte) {
	c, err := fuse.Mount(
		mountpoint,
		fuse.FSName("blocks"),
		fuse.Subtype("minecraftFS"),
		fuse.LocalVolume(),
		fuse.VolumeName("Block Files"),
		fuse.AllowOther(),
	)
	if err != nil {
		log.Fatal(err)
	}
	defer c.Close()

	go serve(port, buffer)

	system := FS{d: Dir{f: File{buffer}}}
	err = fs.Serve(c, &system)
	if err != nil {
		log.Fatal(err)
	}

	// check if the mount process has an error to report
	<-c.Ready
	if err := c.MountError; err != nil {
		log.Fatal(err)
	}

}

// Serves up the buffer for random manipulation via a socket.
// Panic if it sees any errors, since this is a very jumpy server.
func serve(port string, buffer *[]byte) {
	fmt.Printf("Strated listening on port %s\n", port)
	ln, err := net.Listen("tcp", port)
	if err != nil {
		panic(err)
	}
	for {
		conn, err := ln.Accept()
		if err != nil {
			panic(err)
		}
		go handleConnection(conn, buffer)
	}
}

// handleConnection handles the TCP connection.
// Request protocol is:
//  byte 0:           0 read / 1 write
//  byte 1-8:         offset
//  byte 9-16:        length
//  byte 17...length: data (if write)
// Response protocol is:
//  byte 0:           0 read / 1 write
//  byte 1...length:  data
// TODO: This doesn't check errors from the connection reads and writes, which seems failure prone,
//  at best.
func handleConnection(conn net.Conn, buffer *[]byte) {
	for {
		header := make([]byte, 17)
		n, err := conn.Read(header)
		if n != 17 {
			continue
		}
		if err != nil {
			panic(err)
		}
		kind := header[0]
		offset := binary.BigEndian.Uint64(header[1:9])
		length := binary.BigEndian.Uint64(header[9:17])
		if kind == 1 { // Write
			fmt.Printf("Got write command of length %d to offset %d\n", length, offset)
			data := make([]byte, length)
			conn.Read(data)
			fmt.Printf("Writing data %s\n", string(data))
			for i := 0; i < len(data); i++ {
				(*buffer)[int(offset)+i] = data[i]
			}
			conn.Write([]byte{kind})
		} else {
			fmt.Printf("Got read command of length %d to offset %d\n", length, offset)
			data := make([]byte, length+1)
			data[0] = kind
			for i := 0; i <= int(length); i++ {
				data[i+1] = (*buffer)[int(offset)+i]
			}
			conn.Write(data)
		}
	}
}

// FS implements the hello world file system.
type FS struct {
	d Dir
}

func (fs *FS) Root() (fs.Node, error) {
	return &fs.d, nil
}

// Dir implements both Node and Handle for the root directory.
type Dir struct {
	f File
}

func (d *Dir) Attr(ctx context.Context, a *fuse.Attr) error {
	a.Inode = 1000
	a.Mode = os.ModeDir | 0664
	return nil
}

func (d *Dir) Lookup(ctx context.Context, name string) (fs.Node, error) {
	if name == "blocks" {
		return &d.f, nil
	}
	return nil, syscall.ENOENT
}

var DirDirs = []fuse.Dirent{
	{Inode: 2, Name: "blocks", Type: fuse.DT_File},
}

func (d *Dir) ReadDirAll(ctx context.Context) ([]fuse.Dirent, error) {
	return DirDirs, nil
}

// File implements both Node and Handle for the blocks File.
type File struct {
	contents *[]byte
}

func (f *File) Attr(ctx context.Context, a *fuse.Attr) error {
	a.Inode = 2000
	a.Mode = 0664
	a.Size = uint64(len(*f.contents))
	a.Blocks = uint64(len(*f.contents)) / 512
	return nil
}

func (f *File) Access(ctx context.Context, req *fuse.AccessRequest) error {
	return nil
}

func (f *File) Setattr(ctx context.Context, req *fuse.SetattrRequest, resp *fuse.SetattrResponse) error {
	return nil
}

func (f *File) ReadAll(ctx context.Context) ([]byte, error) {
	return []byte(*f.contents), nil
}

func (f *File) Read(ctx context.Context, req *fuse.ReadRequest, resp *fuse.ReadResponse) error {
	fuseutil.HandleRead(req, resp, *f.contents)
	return nil
}

func (f *File) Write(ctx context.Context, req *fuse.WriteRequest, resp *fuse.WriteResponse) error {
	if int(req.Offset)+len(req.Data) > len(*f.contents) {
		new_contents := make([]byte, int(req.Offset)+len(req.Data))
		copy(new_contents, *f.contents)
		f.contents = &new_contents
	}
	for i := 0; i < len(req.Data); i++ {
		(*f.contents)[int(req.Offset)+i] = req.Data[i]
	}
	resp.Size = len(req.Data)
	return nil
}

func (f *File) Fsync(ctx context.Context, req *fuse.FsyncRequest) error {
	return nil
}
