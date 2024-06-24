package com.nosuchdevice

import com.bitwig.extension.api.PlatformType
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList
import com.bitwig.extension.controller.ControllerExtension
import com.bitwig.extension.controller.ControllerExtensionDefinition
import com.bitwig.extension.controller.api.ControllerHost
import java.util.*

class OxiOneExtensionDefinition : ControllerExtensionDefinition() {
    override fun getName(): String = "OXI_ONE"

    override fun getAuthor(): String = "thorinside"

    override fun getVersion(): String = "0.0.1"

    override fun getId(): UUID = UUID.fromString("E785ACAE-83BA-4A83-A74F-3527A9F3911B")

    override fun getRequiredAPIVersion(): Int = 18

    override fun getHardwareVendor(): String = "OXI"

    override fun getHardwareModel(): String = "ONE"

    override fun getNumMidiInPorts(): Int = 1

    override fun getNumMidiOutPorts(): Int = 1

    override fun shouldFailOnDeprecatedUse(): Boolean {
        return true
    }

    override fun listAutoDetectionMidiPortNames(
            list: AutoDetectionMidiPortNamesList,
            platformType: PlatformType
    ) {
        when (platformType) {
            PlatformType.WINDOWS -> {
                // TODO: Set the correct names of the ports for auto detection on Windows platform
                // here
                // and uncomment this when port names are correct.
                // list.add(new String[]{"Input Port 0"}, new String[]{"Output Port 0"});
            }
            PlatformType.MAC -> {
                list.add(arrayOf("OXI ONE Port 1"), arrayOf("OXI ONE Port 1"))
            }
            PlatformType.LINUX -> {
                // TODO: Set the correct names of the ports for auto detection on Windows platform
                // here
                // and uncomment this when port names are correct.
                // list.add(new String[]{"Input Port 0"}, new String[]{"Output Port 0"});
            }
        }
    }

    override fun createInstance(host: ControllerHost): ControllerExtension {
        return OxiOneExtension(this, host)
    }
}
