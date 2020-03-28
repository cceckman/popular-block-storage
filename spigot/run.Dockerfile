FROM spigot-base:latest

COPY  --chown=spigot:spigot \
    output/spigot-*.jar spigot.jar
COPY  --chown=spigot:spigot \
    server.properties server.properties
COPY --chown spigot:spigot \

# NOTE: by running this server, you're agreeing to the EULA!
# What EULA? I'm not sure.
RUN echo "eula=true" >eula.txt

EXPOSE 4601
CMD java \
    -Xms1G \
    -Xmx1G \
    -XX:+UseConcMarkSweepGC \
    -jar ./spigot.jar

# TODO(cceckman): Add mods