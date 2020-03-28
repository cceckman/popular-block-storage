
# Dofile for Spigot API & binary

exec 1>&2

redo-ifchange \
    build.sh \
    build.Dockerfile

./build.sh

cp output/spigot-1.15.2.jar output/spigot.jar
cp output/Spigot/Spigot-API/target/spigot-api-*-shaded.jar output/spigot-api-shaded.jar

sha256sum \
    output/spigot.jar \
    output/spigot-api-shaded.jar \
| redo-stamp