package pt.isel.pc.sync.unit

import org.junit.Test
import pt.isel.pc.sync.BlockingMessageQueueFuture
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class BlockingMessageQueueFutureUnitTests {

    @Test
    fun `simple test for blocking future queue`() {
        val queue = BlockingMessageQueueFuture<Int>(2)

        val future = queue.tryDequeue()
        val future2 = queue.tryDequeue()

        assertFalse(future.isDone)
        assertFalse(future2.isDone)

        queue.tryEnqueue(listOf(1, 2), 1.seconds)

        assertTrue(future.isDone)
        assertTrue(future2.isDone)

        assertEquals(1, future.get())
        assertEquals(2, future2.get())

    }

    @Test
    fun `interrupt a thread that's trying to enqueue`(){

        // Interrupt a thread and catch InterruptedException assigning it to a variable
        val queue = BlockingMessageQueueFuture<Int>(1)
        var success = false
        val t1 = Thread {
            try {
                // Enqueue once to block second enqueue and permit interruption
                queue.tryEnqueue(listOf(1), 2.seconds)
                // Enqueue second time to block and wait for interruption
                queue.tryEnqueue(listOf(1), 2.seconds)
            } catch (e: InterruptedException) {
                success = true
            }
        }

        t1.start()
        t1.interrupt()
        t1.join()

        assertTrue(success)
    }


}