package com.nosuchdevice.transport

import com.bitwig.extension.controller.api.HardwareSurface
import com.bitwig.extension.controller.api.MidiIn
import com.bitwig.extension.controller.api.Transport
import com.nosuchdevice.OxiOneHardware

class TransportHandler(
    val inPort: MidiIn,
    val transport: Transport,
    val hardwareSurface: HardwareSurface,
    val hardware: OxiOneHardware,
) {
  init {
    val stopButton = hardwareSurface.createHardwareButton("STOP_BUTTON")
    val stopButtonLight = hardwareSurface.createOnOffHardwareLight("STOP_BUTTON_LIGHT")

    val recordButton = hardwareSurface.createHardwareButton("RECORD_BUTTON")
    val recordButtonLight = hardwareSurface.createOnOffHardwareLight("RECORD_BUTTON_LIGHT")

    val playButton = hardwareSurface.createHardwareButton("PLAY_BUTTON")
    val playButtonLight = hardwareSurface.createOnOffHardwareLight("PLAY_BUTTON_LIGHT")

    stopButton.setBackgroundLight(stopButtonLight)
    stopButton
        .pressedAction()
        .setActionMatcher(inPort.createNoteOnActionMatcher(1, OxiOneHardware.BUTTON_STOP))
    stopButton.pressedAction().setBinding(transport.stopAction())

    recordButton.setBackgroundLight(recordButtonLight)
    recordButton
        .pressedAction()
        .setActionMatcher(inPort.createNoteOnActionMatcher(1, OxiOneHardware.BUTTON_RECORD))
    recordButton.pressedAction().setBinding(transport.recordAction())

    playButton.setBackgroundLight(playButtonLight)
    playButton
        .pressedAction()
        .setActionMatcher(inPort.createNoteOnActionMatcher(1, OxiOneHardware.BUTTON_PLAY))
    playButton.pressedAction().setBinding(transport.playAction())

    transport.isPlaying.markInterested()
    playButtonLight.isOn.setValueSupplier { transport.isPlaying.get() }

    transport.isArrangerRecordEnabled.markInterested()
    recordButtonLight.isOn.setValueSupplier { transport.isArrangerRecordEnabled.get() }

    stopButtonLight.isOn.setValueSupplier { transport.isPlaying.get() }

    playButtonLight.isOn.onUpdateHardware {
      hardware.setLight(
          OxiOneHardware.BUTTON_PLAY_LIGHT,
          if (it) OxiOneHardware.LEDS_ON else OxiOneHardware.LEDS_OFF
      )
    }

    stopButtonLight.isOn.onUpdateHardware {
      hardware.setLight(
          OxiOneHardware.BUTTON_STOP_LIGHT,
          if (it) OxiOneHardware.LEDS_OFF else OxiOneHardware.LEDS_SHORT_ON
      )
    }

    recordButtonLight.isOn.onUpdateHardware {
      hardware.setLight(
          OxiOneHardware.BUTTON_RECORD_LIGHT,
          if (it) OxiOneHardware.LEDS_ON else OxiOneHardware.LEDS_OFF
      )
    }
  }
}
