#!/usr/bin/env python3

import socket

conn = socket.create_connection(("localhost", 4602))

read_size = 20
read_start = 4

command = bytearray(1 + 4 + 4)

command[0] = 0                                             # read
command[1:5] = read_start.to_bytes(4, byteorder='little')  # at byte 4
command[5:9] = read_size.to_bytes(4, byteorder='little')  # twenty bytes

conn.send(command)

resp = conn.recv(len(command) + read_size)
print("Raw bytes: {}".format(resp))

conn.close()
