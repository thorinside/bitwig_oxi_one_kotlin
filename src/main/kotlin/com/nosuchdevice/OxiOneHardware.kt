package com.nosuchdevice

import com.bitwig.extension.api.Color
import com.bitwig.extension.controller.api.HardwareLightVisualState
import com.bitwig.extension.controller.api.InternalHardwareLightState
import com.bitwig.extension.controller.api.MidiIn
import com.bitwig.extension.controller.api.MidiOut
import java.awt.Font
import java.awt.image.BufferedImage
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.*

sealed class MultiColorLightStateEnum(val color: Color) {
  object YELLOW : MultiColorLightStateEnum(Color.fromRGB(1.0, 1.0, 0.0))
  object RED : MultiColorLightStateEnum(Color.fromRGB(1.0, 0.0, 0.0))
  object GREEN : MultiColorLightStateEnum(Color.fromRGB(0.0, 1.0, 0.0))
  data class BLINK_GREEN(val rgb: Color) : MultiColorLightStateEnum(rgb)
  data class CUSTOM(val rgb: Color) : MultiColorLightStateEnum(rgb)
  object OFF : MultiColorLightStateEnum(Color.fromRGB(0.0, 0.0, 0.0))
}

class MultiColorLightState(val state: MultiColorLightStateEnum) : InternalHardwareLightState() {
  override fun getVisualState(): HardwareLightVisualState {
    return when (state) {
      is MultiColorLightStateEnum.YELLOW -> HardwareLightVisualState.createForColor(state.color)
      is MultiColorLightStateEnum.RED -> HardwareLightVisualState.createForColor(state.color)
      is MultiColorLightStateEnum.GREEN -> HardwareLightVisualState.createForColor(state.color)
      is MultiColorLightStateEnum.BLINK_GREEN ->
          HardwareLightVisualState.createBlinking(
              Color.fromRGB(0.0, 1.0, 0.0),
              Color.fromRGB(1.0, 1.0, 0.0),
              1.0,
              1.0
          )
      is MultiColorLightStateEnum.CUSTOM -> HardwareLightVisualState.createForColor(state.color)
      is MultiColorLightStateEnum.OFF -> HardwareLightVisualState.createForColor(Color.blackColor())
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as MultiColorLightState

    return state == other.state
  }

  override fun hashCode(): Int {
    return state.hashCode()
  }
}

enum class OxiFunctionButtons(val value: Int) {
  ARP(0),
  SEQ_4(1),
  LEFT_16(2),
  END_2(3),
  KEYBOARD_PREVIEW(4),
  SEQ_3(5),
  UP_32(6),
  INIT_X2(7),
  ARRANGER(8),
  SEQ_2(9),
  DOWN_48(10),
  SAVE(11),
  BACK(12),
  SEQ_1(13),
  RIGHT_64(14),
  LOAD(15),
  ENCODER1(16),
  SHIFT(17),
  MOD(18),
  COPY_DUPLICATE(19),
  ENCODER2(20),
  STOP(21),
  DIVISION(22),
  PASTE_CLEAR(23),
  ENCODER3(24),
  PLAY(25),
  LFO(26),
  UNDO(27),
  ENCODER4(28),
  REC(29),
  STEP_CHORD(30),
  RANDOM(31),
  MUTE(32),
}

enum class OxiLights(val value: Int) {
  BACK(0),
  CONFIG(1),
  ARRANGER_SHOW(2),
  ARRANGER_STATE(3),
  KEYBOARD(4),
  PREVIEW(5),
  ARP(6),
  ARP_HOLD(7),
  SHIFT(8),
  STOP(9),
  SEQ_1(10),
  REC(11),
  PLAY(12),
  SEQ_3(13),
  NUDGE(14),
  SYNC(15),
  SEQ_1_SEL(16),
  SEQ_2(17),
  SEQ_2_SEL(18),
  MUTE(19),
  LOAD(20),
  SEQ_4(21),
  SEQ_4_SEL(22),
  SEQ_3_SEL(23),
  SAVE(24),
  CLEAR(25),
  DUPLICATE(26),
  PASTE(27),
  COPY(28),
  UNDO(29),
  RANDOM(30),
  REDO(31),
  RANDOM2(32),
  INIT(33),
  X2(34),
  END(35),
  TWO(36),
  MOD(37),
  DIVISION(38),
  FOLLOW(39),
  LFO(40),
  CONDENSE(41),
  CVOUT(42),
  STEP_CHORD(43),
  EXPAND(44),
  SIXTEEN(45),
  LEFT(46),
  THIRTY_TWO(47),
  UP(48),
  FORTY_EIGHT(49),
  RIGHT(50),
  SIXTY_FOUR(51),
  DOWN(52),
  NO_LED(53),
  PLAY2(54),
}

enum class OxiLightStates(val value: Int) {
  OFF(0),
  ON(1),
  BLINK_ON(2),
  BLINK_FAST_ON(4),
  SHORT_ON(6),
}

class OxiOneHardware(private val inputPort: MidiIn, private val outputPort: MidiOut) :
    CoroutineScope {

  private val job = Job()

  override val coroutineContext: CoroutineContext
    get() = Dispatchers.IO + job

  private val blinkMap: MutableMap<Int, Job> = mutableMapOf()

  // Create a 128x8 bitmap and draw a recognizable pattern
  private fun createBitmap(width: Int, height: Int, lines: List<String>): BufferedImage {
    val image = BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY)
    val graphics = image.createGraphics()

    // Set the background to white
    graphics.color = java.awt.Color.WHITE
    graphics.fillRect(0, 0, width, height)

    // Set the font to a predefined size
    graphics.font = Font(Font.MONOSPACED, Font.PLAIN, 10)
    val metrics = graphics.fontMetrics

    // Draw each line of text
    var y = metrics.ascent // Start from the top
    lines.forEachIndexed { index, line ->
      if (index == 4) {
        // Draw a black background for this line
        graphics.color = java.awt.Color.BLACK
        graphics.fillRect(0, y - metrics.ascent, width, metrics.height)

        // Draw the text in white
        graphics.color = java.awt.Color.WHITE
      } else {
        graphics.color = java.awt.Color.BLACK
      }

      graphics.drawString(line, 0, y)
      y += metrics.height // Move to the next line
    }

    graphics.dispose()
    return image
  }

