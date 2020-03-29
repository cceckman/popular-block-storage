#!/bin/sh

# Run spigot in a container.

set -ex

if ! test -d $PWD/world
then
    mkdir $PWD/world
    chmod 0777 "$PWD/world"
fi

redo-ifchange spigot/plugged-in.img

docker run -it \
    --mount type=bind,source=$PWD/world,destination=/home/spigot/world \
    --publish 4601:4601 \
    --publish 4602:4602 \
    spigot-runner:latest