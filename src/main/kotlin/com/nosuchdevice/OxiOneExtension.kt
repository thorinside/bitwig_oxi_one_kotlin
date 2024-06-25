package com.nosuchdevice

import com.bitwig.extension.api.util.midi.ShortMidiMessage
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback
import com.bitwig.extension.controller.ControllerExtension
import com.bitwig.extension.controller.api.ControllerHost
import com.bitwig.extension.controller.api.HardwareSurface
import com.nosuchdevice.remote.RemoteHandler
import com.nosuchdevice.track.TrackHandler
import com.nosuchdevice.transport.TransportHandler

class OxiOneExtension(definition: OxiOneExtensionDefinition, host: ControllerHost) :
    ControllerExtension(definition, host) {

  private var transportHandler: TransportHandler? = null
  private var trackHandler: TrackHandler? = null
  private var remoteHandler: RemoteHandler? = null
  private lateinit var hardwareSurface: HardwareSurface
  private lateinit var outPort: com.bitwig.extension.controller.api.MidiOut

  override fun init() {

    val inPort = host.getMidiInPort(0)
    outPort = host.getMidiOutPort(0)
    hardwareSurface = host.createHardwareSurface()

    outPort.sendSysex("f0 00 21 5b 00 01 06 55 f7")

    inPort.setMidiCallback(
        ShortMidiMessageReceivedCallback { msg: ShortMidiMessage -> onMidi0(msg) }
    )

    val hardware = OxiOneHardware(inPort, outPort)

    // hardware.testLights()
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

    trackHandler =
        TrackHandler(
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

    hardwareSurface.setPhysicalSize(16 * 10.0 + 60f, 80.0)

    hardwareSurface.apply {
      hardwareElementWithId("SHIFT_BUTTON").setBounds(0.0, 70.0, 10.0, 10.0)
      hardwareElementWithId("STOP_BUTTON").setBounds(10.0, 70.0, 10.0, 10.0)
      hardwareElementWithId("RECORD_BUTTON").setBounds(20.0, 70.0, 10.0, 10.0)
      hardwareElementWithId("PLAY_BUTTON").setBounds(30.0, 70.0, 10.0, 10.0)
      // hardwareElementWithId("ENC_1").setBounds(27.25, 6.5, 10.0, 10.0)
      // hardwareElementWithId("ENC_2").setBounds(39.25, 6.5, 10.0, 10.0)
      // hardwareElementWithId("ENC_3").setBounds(51.25, 6.5, 10.0, 10.0)
      // hardwareElementWithId("ENC_4").setBounds(63.25, 6.5, 10.0, 10.0)
      // hardwareElementWithId("ENC_1_CLICK").setBounds(27.75, 18.75, 10.0, 10.0)
      // hardwareElementWithId("ENC_2_CLICK").setBounds(39.75, 18.75, 10.0, 10.0)
      // hardwareElementWithId("ENC_3_CLICK").setBounds(51.75, 18.75, 10.0, 10.0)
      // hardwareElementWithId("ENC_4_CLICK").setBounds(64.0, 18.5, 10.0, 10.0)

      for (i in 0 until trackBank.sizeOfBank) {
        val track = trackBank.getItemAt(i)
        for (j in 0 until track.clipLauncherSlotBank().sizeOfBank) {
          hardwareElementWithId("GRID_BUTTON_${i}_$j")
              .setBounds(60.0 + i * 10, 0.0 + j * 10, 10.0, 10.0)
        }
      }
    }

    //   surface.hardwareElementWithId("REL_4").setBounds(41.0, 233.0, 10.0, 10.0)
    //   surface.hardwareElementWithId("REL_4_BUTTON").setBounds(41.0, 233.0, 10.0, 10.0)
    //   surface.hardwareElementWithId("REL_5").setBounds(55.75, 233.0, 10.0, 10.0)
    //   surface.hardwareElementWithId("PLAY_BUTTON_0_0").setBounds(27.5, 170.0, 10.0, 10.0)
    //   surface.hardwareElementWithId("PLAY_BUTTON_0_1").setBounds(39.5, 170.0, 10.0, 10.0)
    //   surface.hardwareElementWithId("PLAY_BUTTON_0_2").setBounds(51.5, 170.0, 10.0, 10.0)
    //   surface.hardwareElementWithId("PLAY_BUTTON_0_3").setBounds(63.5, 170.0, 10.0, 10.0)
    //   surface.hardwareElementWithId("PLAY_BUTTON_1_0").setBounds(27.75, 181.5, 10.0, 10.0)
    //   surface.hardwareElementWithId("PLAY_BUTTON_1_1").setBounds(39.5, 181.75, 10.0, 10.0)
    //   surface.hardwareElementWithId("PLAY_BUTTON_1_2").setBounds(51.5, 181.75, 10.0, 10.0)
    //   surface.hardwareElementWithId("PLAY_BUTTON_1_3").setBounds(63.5, 181.75, 10.0, 10.0)
    //   surface.hardwareElementWithId("PLAY_BUTTON_2_0").setBounds(28.0, 193.75, 10.0, 10.0)
    //   surface.hardwareElementWithId("PLAY_BUTTON_2_1").setBounds(40.0, 193.75, 10.0, 10.0)
    //   surface.hardwareElementWithId("PLAY_BUTTON_2_2").setBounds(52.0, 193.75, 10.0, 10.0)
    //   surface.hardwareElementWithId("PLAY_BUTTON_2_3").setBounds(64.0, 193.75, 10.0, 10.0)
    //   surface.hardwareElementWithId("PLAY_BUTTON_3_0").setBounds(28.25, 206.0, 10.0, 10.0)
    //   surface.hardwareElementWithId("PLAY_BUTTON_3_1").setBounds(40.5, 206.0, 10.0, 10.0)
    //   surface.hardwareElementWithId("PLAY_BUTTON_3_2").setBounds(52.5, 206.0, 10.0, 10.0)
    //   surface.hardwareElementWithId("PLAY_BUTTON_3_3").setBounds(64.5, 206.0, 10.0, 10.0)
    //   surface.hardwareElementWithId("LIGHT_0").setBounds(20.25, 43.75, 10.0, 10.0)
    //   surface.hardwareElementWithId("LIGHT_1").setBounds(38.25, 43.75, 10.0, 10.0)
    //   surface.hardwareElementWithId("LIGHT_2").setBounds(55.25, 43.75, 10.0, 10.0)
    //   surface.hardwareElementWithId("LIGHT_3").setBounds(55.25, 43.75, 10.0, 10.0)
    //   surface.hardwareElementWithId("KNOB_0").setBounds(20.25, 43.75, 10.0, 10.0)
    //   surface.hardwareElementWithId("KNOB_1").setBounds(38.25, 43.75, 10.0, 10.0)
    //   surface.hardwareElementWithId("KNOB_2").setBounds(55.25, 43.75, 10.0, 10.0)
    //   surface.hardwareElementWithId("KNOB_3").setBounds(69.5, 43.75, 10.0, 10.0)
    //   surface.hardwareElementWithId("KNOB_4").setBounds(20.25, 60.75, 10.0, 10.0)
    //   surface.hardwareElementWithId("KNOB_5").setBounds(38.25, 60.75, 10.0, 10.0)
    //   surface.hardwareElementWithId("KNOB_6").setBounds(55.25, 60.75, 10.0, 10.0)
    //   surface.hardwareElementWithId("KNOB_7").setBounds(69.5, 60.75, 10.0, 10.0)

    //   // surface.hardwareElementWithId("BUTTON_8").setBounds(15.5, 170.0, 10.0,
    //   // 10.0)
    //   // surface.hardwareElementWithId("KNOB_8").setBounds(20.25, 78.75, 10.0,
    //   // 10.0)
    //   // surface.hardwareElementWithId("KNOB_9").setBounds(38.25, 78.75, 10.0,
    //   // 10.0)
    //   // surface.hardwareElementWithId("KNOB_10").setBounds(55.25, 78.75, 10.0,
    //   // 10.0)
    //   // surface.hardwareElementWithId("KNOB_11").setBounds(69.5, 78.75, 10.0,
    //   // 10.0)
    // }

    host.showPopupNotification("Oxi One Initialized")
  }

  override fun exit() {

    outPort.sendSysex("f0 00 21 5b 00 01 06 55 f7")

    host.showPopupNotification("Oxi One Exited")
  }

  override fun flush() {
    hardwareSurface.updateHardware()
  }

  /** Called when we receive short MIDI message on port 0. */
  private fun onMidi0(msg: ShortMidiMessage) {
    if (trackHandler?.handleMidi(msg) == true) return

    host.println(msg.toString())
  }
}
