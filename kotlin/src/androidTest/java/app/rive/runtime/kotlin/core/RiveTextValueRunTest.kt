package app.rive.runtime.kotlin.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.errors.RiveException
import app.rive.runtime.kotlin.core.errors.TextValueRunException
import app.rive.runtime.kotlin.test.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class RiveTextValueRunTest {
    private val testUtils = TestUtils()
    private val appContext = testUtils.context
    private lateinit var file: File
    private lateinit var nestePathFile: File
    private lateinit var mockView: RiveAnimationView

    @Before
    fun init() {
        file = File(
            appContext.resources.openRawResource(R.raw.hello_world_text).readBytes()
        )
        nestePathFile= File(
            appContext.resources.openRawResource(R.raw.runtime_nested_text_runs).readBytes()
        )

        mockView = TestUtils.MockRiveAnimationView(appContext)
    }

    @Test
    fun read_and_update_text_run() {
        var artboard = file.firstArtboard;
        assertEquals(artboard.dependencies.count(), 0)

        val textRun = artboard.textRun("name")

        // Confirm original value
        assertEquals("world", textRun.text)
        assertEquals("world", artboard.getTextRunValue("name"))

        var updateValue = "username"

        // Setting the text run directly
        textRun.text = updateValue
        assertEquals(updateValue, textRun.text)
        assertEquals(updateValue, artboard.getTextRunValue("name"))

        // Setting through the helper method
        updateValue = "new value"
        artboard.setTextRunValue("name",updateValue)
        assertEquals(updateValue, textRun.text)
        assertEquals(updateValue, artboard.getTextRunValue("name"))

        // Only accessing .textRun should add to the dependencies.
        assertEquals(artboard.dependencies.count(), 1)
    }

    @Test(expected = TextValueRunException::class)
    fun read_non_existing_text_run() {
        file.firstArtboard.textRun("wrong-name")
    }

    @Test
    fun read_and_update_text_run_at_path() {
        val artboard = nestePathFile.firstArtboard;
        assertEquals(artboard.dependencies.count(), 0)

        nestedTextRunHelper(artboard, "ArtboardBRun", "ArtboardB-1","Artboard B Run", "ArtboardB-1" )
        nestedTextRunHelper(artboard, "ArtboardBRun", "ArtboardB-2","Artboard B Run", "ArtboardB-2" )
        nestedTextRunHelper(artboard, "ArtboardCRun", "ArtboardB-1/ArtboardC-1","Artboard C Run", "ArtboardB-1/C-1" )
        nestedTextRunHelper(artboard, "ArtboardCRun", "ArtboardB-1/ArtboardC-2","Artboard C Run", "ArtboardB-1/C-2" )
        nestedTextRunHelper(artboard, "ArtboardCRun", "ArtboardB-2/ArtboardC-1","Artboard C Run", "ArtboardB-2/C-1" )
        nestedTextRunHelper(artboard, "ArtboardCRun", "ArtboardB-2/ArtboardC-2","Artboard C Run", "ArtboardB-2/C-2" )

        // Only accessing the textRun directly should increase the dependency.
        // Calling getTextRunValue and setTextRunValue should not.
        assertEquals(artboard.dependencies.count(), 6)
    }

    private fun nestedTextRunHelper(artboard: Artboard, name: String, path: String, originalValue: String, updatedValue: String) {
        // Get the text value run. This should increase the dependency count
        val textRun = artboard.textRun(name, path = path)

        // Assert the original value is correct
        assertEquals(originalValue, textRun.text)
        assertEquals(originalValue, artboard.getTextRunValue(name, path = path))

        // Update the `textRun` reference directly
        textRun.text = updatedValue
        assertEquals(updatedValue, textRun.text)
        assertEquals(updatedValue, artboard.getTextRunValue(name, path = path))

        // Update the text run back to the original value through the helper method
        artboard.setTextRunValue(name, originalValue, path)
        assertEquals(originalValue, textRun.text)
        assertEquals(originalValue, artboard.getTextRunValue(name, path = path))
    }

    @Test
    fun viewSetGetTextRun() {
        UiThreadStatement.runOnUiThread {
            mockView.setRiveResource(R.raw.hello_world_text, autoplay = false)
            mockView.play(listOf("State Machine 1"), areStateMachines = true)
            assertEquals(true, mockView.isPlaying)

            assertEquals(mockView.controller.activeArtboard?.dependencies?.count(), 1)
            val textValue = mockView.getTextRunValue("name")
            assertEquals(textValue, "world")

            var newValue = "New Value";
            mockView.setTextRunValue("name", newValue)
            val textValueUpdated = mockView.getTextRunValue("name")
            assertEquals(textValueUpdated, newValue)

            assertEquals(mockView.controller.activeArtboard?.dependencies?.count(), 1)

            // Test for throwing an error when giving a wrong text run name
            try {
                mockView.setTextRunValue("non_existent_text_run", "Some Value")
                fail("Expected an exception to be thrown")
            } catch (e: Exception) {
                assertTrue(e is RiveException)
                assertTrue(e.message?.contains("No Rive TextValueRun found") == true)
            }

            // Test for throwing an error when giving a wrong text run name for a nested artboard
            try {
                mockView.setTextRunValue("non_existent_text_run", "Some Value", "ArtboardB-1")
                fail("Expected an exception to be thrown")
            } catch (e: Exception) {
                assertTrue(e is RiveException)
                assertTrue(e.message?.contains("No Rive TextValueRun found") == true)
            }
        }
    }
    @Test
    fun viewSetGetNestedTextRun() {
        UiThreadStatement.runOnUiThread {
            mockView.setRiveResource(R.raw.runtime_nested_text_runs, autoplay = false)
            mockView.play(listOf("State Machine 1"), areStateMachines = true)
            assertEquals(true, mockView.isPlaying)

            assertEquals(mockView.controller.activeArtboard?.dependencies?.count(), 1)
            val textValue = mockView.getTextRunValue("ArtboardBRun", "ArtboardB-1")
            assertEquals(textValue, "Artboard B Run")

            var newValue = "New Value";
            mockView.setTextRunValue("ArtboardBRun", newValue, "ArtboardB-1" )
            val textValueUpdated = mockView.getTextRunValue("ArtboardBRun", "ArtboardB-1")
            assertEquals(textValueUpdated, newValue)

            assertEquals(mockView.controller.activeArtboard?.dependencies?.count(), 1)

            // Test for throwing an error when giving a wrong text run name for a nested artboard
            try {
                mockView.setTextRunValue("non_existent_text_run", "Some Value", "ArtboardB-1")
                fail("Expected an exception to be thrown")
            } catch (e: Exception) {
                assertTrue(e is RiveException)
                assertTrue(e.message?.contains("No Rive TextValueRun found") == true)
            }

            // Test for throwing an error when giving a wrong path for a nested artboard
            try {
                mockView.setTextRunValue("ArtboardBRun", "Some Value", "non_existent_path")
                fail("Expected an exception to be thrown")
            } catch (e: Exception) {
                assertTrue(e is RiveException)
                assertTrue(e.message?.contains("No Rive TextValueRun found") == true)
            }
        }
    }
}