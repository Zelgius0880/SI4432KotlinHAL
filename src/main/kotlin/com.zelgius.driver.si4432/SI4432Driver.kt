package com.zelgius.driver.si4432


abstract class SI4432Driver() {

    abstract fun writeBurst(register: Register, data: ByteArray)
    abstract fun readBurst(register: Register, length: Int): Int
    abstract fun readRegister(register: Register): Byte
    abstract fun turnOn()
    abstract fun turnOff()
    abstract fun hardReset()
    abstract fun softReset()
    abstract fun chipSelect(select: Boolean)


    class Register {
        companion object {
            val REG_DEV_TYPE = 0x00
            val REG_DEV_VERSION = 0x01
            val REG_DEV_STATUS = 0x02

            val REG_INT_STATUS1 = 0x03
            val REG_INT_STATUS2 = 0x04
            val REG_INT_ENABLE1 = 0x05
            val REG_INT_ENABLE2 = 0x06
            val REG_STATE = 0x07
            val REG_OPERATION_CONTROL = 0x08

            val REG_GPIO0_CONF = 0x0B
            val REG_GPIO1_CONF = 0x0C
            val REG_GPIO2_CONF = 0x0D
            val REG_IOPORT_CONF = 0x0E

            val REG_IF_FILTER_BW = 0x1C
            val REG_AFC_LOOP_GEARSHIFT_OVERRIDE = 0x1D
            val REG_AFC_TIMING_CONTROL = 0x1E
            val REG_CLOCK_RECOVERY_GEARSHIFT = 0x1F
            val REG_CLOCK_RECOVERY_OVERSAMPLING = 0x20
            val REG_CLOCK_RECOVERY_OFFSET2 = 0x21
            val REG_CLOCK_RECOVERY_OFFSET1 = 0x22
            val REG_CLOCK_RECOVERY_OFFSET0 = 0x23
            val REG_CLOCK_RECOVERY_TIMING_GAIN1 = 0x24
            val REG_CLOCK_RECOVERY_TIMING_GAIN0 = 0x25
            val REG_RSSI = 0x26
            val REG_RSSI_THRESHOLD = 0x27

            val REG_AFC_LIMITER = 0x2A
            val REG_AFC_CORRECTION_READ = 0x2B

            val REG_DATAACCESS_CONTROL = 0x30
            val REG_EZMAC_STATUS = 0x31
            val REG_HEADER_CONTROL1 = 0x32
            val REG_HEADER_CONTROL2 = 0x33
            val REG_PREAMBLE_LENGTH = 0x34
            val REG_PREAMBLE_DETECTION = 0x35
            val REG_SYNC_WORD3 = 0x36
            val REG_SYNC_WORD2 = 0x37
            val REG_SYNC_WORD1 = 0x38
            val REG_SYNC_WORD0 = 0x39
            val REG_TRANSMIT_HEADER3 = 0x3A
            val REG_TRANSMIT_HEADER2 = 0x3B
            val REG_TRANSMIT_HEADER1 = 0x3C
            val REG_TRANSMIT_HEADER0 = 0x3D

            val REG_PKG_LEN = 0x3E

            val REG_CHECK_HEADER3 = 0x3F
            val REG_CHECK_HEADER2 = 0x40
            val REG_CHECK_HEADER1 = 0x41
            val REG_CHECK_HEADER0 = 0x42

            val REG_RECEIVED_HEADER3 = 0x47
            val REG_RECEIVED_HEADER2 = 0x48
            val REG_RECEIVED_HEADER1 = 0x49
            val REG_RECEIVED_HEADER0 = 0x4A

            val REG_RECEIVED_LENGTH = 0x4B

            val REG_CHARGEPUMP_OVERRIDE = 0x58
            val REG_DIVIDER_CURRENT_TRIM = 0x59
            val REG_VCO_CURRENT_TRIM = 0x5A

            val REG_AGC_OVERRIDE = 0x69

            val REG_TX_POWER = 0x6D
            val REG_TX_DATARATE1 = 0x6E
            val REG_TX_DATARATE0 = 0x6F

            val REG_MODULATION_MODE1 = 0x70
            val REG_MODULATION_MODE2 = 0x71
            val REG_FREQ_DEVIATION = 0x72
            val REG_FREQ_OFFSET1 = 0x73
            val REG_FREQ_OFFSET2 = 0x74
            val REG_FREQBAND = 0x75
            val REG_FREQCARRIER_H = 0x76
            val REG_FREQCARRIER_L = 0x77

            val REG_FREQCHANNEL = 0x79
            val REG_CHANNEL_STEPSIZE = 0x7A

            val REG_FIFO = 0x7F
        }
    }


}
