redo-always
exec 1>&2

docker container prune -f
docker images prune -a

rm -rf spigot/output spigot/spigot-outputs spigot/plugged-in.img
sudo rm -rf world

rm -f blockfiles.pid blockfiles.log blockfiles/blockfiles
rm -rf blocks

rm -rf spigot-plugin/plugin.jar
cd spigot-plugin/popular-block-storage && mvn clean && cd ..

