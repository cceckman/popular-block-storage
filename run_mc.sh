#!/bin/sh

# Run spigot in a container.

set -ex

echo $PATH

if ! test -d $PWD/world
then
    mkdir $PWD/world
    chmod 0777 "$PWD/world"
fi

if ! test -f spigot/plugged-in.img
then
  echo >&2 "It doesn't look like the server and plugin aren't built!"
  echo >&2 "Try running './do.sh all' first!"
  exit 1
fi

docker run -it \
    --mount type=bind,source=$PWD/world,destination=/home/spigot/world \
    --publish 4601:4601 \
    --publish 4602:4602 \
    spigot-runner:latest
