#!/bin/sh

set -ex

if test -z "$SPIGOT_OUTPUT"
then
    # Outside container. Build container and run it.
    SPIGOT_OUTPUT="$(pwd)/output"
    rm -rf "$SPIGOT_OUTPUT" # Don't pollute the docker buildenv.

    docker build \
        -f build.Dockerfile \
        -t spigot-builder:latest \
        .

    mkdir "$SPIGOT_OUTPUT"
    chmod 0777 "$SPIGOT_OUTPUT"

    docker run \
        -ti \
        --mount type=bind,source="$SPIGOT_OUTPUT",target=/output \
        --env "SPIGOT_OUTPUT=/output" \
        spigot-builder:latest
else
    VERSION="1.15.2"
    # Inside container.
    java -jar BuildTools.jar \
        --rev "$VERSION"
    cp "*.jar" "$SPIGOT_OUTPUT"
fi