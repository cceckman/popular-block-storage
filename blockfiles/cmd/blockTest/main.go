package main

import (
	"encoding/binary"
	"flag"
	"fmt"
	"io"
	"net"
	"os"
	"strconv"
)

func usage() {
	fmt.Fprintf(os.Stderr, "Usage of %s:\n", os.Args[0])
	fmt.Fprintf(os.Stderr, "  %s [port] [read|write] [offset] [length|values]", os.Args[0])
	flag.PrintDefaults()
}

func main() {
	flag.Usage = usage
	flag.Parse()

	if flag.NArg() != 4 {
		fmt.Printf("Got %d args", flag.NArg())
		usage()
		os.Exit(2)
	}

	conn, err := net.Dial("tcp", flag.Arg(0))
	if err != nil {
		panic(err)
	}

	if flag.Arg(1) == "read" {
		header := make([]byte, 17)
		offset, err := strconv.Atoi(flag.Arg(2))
		if err != nil {
			panic(err)
		}
		length, err := strconv.Atoi(flag.Arg(3))
		if err != nil {
			panic(err)
		}
		binary.BigEndian.PutUint64(header[1:9], uint64(offset))
		binary.BigEndian.PutUint64(header[9:17], uint64(length))
		conn.Write(header)
		data := make([]byte, length)
		conn.Read(data)
		fmt.Println(string(data[1:]))
		conn.Write([]byte(io.EOF))
	} else if flag.Arg(1) == "write" {
		offset, err := strconv.Atoi(flag.Arg(2))
		if err != nil {
			panic(err)
		}
		length := len(flag.Arg(3))
		bufsize := 17 + length
		buf := make([]byte, bufsize)
		buf[0] = 1
		binary.BigEndian.PutUint64(buf[1:9], uint64(offset))
		binary.BigEndian.PutUint64(buf[9:17], uint64(length))
		for i := 0; i < length; i++ {
			buf[i+17] = flag.Arg(3)[i]
		}
		conn.Write(buf)
		conn.Write([]byte(io.EOF))
	} else {
		fmt.Printf("Unknown command %s", flag.Arg(1))
		usage()
		os.Exit(2)
	}
}
