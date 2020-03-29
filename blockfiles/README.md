# Filesystem

we turn the memory region into a block device by first defining in in Golang, presenting it as a
file in a FUSE filesystem using the Golang bazil.org/fuse library, and then mount it as a loopback
device.

## How to use this?

```sh
$ go get .
$ go build
$ mkdir [directory]
$ chmod 0777 [directory]
$ sudo ./blockfiles [directory] [host:port]
$ mkfs.ext4 [directory]/blocks
$ sudo sudo losetup --find --show [directory]
/dev/loopN
$ sudo mount /dev/loop0 /mnt/blocks
```

This connects to a TCP socket on `[host:port]`, and sends out read and write requests to the fake file.

If you're not running the world's most popular block storage device already, you can emulate it with
the blockTest program:

```sh
$ go build ./cmd/blockTest
$ blockTest [host:port]
```

## Why not use NBD?

I couldn't get it working quickly.
