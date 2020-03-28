FROM spigot-base:latest

RUN curl -o BuildTools.jar \
    https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar

COPY build.sh build.sh
CMD ./build.sh