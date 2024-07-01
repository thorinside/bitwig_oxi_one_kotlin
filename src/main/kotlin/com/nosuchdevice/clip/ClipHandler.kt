package com.nosuchdevice.clip

import com.bitwig.extension.api.util.midi.ShortMidiMessage
import com.bitwig.extension.controller.api.*
import com.nosuchdevice.MultiColorLightState
import com.nosuchdevice.MultiColorLightStateEnum
import com.nosuchdevice.OxiOneHardware
import java.util.function.Supplier

class ClipHandler(
    private val inPort: MidiIn,
    private val trackBank: TrackBank,
    private val hardwareSurface: HardwareSurface,
    private val hardware: OxiOneHardware,
    private val host: ControllerHost,
    private val isShiftPressed: BooleanValue,
) {

  private val sceneBank = trackBank.sceneBank()

  init {
    addClipLaunching()
  }

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

        val gridButton = hardwareSurface.createHardwareButton("GRID_BUTTON_${i}_$j")
        val gridButtonLight =
            hardwareSurface.createMultiStateHardwareLight("GRID_BUTTON_LIGHT_${i}_$j")

        gridButton.setBackgroundLight(gridButtonLight)
        val buttonNote = hardware.getMidiNoteForGridButton(x = i, y = j)

        gridButton.releasedAction().apply {
          setActionMatcher(inPort.createNoteOffActionMatcher(0, buttonNote))
          setBinding(
              host.createAction(
                  Runnable {
                    if (clip.isPlaying.get()) {
                      if (isShiftPressed.get()) {
                        clip.launchReleaseAlt()
                      } else {
                        clip.launchRelease()
                      }
                    }
                  },
                  Supplier { "Release playing" }
              )
          )
        }

        gridButton.pressedAction().apply {
          setActionMatcher(inPort.createNoteOnActionMatcher(0, buttonNote))
          setBinding(
              host.createAction(
                  Runnable {
                    val y = buttonNote % 16
                    val x = buttonNote / 16

                    host.println("Pressed GRID_BUTTON_${x}_$y")

                    if (clip.isPlaying.get() || clip.isRecording.get()) {
                      track.stop()
                    } else {
                      if (isShiftPressed.get()) {
                        clip.launchAlt()
                      } else {
                        clip.launch()
                      }
                    }
                  },
                  Supplier { "Toggle playing" }
              )
          )
        }

        gridButtonLight.state().apply {
          setValueSupplier {
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

          onUpdateHardware { hardware.updateLED(buttonNote, (it as MultiColorLightState).state) }
        }
      }
    }
  }

  fun handleMidi(@Suppress("UNUSED_PARAMETER") msg: ShortMidiMessage): Boolean {
    return false
  }
}
