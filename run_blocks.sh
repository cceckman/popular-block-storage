#!/bin/sh

# Mount the block server as an ext4 filesystem at $1.

set -ex

if test -z "$1"
then
    echo >&2 "Please provide a directory to mount to!"
    exit 3
fi

if ! test -e "$1"
then
    mkdir -p "$1"
fi

if ! test -z "$(ls -A $1)"
then
    echo >&2 "Directory $1 appears to be nonempty. Cowardly refusing to mount over it."
    exit 1
fi

MKFS="${MKFS:-mkfs.ext4}"
if ! which "$MKFS"
then
    # Attempt some other PATHs...
    MKFS="$(find /usr/bin /usr/sbin /sbin /bin -name $MKFS)"
fi
if ! which "$MKFS"
then
    echo >&2 "Could not find mkfs $MKFS in path."
    echo >&2 "Do you need to modify your \$PATH?"
    echo >&2 "Or, explicitly set \$MKFS to the mkfs binary you want."
    exit 4
fi

TD="$(mktemp -d)"

redo-ifchange blockfiles/blockfiles

sudo -n true || {
    echo >&2 "We need sudo credentials to mount the device. Sorry!"
    exit 2
}

sudo -n \
    blockfiles/blockfiles "$TD" localhost:4602 \
    > blockfiles.log &
echo "$!" >blockfiles.pid
# Wait a moment for it to start up and connect.
sleep 1

# Set up a filesystem.
"$MKFS" "$TD"/blocks
BLOCKDEV="$(sudo losetup --find --show "$TD"/blocks)"
sudo mount "$BLOCKDEV" "$1"

echo >&2 "Mounted $BLOCKDEV at $1. Press enter to clean up."

read INPUT

sudo umount "$1"
sudo kill "$(cat blockfiles.pid)"