# Integrating Popular Block Storage into Linux

Slides [here](https://docs.google.com/presentation/d/1wgD_L70IqBUjulB82w8W-aECEOIKt1o3ZRgdD2w-P1Q/edit?usp=sharing)

Repository for [cceckman](https://github.com/cceckman) and [slongfield](https://github.com/slongfield)'s 2020 SIGBOVIK submission: Integrating Popular Block Storage into Linux.

The code / instructions in this repository allow you to use Minecraft chests as Linux block storage devices.

## Howto

### Prerequisites

- (Legitimate) access to [Minecraft Java Edition](https://www.minecraft.net/en-us/store/minecraft-java-edition/)
- A Linux host with:
    - Docker
    - A recent [Go](https://golang.org) toolchain
    - openjdk-11-jre and openjdk-11-jdk
    - Apache Maven
    - `e2fsprogs` in `PATH` (e.g. `mkfs.ext4`)
    - Probably some other things we didn't notice we had. Sorry!

## How-to
Clone this repository:

```
$ git clone https://github.com/cceckman/popular-block-storage.git
```

Build everything:

```
$ ./do all
```

To run the Minecraft server and plugins:

```
$ ./run_mc.sh
```

The Minecraft server will run on port 4601; you can connect your Minecraft client to it right away!

The block storage server will run on port 4602.

With that running, connect the server to your filesystem:

```
$ ./run_blocks.sh my-directory
```

That will create a `blocks` file in a temporary directory, put an `ext4` filesystem on it, and mount it at `my-directory`.
