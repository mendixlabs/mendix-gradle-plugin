package mxw.gradle;

import org.gradle.platform.Architecture
import org.gradle.platform.OperatingSystem
import java.io.File

enum class Arch {
    ARM, INTEL
}

enum class Os {
    LINUX, OSX, WIN
}
class MxTools(val version: String) {

    fun getOs(): Os {
//        org.gradle.platform.OperatingSystem
        return Os.WIN
    }

    fun getArch(): Arch {
        return Arch.ARM
    }

    fun getMxBuildLocation(): File {
        return File("C:/Program Files/Mendix/${version}/modeler/mxbuild.exe")
    }

}
