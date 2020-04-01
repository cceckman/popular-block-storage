
# Dofile for Spigot API & binary

exec 1>&2

redo-ifchange \
    build.sh \
    build.Dockerfile

./build.sh

# Create a dummy output file to force a flush of the redo cache
sha256sum \
    output/spigot.jar \
    output/spigot-api-shaded.jar \
    >$3