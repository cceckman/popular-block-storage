# Integrating Popular Block Storage into Linux

Slides [here](https://docs.google.com/presentation/d/1wgD_L70IqBUjulB82w8W-aECEOIKt1o3ZRgdD2w-P1Q/edit?usp=sharing)

Repository for @cceckman and @slongfield's 2020 SIGBOVIK submission: Integrating Popular Block Storage into Linux.


## Howto

### Prerequisites

- (Legitimate) access to [Minecraft Java Edition](https://www.minecraft.net/en-us/store/minecraft-java-edition/)
- A Linux host with:
    - Docker
    - A recent [Go](https://golang.org) toolchain
    - openjdk-11-jre and openjdk-11-jdk
    - Apache Maven
    - github.com/apenwarr/redo (TODO: Replace with do.sh)
    - `e2fsprogs` in `PATH` (e.g. `mkfs.ext4`)
    - Probably some other things we didn't notice we had. Sorry!

## How-to
To build and run the Minecraft server and plugins, clone this Git repository, and run:

```
$ ./run_mc.sh
```

The Minecraft server will run on port 4601; you can connect your Minecraft game to it right away!

The block storage server will run on port 4602.

With that running, connect the server to a block device:

```
$ ./run_blocks.sh my-directory
```

That will