  // Convert the bitmap to a hex string with the specified format
  private fun bitmapToHexString(image: BufferedImage): String {
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
            if (pixel == java.awt.Color.BLACK.rgb) {
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

  fun clearScreen() {
    val width = 128
    val height = 64

    val image = createBitmap(width, height, List(4) { "" })
    val hexString = bitmapToHexString(image)
    outputPort.sendSysex(hexString)
  }

  fun updateScreen(data: String) {
    val width = 128
    val height = 64

    val lines = data.split("\n")
    val image = createBitmap(width, height, lines)
    val hexString = bitmapToHexString(image)
    outputPort.sendSysex(hexString)
  }

  private fun testLights() {

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

  private fun encodeColor(number: Int): Pair<Int, Int> {
    val highByte = number shr 7
    val lowByte = number and 0x7F
    return Pair(highByte, lowByte)
  }

  private fun setLEDColor(x: Int, y: Int, r: Int, g: Int, b: Int) {
    val red = encodeColor(r)
    val green = encodeColor(g)
    val blue = encodeColor(b)
    val sysexMessage =
        "f0 01 " +
            String.format("%02x %02x ", x, y) +
            String.format("%02x %02x ", red.first, red.second) +
            String.format("%02x %02x ", green.first, green.second) +
            String.format("%02x %02x ", blue.first, blue.second) +
            "f7"

    outputPort.sendSysex(sysexMessage)
  }

  fun setLight(light: OxiLights, state: OxiLightStates) {
    val id = light.value
    val value = state.value

    val idHigh = (id shr 4) and 0x0F
    val idLow = id and 0x0F
    val valueHigh = (value shr 4) and 0x0F
    val valueLow = value and 0x0F
    val sysexMessage =
        "f0 02 " +
            String.format("%02x %02x ", idHigh, idLow) +
            String.format("%02x %02x ", valueHigh, valueLow) +
            "f7"

    outputPort.sendSysex(sysexMessage)
  }

  fun updateLED(buttonNote: Int, state: MultiColorLightStateEnum, blink: Boolean = false) {
    val y = buttonNote % 16
    val x = buttonNote / 16

    if (!blink) {
      if (state is MultiColorLightStateEnum.BLINK_GREEN) {
        if (blinkMap[buttonNote] == null) blinkLED(buttonNote, state.color)
      } else {
        cancelBlink(buttonNote)
      }
    }

    setLEDColor(
        x,
        y,
        (state.color.red * 255).toInt(),
        (state.color.green * 255).toInt(),
        (state.color.blue * 255).toInt()
    )
  }

  private fun blinkLED(buttonNote: Int, color: Color) {
    blinkMap[buttonNote]?.cancel()
    blinkMap[buttonNote] = launch {
      while (isActive) {
        updateLED(buttonNote, MultiColorLightStateEnum.GREEN, blink = true)
        delay(150)
        updateLED(buttonNote, MultiColorLightStateEnum.CUSTOM(color), blink = true)
        delay(500)
      }
    }
  }

  private fun cancelBlink(buttonNote: Int) {
    blinkMap.remove(buttonNote)?.cancel()
  }

  fun getMidiNoteForGridButton(x: Int, y: Int): Int {
    val mappedY = 7 - y // This changes the orientation of the grid

    return x + (mappedY * 16)
  }
}
