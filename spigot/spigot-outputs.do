
# Dofile for Spigot API & binary

exec 1>&2

redo-ifchange \
    build.sh \
    build.Dockerfile

./build.sh

sha256sum \
    output/spigot.jar \
    output/spigot-api-shaded.jar \
| redo-stamp