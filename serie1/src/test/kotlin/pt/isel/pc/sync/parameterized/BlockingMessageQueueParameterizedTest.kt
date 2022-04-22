package pt.isel.pc.sync.parameterized

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.fail
import mu.KLogger
import mu.KotlinLogging
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import pt.isel.pc.sync.BlockingMessageQueue
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration



@RunWith(Parameterized::class)
class BlockingMessageQueueParameterizedTest(val NREADERS: Int, val NWRITERS: Int, val CAPACITY: Int){

    private val logger : KLogger = KotlinLogging.logger {}

    companion object{

        @JvmStatic
        @Parameterized.Parameters(name = "NREADERS={0}, NWRITERS={1}, CAPACITY={2}")
        fun setup(): Collection<Array<Any>>{

            val params = mutableListOf<Array<Any>>()

            // Varying the number of readers, writers and capacity
            for(readers in 5..10){
                for(writers in 5..10){
                    for(capacity in 5..10){
                        params.add(arrayOf(readers, writers, capacity))
                    }
                }
            }

            return params
        }

    }


    /**
     * A more complicated test that should be done after the simple tests
     * already succeed
     * In this case we have an arbitrary number of enqueuers and dequeuers
     * the test parametrizes the number of each.
     * Initially it should be parametrized to the most simple situation, NREADERS=NWRITERS=1
     * Varying capacity can test scenarios where enqueuers need to block
     * Note that this code could be refactored to an auxiliary method that
     * could be used to build tests for different scenarios.
     * Something like this:
     *   private fun <T>  exec_bmq_test(nwriters : Int = 1,
     *                                  nreaders : Int = 1,
     *                                  capacity : Int = 1,
     *                                  colSupplier : () -> Collection<T>)
     *                                      : Collection<T>
     */
    @Test
    fun blocking_message_multiple_send_receive() {
        val listOfLists = listOf(
            listOf(1,2,3),
            listOf(4,5),
            listOf(6),
            listOf(7,8,9,10),
            listOf(11, 12, 13),
            listOf(14,15,16,17),
            listOf(18,19,20))


        val RESULT_SIZE =
            listOfLists
                .flatMap { it }
                .count()

        val msgQueue = BlockingMessageQueue<Int>(CAPACITY)

        // the result queue is used for multiple readers support
        // we can also use a thread safe set collection but we
        // talk about thread safe collections later
        val resultQueue = BlockingMessageQueue<Int?>(RESULT_SIZE)

        // this index use is protected by a mutex
        // in oprder to support multiple writers
        var writeInputIndex = 0
        val mutex = ReentrantLock()

        val expectedSet = IntRange(1, 20).toSet()

        val writerThreads = mutableListOf<Thread>()
        val readerThreads = mutableListOf<Thread>()

        repeat(NWRITERS) {
            val thread = thread {
                logger.info("start writer")
                while(true) {
                    var localIdx = listOfLists.size
                    mutex.withLock {
                        if (writeInputIndex <  listOfLists.size) {
                            localIdx = writeInputIndex++
                        }
                    }
                    if (localIdx < listOfLists.size ) {
                        val value = listOfLists.get(localIdx)
                        logger.info("writer try send $value")
                        if (msgQueue.tryEnqueue(value, Duration.INFINITE))
                            logger.info("writer send $value")
                    }
                    else {
                        break
                    }

                }
                logger.info("end writer")
            }
            writerThreads.add(thread)
        }

        repeat(NREADERS) {
            logger.info("start reader}")
            val thread = thread {
                while(true) {
                    val value =
                        msgQueue.tryDequeue(200.toDuration(DurationUnit.MILLISECONDS)) ?: break
                    if (resultQueue.tryEnqueue(listOf(value), Duration.INFINITE))
                        logger.info("reader get $value")
                }
                logger.info("end reader}")
            }
            readerThreads.add(thread)
        }

        // wait for writers termination (with a timeout)
        for(wt in writerThreads) {
            wt.join(300)
            if (wt.isAlive) {
                fail("too much execution time for writer thread")
            }
        }

        // wait for readers termination (with a timeout)
        for(rt in readerThreads) {
            rt.join(300)
            if (rt.isAlive) {
                fail("too much execution time for reader thread")
            }
        }

        // final assertions
        val resultSet = TreeSet<Int>()

        repeat(RESULT_SIZE) {
            val r = resultQueue.tryDequeue(Duration.ZERO)
            if (r == null) fail("failed tryDequeue operation on result queue")
            resultSet.add(r!!)
        }

        assertEquals(RESULT_SIZE, resultSet.size)
        assertEquals(expectedSet, resultSet)
    }
}