
find . \
    -name '*.java' \
        -or \
    -name '*.yml' \
        -or \
    -name pom.xml \
| xargs redo-ifchange

redo-ifchange ../spigot/spigot-outputs

exec 1>&2

mvn install -f popular-block-storage/pom.xml

cp popular-block-storage/target/popular-block-storage-*.jar "$3"