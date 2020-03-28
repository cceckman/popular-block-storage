FROM spigot-base:latest

COPY output/spigot-*.jar spigot.jar

CMD java \
    -Xms1G \
    -Xmx1G \
    -XX:+UseConcMarkSweepGC \
    -jar spigot.jar

# TODO(cceckman): Add mods