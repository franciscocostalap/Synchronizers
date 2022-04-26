package pt.isel.pc.sync.parameterized

import mu.KLogger
import mu.KotlinLogging
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import pt.isel.pc.sync.Exchanger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds



@RunWith(Parameterized::class)
class ExchangerParameterizedTest(private val nExchanges: Int, private val synchronized: Boolean) {

    private val logger : KLogger = KotlinLogging.logger {}

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name="Exchanges: {0}, Synchronized: {1}")
        fun setup(): Collection<Array<Any>> {

            val params = mutableListOf<Array<Any>>()
            val MAX_CORES = Runtime.getRuntime().availableProcessors()

            for (exchangers in 4..MAX_CORES*20 step 2 )
                for (sync in listOf(true, false))
                params.add(arrayOf(exchangers, sync))

            return params
        }

    }

    /**
     * This test is a parameterized test.
     * It is executed for each value of the parameter NEXCHANGES.
     * Tries to make NEXCHANGES exchanges between two threads with and without synchronization, which means that
     *
     * - Synchronized - ensures that the exchange is made in each iteration of the loop t1 trades always with t2 in the iteration.
     *
     * - Unsynchronized - ensures that all values are exchanged correctly.
     *      The values I (a thread) get are the values that are sent from the thread that received my value.
     *      Tests the behaviour of multiple threads trying to exchange at the same time.
     *
     */
    @Test fun `Exchanger parameterized test`(){

        val listOfValues = (1..nExchanges*2).toList()
        val t1Values = listOfValues.subList(0, nExchanges)
        val t2Values = listOfValues.subList(nExchanges, nExchanges*2)

        val exchanger = Exchanger<Int>()
        val mutex = ReentrantLock()
        val exchangers = mutableListOf<Thread>()
        val resultMap = ConcurrentHashMap<Int, Int?>()

        for(i in 0 until nExchanges){
            val thread1 = thread {
                val input = listOfValues[i]
                val message = exchanger.exchange(input, 2.seconds)
                logger.info { "Thread 1 received $message" }
                mutex.withLock {
                    resultMap[input] = message
                }
            }
            val thread2 = thread {
                val input = listOfValues[i + nExchanges]
                val message = exchanger.exchange(input,2.seconds)
                logger.info { "Thread 2 received $message" }
                mutex.withLock {
                    resultMap[input] = message
                }
            }

            if(synchronized){
                thread1.join()
                thread2.join()
            }else{
                exchangers.add(thread1)
                exchangers.add(thread2)
            }
        }

        // Wait for all to Finish
        if(!synchronized) exchangers.forEach { it.join(3000) }

        // Ensure
        t1Values.forEach { sent ->

            assertEquals(sent, resultMap[resultMap[sent]])
            val received = resultMap[sent]
            assertNotNull(received) // Ensure all values from t1 were exchanged

            logger.info("T1: $sent -> $received")

            if(synchronized)
                // Ensures the values received are indeed from the second half of the list
                assertTrue(received > nExchanges)
        }

        t2Values.forEach { sent ->

            assertEquals(sent, resultMap[resultMap[sent]])
            val received = resultMap[sent]
            assertNotNull(received) // Ensure all values from t2 were exchanged

            logger.info("T2: $sent -> $received")

            if(synchronized)
                // Ensures the values received are indeed from the first half of the list
                assertTrue(received <= nExchanges)
        }
    }

}