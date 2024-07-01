package com.nosuchdevice.remote

import com.bitwig.extension.api.util.midi.ShortMidiMessage
import com.bitwig.extension.controller.api.*
import com.nosuchdevice.OxiOneHardware

class RemoteHandler(
    private val inPort: MidiIn,
    private val remoteControlBank: CursorRemoteControlsPage,
    private val hardwareSurface: HardwareSurface,
    private val hardware: OxiOneHardware,
    private val host: ControllerHost,
    private val cursorDevice: CursorDevice,
    private val isShiftPressed: BooleanValue,
) {

  data class Binding(val knob: RelativeHardwareKnob, var binding: HardwareBinding?)

  private val knobs = mutableListOf<Binding>()

  init {
    addRemoteControlKnobs()
    isShiftPressed.addValueObserver {
      updateBindings()
      updateOLEDDisplay()
    }
  }

  private fun addRemoteControlKnobs() {
    for (i in 0 until 4) {
      knobs.add(
          Binding(
              hardwareSurface.createRelativeHardwareKnob("ENC_$i").apply {
                setAdjustValueMatcher(inPort.createRelative2sComplementCCValueMatcher(0, i, 127))
              },
              null
          )
      )
    }

    for (i in 0 until remoteControlBank.parameterCount) {
      remoteControlBank.getParameter(i).apply {
        markInterested()
        setIndication(true)
        value().addValueObserver { updateOLEDDisplay() }
        name().addValueObserver { updateOLEDDisplay() }
      }
    }

    updateBindings()
  }

  private fun updateBindings() {
    var i = 0

    knobs.forEach { it.binding?.removeBinding() }

    val offset = if (isShiftPressed.get()) 4 else 0

    for (j in 0 until remoteControlBank.parameterCount) {
      remoteControlBank.getParameter(j).apply {
        setIndication(false)
        if (j >= offset && i < 4) {
          setIndication(true)
          knobs[i].binding = addBinding(knobs[i].knob)
          i++
        }
      }
    }
  }

  private fun updateOLEDDisplay() {
    var i = 0
    val displayData = StringBuilder()

    val offset = if (isShiftPressed.get()) 4 else 0

    for (j in 0 until remoteControlBank.parameterCount) {
      if (j >= offset && i < 4) {
        val parameter = remoteControlBank.getParameter(j)
        val name = parameter.name().get()
        val value = parameter.value().get()

        displayData.append(
            String.format("%d) %-10s: %3d%%\n", j + 1, name.take(10), (value * 100).toInt())
        )
        i++
      }
    }
    displayData.append("(${1 + offset})   (${2 + offset})   (${3 + offset})   (${4 + offset})\n")
    sendToOLEDDisplay(displayData.toString())
  }

  private fun sendToOLEDDisplay(data: String) {
    hardware.updateScreen(data)
  }

  fun handleMidi(@Suppress("UNUSED_PARAMETER") msg: ShortMidiMessage): Boolean {
    return false
  }
}
