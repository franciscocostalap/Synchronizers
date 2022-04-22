package pt.isel.pc.sync.parameterized

import mu.KotlinLogging.logger
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import pt.isel.pc.sync.BlockingMessageQueueFuture
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.time.Duration



@RunWith(Parameterized::class)
class BlockingMessageQueueFutureParameterizedTest(private val nWriters: Int, private val nReaders: Int, private val CAPACITY: Int) {

    private val logger = logger {}

    companion object{

        @JvmStatic
        @Parameterized.Parameters(name = "NREADERS={0}, NWRITERS={1}, CAPACITY={2}")
        fun setup(): Collection<Array<Any>>{

            val params = mutableListOf<Array<Any>>()
            val maxCores = Runtime.getRuntime().availableProcessors()

            // Varying the number of readers, writers and capacity
            for(readers in 5..maxCores){
                for(writers in 5..maxCores){
                    for(capacity in 5..maxCores){
                        params.add(arrayOf(readers, writers, capacity))
                    }
                }
            }

            return params
        }

    }

    @Test
    fun blocking_message_future_multiple_send_receive() {
        val listOfLists = listOf(
            listOf(1, 2, 3),
            listOf(4, 5),
            listOf(6),
            listOf(7, 8, 9, 10),
            listOf(11, 12, 13),
            listOf(14, 15, 16, 17),
            listOf(18, 19, 20)
        )


        val resultSize = listOfLists
                .flatten()
                .count()

        val msgQueue = BlockingMessageQueueFuture<Int>(CAPACITY)

        // the result queue is used for multiple readers support
        // we can also use a thread safe set collection but we
        // talk about thread safe collections later
        val resultQueue = BlockingMessageQueueFuture<Int?>(resultSize)

        // this index use is protected by a mutex
        // in oprder to support multiple writers
        var writeInputIndex = 0
        val mutex = ReentrantLock()

        val expectedSet = IntRange(1, 20).toSet()

        val writerThreads = mutableListOf<Thread>()
        val readerThreads = mutableListOf<Thread>()

        repeat(nWriters) {
            val thread = thread {
                logger.info("start writer")
                while (true) {
                    var localIdx = listOfLists.size
                    mutex.withLock {
                        if (writeInputIndex < listOfLists.size) {
                            localIdx = writeInputIndex++
                        }
                    }
                    if (localIdx < listOfLists.size) {
                        val value = listOfLists[localIdx]
                        logger.info("writer try send $value")
                        if (msgQueue.tryEnqueue(value, Duration.INFINITE))
                            logger.info("writer send $value")
                    } else {
                        break
                    }

                }
                logger.info("end writer")
            }
            writerThreads.add(thread)
        }

        repeat(nReaders) {
            logger.info("start reader")
            val thread = thread {
                while (true) {
                    val r =
                        msgQueue.tryDequeue()
                    logger.info("start get result")
                    var msg: Int? = null
                    if(r.isDone){
                        msg = r.get(100, TimeUnit.MILLISECONDS)
                    }else {
                        while (!r.isDone) {
                            msg = r.get(100, TimeUnit.MILLISECONDS)

                        }
                    }
                    logger.info("end get result $msg")
                    if (resultQueue.tryEnqueue(listOf(msg), Duration.INFINITE))
                        logger.info("reader get $msg")
                }
            }
            readerThreads.add(thread)
        }

        // wait for writers termination (with a timeout)
        for (wt in writerThreads) {
            wt.join(200)
            if (wt.isAlive) {
                fail("too much execution time for writer thread")
            }
        }
        // wait for readers termination (with a timeout)
        for (rt in readerThreads) {
            rt.join(200)
            if (rt.isAlive) {
                fail("too much execution time for reader thread")
            }
        }

        // final assertions
        val resultSet = TreeSet<Int>()

        repeat(resultSize) {
            val r = resultQueue.tryDequeue()
            var msg: Int? = null
            if(r.isDone){
                msg = r.get(100, TimeUnit.MILLISECONDS)
                resultSet.add(msg!!)
            }else{
                while(!r.isDone){
                    msg = r.get(100, TimeUnit.MILLISECONDS)
                }
                resultSet.add(msg!!)
            }
        }

        assertEquals(resultSize, resultSet.size)
        assertEquals(expectedSet, resultSet)
    }
}