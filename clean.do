redo-always
exec 1>&2

docker container prune -f
docker images prune -a

rm -rf spigot/output
rm -rf spigot/world

rm -rf plugin/plugin.jar
{ cd spigot-plugin/popular-block-storage && mvn clean; }