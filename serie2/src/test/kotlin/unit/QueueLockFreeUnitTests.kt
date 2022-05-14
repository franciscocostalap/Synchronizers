package unit

import org.junit.Test
import pt.isel.pc.sync.QueueLockFree
import kotlin.test.assertEquals

/**
 *
 */
class QueueLockFreeUnitTests {


    @Test
    fun `simple enqueue and dequeue`(){

        val queue = QueueLockFree<Int>()

        queue.enqueue(1)

        assertEquals(1, queue.take())
    }

    @Test
    fun `take gives null if queue is empty`(){

        val queue = QueueLockFree<String>()
        val expected = "Hello"
        queue.enqueue(expected)

        assertEquals(expected, queue.take())
        assertEquals(null, queue.take())
    }


}