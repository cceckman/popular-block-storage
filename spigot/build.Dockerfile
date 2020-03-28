FROM debian:buster-slim

# Build Spigot per https://www.spigotmc.org/wiki/buildtools/

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

RUN curl -o BuildTools.jar \
    https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar

COPY build.sh build.sh
CMD ./build.sh