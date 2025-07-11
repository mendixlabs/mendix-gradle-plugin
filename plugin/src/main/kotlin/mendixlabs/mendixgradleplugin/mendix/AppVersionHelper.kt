package mendixlabs.mendixgradleplugin.mendix

import org.gradle.api.Project
import java.io.File
import java.sql.DriverManager

fun GetAppVersion(mprFile: File): String {
    if (!mprFile.exists()) {
        throw RuntimeException("MPR file can't be found: ${mprFile.name}")
    }

    //
    // Caused by: java.sql.SQLException: No suitable driver found for jdbc:sqlite:C:\project\App.mpr
    //	at java.sql/java.sql.DriverManager.getConnection(DriverManager.java:708)
    //	at java.sql/java.sql.DriverManager.getConnection(DriverManager.java:253)
    //	at mendixlabs.mendixgradleplugin.mendix.AppVersionHelperKt.GetAppVersion(AppVersionHelper.kt:13)
    // 
    // Sometimes Gradle throws an error on loading the MPR file that JDBC driver can't be found.
    // It's unclear what causes this, `gradlew --stop` sometimes resolves the issue, but not always.
    // Could the gradle daemon be interfering with the classpath? To bypass this, we explicitly
    // load the SQLite JDBC driver here.
    //
    Class.forName("org.sqlite.JDBC");
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
