package mendixlabs.mendixgradleplugin.mendix

import org.gradle.api.Project
import java.io.File
import java.sql.DriverManager

fun GetAppVersion(mprFile: File): String {
    if (!mprFile.exists()) {
        throw RuntimeException("MPR file can't be found: ${mprFile.name}")
    }

    // Class.forName("org.sqlite.JDBC");
    DriverManager.getConnection("jdbc:sqlite:${mprFile.absolutePath}").use { con ->
        con.createStatement().use { statement ->
            val rs = statement.executeQuery ("SELECT _ProductVersion FROM _MetaData")
            if (rs.next()) {
                return rs.getString("_ProductVersion")
            }
        }
    }

    throw RuntimeException("Version can't be found in MPR file: ${mprFile.name}")
}
