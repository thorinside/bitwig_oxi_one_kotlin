package com.nosuchdevice

import com.bitwig.extension.api.util.midi.ShortMidiMessage
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback
import com.bitwig.extension.controller.ControllerExtension
import com.bitwig.extension.controller.api.BooleanValue
import com.bitwig.extension.controller.api.ControllerHost
import com.bitwig.extension.controller.api.CursorDeviceFollowMode
import com.bitwig.extension.controller.api.CursorRemoteControlsPage
import com.bitwig.extension.controller.api.CursorTrack
import com.bitwig.extension.controller.api.HardwareSurface
import com.bitwig.extension.controller.api.MidiIn
import com.bitwig.extension.controller.api.PinnableCursorDevice
import com.bitwig.extension.controller.api.TrackBank
import com.nosuchdevice.clip.ClipHandler
import com.nosuchdevice.nav.NavHandler
import com.nosuchdevice.remote.RemoteHandler
import com.nosuchdevice.transport.TransportHandler

class OxiOneExtension(definition: OxiOneExtensionDefinition, host: ControllerHost) :
    ControllerExtension(definition, host) {

    private var transportHandler: TransportHandler? = null
    private var clipHandler: ClipHandler? = null
    private var remoteHandler: RemoteHandler? = null
    private var navHandler: NavHandler? = null
    private lateinit var cursorDevice: PinnableCursorDevice
    private lateinit var cursorTrack: CursorTrack
    private lateinit var trackBank: TrackBank
    private lateinit var remoteControlBank: CursorRemoteControlsPage
    private lateinit var hardware: OxiOneHardware

    private lateinit var hardwareSurface: HardwareSurface
    private lateinit var outPort: com.bitwig.extension.controller.api.MidiOut
    private var hasSentRemoteModeSysex = false

    override fun init() {
        val inPort = host.getMidiInPort(0)
        outPort = host.getMidiOutPort(0)
        hardwareSurface = host.createHardwareSurface()

        inPort.setMidiCallback(
            ShortMidiMessageReceivedCallback { msg: ShortMidiMessage -> onMidi0(msg) }
        )

        inPort.setSysexCallback { data: String -> onSysex(data) }

        hardware = OxiOneHardware(inPort, outPort)
        if (!hasSentRemoteModeSysex) {
            hardware.enterRemoteMode()
            hasSentRemoteModeSysex = true
        }

        transportHandler =
            TransportHandler(
                inPort = inPort,
                transport = host.createTransport(),
                hardware = hardware,
                hardwareSurface = hardwareSurface,
            )

        trackBank = host.createMainTrackBank(16, 0, 8)
        cursorTrack = host.createCursorTrack("OXI_ONE_CURSOR", "Cursor Track", 0, 8, true)

        val isShiftPressed = addShiftButton(inPort)

        clipHandler =
            ClipHandler(
                inPort = inPort,
                trackBank = trackBank,
                hardwareSurface = hardwareSurface,
                hardware = hardware,
                host = host,
                isShiftPressed = isShiftPressed,
            )

        cursorDevice =
            cursorTrack.createCursorDevice(
                "OXI_ONE_CURSOR",
                "Cursor Device",
                2,
                CursorDeviceFollowMode.FOLLOW_SELECTION
            )

        remoteControlBank = cursorDevice.createCursorRemoteControlsPage(8)

        trackBank.followCursorTrack(cursorTrack)

        cursorTrack.solo().markInterested()
        cursorTrack.mute().markInterested()

        cursorDevice.isEnabled.markInterested()
        cursorDevice.isWindowOpen.markInterested()

        remoteHandler =
            RemoteHandler(
                inPort = inPort,
                remoteControlBank = remoteControlBank,
                cursorDevice = cursorDevice,
                hardwareSurface = hardwareSurface,
                hardware = hardware,
                host = host,
                isShiftPressed = isShiftPressed,
            )

        navHandler =
            NavHandler(
                inPort = inPort,
                remoteControlBank = remoteControlBank,
                cursorDevice = cursorDevice,
                cursorTrack = cursorTrack,
                hardware = hardware,
                hardwareSurface = hardwareSurface,
                host = host,
                trackBank = trackBank,
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

    private fun addShiftButton(inPort: MidiIn): BooleanValue {
        val shiftButton = hardwareSurface.createHardwareButton("SHIFT_BUTTON")
        val shiftButtonLight = hardwareSurface.createOnOffHardwareLight("SHIFT_BUTTON_LIGHT")

        shiftButton.setBackgroundLight(shiftButtonLight)
        shiftButton.releasedAction().apply {
            setActionMatcher(inPort.createNoteOffActionMatcher(1, OxiFunctionButtons.SHIFT.value))
            // setBinding(
            //     host.createAction(Runnable { isShiftPressed = false }, Supplier { "Release Shift" })
            // )
        }
        shiftButton.pressedAction().apply {
            setActionMatcher(inPort.createNoteOnActionMatcher(1, OxiFunctionButtons.SHIFT.value))
            // setBinding(host.createAction(Runnable { isShiftPressed = true }, Supplier { "Shift" }))
        }

        shiftButton.isPressed.markInterested()
        shiftButtonLight.isOn.apply {
            setValueSupplier { shiftButton.isPressed.get() }

            onUpdateHardware {
                hardware.setLight(OxiLights.SHIFT, if (it) OxiLightStates.ON else OxiLightStates.OFF)
            }
        }

        return shiftButton.isPressed
    }

    override fun exit() {
        if (hasSentRemoteModeSysex) {
            hardware.exitRemoteMode()
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

    private fun onSysex(data: String) {
        host.println("sysex: " + data)
        if (data.contentEquals(REMOTE_MODE_SYSEX_ACK)) {
            hasSentRemoteModeSysex = true
        }
    }
}
