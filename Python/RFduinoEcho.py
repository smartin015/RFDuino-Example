#!/usr/bin/env python
# RFDuinoEcho.py Paul Lutz
#
# This test script connects to the RFduino, sends it a user-defined string
# and prints the echo response back to the user.

from RFduino import RFduino
import sys

DONGLE_ID = "hci1"
RFDUINO_NAME = "RFduino"
device = RFduino(DONGLE_ID, RFDUINO_NAME)

def main():
  if(device.find()):
    print "RFduino found"
  else:
    print "RFduino not found."
    sys.exit(-1)

  while True:
    toSend = raw_input("String to send to RFduino: ")
    device.send(toSend)
    print "Response from RFduino: " + device.read()
  
if __name__ == "__main__":
  main()
  
