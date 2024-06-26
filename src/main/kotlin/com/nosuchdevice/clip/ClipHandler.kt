package com.nosuchdevice.clip

import com.bitwig.extension.api.util.midi.ShortMidiMessage
import com.bitwig.extension.controller.api.*
import com.nosuchdevice.MultiColorLightState
import com.nosuchdevice.MultiColorLightStateEnum
import com.nosuchdevice.OxiFunctionButtons
import com.nosuchdevice.OxiLightStates
import com.nosuchdevice.OxiLights
import com.nosuchdevice.OxiOneHardware
import java.util.function.Supplier

class ClipHandler(
    private val inPort: MidiIn,
    private val trackBank: TrackBank,
    private val cursorTrack: CursorTrack,
    private val hardwareSurface: HardwareSurface,
    private val hardware: OxiOneHardware,
    private val host: ControllerHost,
) {

  enum class NavigationMode {
    SCENE,
    DEVICE,
    TRACK,
  }

  private var currentNavigationMode: NavigationMode = NavigationMode.SCENE
  private var isShiftPressed: Boolean = false

  private val sceneBank = trackBank.sceneBank()

  private val cursorDevice =
      cursorTrack.createCursorDevice(
          "XONE_CURSOR_DEVICE",
          "Cursor Device",
          0,
          CursorDeviceFollowMode.FOLLOW_SELECTION
      )
  private val remoteControlBank = cursorDevice.createCursorRemoteControlsPage(4)

  // private val rel4 = hardwareSurface.createRelativeHardwareKnob("REL_4")
  // private val rel5 = hardwareSurface.createRelativeHardwareKnob("REL_5")

  // private var verticalNavigation: HardwareBinding? = null
  // private var horizontalNavigation: HardwareBinding? = null

  init {
    sceneBank.setIndication(false)

    //    addNavigationKnobs()

    //    addVolumeFaders()
    //    addPanners()
    //    updateNavigation(currentNavigationMode)
    //    addNavigationModeButton()
    //    addWindowOpenToggle()
    //    addDeviceEnabledToggle()
    addShiftButton()
    addClipLaunching()
  }

  // private fun addNavigationKnobs() {
  //   // Set up navigation knobs
  //   rel4.setAdjustValueMatcher(inPort.createRelative2sComplementCCValueMatcher(0, REL_4,
  // 10))
  //   rel5.setAdjustValueMatcher(inPort.createRelative2sComplementCCValueMatcher(0, REL_5,
  // 10))
  // }

  private fun addClipLaunching() {
    for (i in 0 until trackBank.sizeOfBank) {
      val track = this.trackBank.getItemAt(i)

      for (j in 0 until track.clipLauncherSlotBank().sizeOfBank) {
        val clip = track.clipLauncherSlotBank().getItemAt(j)
        clip.isPlaybackQueued.markInterested()
        clip.isPlaying.markInterested()
        clip.isRecording.markInterested()
        clip.color().markInterested()
        clip.hasContent().markInterested()

        host.println("Adding GRID_BUTTON_${i}_$j")

        val gridButton = hardwareSurface.createHardwareButton("GRID_BUTTON_${i}_$j")
        val gridButtonLight =
            hardwareSurface.createMultiStateHardwareLight("GRID_BUTTON_LIGHT_${i}_$j")

        gridButton.setBackgroundLight(gridButtonLight)
        val buttonNote = hardware.getMidiNoteForGridButton(x = i, y = j)

        gridButton
            .releasedAction()
            .setActionMatcher(inPort.createNoteOffActionMatcher(0, buttonNote))
        gridButton
            .releasedAction()
            .setBinding(
                host.createAction(
                    Runnable {
                      if (clip.isPlaying.get()) {
                        if (isShiftPressed) {
                          clip.launchReleaseAlt()
                        } else {
                          clip.launchRelease()
                        }
                      }
                    },
                    Supplier { "Release playing" }
                )
            )

        gridButton.pressedAction().setActionMatcher(inPort.createNoteOnActionMatcher(0, buttonNote))
        gridButton
            .pressedAction()
            .setBinding(
                host.createAction(
                    Runnable {
                      val y = buttonNote % 16
                      val x = buttonNote / 16

                      host.println("Pressed GRID_BUTTON_${x}_$y")

                      if (clip.isPlaying.get() || clip.isRecording.get()) {
                        track.stop()
                      } else {
                        if (isShiftPressed) {
                          clip.launchAlt()
                        } else {
                          clip.launch()
                        }
                      }
                    },
                    Supplier { "Toggle playing" }
                )
            )

        gridButtonLight.state().setValueSupplier {
          MultiColorLightState(
              when {
                clip.isPlaybackQueued.get() ->
                    MultiColorLightStateEnum.BLINK_GREEN(clip.color().get())
                clip.isPlaying.get() -> MultiColorLightStateEnum.GREEN
                clip.isRecording.get() -> MultiColorLightStateEnum.RED
                clip.hasContent().get() -> MultiColorLightStateEnum.CUSTOM(clip.color().get())
                else -> MultiColorLightStateEnum.OFF
              }
          )
        }

        gridButtonLight.state().onUpdateHardware {
          hardware.updateLED(buttonNote, (it as MultiColorLightState).state)
        }
      }

      var p = track.pan()
      p.markInterested()
      p.setIndication(true)

      p = track.volume()
      p.markInterested()
      p.setIndication(true)
    }
  }

  private fun addShiftButton() {
    val shiftButton = hardwareSurface.createHardwareButton("SHIFT_BUTTON")
    val shiftButtonLight = hardwareSurface.createOnOffHardwareLight("SHIFT_BUTTON_LIGHT")

    shiftButton.setBackgroundLight(shiftButtonLight)
    shiftButton
        .releasedAction()
        .setActionMatcher(inPort.createNoteOffActionMatcher(1, OxiFunctionButtons.SHIFT.value))
    shiftButton
        .releasedAction()
        .setBinding(
            host.createAction(Runnable { isShiftPressed = false }, Supplier { "Release Shift" })
        )
    shiftButton
        .pressedAction()
        .setActionMatcher(inPort.createNoteOnActionMatcher(1, OxiFunctionButtons.SHIFT.value))
    shiftButton
        .pressedAction()
        .setBinding(host.createAction(Runnable { isShiftPressed = true }, Supplier { "Shift" }))

    shiftButton.isPressed.markInterested()
    shiftButtonLight.isOn.setValueSupplier { shiftButton.isPressed.get() }

    shiftButtonLight.isOn.onUpdateHardware {
      hardware.setLight(
          OxiLights.SHIFT.value,
          if (it) OxiLightStates.ON.value else OxiLightStates.OFF.value
      )
    }
  }

  // private fun addNavigationModeButton() {
  //   val hardwareButton = hardwareSurface.createHardwareButton("LAYER")
  //   hardwareButton
  //       .pressedAction()
  //       .setActionMatcher(inPort.createNoteOnActionMatcher(0, BUTTON_LAYER))
  //   hardwareButton
  //       .pressedAction()
  //       .setBinding(
  //           host.createAction(
  //               Runnable {
  //                 updateNavigation(
  //                     when (currentNavigationMode) {
  //                       NavigationMode.SCENE -> NavigationMode.TRACK
  //                       NavigationMode.TRACK -> NavigationMode.DEVICE
  //                       NavigationMode.DEVICE -> NavigationMode.SCENE
  //                     }
  //                 )
  //               },
  //               Supplier { "Change Navigation Mode" }
  //           )
  //       )
  // }

  // private fun addWindowOpenToggle() {
  //   val hardwareButton = hardwareSurface.createHardwareButton("OPEN_WINDOW")
  //   hardwareButton
  //       .pressedAction()
  //       .setActionMatcher(inPort.createNoteOnActionMatcher(0, REL_5_BUTTON))
  //   hardwareButton
  //       .pressedAction()
  //       .setBinding(
  //           host.createAction(
  //               Runnable {
  //                 if (currentNavigationMode == NavigationMode.DEVICE) {
  //                   cursorDevice.isWindowOpen.toggle()
  //                 }
  //               },
  //               Supplier { "Toggle Device Window" }
  //           )
  //       )
  // }

  // private fun addDeviceEnabledToggle() {
  //   val hardwareButton = hardwareSurface.createHardwareButton("ENABLE_DEVICE")
  //   hardwareButton
  //       .pressedAction()
  //       .setActionMatcher(inPort.createNoteOnActionMatcher(0, REL_4_BUTTON))
  //   hardwareButton
  //       .pressedAction()
  //       .setBinding(
  //           host.createAction(
  //               Runnable {
  //                 if (currentNavigationMode == NavigationMode.DEVICE) {
  //                   cursorDevice.isEnabled.toggle()
  //                 }
  //               },
  //               Supplier { "Toggle Device Enabled" }
  //           )
  //       )
  // }

  // private fun addVolumeFaders() {
  //   val slider0 = hardwareSurface.createHardwareSlider("SLIDER_0")
  //   val slider1 = hardwareSurface.createHardwareSlider("SLIDER_1")
  //   val slider2 = hardwareSurface.createHardwareSlider("SLIDER_2")
  //   val slider3 = hardwareSurface.createHardwareSlider("SLIDER_3")

  //   slider0.setAdjustValueMatcher(inPort.createAbsoluteCCValueMatcher(0, FADER_0))
  //   slider1.setAdjustValueMatcher(inPort.createAbsoluteCCValueMatcher(0, FADER_1))
  //   slider2.setAdjustValueMatcher(inPort.createAbsoluteCCValueMatcher(0, FADER_2))
  //   slider3.setAdjustValueMatcher(inPort.createAbsoluteCCValueMatcher(0, FADER_3))

  //   slider0.setBinding(trackBank.getItemAt(0).volume())
  //   slider1.setBinding(trackBank.getItemAt(1).volume())
  //   slider2.setBinding(trackBank.getItemAt(2).volume())
  //   slider3.setBinding(trackBank.getItemAt(3).volume())
  // }

  // private fun addPanners() {
  //   val rel0 = hardwareSurface.createRelativeHardwareKnob("REL_0")
  //   val rel1 = hardwareSurface.createRelativeHardwareKnob("REL_1")
  //   val rel2 = hardwareSurface.createRelativeHardwareKnob("REL_2")
  //   val rel3 = hardwareSurface.createRelativeHardwareKnob("REL_3")

  //   val rel0button = hardwareSurface.createHardwareButton("REL_0_CLICK")
  //   val rel1button = hardwareSurface.createHardwareButton("REL_1_CLICK")
  //   val rel2button = hardwareSurface.createHardwareButton("REL_2_CLICK")
  //   val rel3button = hardwareSurface.createHardwareButton("REL_3_CLICK")

  //   rel0.setAdjustValueMatcher(inPort.createRelative2sComplementCCValueMatcher(0, REL_0,
  // 128))
  //   rel1.setAdjustValueMatcher(inPort.createRelative2sComplementCCValueMatcher(0, REL_1,
  // 128))
  //   rel2.setAdjustValueMatcher(inPort.createRelative2sComplementCCValueMatcher(0, REL_2,
  // 128))
  //   rel3.setAdjustValueMatcher(inPort.createRelative2sComplementCCValueMatcher(0, REL_3,
  // 128))

  //   rel0button.pressedAction().setActionMatcher(inPort.createNoteOnActionMatcher(0,
  // REL_0_CLICK))
  //   rel1button.pressedAction().setActionMatcher(inPort.createNoteOnActionMatcher(0,
  // REL_1_CLICK))
  //   rel2button.pressedAction().setActionMatcher(inPort.createNoteOnActionMatcher(0,
  // REL_2_CLICK))
  //   rel3button.pressedAction().setActionMatcher(inPort.createNoteOnActionMatcher(0,
  // REL_3_CLICK))

  //   trackBank.getItemAt(0).pan().addBinding(rel0)
  //   trackBank.getItemAt(1).pan().addBinding(rel1)
  //   trackBank.getItemAt(2).pan().addBinding(rel2)
  //   trackBank.getItemAt(3).pan().addBinding(rel3)

  //   rel0button
  //       .pressedAction()
  //       .setBinding(
  //           host.createAction(
  //               Runnable { trackBank.getItemAt(0).pan().reset() },
  //               Supplier { "Reset Pan on Track 0" }
  //           )
  //       )

  //   rel1button
  //       .pressedAction()
  //       .setBinding(
  //           host.createAction(
  //               Runnable { trackBank.getItemAt(1).pan().reset() },
  //               Supplier { "Reset Pan on Track 1" }
  //           )
  //       )

  //   rel2button
  //       .pressedAction()
  //       .setBinding(
  //           host.createAction(
  //               Runnable { trackBank.getItemAt(2).pan().reset() },
  //               Supplier { "Reset Pan on Track 2" }
  //           )
  //       )

  //   rel3button
  //       .pressedAction()
  //       .setBinding(
  //           host.createAction(
  //               Runnable { trackBank.getItemAt(3).pan().reset() },
  //               Supplier { "Reset Pan on Track 3" }
  //           )
  //       )
  // }

  // private fun updateNavigation(mode: NavigationMode) {

  //   host.showPopupNotification(
  //       "${
  //               mode.name.lowercase()
  //                   .replaceFirstChar { if (it.isLowerCase())
  // it.titlecase(Locale.getDefault())
  // else it.toString() }
  //           } Mode"
  //   )

  //   verticalNavigation?.removeBinding()
  //   horizontalNavigation?.removeBinding()

  //   when (mode) {
  //     NavigationMode.SCENE -> {
  //       verticalNavigation = trackBank.addBinding(rel4)
  //       horizontalNavigation = sceneBank.addBinding(rel5)
  //       hardware.updateLED(BUTTON_LAYER, YELLOW)
  //     }
  //     NavigationMode.TRACK -> {
  //       verticalNavigation = cursorTrack.addBinding(rel4)
  //       horizontalNavigation = cursorDevice.addBinding(rel5)
  //       hardware.updateLED(BUTTON_LAYER, GREEN)
  //     }
  //     NavigationMode.DEVICE -> {
  //       verticalNavigation = remoteControlBank.addBinding(rel4)
  //       horizontalNavigation = cursorDevice.addBinding(rel5)
  //       hardware.updateLED(BUTTON_LAYER, RED)
  //     }
  //   }

  //   currentNavigationMode = mode
  // }

  fun handleMidi(@Suppress("UNUSED_PARAMETER") msg: ShortMidiMessage): Boolean {
    return false
  }
}
