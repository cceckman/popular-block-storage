package main

import (
	"encoding/binary"
	"flag"
	"fmt"
	"net"
	"os"
)

const _SIZE = 1024 * 1024

func usage() {
	fmt.Fprintf(os.Stderr, "Usage of %s:\n", os.Args[0])
	fmt.Fprintf(os.Stderr, "  %s [port]", os.Args[0])
	flag.PrintDefaults()
}

func main() {
	flag.Usage = usage
	flag.Parse()

	if flag.NArg() != 1 {
		fmt.Printf("Got %d args\n", flag.NArg())
		usage()
		os.Exit(2)
	}

	buf := make([]byte, _SIZE)
	serve(flag.Arg(0), &buf)
}

func serve(port string, buffer *[]byte) {
	fmt.Printf("Started listening on port %s\n", port)
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

func handleConnection(conn net.Conn, buffer *[]byte) {
	for {
		header := make([]byte, 9)
		n, err := conn.Read(header)
		if n != 9 {
			continue
		}
		if err != nil {
			panic(err)
		}
		kind := header[0]
		offset := binary.BigEndian.Uint32(header[1:5])
		length := binary.BigEndian.Uint32(header[5:9])
		if kind == 1 { // Write
			fmt.Printf("Got write command of length %d to offset %d\n", length, offset)
			data := make([]byte, length)
			conn.Read(data)
			fmt.Printf("Writing data %s\n", string(data))
			for i := 0; i < len(data); i++ {
				if (int(offset) + i + 1) < len(*buffer) {
					(*buffer)[int(offset)+i] = data[i]
				}
			}
			conn.Write(header)
		} else {
			fmt.Printf("Got read command of length %d to offset %d\n", length, offset)
			data := make([]byte, 9+length)
			copy(data, header)
			for i := 0; i <= int(length); i++ {
				if (int(offset) + i + 1) < len(*buffer) {
					data[i+9] = (*buffer)[int(offset)+i]
				}
			}
			conn.Write(data)
		}
	}
}
