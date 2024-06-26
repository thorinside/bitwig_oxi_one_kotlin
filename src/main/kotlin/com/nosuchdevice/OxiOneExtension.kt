package com.nosuchdevice

import com.bitwig.extension.api.util.midi.ShortMidiMessage
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback
import com.bitwig.extension.controller.ControllerExtension
import com.bitwig.extension.controller.api.ControllerHost
import com.bitwig.extension.controller.api.HardwareSurface
import com.nosuchdevice.clip.ClipHandler
import com.nosuchdevice.remote.RemoteHandler
import com.nosuchdevice.transport.TransportHandler

class OxiOneExtension(definition: OxiOneExtensionDefinition, host: ControllerHost) :
    ControllerExtension(definition, host) {

  private var transportHandler: TransportHandler? = null
  private var clipHandler: ClipHandler? = null
  private var remoteHandler: RemoteHandler? = null
  private lateinit var hardwareSurface: HardwareSurface
  private lateinit var outPort: com.bitwig.extension.controller.api.MidiOut
  private var hasSentRemoteModeSysex = false

  override fun init() {
    val inPort = host.getMidiInPort(0)
    outPort = host.getMidiOutPort(0)
    hardwareSurface = host.createHardwareSurface()

    if (!hasSentRemoteModeSysex) {
      outPort.sendSysex("f0 00 21 5b 00 01 06 55 f7")
      hasSentRemoteModeSysex = true
    }

    inPort.setMidiCallback(
        ShortMidiMessageReceivedCallback { msg: ShortMidiMessage -> onMidi0(msg) }
    )

    val hardware = OxiOneHardware(inPort, outPort)

    hardware.clearScreen()

    transportHandler =
        TransportHandler(
            inPort = inPort,
            transport = host.createTransport(),
            hardware = hardware,
            hardwareSurface = hardwareSurface,
        )

    val trackBank = host.createMainTrackBank(15, 0, 8)
    val cursorTrack = host.createCursorTrack("OXI_CURSOR_TRACK", "Cursor Track", 0, 4, true)

    clipHandler =
        ClipHandler(
            inPort = inPort,
            trackBank = trackBank,
            cursorTrack = cursorTrack,
            hardwareSurface = hardwareSurface,
            hardware = hardware,
            host = host
        )

    remoteHandler =
        RemoteHandler(
            inPort = inPort,
            trackBank = trackBank,
            cursorTrack = cursorTrack,
            hardwareSurface = hardwareSurface,
            hardware = hardware,
            host = host
        )

    host.showPopupNotification("OXI ONE Remote")

    hardwareSurface.setPhysicalSize(16 * 10.0 + 60f, 80.0)

    hardwareSurface.apply {
      hardwareElementWithId("SHIFT_BUTTON").setBounds(5.0, 70.0, 10.0, 10.0)
      hardwareElementWithId("STOP_BUTTON").setBounds(15.0, 70.0, 10.0, 10.0)
      hardwareElementWithId("RECORD_BUTTON").setBounds(25.0, 70.0, 10.0, 10.0)
      hardwareElementWithId("PLAY_BUTTON").setBounds(35.0, 70.0, 10.0, 10.0)
      hardwareElementWithId("ENC_0").setBounds(5.0, 0.5, 10.0, 10.0)
      hardwareElementWithId("ENC_1").setBounds(20.0, 0.5, 10.0, 10.0)
      hardwareElementWithId("ENC_2").setBounds(35.0, 0.5, 10.0, 10.0)
      hardwareElementWithId("ENC_3").setBounds(50.0, 0.5, 10.0, 10.0)

      for (i in 0 until trackBank.sizeOfBank) {
        val track = trackBank.getItemAt(i)
        for (j in 0 until track.clipLauncherSlotBank().sizeOfBank) {
          hardwareElementWithId("GRID_BUTTON_${i}_$j")
              .setBounds(60.0 + i * 10, 0.0 + j * 10, 10.0, 10.0)
        }
      }
    }
  }

  override fun exit() {
    if (hasSentRemoteModeSysex) {
      outPort.sendSysex("f0 00 21 5b 00 01 06 55 f7")
    }
  }

  override fun flush() {
    hardwareSurface.updateHardware()
  }

  /**
   * Called when we receive short MIDI message on port 0. This indicates a message that is not
   * hooked up to the hardware surface.
   */
  private fun onMidi0(msg: ShortMidiMessage) {
    if (clipHandler?.handleMidi(msg) == true) return
    if (remoteHandler?.handleMidi(msg) == true) return

    host.println(msg.toString())
  }
}
