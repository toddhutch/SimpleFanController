SimpleFanController

This project is a Java application for controlling a Bluetooth-enabled fan using a Raspberry Pi.

Requirements

- Raspberry Pi OS (Tested on Raspberry Pi 3B+ OS Raspbian GNU/Linux 11 (bullseye))
- Java 8
- TinyB library

Installation

Install Java 8:
sudo apt-get update
sudo apt-get install openjdk-8-jdk

Install TinyB Library:
1. Install dependencies:
   sudo apt-get install libboost-all-dev libglib2.0-dev libdbus-1-dev

2. Download and build TinyB:
   git clone https://github.com/intel-iot-devkit/tinyb.git
   cd tinyb
   mkdir build
   cd build
   cmake ..
   make

3. Install TinyB:
   sudo make install
   sudo cp /usr/local/lib/arm-linux-gnueabihf/libtinyb.so /usr/lib/arm-linux-gnueabihf/

Clone the Repository:
git clone https://github.com/yourusername/SimpleFanController.git
cd SimpleFanController

Compile the Java Program:
javac -cp .:/usr/local/lib/lib/java/tinyb.jar SimpleFanController.java

Run the Java Program:
Turn the Fan On:
sudo java -Djava.library.path=/usr/local/lib/arm-linux-gnueabihf -cp .:/usr/local/lib/lib/java/tinyb.jar SimpleFanController 78:04:73:19:77:BC on

Turn the Fan Off:
sudo java -Djava.library.path=/usr/local/lib/arm-linux-gnueabihf -cp .:/usr/local/lib/lib/java/tinyb.jar SimpleFanController 78:04:73:19:77:BC off

Set Fan Speed:
sudo java -Djava.library.path=/usr/local/lib/arm-linux-gnueabihf -cp .:/usr/local/lib/lib/java/tinyb.jar SimpleFanController 78:04:73:19:77:BC speed 10

Change Fan Direction to Forward:
sudo java -Djava.library.path=/usr/local/lib/arm-linux-gnueabihf -cp .:/usr/local/lib/lib/java/tinyb.jar SimpleFanController 78:04:73:19:77:BC direction forward

Change Fan Direction to Reverse:
sudo java -Djava.library.path=/usr/local/lib/arm-linux-gnueabihf -cp .:/usr/local/lib/lib/java/tinyb.jar SimpleFanController 78:04:73:19:77:BC direction reverse

License

This project is licensed under the MIT License - see the LICENSE.md file for details.
