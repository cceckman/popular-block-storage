
# Build image with plugins.

exec 1>&2

redo-ifchange \
    ../spigot-plugin/plugin.jar \
    spigot-outputs \
    base.Dockerfile \
    run.Dockerfile

cp ../spigot-plugin/plugin.jar outputs/plugin.jar

docker build \
    -f base.Dockerfile \
    -t spigot-base:latest \
    .

docker build \
    -f run.Dockerfile \
    -t spigot-runner:latest \
    .

docker images ls -q spigot-runner:latest
