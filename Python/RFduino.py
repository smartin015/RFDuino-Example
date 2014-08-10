#!/usr/bin/env python
# RFDuinoEcho.py Paul Lutz
#
# The RFduino object

import os
import signal
import subprocess
import commands
import re
import time

class RFduino:
  def __init__(self, dongle_id, name):
    self.mac = None
    self.dongle_id = dongle_id
    self.name = name

  def get_mac_address(self):
    """ Gets the resolved MAC address of the RFduino. Must 
        have called find() and found a device beforehand.
    """
    return self.mac

  def reset_service(self):
    """ Resets the LE service - useful when dongle is stuck in search mode
    """
    result = subprocess.call("hciconfig " + self.dongle_id +" reset", shell=True)
    if result != 0:
      raise Exception("Problem resetting service")
    print "Reset service"

  def find(self):
    """ Finds the MAC address of the RFDuino by checking 
        hcitool output for the name of the RFDuino
    """
    self.reset_service()


    """timeout -s INT 0.5 lets us run a process for 0.5 seconds,
       using the hcitool command,
       -i determines which bluetooth device to scan with,
       and lescan is the hcitool command that actually scans for devices
    """
    p = subprocess.Popen(
      'timeout -s INT 0.5 hcitool -i %s lescan' % self.dongle_id, 
      bufsize = 0,
      shell = True, 
      stdout = subprocess.PIPE,
      stderr = subprocess.STDOUT
    )
    (out, err) = p.communicate()
    m = re.search('(.*?) ' + self.name, out) 
    if m:
      self.mac = m.group(1)
      return True
    return False

  def send(self, msg):
    """ Sends a character string to the RFduino. """
    if not self.mac:
      raise Exception("Unresolved MAC Address - did you find()?")
    
    
    """ for c in msg accesses each character in the string msg as c, one by one,
        ord() takes the unicode of the character, 
        hex() turns it into a hex,
        [2:] removes the first two characters "0x", 
        zfill() ensures that the resulting string is two characters(padding with 0's if only one).
    """
    msg_hex = "".join([hex(ord(c))[2:].zfill(2) for c in msg])
    
    """ The gatttool command establishes a connection with a ble device
        -b is the device that gatt will conenct to
        -i is the id of the bluetooth dongle
        -t random because the rfduino uses a random address, it changes periodically
        --char-write tells gatt that we want to write a char
        --handle=0x0011 specifies where on the device to write(handle is kind of like a port, and 0x0011 is the port id)
        --value is the imput string that we formatted into hex
    """
    cmd = "gatttool -b %s -i %s -t random --char-write --handle=0x0011 --value=%s" % (self.mac, self.dongle_id, msg_hex)
    p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
    (out, error) = p.communicate()

  def read(self):
    if not self.mac:
      raise Exception("Unresolved MAC Address - did you find()?")

    
    """ The gatttool command establishes a connection with a ble device
        -b is the device that gatt will conenct to
        -i is the id of the bluetooth dongle
        -t random because the rfduino uses a random address, it changes periodically
        --char-read tells gatt that we want to read a char
        --handle=0x000e specifies where on the device to read(handle is kind of like a port, and 0x000e is the port id)
    """
    cmd = "gatttool -b %s -i %s -t random --char-read --handle=0x000e" % (self.mac, self.dongle_id)
    p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
    (out, error) = p.communicate()
    out = out.replace(" ", "").strip().split(":")[1]
    return out.decode("hex")
    
