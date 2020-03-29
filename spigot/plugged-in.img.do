
# Build image with plugins.

exec 1>&2

redo-ifchange \
    ../spigot-plugin/plugin.jar \
    spigot-outputs \
    base.Dockerfile \
    plugged-in.Dockerfile \
    server.properties \
    bukkit.yml

# Copy the plugin to this directory, so it's available to Docker.
cp ../spigot-plugin/plugin.jar output/plugin.jar

docker build \
    -f base.Dockerfile \
    -t spigot-base:latest \
    .

docker build \
    -f plugged-in.Dockerfile \
    -t spigot-runner:latest \
    .

docker image ls -q spigot-runner:latest >$3
