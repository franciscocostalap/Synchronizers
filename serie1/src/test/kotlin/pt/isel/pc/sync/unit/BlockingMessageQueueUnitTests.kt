package pt.isel.pc.sync.unit

import junit.framework.TestCase.*
import org.junit.Test
import pt.isel.pc.sync.BlockingMessageQueue
import java.lang.Thread.sleep
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class BlockingMessageQueueUnitTests {

    @Test
    fun `simple enqueue and dequeue`() {
        val queue = BlockingMessageQueue<Int>(1)
        var value: Int? = null
        var success = false
        val t1 = Thread {
            success = queue.tryEnqueue(listOf(1), Duration.INFINITE)
        }

        val t2 = Thread {
            value = queue.tryDequeue(Duration.INFINITE)
        }

        t1.start()
        t2.start()
        t1.join(2000)
        t2.join(2000)

        assertEquals(1, value)
        assertNull(queue.tryDequeue(10.toDuration(DurationUnit.MILLISECONDS)))
        assertTrue(success)
    }

    @Test
    fun `simple enqueue and dequeue with timeout`() {
        val queue = BlockingMessageQueue<Int>(1)
        var value: Int? = null
        var success = false
        val t1 = Thread {
            success = queue.tryEnqueue(listOf(1), 100.toDuration(DurationUnit.MILLISECONDS))
        }

        val t2 = Thread {
            value = queue.tryDequeue(100.toDuration(DurationUnit.MILLISECONDS))
        }

        t1.start()
        t2.start()
        t1.join(200)
        t2.join(200)

        assertEquals(1, value)
        assertNull(queue.tryDequeue(100.toDuration(DurationUnit.MILLISECONDS)))
        assertTrue(success)
    }

    @Test
    fun `simple enqueue and dequeue with timeout and multiple messages`() {
        val queue = BlockingMessageQueue<Int>(2)
        var value: Int? = null
        var success = false
        val t1 = Thread {
            success = queue.tryEnqueue(listOf(1, 2), 10.toDuration(DurationUnit.MILLISECONDS))
        }

        val t2 = Thread {
            value = queue.tryDequeue(10.toDuration(DurationUnit.MILLISECONDS))
        }

        t1.start()
        t2.start()
        t1.join()
        t2.join()

        assertEquals(1, value)
        assertEquals(2, queue.tryDequeue(10.toDuration(DurationUnit.MILLISECONDS)))
        assertTrue(success)
    }

    @Test
    fun `try to enqueue more than the capacity is not allowed`() {
        val queue = BlockingMessageQueue<Int>(1)
        assertFailsWith<IllegalArgumentException> {
            queue.tryEnqueue(listOf(1, 2), Duration.INFINITE)
        }
    }


    @Test
    fun `simple interrupt test for enqueue`(){
        // Interrupt a thread and catch InterruptedException assigning it to a variable
        val queue = BlockingMessageQueue<Int>(1)
        var success = false
        val t1 = Thread {
            try {
                // Enqueue once to block second enqueue and permit interruption
                queue.tryEnqueue(listOf(1), Duration.INFINITE)
                // Enqueue second time to block and wait for interruption
                queue.tryEnqueue(listOf(1), Duration.INFINITE)
            } catch (e: InterruptedException) {
                success = true
            }
        }

        t1.start()
        t1.interrupt()
        t1.join()

        assertTrue(success)
    }

    @Test
    fun `simple interrupt test for dequeue`(){
        // Interrupt a thread and catch InterruptedException assigning it to a variable
        val queue = BlockingMessageQueue<Int>(1)
        var success = false
        val t1 = Thread {
            try {
                // Dequeue without messages to block and wait for interruption
                queue.tryDequeue(Duration.INFINITE)
            } catch (e: InterruptedException) {
                success = true
            }
        }

        t1.start()
        t1.interrupt()
        t1.join()

        assertTrue(success)
    }

    @Test
    fun `force a fastDeliver enqueue`(){
        val queue = BlockingMessageQueue<Int>(1)
        var success = false
        var value: Int? = null
        val t1 = Thread {
            value = queue.tryDequeue(2.seconds) // Blocks waiting for enqueue
        }
        val t2 = Thread {
            sleep(1000) // Ensure t1 runs first
            success = queue.tryEnqueue(listOf(1), 2.seconds)
        }

        t1.start()
        t2.start()
        t1.join()
        t2.join()

        assertEquals(1, value)
        assertTrue(success)
    }




}