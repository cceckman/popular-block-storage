FROM debian:buster-slim

# Shared Spigot base; build and run.

# Highly-cacheable commands first:

ENV DEBIAN_FRONTEND=noninteractive
RUN groupadd -r spigot && useradd -r -g spigot spigot

# Workaround: https://github.com/puckel/docker-airflow/issues/182
RUN mkdir -p /usr/share/man/man1 

RUN apt-get update && \
    apt-get install -y \
        git \
        openjdk-11-jre-headless \
        openjdk-11-jdk-headless \
        curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /home/spigot
RUN chown spigot:spigot .
USER spigot