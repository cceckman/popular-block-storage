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
	"syscall"

	"bazil.org/fuse"
	"bazil.org/fuse/fs"
	_ "bazil.org/fuse/fs/fstestutil"
)

// Size of the remote buffer in bytes.
const _DISK_SIZE = 1 * 1024 * 1024

// How many bytes to transfer in each transaction.
const _PAGE_SIZE = 4096

// TODO(slongfield): For faster reads, we should cache the data if we don't write between reads.

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
	New(mountpoint, port)
}

type BlockRequest struct {
	write  bool
	offset uint32
	length uint32
	data   []byte
}

type BlockResponse struct {
	write  bool
	offset uint32
	length uint32
	data   []byte
}

func New(mountpoint, port string) {
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

	requests := make(chan BlockRequest)
	responses := make(chan BlockResponse)
	defer close(requests)
	defer close(responses)
	go getBlocks(port, requests, responses)

	system := FS{d: Dir{f: File{requests, responses}}}
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

func parseResponse(data []byte) BlockResponse {
	kind := data[0]
	offset := binary.BigEndian.Uint32(data[1:5])
	length := binary.BigEndian.Uint32(data[5:9])
	if kind == 0 { // Reads should get data back.
		read := make([]byte, length)
		for i := 0; i < int(length); i++ {
			read[i] = data[i+9]
		}
		return BlockResponse{
			write:  false,
			offset: offset,
			length: length,
			data:   read,
		}
	}
	return BlockResponse{
		write:  true,
		offset: offset,
		length: length,
		data:   nil,
	}
}

// getBlocks handles the TCP connection.
// Request protocol is:
//  byte 0:          0 read / 1 write
//  byte 1-4:        offset
//  byte 5-9:        length
//  byte 9...length: data (if write)
// Response protocol is:
//  byte 0:           0 read / 1 write
//  byte 1-4:         offset
//  byte 5-9:         length
//  byte 9...length:  data (if read)
// TODO: This doesn't check errors from the connection reads and writes, which seems failure prone,
//  at best.
func getBlocks(port string, send chan BlockRequest, recieve chan BlockResponse) {
	fmt.Printf("Connecting to blocks at port %s ... ", port)
	conn, err := net.Dial("tcp", port)
	if err != nil {
		panic(err)
	}
	fmt.Printf("Connected!\n")
	for request := range send {
		header := make([]byte, 9)
		binary.BigEndian.PutUint32(header[1:5], request.offset)
		binary.BigEndian.PutUint32(header[5:9], request.length)
		if request.write {
			fmt.Printf("Sending a write request with length %d and offset %d\n", request.length, request.offset)
			header[0] = 1
			conn.Write(header)
			conn.Write(request.data)
			response := make([]byte, 9)
			got := 0
			for n := 0; n != 9; got, _ = conn.Read(response[n:]) {
				if got != 0 {
					n += got
					fmt.Printf("Got response of length %d/%d\n", n, 9)
				}
			}
			recieve <- parseResponse(response)
		} else {
			fmt.Printf("Sending a read request with length %d and offset %d\n", request.length, request.offset)
			header[0] = 0
			conn.Write(header)
			response := make([]byte, 9+request.length)
			got := 0
			for n := 0; n != 9+int(request.length); got, _ = conn.Read(response[n:]) {
				if got != 0 {
					n += got
					fmt.Printf("Got response of length %d/%d\n", n, 9+int(request.length))
				}
			}
			recieve <- parseResponse(response)
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
	send    chan BlockRequest
	receive chan BlockResponse
}

func (f *File) Attr(ctx context.Context, a *fuse.Attr) error {
	a.Inode = 2000
	a.Mode = 0664
	a.Size = _DISK_SIZE
	a.Blocks = _DISK_SIZE / 512
	return nil
}

func (f *File) Access(ctx context.Context, req *fuse.AccessRequest) error {
	return nil
}

func (f *File) Setattr(ctx context.Context, req *fuse.SetattrRequest, resp *fuse.SetattrResponse) error {
	return nil
}

func (f *File) handleRead(offset, length int) []byte {
	received := 0
	data := make([]byte, length)
	for ; received < length; received += _PAGE_SIZE {
		var transaction_length int
		if received+_PAGE_SIZE < length {
			transaction_length = _PAGE_SIZE
		} else {
			transaction_length = length - received
		}
		f.send <- BlockRequest{
			write:  false,
			offset: uint32(offset + received),
			length: uint32(transaction_length),
		}
		resp := <-f.receive
		copy(data[received:(received+transaction_length)], resp.data)
	}
	return data
}

func (f *File) Read(ctx context.Context, req *fuse.ReadRequest, resp *fuse.ReadResponse) error {
	resp.Data = f.handleRead(int(req.Offset), int(req.Size))
	return nil
}

func (f *File) Write(ctx context.Context, req *fuse.WriteRequest, resp *fuse.WriteResponse) error {
	written := 0
	length := int(len(req.Data))
	for written < length {
		var transaction_length int
		if written+_PAGE_SIZE < length {
			transaction_length = _PAGE_SIZE
		} else {
			transaction_length = length - written
		}
		f.send <- BlockRequest{
			write:  true,
			offset: uint32(int(req.Offset) + written),
			length: uint32(transaction_length),
			data:   req.Data[written:(written + transaction_length)],
		}
		my_resp := <-f.receive
		written += int(my_resp.length)
	}
	resp.Size = written
	return nil
}

func (f *File) Fsync(ctx context.Context, req *fuse.FsyncRequest) error {
	f.handleRead(0, 0)
	return nil
}
