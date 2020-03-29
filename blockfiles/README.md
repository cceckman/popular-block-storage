# Filesystem

we turn the memory region into a block device by first defining in in Golang, presenting it as a
file in a FUSE filesystem using the Golang bazil.org/fuse library, and then mount it as a loopback
device.

## How to use this?

```sh
$ go build
$ sudo ./blockfiles [directory] [port]
$ mkfs.ext4 [directory]
$ sudo sudo losetup --find --show [directory]
/dev/loopN
$ sudo mount /dev/loop0 /mnt/blocks
```

This exposes a unix socket on `[port]`, which sends out read and write requests as they come in.

If you're not running the world's most popular block storage device already, you can emulate it with
the blockTest program:

```sh
$ go build cmd/blockTest
$ blockTest [port]
```

## Why not use NBD?

I couldn't get it working quickly.
