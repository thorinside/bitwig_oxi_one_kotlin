package com.nosuchdevice

import com.bitwig.extension.controller.api.MidiIn
import com.bitwig.extension.controller.api.MidiOut
import com.nosuchdevice.track.LightStateEnum
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.*

class OxiOneHardware(private val inputPort: MidiIn, private val outputPort: MidiOut) :
    CoroutineScope {

  private val job = Job()

  override val coroutineContext: CoroutineContext
    get() = Dispatchers.IO + job

  private val blinkMap: MutableMap<Int, Job> = mutableMapOf()

  // Create a 128x8 bitmap and draw a recognizable pattern
  fun createBitmap(width: Int, height: Int, lines: List<String>): BufferedImage {
    val image = BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY)
    val graphics = image.createGraphics()

    // Set the background to white
    graphics.color = Color.WHITE
    graphics.fillRect(0, 0, width, height)

    // Set the font to a predefined size
    graphics.font = Font(Font.MONOSPACED, Font.PLAIN, 10)
    val metrics = graphics.fontMetrics

    // Draw each line of text
    var y = metrics.ascent // Start from the top
    lines.forEachIndexed { index, line ->
      if (index == 4) {
        // Draw a black background for this line
        graphics.color = Color.BLACK
        graphics.fillRect(0, y - metrics.ascent, width, metrics.height)

        // Draw the text in white
        graphics.color = Color.WHITE
      } else {
        graphics.color = Color.BLACK
      }

      graphics.drawString(line, 0, y)
      y += metrics.height // Move to the next line
    }

    graphics.dispose()
    return image
  }

  // Convert the bitmap to a hex string with the specified format
  fun bitmapToHexString(image: BufferedImage): String {
    val width = image.width
    val height = image.height
    val hexString = StringBuilder("f0 03 ")

    // Iterate over 8 pages
    for (page in 0 until 8) {
      // For each column in the page
      for (x in 0 until width) {
        var byte = 0
        // For each pixel in the 8-pixel tall column
        for (yOffset in 0 until 8) {
          val y = page * 8 + yOffset
          if (y < height) {
            val pixel = image.getRGB(x, y)
            // Set the bit if the pixel is black (or whatever your "on" color is)
            if (pixel == Color.BLACK.rgb) {
              byte = byte or (1 shl yOffset)
            }
          }
        }

        // Split the byte into two nibbles and append as hex
        val highNibble = (byte shr 4) and 0x0F
        val lowNibble = byte and 0x0F
        hexString.append(String.format("%02x %02x ", highNibble, lowNibble))
      }
    }
    hexString.append("f7")
    return hexString.toString().trim()
  }

  fun testScreen(host: com.bitwig.extension.controller.api.ControllerHost) {
    val width = 128
    val height = 64

    // Create the bitmap with a pattern
    val image =
        createBitmap(
            width,
            height,
            """
012345678901234567890
One Two Three Four
Five Six Seven Eight
Oxi One is so Great!
            """.split(
                "\n"
            )
        )

    // Convert the bitmap to a hex string
    val hexString = bitmapToHexString(image)

    host.println(hexString)

    outputPort.sendSysex(hexString)
    host.println("Sent screen test")
  }

  fun testLights() {

    val maxValue = 255 // Maximum value for each color component in hex

    for (y in 0..15) { // 16 columns
      for (x in 0..7) { // 8 rows
        val colorIndex = y % 3 // Determine the color index within the column
        val r = if (colorIndex == 0) 1 else 0 // Red component
        val g = if (colorIndex == 1) 1 else 0 // Green component
        val b = if (colorIndex == 2) 1 else 0 // Blue component
        val brightness = (x * maxValue / 7f + 1).toInt() // Brightness component
        setLEDColor(x, y, r * brightness, g * brightness, b * brightness)
      }
    }
  }

  fun setLEDColor(x: Int, y: Int, r: Int, g: Int, b: Int) {
    val rHigh = r shr 7
    val rLow = r and 0x7F
    val gHigh = g shr 7
    val gLow = g and 0x7F
    val bHigh = b shr 7
    val bLow = b and 0x7F
    val sysexMessage =
        "f0 01 " +
            String.format("%02x %02x ", x, y) +
            String.format("%02x %02x ", rHigh, rLow) +
            String.format("%02x %02x ", gHigh, gLow) +
            String.format("%02x %02x ", bHigh, bLow) +
            "f7"

    outputPort.sendSysex(sysexMessage)
  }

  fun setLight(id: Int, value: Int) {
    val idHigh = id shr 7
    val idLow = id and 0x7F
    val valueHigh = value shr 7
    val valueLow = value and 0x7F
    val sysexMessage =
        "f0 02 " +
            String.format("%02x %02x ", idHigh, idLow) +
            String.format("%02x %02x ", valueHigh, valueLow) +
            "f7"

    outputPort.sendSysex(sysexMessage)
  }

  fun updateLED(buttonNote: Int, state: LightStateEnum) {
    val y = buttonNote % 16
    val x = buttonNote / 16

    if (state == LightStateEnum.BLINK_GREEN) {
      if (blinkMap[buttonNote] == null) blinkLED(buttonNote)
    } else {
      cancelBlink(buttonNote)
    }

    val color =
        when (state) {
          LightStateEnum.GREEN -> Color.GREEN
          LightStateEnum.RED -> Color.RED
          LightStateEnum.YELLOW -> Color.YELLOW
          else -> Color.BLACK
        }
    setLEDColor(x, y, color.red, color.green, color.blue)
  }

  private fun blinkLED(buttonNote: Int) {
    blinkMap[buttonNote]?.cancel()
    blinkMap[buttonNote] = launch {
      while (isActive) {
        updateLED(buttonNote, LightStateEnum.GREEN)
        delay(150)
        updateLED(buttonNote, LightStateEnum.YELLOW)
        delay(500)
      }
    }
  }

  private fun cancelBlink(buttonNote: Int) {
    blinkMap.remove(buttonNote)?.cancel()
  }

  fun getMidiNoteForGridButton(x: Int, y: Int): Int {
    return x + (y * 16)
  }

  companion object {

    // Oxi Button Mappings
    const val BUTTON_STOP = 21
    const val BUTTON_STOP_LIGHT = 25
    const val BUTTON_RECORD = 29
    const val BUTTON_RECORD_LIGHT = 27
    const val BUTTON_PLAY = 25
    const val BUTTON_PLAY_LIGHT = 28

    const val ENC_1_CLICK = 0x34
    const val ENC_2_CLICK = 0x35
    const val ENC_3_CLICK = 0x36
    const val ENC_4_CLICK = 0x37

    const val ENC_1 = 0x00
    const val ENC_2 = 0x01
    const val ENC_3 = 0x02
    const val ENC_4 = 0x03

    const val SHIFT_BUTTON = 0x3A

    const val LEDS_OFF = 0
    const val LEDS_ON = 1
    const val LEDS_BLINK_ON = 2
    const val LEDS_BLINK_FAST_ON = 4
    const val LEDS_SHORT_ON = 6
  }
}
