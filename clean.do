redo-always
exec 1>&2

docker container prune -f
docker images prune -a

rm -rf spigot/output spigot/spigot-outputs
sudo rm -rf world

rm -rf spigot-plugin/plugin.jar
cd spigot-plugin/popular-block-storage && mvn clean && cd ..
rm -f blockfiles.pid
rm -rf blocks
