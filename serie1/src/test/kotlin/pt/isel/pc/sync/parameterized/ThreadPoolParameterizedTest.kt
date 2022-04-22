package pt.isel.pc.sync.parameterized

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import pt.isel.pc.sync.BlockingMessageQueue
import pt.isel.pc.sync.ThreadPoolExecutor
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds


@RunWith(Parameterized::class)
class ThreadPoolParameterizedTest(val MAX_POOL_SIZE: Int, val NUMBER_OF_TASKS: Int, KEEP_ALIVE: Int) {

    private val KEEP_ALIVE_TIME = KEEP_ALIVE.seconds

    companion object{

        /**
         * This method is used to generate the test cases.
         * The test is ran with the following parameters:
         * MAX_POOL_SIZE, NUMBER_OF_TASKS, KEEP_ALIVE
         *
         * This method sets up the parameters.
         */
        @JvmStatic
        @Parameterized.Parameters(name = "MaxWorkers={0}, Tasks={1}, KeepAliveSeconds={2}")
        fun setUp(): Collection<Array<Any>> {

            val maxCores = Runtime.getRuntime().availableProcessors() // Get number of cores
            val maxTasks = maxCores * 10  // tasks per number of threads

            val data = mutableListOf<Array<Any>>()

            for(threads in 1..maxCores){
                for(tasks in 1..maxTasks){
                    data.add(arrayOf(threads, tasks, threads))
                }
            }

            return data
        }
    }

    /**
     * Enqueues [NUMBER_OF_TASKS] messages to a queue using a thread pool with [MAX_POOL_SIZE] threads.
     * The thread pool is configured with [KEEP_ALIVE_TIME] and [MAX_POOL_SIZE].
     * The test dequeues the messages enqueued and ensures that the number of messages dequeued is equal to [NUMBER_OF_TASKS].
     * And that the messages are exactly the same as the messages enqueued.
     */
    @Test
    fun `multiple cases using a parameterized test`() {
        val pool = ThreadPoolExecutor(MAX_POOL_SIZE, KEEP_ALIVE_TIME)
        val resultQueue = BlockingMessageQueue<Int>(NUMBER_OF_TASKS)
        val tasks = (1..NUMBER_OF_TASKS).map {
            pool.execute {
                resultQueue.tryEnqueue(listOf(it), KEEP_ALIVE_TIME)
            }
        }

        pool.shutdown()
        pool.awaitTermination(KEEP_ALIVE_TIME)

        val resultSet = mutableSetOf<Int>()
        tasks.forEach{ _ ->
            val result = resultQueue.tryDequeue(KEEP_ALIVE_TIME)
            result?.let { resultSet.add(it) }
        }
        val expected = (1..NUMBER_OF_TASKS).toSet()

        assertEquals(NUMBER_OF_TASKS, resultSet.size)
        assertEquals(expected, resultSet)
    }




}