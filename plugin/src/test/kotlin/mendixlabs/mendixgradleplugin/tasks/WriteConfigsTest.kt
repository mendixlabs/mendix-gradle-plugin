package mendixlabs.mendixgradleplugin.tasks

import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.test.*

class WriteConfigsTest {

    private fun createTask(): WriteConfigs {
        val project = ProjectBuilder.builder().build()
        return project.tasks.create("testWriteConfigs", WriteConfigs::class.java)
    }

    @Test
    fun `should parse configurations from App json using streaming`() {
        val task = createTask()
        val appJsonFile = File(javaClass.classLoader.getResource("App-stripped.json")!!.file)

        assertTrue(appJsonFile.exists(), "App.json should exist in test resources")

        val configurations = task.readProjectSettingsFromMpr(appJsonFile)

        assertTrue(configurations.isNotEmpty(), "Should find at least one configuration")
        assertEquals(configurations.size, 2);
        configurations.forEach { config ->
            assertNotNull(config.name, "Configuration should have a name")
            assertTrue(config.runtimePortNumber > 0, "Runtime port should be positive")
            assertTrue(config.adminPortNumber > 0, "Admin port should be positive")
            assertNotNull(config.databaseType, "Database type should not be null")
            assertNotNull(config.applicationRootUrl, "Application root URL should not be null")
        }
    }

    @Test
    fun `should find Default configuration with correct properties`() {
        val task = createTask()
        val appJsonFile = File(javaClass.classLoader.getResource("App-stripped.json")!!.file)

        val configurations = task.readProjectSettingsFromMpr(appJsonFile)

        val defaultConfig = configurations.find { it.name == "Default" }
        assertNotNull(defaultConfig, "Should find 'Default' configuration")
        assertEquals("Hsqldb", defaultConfig.databaseType)
        assertEquals(8080, defaultConfig.runtimePortNumber)
        assertEquals(8090, defaultConfig.adminPortNumber)
        assertFalse(defaultConfig.runtimePortOnlyLocal)
        assertTrue(defaultConfig.adminPortOnlyLocal)
    }

}
