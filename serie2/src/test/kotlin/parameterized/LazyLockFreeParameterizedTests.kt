package parameterized

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import pt.isel.pc.sync.LazyLockFree
import pt.isel.pc.sync.MessageBox
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertTrue


@RunWith(Parameterized::class)
class LazyLockFreeParameterizedTests(private val nConsumers: Int){


    companion object{

        /**
         * Sets up the parameters for the test.
         *
         */
        @JvmStatic
        @Parameterized.Parameters(name = "nConsumers = {0}")
        fun setup(): Collection<Array<Any>> {

            val maxConsumers = Runtime.getRuntime().availableProcessors() * 50
            val list = mutableListOf<Array<Any>>()

            for (consumers in 1..maxConsumers){
                list.add(arrayOf(consumers))
            }

            return list
        }

    }

    /**
     * Tests the LazyLockFree class with [nConsumers] consumers trying to get the value.
     *
     * The test is successful if all consumers get the value, the value is the same for all consumers
     * and the value is only computed once
     */
    @Test
    fun `Lazy lock free thread safe parameterized test`(){

        val counter = MessageBox<Int>()
        val message = 0
        counter.publish(message, nConsumers) // Publish a message with nConsumers lives
        val resultList = mutableListOf<Int>() // Ensure all consumers get the value
        val lazyValue = 10

        val lazyObject = LazyLockFree{ // Lazy computation
            counter.tryConsume() // Consume the message
            lazyValue // Return the value
        }

        val pool = Executors.newCachedThreadPool()

        for (i in 1..nConsumers){ // Get the value nConsumers times
            pool.submit {
                val value = lazyObject.value
                synchronized(this){
                    resultList.add(value)
                }
            }
        }

        pool.shutdown()
        pool.awaitTermination(1, java.util.concurrent.TimeUnit.MINUTES) // Sync with the consumers

        // Ensure there are still nConsumers-1 lives in the box.
        // That means the computation was only done once
        for (i in 1 until nConsumers) assertEquals(message, counter.tryConsume())
        assertEquals(null, counter.tryConsume()) // Ensure the box is empty after all lives are consumed

        // Ensure all consumers got the value
        assertEquals(nConsumers, resultList.size)
        assertTrue { resultList.all { it == lazyValue } } // Ensure the value is the same for all consumers

    }


}