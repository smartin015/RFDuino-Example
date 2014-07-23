#include <RFduinoBLE.h>

int notificationLED  = 4;

void setup() {
  Serial.begin(9600);
  pinMode(notificationLED , OUTPUT);
  
  RFduinoBLE.advertisementData = "RFduino";
  
  RFduinoBLE.begin();
}

void loop() {
  RFduino_ULPDelay(INFINITE);
}

void RFduinoBLE_onConnect() {
  Serial.println("connected");
}

void RFduinoBLE_onDisconnect() {
  Serial.println("disconnected");
}

void RFduinoBLE_onReceive(char *data, int len) {
  digitalWrite(notificationLED , HIGH);
  for(int i = 0; i <len;i++)
  {
    Serial.print(data[i]);
  }
  Serial.print("\n");
  
  RFduinoBLE.send(data, len);
  
  digitalWrite(notificationLED , LOW);
}
