// rawTest is a simple read-after-write test using the blockfiles protocol.
package main

import (
	"encoding/binary"
	"flag"
	"fmt"
	"net"
	"os"
)

func usage() {
	fmt.Fprintf(os.Stderr, "Usage of %s:\n", os.Args[0])
	fmt.Fprintf(os.Stderr, "  %s [port]\n", os.Args[0])
	flag.PrintDefaults()
}

func main() {
	flag.Usage = usage
	flag.Parse()

	if flag.NArg() != 1 {
		usage()
		os.Exit(2)
	}
	port := flag.Arg(0)

	fmt.Printf("Connecting to blocks at port %s ... ", port)
	conn, err := net.Dial("tcp", port)
	if err != nil {
		panic(err)
	}
	fmt.Printf("Connected!\n")
	offset := 0
	length := 256

	write := make([]byte, 9+length)
	write[0] = 1
	binary.BigEndian.PutUint32(write[1:5], uint32(offset))
	binary.BigEndian.PutUint32(write[5:9], uint32(length))
	for i := 0; i < length; i++ {
		write[9+i] = byte(i)
	}
	conn.Write(write)

	write_response := make([]byte, 9)
	conn.Read(write_response)
	write_resp_offset := binary.BigEndian.Uint32(write_response[1:5])
	write_resp_length := binary.BigEndian.Uint32(write_response[5:9])

	if write_resp_offset != uint32(offset) {
		fmt.Printf("Write response ffset mismatched!")
	}
	if write_resp_length != uint32(length) {
		fmt.Printf("Write response length mismatched!")
	}

	read := make([]byte, 9)
	read[0] = 0
	binary.BigEndian.PutUint32(read[1:5], uint32(offset))
	binary.BigEndian.PutUint32(read[5:9], uint32(length))
	conn.Write(read)

	response := make([]byte, 9+length)
	got := 0
	for n := 0; n != 9+length; got, _ = conn.Read(response[n:]) {
		if got != 0 {
			n += got
		}
	}

	resp_offset := binary.BigEndian.Uint32(response[1:5])
	resp_length := binary.BigEndian.Uint32(response[5:9])

	if resp_offset != uint32(offset) {
		fmt.Printf("Offset mismatched!")
	}
	if resp_length != uint32(length) {
		fmt.Printf("Length mismatched!")
	}
	for i := 0; i < length-9; i++ {
		if response[i+9] != write[i+9] {
			fmt.Printf("Mismatch at byte %d. Saw %d, expected %d\n", 9+i, response[i+9], write[i+9])
		}
	}

}
