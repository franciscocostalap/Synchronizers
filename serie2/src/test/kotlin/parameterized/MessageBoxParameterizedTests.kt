package parameterized

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import pt.isel.pc.sync.MessageBox
import java.util.concurrent.Executors
import kotlin.test.assertEquals


@RunWith(Parameterized::class)
class MessageBoxParameterizedTests(val nConsumers: Int, val lives: Int) {

    companion object{

        @JvmStatic
        @Parameterized.Parameters(name = "Consumers = {0}, Lives = {1}")
        fun setup(): Collection<Array<Any>> {

            val data = mutableListOf<Array<Any>>()
            val consumers = Runtime.getRuntime().availableProcessors()
            val maxLives = 1000

            for (nConsumers in 1..consumers) {
                for(lives in 1..maxLives) {
                    data.add(arrayOf(nConsumers, lives))
                }
            }

            return data
        }
    }

    /**
     * One consumer tries to consume all messages.
     */
    @Test
    fun `message box parameterized test`() {
        val messageBox = MessageBox<Int>()
        val consumerPool = Executors.newCachedThreadPool()
        val resultList = mutableListOf<Int?>()
        val publishedMessage = 1000

        messageBox.publish(publishedMessage, lives)

        for (i in 1..nConsumers) {
            consumerPool.execute {
                for (j in 1..lives) {
                    val message = messageBox.tryConsume()
                    synchronized(resultList) {
                        resultList.add(message)
                    }
                }
            }
        }

        val expectedNulls = nConsumers * lives - lives

        consumerPool.shutdown()
        consumerPool.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)

        assertEquals(resultList.size, lives + expectedNulls)
        assertEquals(resultList.count { it == null }, expectedNulls)
        assertEquals(resultList.count { it == publishedMessage }, lives)
    }




}