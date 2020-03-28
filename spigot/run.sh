#!/bin/sh

# Run spigot in a container.

set -ex

if ! test -d $PWD/world
then
    mkdir $PWD/world
    chmod 0777 "$PWD/world"
fi

redo-ifchange plugged-in.img

docker run -it \
    --mount type=bind,source=$PWD/world,destination=/home/spigot/world \
    --publish 4601:4601 \
    spigot-runner:latest
