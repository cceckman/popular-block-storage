#!/bin/sh

set -ex

if test -z "$SPIGOT_OUTPUT"
then
    # Outside container. Build container and run it.
    SPIGOT_OUTPUT="$(pwd)/output"
    rm -rf "$SPIGOT_OUTPUT" # Don't pollute the docker buildenv.

    docker build \
        -f base.Dockerfile \
        -t spigot-base:latest \
        .
    docker build \
        -f build.Dockerfile \
        -t spigot-builder:latest \
        .

    mkdir "$SPIGOT_OUTPUT"
    chmod 0777 "$SPIGOT_OUTPUT"

    docker run \
        -t \
        --mount type=bind,source="$SPIGOT_OUTPUT",target=/output \
        --env "SPIGOT_OUTPUT=/output" \
        spigot-builder:latest

    sudo chown --reference output --recursive output
else
    VERSION="1.15.2"
    # Inside container.
    java -jar BuildTools.jar \
        --rev "$VERSION"
    find . | grep '\.jar'

    cp spigot-1.15.2.jar /output/spigot.jar
    cp Spigot/Spigot-API/target/spigot-api-*-shaded.jar /output/spigot-api-shaded.jar

    # TODO: Filter down to just those files of interest
fi