package com.zelgius.driver.si4432

import kotlinx.coroutines.*
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.roundToInt


/**
 *
 * Code translation from https://github.com/ADiea/si4432
 */
abstract class SI4432Driver {

    companion object {
        const val MAX_TRANSMIT_TIMEOUT = 1000L

        //values here are kept in khz x 10 format (for not to deal with decimals) - look at AN440 page 26 for whole table
        val IF_FILTER_TABLE = arrayOf(
            322 to 0x26, 3355 to 0x88, 3618 to 0x89, 4202 to 0x8A, 4684 to 0x8B,
            5188 to 0x8C, 5770 to 0x8D, 6207 to 0x8E
        )
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            turnOff()
            chipSelect(false)
        }
    }

    protected abstract fun chipSelect(select: Boolean)
    protected abstract fun readIRQ(): Boolean
    protected abstract fun writeSdn(value: Boolean)
    protected abstract fun spiWrite(value: Byte)
    protected abstract fun spiReadByte(): Byte

    private fun changeRegister(register: Register, value: Byte) {
        writeBurst(register, byteArrayOf(value))
    }


    private fun readRegister(register: Register): Byte = readBurst(register, 1).first()
    private fun writeBurst(register: Register, data: ByteArray) {
        val regValue = register.number or 0x80
        chipSelect(true)

        spiWrite(regValue.toByte())
        data.forEachIndexed { i, v ->

            println("Writing: ${(if (regValue != 0xFF) regValue + i and 0x7F else 0x7F).toHex()} | ${v.toHex()}")
            spiWrite(v)
        }

        chipSelect(false)
    }

    private fun readBurst(register: Register, length: Int): ByteArray {
        val regValue = register.number or 0x7F

        chipSelect(true)
        spiWrite(regValue.toByte())

        val array = ByteArray(length)
        for(i in 0 until length) {
            array[i] = spiReadByte()
            println("Reading: ${(if(regValue != 0x7F) (regValue + 1) and 0x7F else 0x7F).toHex()} | ${array[i].toHex()}")
        }

        return array
    }


    private var frequencyCarrier = 433
    private var signature: Int = 0
    private var kbps: Int = 100
    private var channel: Int = 0

    enum class Register(val number: Int) {
        REG_DEV_TYPE(0x00),
        REG_DEV_VERSION(0x01),
        REG_DEV_STATUS(0x02),

        REG_INT_STATUS1(0x03),
        REG_INT_STATUS2(0x04),
        REG_INT_ENABLE1(0x05),
        REG_INT_ENABLE2(0x06),
        REG_STATE(0x07),
        REG_OPERATION_CONTROL(0x08),

        REG_GPIO0_CONF(0x0B),
        REG_GPIO1_CONF(0x0C),
        REG_GPIO2_CONF(0x0D),
        REG_IOPORT_CONF(0x0E),

        REG_IF_FILTER_BW(0x1C),
        REG_AFC_LOOP_GEARSHIFT_OVERRIDE(0x1D),
        REG_AFC_TIMING_CONTROL(0x1E),
        REG_CLOCK_RECOVERY_GEARSHIFT(0x1F),
        REG_CLOCK_RECOVERY_OVERSAMPLING(0x20),
        REG_CLOCK_RECOVERY_OFFSET2(0x21),
        REG_CLOCK_RECOVERY_OFFSET1(0x22),
        REG_CLOCK_RECOVERY_OFFSET0(0x23),
        REG_CLOCK_RECOVERY_TIMING_GAIN1(0x24),
        REG_CLOCK_RECOVERY_TIMING_GAIN0(0x25),
        REG_RSSI(0x26),
        REG_RSSI_THRESHOLD(0x27),

        REG_AFC_LIMITER(0x2A),
        REG_AFC_CORRECTION_READ(0x2B),

        REG_DATAACCESS_CONTROL(0x30),
        REG_EZMAC_STATUS(0x31),
        REG_HEADER_CONTROL1(0x32),
        REG_HEADER_CONTROL2(0x33),
        REG_PREAMBLE_LENGTH(0x34),
        REG_PREAMBLE_DETECTION(0x35),
        REG_SYNC_WORD3(0x36),
        REG_SYNC_WORD2(0x37),
        REG_SYNC_WORD1(0x38),
        REG_SYNC_WORD0(0x39),
        REG_TRANSMIT_HEADER3(0x3A),
        REG_TRANSMIT_HEADER2(0x3B),
        REG_TRANSMIT_HEADER1(0x3C),
        REG_TRANSMIT_HEADER0(0x3D),

        REG_PKG_LEN(0x3E),

        REG_CHECK_HEADER3(0x3F),
        REG_CHECK_HEADER2(0x40),
        REG_CHECK_HEADER1(0x41),
        REG_CHECK_HEADER0(0x42),

        REG_RECEIVED_HEADER3(0x47),
        REG_RECEIVED_HEADER2(0x48),
        REG_RECEIVED_HEADER1(0x49),
        REG_RECEIVED_HEADER0(0x4A),

        REG_RECEIVED_LENGTH(0x4B),

        REG_CHARGEPUMP_OVERRIDE(0x58),
        REG_DIVIDER_CURRENT_TRIM(0x59),
        REG_VCO_CURRENT_TRIM(0x5A),

        REG_AGC_OVERRIDE(0x69),

        REG_TX_POWER(0x6D),
        REG_TX_DATARATE1(0x6E),
        REG_TX_DATARATE0(0x6F),

        REG_MODULATION_MODE1(0x70),
        REG_MODULATION_MODE2(0x71),
        REG_FREQ_DEVIATION(0x72),
        REG_FREQ_OFFSET1(0x73),
        REG_FREQ_OFFSET2(0x74),
        REG_FREQBAND(0x75),
        REG_FREQCARRIER_H(0x76),
        REG_FREQCARRIER_L(0x77),

        REG_FREQCHANNEL(0x79),
        REG_CHANNEL_STEPSIZE(0x7A),

        REG_FIFO(0x7F)

    }

    enum class AntennaMode(val value: Int) {
        READY(0x01),
        TUNE_MODE(0x02),
        RX_MODE(0x04),
        TX_MODE(0x08)
    }


    fun setFrequency(f: Int) {
        if (f < 240 || f > 930) return

        this.frequencyCarrier = f
        val highBand = if (frequencyCarrier >= 480)
            0x01
        else
            0x0

        val fPart = f / (10.0 * (highBand + 1)) - 24
        val fBand = fPart.toInt()
        val fCarrier = ((fPart - fBand) * 64000).toInt()
        val bytes = byteArrayOf(
            (0x40 or (highBand shl 5) or (fBand and 0x3F)).toByte(),
            (fCarrier shr 8).toByte(),
            (fCarrier and 0xFF).toByte()
        )

        writeBurst(Register.REG_FREQBAND, bytes)
    }

    fun setCommsSignature(signature: Int) {
        this.signature = signature
        changeRegister(Register.REG_TRANSMIT_HEADER3, (signature shr 8).toByte()) // header (signature) byte 3 val

        changeRegister(Register.REG_TRANSMIT_HEADER2, (signature and 0xFF).toByte()) // header (signature) byte 2 val


        changeRegister(
            Register.REG_CHECK_HEADER3,
            (signature shr 8).toByte()
        ) // header (signature) byte 3 val for receive checks

        changeRegister(
            Register.REG_CHECK_HEADER2,
            (signature and 0xFF).toByte()
        ) // header (signature) byte 2 val for receive checks

    }

    private fun boot() {
        /*
	 byte currentFix[] = { 0x80, 0x40, 0x7F }
	 BurstWrite(REG_CHARGEPUMP_OVERRIDE, currentFix, 3) // refer to AN440 for reasons
	 changeRegister(REG_GPIO0_CONF, 0x0F) // tx/rx data clk pin
	 changeRegister(REG_GPIO1_CONF, 0x00) // POR inverted pin
	 changeRegister(REG_GPIO2_CONF, 0x1C) // clear channel pin
	 */
        changeRegister(Register.REG_AFC_TIMING_CONTROL, 0x02) // refer to AN440 for reasons
        changeRegister(Register.REG_AFC_LIMITER, 0xFF.toByte()) // write max value - excel file did that.
        changeRegister(Register.REG_AGC_OVERRIDE, 0x60) // max gain control
        changeRegister(Register.REG_AFC_LOOP_GEARSHIFT_OVERRIDE, 0x3C) // turn off AFC
        changeRegister(
            Register.REG_DATAACCESS_CONTROL,
            0xAD.toByte()
        ) // enable rx packet handling, enable tx packet handling, enable CRC, use CRC-IBM
        changeRegister(
            Register.REG_HEADER_CONTROL1,
            0x0C
        ) // no broadcast address control, enable check headers for bytes 3 & 2
        changeRegister(
            Register.REG_HEADER_CONTROL2,
            0x22
        )  // enable headers byte 3 & 2, no fixed package length, sync word 3 & 2
        changeRegister(Register.REG_PREAMBLE_LENGTH, 0x08) // 8 * 4 bits = 32 bits (4 bytes) preamble length
        changeRegister(Register.REG_PREAMBLE_DETECTION, 0x3A) // validate 7 * 4 bits of preamble  in a package
        changeRegister(Register.REG_SYNC_WORD3, 0x2D) // sync byte 3 val
        changeRegister(Register.REG_SYNC_WORD2, 0xD4.toByte()) // sync byte 2 val

        changeRegister(Register.REG_TX_POWER, 0x1F) // max power

        changeRegister(Register.REG_CHANNEL_STEPSIZE, 0x64) // each channel is of 1 Mhz interval

        setFrequency(frequencyCarrier) // default freq
        setBaudRate(kbps) // default baud rate is 100kpbs
        setChannel(channel.toByte()) // default channel is 0
        setCommsSignature(signature) // default signature

        switchMode(AntennaMode.READY)
    }

    suspend fun sendPacket(data: ByteArray, ackTimeout: Long? = null): ByteArray? =
        withContext(Dispatchers.IO) {
            clearTxFIFO()
            changeRegister(Register.REG_PKG_LEN, data.size.toByte())

            writeBurst(Register.REG_FIFO, data)

            changeRegister(Register.REG_INT_ENABLE1, 0x04) // set interrupts on for package sent
            changeRegister(Register.REG_INT_ENABLE2, 0x00) // set interrupts off for anything else
            //read interrupt registers to clean them
            readRegister(Register.REG_INT_STATUS1)
            readRegister(Register.REG_INT_STATUS2)

            switchMode(AntennaMode.TX_MODE, AntennaMode.READY)

            val enterMillis = Instant.now().toEpochMilli()

            while (Instant.now().toEpochMilli() - enterMillis < MAX_TRANSMIT_TIMEOUT) {

                if (readIRQ()) { // TODO possible error here
                    continue
                }

                val intStatus = readRegister(Register.REG_INT_STATUS1).toInt()
                readRegister(Register.REG_INT_STATUS2)

                if (intStatus and 0x04 > 0) {
                    switchMode(AntennaMode.READY, AntennaMode.TUNE_MODE)
                    println("Package sent! -- ${intStatus.toHex()}")

                    // package sent. now, return true if not to wait ack, or wait ack (wait for packet only for 'remaining' amount of time)
                    if (ackTimeout != null) {
                        if (waitForPacket(ackTimeout)) {
                            return@withContext getPacketReceived()
                        } else {
                            return@withContext null
                        }
                    } else {
                        return@withContext byteArrayOf()
                    }
                }

                delay(1)
            }

            //timeout occurred.
            println("Timeout in Transit -- ")
            switchMode(AntennaMode.READY)

            if (readRegister(Register.REG_DEV_STATUS).toInt() and 0x80 > 0) {
                clearFIFO()
            }

            null
        }

    fun setChannel(channel: Byte) {
        changeRegister(Register.REG_FREQCHANNEL, channel)
    }

    suspend fun waitForPacket(timeout: Long): Boolean =
        withContext(Dispatchers.IO) {

            startListening()

            val enterMillis = Instant.now().toEpochMilli()
            while (Instant.now().toEpochMilli() - enterMillis < timeout) {
                return@withContext if (!isPacketReceived()) {
                    continue
                } else {
                    true
                }
            }
            //timeout occurred.

            //timeout occurred.
            println("Timeout in receive-- ")

            switchMode(AntennaMode.READY)
            clearRxFIFO()

            false
        }

    fun getPacketReceived(): ByteArray {
        val length = readRegister(Register.REG_RECEIVED_LENGTH)
        val array = readBurst(Register.REG_FIFO, length.toInt())
        clearRxFIFO()
        return array
    }

    fun clearTxFIFO() {
        changeRegister(Register.REG_OPERATION_CONTROL, 0x01)
        changeRegister(Register.REG_OPERATION_CONTROL, 0x00)
    }

    fun clearRxFIFO() {
        changeRegister(Register.REG_OPERATION_CONTROL, 0x02)
        changeRegister(Register.REG_OPERATION_CONTROL, 0x00)
    }

    fun clearFIFO() {
        changeRegister(Register.REG_OPERATION_CONTROL, 0x03)
        changeRegister(Register.REG_OPERATION_CONTROL, 0x00)
    }

    suspend fun softwareReset() =
        withContext(Dispatchers.IO) {
            changeRegister(Register.REG_STATE, 0x80.toByte())

            var reg = readRegister(Register.REG_INT_STATUS2).toInt()
            while (reg and 0x02 != 0x02) {
                delay(1)
                reg = readRegister(Register.REG_INT_STATUS2).toInt()
            }

            boot()
        }

    suspend fun hardwareReset() =
        withContext(Dispatchers.IO) {

            turnOff()
            turnOn()

            var reg = readRegister(Register.REG_INT_STATUS2).toInt()
            while (reg and 0x02 != 0x02) {
                println("POR: ${reg.toHex()}")
                delay(1)
                reg = readRegister(Register.REG_INT_STATUS2).toInt()
            }

            boot()
        }

    fun startListening() {
        clearRxFIFO() // clear first, so it doesn't overflow if packet is big

        changeRegister(Register.REG_INT_ENABLE1, 0x03) // set interrupts on for package received and CRC error

        //#ifdef DEBUG
        changeRegister(Register.REG_INT_ENABLE2, 0xC0.toByte())
        //#else
        //changeRegister(Register.REG_INT_ENABLE2, 0x00) // set other interrupts off
        //#endif
        //read interrupt registers to clean them
        readRegister(Register.REG_INT_STATUS1)
        readRegister(Register.REG_INT_STATUS2)

        switchMode(AntennaMode.RX_MODE, AntennaMode.READY)
    }

    private fun switchMode(vararg mode: AntennaMode) {
        var byte = 0x00
        mode.forEach {
            byte = byte or it.value
        }

        changeRegister(Register.REG_STATE, byte.toByte()) // receive mode
        //delay(20)
        byte = readRegister(Register.REG_DEV_STATUS).toInt()
        if (byte == 0 || byte == 0xFF) {
            println("${byte.toHex()}  -- WHAT THE HELL!!")
        }
    }

    fun isPacketReceived(): Boolean {
        if (readIRQ()) { // TODO possible error here
            return false // if no interrupt occurred, no packet received is assumed (since startListening will be called prior, this assumption is enough)
        }
        // check for package received status interrupt register
        val intStatus = readRegister(Register.REG_INT_STATUS1).toInt()

        val intStatus2 = readRegister(Register.REG_INT_STATUS2).toInt()

        if ((intStatus2 and 0x40) > 0) { //interrupt occurred, check it && read the Interrupt Status1 register for 'preamble '
            println("HEY!! HEY!! Valid Preamble detected -- ${intStatus2.toHex()}")
        }

        if (intStatus2 and 0x80 > 0) { //interrupt occurred, check it && read the Interrupt Status1 register for 'preamble '

            println("HEY!! HEY!! SYNC WORD detected -- ${intStatus2.toHex()}")
        }

        if (intStatus and 0x02 > 0) { //interrupt occurred, check it && read the Interrupt Status1 register for 'valid packet'
            switchMode(
                AntennaMode.READY,
                AntennaMode.TUNE_MODE
            ) // if packet came, get out of Rx mode till the packet is read out. Keep PLL on for fast reaction

            println("Packet detected -- ${intStatus.toHex()}")
            return true
        } else if (intStatus and 0x01 > 0) { // packet crc error
            switchMode(AntennaMode.READY) // get out of Rx mode till buffers are cleared

            println("CRC Error in Packet detected!-- ${intStatus2.toHex()}")

            clearRxFIFO()
            switchMode(AntennaMode.RX_MODE, AntennaMode.READY) // get back to work
            return false
        }

        //no relevant interrupt? no packet!

        return false
    }

    fun setBaudRate(kbps: Int) {
        this.kbps = kbps

        // chip normally supports very low bps values, but they are cumbersome to implement - so I just didn't implement lower bps values
        if ((kbps > 256) || (kbps < 1))
            return
        this.kbps = kbps

        val freqDev = if (kbps <= 10) 15 else 150        // 15khz / 150 khz
        val modulationValue = if (this.kbps < 30) 0x4c else 0x0c        // use FIFO Mode, GFSK, low baud mode on / off

        val modulationValues = byteArrayOf(
            modulationValue.toByte(),
            0x23,
            ((freqDev * 1000.0f) / 625.0f).roundToInt().toByte()
        )// msb of the kpbs to 3rd bit of register
        writeBurst(Register.REG_MODULATION_MODE1, modulationValues)

        // set data rate
        val bpsRegVal = (kbps * if (kbps < 30) 2097152.0f else 65536.0f / 1000.0f).roundToInt()
        val dataRateValues = byteArrayOf((bpsRegVal shr 8).toByte(), (bpsRegVal and 0xFF).toByte())

        writeBurst(Register.REG_TX_DATARATE1, dataRateValues)

        //now set the timings
        val minBandwidth = (2 * freqDev) + kbps
        println("min Bandwidth value: ${minBandwidth.toHex()}")

        //since the table is ordered (from low to high), just find the 'minimum bandwidth which is greater than required'
        val ifValue = IF_FILTER_TABLE.find {
            it.first >= (minBandwidth * 10)
        }?.second ?: 0xFF

        println("Selected IF value: ${ifValue.toHex()}")

        changeRegister(Register.REG_IF_FILTER_BW, ifValue.toByte())

        val dwn3Bypass = if (ifValue and 0x80 > 0) 1 else 0 // if msb is set
        val ndecExp = (ifValue shr 4) and 0x07 // only 3 bits

        val rxOversampling = ((500.0 * (1 + 2 * dwn3Bypass)) / ((2.0.pow(ndecExp - 3)) * kbps)).roundToInt()

        val ncOffset = ceil((kbps * (2.0.pow(ndecExp + 20))) / (500.0 * (1 + 2 * dwn3Bypass))).toInt()

        val crGain = (2 + (65535 * kbps) / (rxOversampling * freqDev)).let {
            if (it > 0x7FF) 0x7FF
            else it
        }
        val crMultiplier = 0x00
        println("dwn3_bypass value: ${dwn3Bypass.toHex()}")
        println("ndec_exp value: ${ndecExp.toHex()}")
        println("rxOversampling value: ${rxOversampling.toHex()}")
        println("ncOffset value: ${ncOffset.toHex()}")
        println("crGain value: ${crGain.toHex()}")
        println("crMultiplier value: ${crMultiplier.toHex()}")


        val timingValues = byteArrayOf(
            (rxOversampling and 0x00FF).toByte(),
            (((rxOversampling and 0x0700) shr 3) or ((ncOffset shr 16) and 0x0F)).toByte(),
            ((ncOffset shr 8) and 0xFF).toByte(),
            (ncOffset and 0xFF).toByte(),
            (((crGain and 0x0700) shr 8) or crMultiplier).toByte(),
            (crGain and 0xFF).toByte()
        )

        writeBurst(Register.REG_CLOCK_RECOVERY_OVERSAMPLING, timingValues)
    }

    suspend fun turnOn() =
        withContext(Dispatchers.IO) {
            writeSdn(false)
            delay(20)
        }

    suspend fun turnOff() =
        withContext(Dispatchers.IO) {
            writeSdn(true)
            delay(1)
        }
}


fun Int.toHex() = String.format("%X", this)
fun Byte.toHex() = String.format("%X", this)
