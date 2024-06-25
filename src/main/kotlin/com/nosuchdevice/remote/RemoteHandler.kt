package com.nosuchdevice.remote

import com.bitwig.extension.api.util.midi.ShortMidiMessage
import com.bitwig.extension.controller.api.*
import com.nosuchdevice.OxiOneHardware

class RemoteHandler(
    private val inPort: MidiIn,
    private val trackBank: TrackBank,
    private val cursorTrack: CursorTrack,
    private val hardwareSurface: HardwareSurface,
    private val hardware: OxiOneHardware,
    private val host: ControllerHost,
) {
  private val cursorDevice =
      cursorTrack.createCursorDevice(
          "XONE_CURSOR_DEVICE",
          "Cursor Device",
          0,
          CursorDeviceFollowMode.FOLLOW_SELECTION
      )
  private val remoteControlBank = cursorDevice.createCursorRemoteControlsPage(4)

  init {
    addRemoteControlKnobs()

    trackBank.followCursorTrack(cursorTrack)

    cursorTrack.solo().markInterested()
    cursorTrack.mute().markInterested()

    cursorDevice.isEnabled.markInterested()
    cursorDevice.isWindowOpen.markInterested()
  }

  private fun addRemoteControlKnobs() {
    for (i in 0 until remoteControlBank.parameterCount) {
      remoteControlBank.getParameter(i).apply {
        markInterested()
        setIndication(true)

        val hardwareKnob = hardwareSurface.createRelativeHardwareKnob("ENC_$i")
        val valueMatcher = inPort.createRelative2sComplementCCValueMatcher(0, i, 127)
        hardwareKnob.setAdjustValueMatcher(valueMatcher)
        addBinding(hardwareKnob)

        value().addValueObserver { updateOLEDDisplay() }
        name().addValueObserver { updateOLEDDisplay() }
      }
    }
  }

  private fun updateOLEDDisplay() {
    val displayData = StringBuilder()

    for (i in 0 until 4) {
      val parameter = remoteControlBank.getParameter(i)
      val name = parameter.name().get()
      val value = parameter.value().get()

      displayData.append(
          String.format("%d) %-10s: %3d%%\n", i + 1, name.take(10), (value * 100).toInt())
      )
    }
    displayData.append("(1)   (2)   (3)   (4)\n")
    sendToOLEDDisplay(displayData.toString())
  }

  private fun sendToOLEDDisplay(data: String) {
    hardware.updateScreen(data)
  }

  fun handleMidi(@Suppress("UNUSED_PARAMETER") msg: ShortMidiMessage): Boolean {
    return false
  }
}