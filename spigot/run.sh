#!/bin/sh

# Run spigot in a container.

set -ex

docker build \
    -f base.Dockerfile \
    -t spigot-base:latest \
    .
docker build \
    -f run.Dockerfile \
    -t spigot-runner:latest \
    .




java -Xms1G -Xmx1G -XX:+UseConcMarkSweepGC -jar spigot.jar

.