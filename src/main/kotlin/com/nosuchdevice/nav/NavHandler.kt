package com.nosuchdevice.nav

import com.bitwig.extension.api.util.midi.ShortMidiMessage
import com.bitwig.extension.controller.api.*
import com.nosuchdevice.OxiFunctionButtons
import com.nosuchdevice.OxiOneHardware
import java.util.*
import java.util.function.Supplier

class NavHandler(
        private val inPort: MidiIn,
        private val trackBank: TrackBank,
        private val remoteControlBank: CursorRemoteControlsPage,
        private val cursorTrack: CursorTrack,
        private val cursorDevice: PinnableCursorDevice,
        private val hardwareSurface: HardwareSurface,
        private val hardware: OxiOneHardware,
        private val host: ControllerHost,
) {
    enum class NavigationMode {
        SCENE,
        DEVICE,
        TRACK,
    }

    private var currentNavigationMode: NavigationMode = NavigationMode.DEVICE

    private val sceneBank = trackBank.sceneBank()

    private val upButton = hardwareSurface.createHardwareButton("UP")
    private val downButton = hardwareSurface.createHardwareButton("DOWN")
    private val leftButton = hardwareSurface.createHardwareButton("LEFT")
    private val rightButton = hardwareSurface.createHardwareButton("RIGHT")

    private var upNavigation: HardwareBinding? = null
    private var downNavigation: HardwareBinding? = null
    private var leftNavigation: HardwareBinding? = null
    private var rightNavigation: HardwareBinding? = null

    init {
        addNavigationButtons()
        addNavigationModeButton()
        addWindowOpenToggle()
        addDeviceEnabledToggle()
    }

    private fun addNavigationButtons() {
        upButton.pressedAction().apply {
            setActionMatcher(inPort.createNoteOnActionMatcher(1, OxiFunctionButtons.UP_32.value))
            setBinding(host.createAction(Runnable { onNavigateUp() }, Supplier { "Navigate Up" }))
        }
        downButton.pressedAction().apply {
            setActionMatcher(inPort.createNoteOnActionMatcher(1, OxiFunctionButtons.DOWN_48.value))
            setBinding(
                    host.createAction(Runnable { onNavigateDown() }, Supplier { "Navigate Down" })
            )
        }
        leftButton.pressedAction().apply {
            setActionMatcher(inPort.createNoteOnActionMatcher(1, OxiFunctionButtons.LEFT_16.value))
            setBinding(
                    host.createAction(Runnable { onNavigateLeft() }, Supplier { "Navigate Left" })
            )
        }
        rightButton.pressedAction().apply {
            setActionMatcher(inPort.createNoteOnActionMatcher(1, OxiFunctionButtons.RIGHT_64.value))
            setBinding(
                    host.createAction(Runnable { onNavigateRight() }, Supplier { "Navigate Right" })
            )
        }
    }

    private fun addNavigationModeButton() {
        val hardwareButton = hardwareSurface.createHardwareButton("LOAD")
        hardwareButton.pressedAction().apply {
            setActionMatcher(inPort.createNoteOnActionMatcher(1, OxiFunctionButtons.LOAD.value))
            setBinding(
                    host.createAction(
                            Runnable {
                                currentNavigationMode =
                                        when (currentNavigationMode) {
                                            NavigationMode.SCENE -> NavigationMode.TRACK
                                            NavigationMode.TRACK -> NavigationMode.DEVICE
                                            NavigationMode.DEVICE -> NavigationMode.SCENE
                                        }

                                host.showPopupNotification(
                                        "${
                                                currentNavigationMode.name.lowercase()
                                                    .replaceFirstChar {
                                                        if (it.isLowerCase())
                                                            it.titlecase(Locale.getDefault())
                                                        else it.toString()
                                                    }
                                            } Mode"
                                )
                            },
                            Supplier { "Change Navigation Mode" }
                    )
            )
        }
    }

    private fun addWindowOpenToggle() {
        val hardwareButton = hardwareSurface.createHardwareButton("OPEN_WINDOW")
        hardwareButton.pressedAction().apply {
            setActionMatcher(inPort.createNoteOnActionMatcher(1, OxiFunctionButtons.ARRANGER.value))
            setBinding(
                    host.createAction(
                            Runnable {
                                if (currentNavigationMode == NavigationMode.DEVICE) {
                                    cursorDevice.isWindowOpen.toggle()
                                }
                            },
                            Supplier { "Toggle Device Window" }
                    )
            )
        }
    }

    private fun addDeviceEnabledToggle() {
        val hardwareButton = hardwareSurface.createHardwareButton("ENABLE_DEVICE")
        hardwareButton.pressedAction().apply {
            setActionMatcher(
                    inPort.createNoteOnActionMatcher(1, OxiFunctionButtons.PASTE_CLEAR.value)
            )
            setBinding(
                    host.createAction(
                            Runnable {
                                if (currentNavigationMode == NavigationMode.DEVICE) {
                                    cursorDevice.isEnabled.toggle()
                                }
                            },
                            Supplier { "Toggle Device Enabled" }
                    )
            )
        }
    }

    private fun onNavigateUp() {
        when (currentNavigationMode) {
            NavigationMode.SCENE -> {
                trackBank.scrollBackwards()
            }
            NavigationMode.TRACK -> {
                cursorTrack.selectPrevious()
            }
            NavigationMode.DEVICE -> {
                cursorDevice.selectPrevious()
            }
        }
    }

    private fun onNavigateDown() {
        when (currentNavigationMode) {
            NavigationMode.SCENE -> {
                trackBank.scrollForwards()
            }
            NavigationMode.TRACK -> {
                cursorTrack.selectNext()
            }
            NavigationMode.DEVICE -> {
                cursorDevice.selectNext()
            }
        }
    }

    private fun onNavigateLeft() {
        when (currentNavigationMode) {
            NavigationMode.SCENE -> {
                sceneBank.scrollBackwards()
            }
            NavigationMode.TRACK -> {
                cursorDevice.selectPrevious()
            }
            NavigationMode.DEVICE -> {
                remoteControlBank.selectPrevious()
            }
        }
    }

    private fun onNavigateRight() {
        when (currentNavigationMode) {
            NavigationMode.SCENE -> {
                sceneBank.scrollForwards()
            }
            NavigationMode.TRACK -> {
                cursorDevice.selectNext()
            }
            NavigationMode.DEVICE -> {
                remoteControlBank.selectNext()
            }
        }
    }

    fun handleMidi(@Suppress("UNUSED_PARAMETER") msg: ShortMidiMessage): Boolean {
        return false
    }
}
