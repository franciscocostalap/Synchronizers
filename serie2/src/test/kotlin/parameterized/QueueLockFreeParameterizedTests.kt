package parameterized


import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import pt.isel.pc.sync.QueueLockFree
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.max
import kotlin.test.*


@RunWith(Parameterized::class)
class QueueLockFreeParameterizedTests(val nConsumers: Int, val nProducers: Int) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Consumers={0}, Producers={1}")
        fun setup(): Collection<Array<Any>> {

            val maxConsumers = 50
            val maxProducers = 50

            val testCases = mutableListOf<Array<Any>>()

            for (consumers in 1..maxConsumers) {
                for (producers in 1..maxProducers) {
                    testCases.add(arrayOf(consumers, producers))
                }
            }

            return testCases

        }
    }


    @Test fun `Queue Lock Free parameterized test`() {
        val queue = QueueLockFree<Int>()
        val producersPool = Executors.newCachedThreadPool()
        val values = (1..nProducers).toSet()
        val result = mutableListOf<Int?>()
        val mutex = ReentrantLock()

        for (i in 1..nProducers) {
            producersPool.execute {
                queue.enqueue(i)
            }
        }

        producersPool.shutdown()
        producersPool.awaitTermination(1, TimeUnit.MINUTES)

        val consumersPool = Executors.newCachedThreadPool()
        for (i in 1..nConsumers) {
            consumersPool.execute {
                val value = queue.take()
                mutex.withLock{
                    result.add(value)
                }
            }
        }

        consumersPool.shutdown()
        consumersPool.awaitTermination(1, TimeUnit.MINUTES)

        val nNulls = max(0, nConsumers - nProducers)

        if(nProducers > nConsumers){
            // When nProducers > nConsumers, there aren't enough takes to fill the result list
            val valuesLeftInQueue = max(0, nProducers - nConsumers)
            repeat(valuesLeftInQueue){
                result.add(queue.take()) // add the remaining values for assertions
            }
        }else if (nProducers < nConsumers){
            // Assert consumer threads have not taken more values than there were in the queue
            assertEquals(nNulls, result.count { it == null })
        }else{
            // When nProducers == nConsumers, there are no nulls in the result list
            assertTrue(result.all { it != null })
        }

        assertNull(queue.take()) // Here the queue should be empty

        assertEquals(values.size + nNulls, result.size)
        assertEquals(result.filterNotNull().sorted(), values.toList())
    }







}