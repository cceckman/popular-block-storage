FROM spigot-base:latest

COPY  --chown=spigot:spigot \
    output/spigot.jar spigot.jar
COPY  --chown=spigot:spigot \
    server.properties server.properties
COPY  --chown=spigot:spigot \
    bukkit.yml bukkit.yml
COPY --chown=spigot:spigot \
    output/plugin.jar plugins/popular-block-storage.jar

# NOTE: by running this server, you're agreeing to the EULA!
# What EULA? I'm not sure.
RUN echo "eula=true" >eula.txt

# Minecraft port
EXPOSE 4601
# MinecraftFS backend port
EXPOSE 4602

CMD java \
    -Xms1G \
    -Xmx1G \
    -XX:+UseConcMarkSweepGC \
    -jar ./spigot.jar

# TODO(cceckman): Add mods