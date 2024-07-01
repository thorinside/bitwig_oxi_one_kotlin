# bitwig_oxi_one_kotlin
An OXI ONE extension written in Kotlin

This extension is based on a pre-release firmware for OXI ONE (4.4.0) and will not work with any other firmware. Join The OXI Discord for the thread where this firmware is being discussed, or wait for some future release.

I am an OXI User, and the brand is not mine. I am just trying to make an extension for myself, and do not wish to support it for other people. If you want to contribute, feel free to add a PR and I'll merge it.

## How to Use This

This Bitwig Extension works for Bitwig 5.1 and highter.

Build it with:

```
$ ./gradlew installBwExtension
```

Then add a controller in Bitwig, find the OXI brand, and add the OXI One Remote. Choose one of the OXI Midi ports.

## Features

This Extension is meant to support the Bitwig Clip launcher. If a track is armed for recording, you can press a pad in the same column as the track you want to make a clip for, and it will start recording after your count-in.

* 8 or more clips per track can be added.
* You can tap a clip to have it queued to lauch playback. Playing clips will be green. Queued clips will blink green.
* A clip being recorded will be red.
* Alternate clip launching, which came out in Bitwig 5, can be accessed by holding shift and holding a clip. When you let go of the clip, you will be returned to the previous clip that was playing.
* Pressing the 'Load' button will toggle between TRACK, DEVICE, and SCENE navigation mode.
* The Green arrow buttons can be used for navigation between tracks, devices, and scenes depending on the mode
* Preessing the Arranger button will open the currently selected Device's Window
* The four encoders can be used to modify macro parameters of a device. Indicators will show which. The screen will also show values in percent as you move the encoders. You can also hold shift to access the next 4 parameters.
* Transport controls do what you would expect. Stop, Record, and Play.

As more features are added, they will be added to a changelist below.
