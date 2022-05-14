package unit

import org.junit.Test
import pt.isel.pc.sync.MessageBox
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 *
 */
class MessageBoxUnitTests {


    @Test
    fun `simple message box get message with one life`() {
        val messageBox = MessageBox<String>()

        messageBox.publish("Hello", 1)

        assertEquals("Hello", messageBox.tryConsume())
        assertEquals(null, messageBox.tryConsume())
    }

    @Test
    fun `getting a message without publish gives null`(){
        val messageBox = MessageBox<String>()
        assertEquals(null, messageBox.tryConsume())
    }

    @Test
    fun `publish overrides previous message`(){
        val messageBox = MessageBox<String>()

        messageBox.publish("Hello", 1)
        messageBox.publish("World", 1)

        assertEquals("World", messageBox.tryConsume())
        assertEquals(null, messageBox.tryConsume())
    }


    @Test
    fun `publish a message with 0 lives always gives null`(){
        val messageBox = MessageBox<String>()
        val pool = Executors.newCachedThreadPool()
        val result = mutableListOf<String?>()

        messageBox.publish("Hello", 0)

        repeat(1000){
            pool.execute {
                val value = messageBox.tryConsume()
                synchronized(this){
                    result.add(value)
                }
            }
        }

        pool.shutdown()
        pool.awaitTermination(1, TimeUnit.SECONDS) // Wait for all executes to finish

        assertTrue(result.all { it == null })
    }

}