# SI4432 Pinout

![SI4432 Pinout](si44322.jpg)

| Number | Pins |Description |
|--------|------|------------|
|1       |GND   |   N/A      |
|2       |GPIO0 |Internal has connected the receiving control module feet|
|3       |GPIO1 |Internal has connected the receiving control module feet|
|4       |GPIO2 |Connect the the chip GPIO2 pins directly|
|5       |VCC   |Connect power positive 3.3 V|
|6       |SDO   |0 ~ VDDV digital output, provide the internal control of the register serial back to read function|
|7       |SDI   |Serial data input. 0 ~ VDD V digital input. The pins for the 4 wire serial data serial data flow bus.|
|8       |SCLK  |A serial clock input. 0 ~ VDDV digital input. The pins provides the 4 wire serial data clock function|
|9       |nSEL  |Serial interface choice input pins. 0 ~ VDDV digital input. The pins for the 4 wire serial data bus provides the choice/make can function, this signal is used to indicate a read/write mode|
|10      |nIRQ  |Interrupt output pins|
|11      |SDN   |Close input pins. 0 ~ VDDV digital input. In addition to shut down all the mode mode SDN = 0. When SDN = 1 chip will be completely shut down and the content of the register will be lost.|
|12      |GND   |N/A|
|13      |ANT   |Pick up the coaxial antenna 50|
|14      |GND   |N/A|
