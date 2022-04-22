package pt.isel.pc.sync.unit

import org.junit.Test
import pt.isel.pc.sync.ThreadPoolExecutor
import java.util.concurrent.RejectedExecutionException
import kotlin.concurrent.thread
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

class ThreadPoolExecutorUnitTests{

    @Test
    fun `trying to create a thread pool with a negative number of threads throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            ThreadPoolExecutor(-1, 1.seconds)
        }
    }

    @Test
    fun `try to create a thread pool with 0 threads throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            ThreadPoolExecutor(0, 1.seconds)
        }
    }

    @Test
    fun `try to create a thread pool with a negative duration throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            ThreadPoolExecutor(1, (-1).seconds)
        }
    }

    @Test
    fun `try to create a thread pool with a zero duration throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            ThreadPoolExecutor(1, 0.seconds)
        }
    }

    @Test
    fun `simple test with one thread one task`() {
        val executor = ThreadPoolExecutor(1, 10.seconds)
        var result: Int? = null
        executor.execute {
            result = 42
        }

        executor.shutdown()
        executor.awaitTermination(10.seconds)

        assertNotNull(result)
    }

    @Test
    fun `simple test with one thread two tasks`() {
        val executor = ThreadPoolExecutor(1, 10.seconds)
        var result1: Int? = null
        var result2: Int? = null
        executor.execute {
            result1 = 42
        }
        executor.execute {
            result2 = 43
        }

        executor.shutdown()
        executor.awaitTermination(10.seconds)

        assertNotNull(result1)
        assertNotNull(result2)
    }

    @Test
    fun `simple test with two threads one task`() {
        val executor = ThreadPoolExecutor(2, 10.seconds)
        var result: Int? = null
        executor.execute {
            result = 42
        }

        executor.shutdown()
        executor.awaitTermination(10.seconds)

        assertNotNull(result)
    }

    @Test
    fun `simple test with two threads two tasks`() {
        val executor = ThreadPoolExecutor(2, 2.seconds)
        var result1: Int? = null
        var result2: Int? = null
        executor.execute {
            result1 = 42
        }
        executor.execute {
            result2 = 43
        }

        executor.shutdown()
        executor.awaitTermination(2.seconds)

        assertNotNull(result1)
        assertNotNull(result2)
    }

    @Test
    fun `simple test with two threads two tasks and one task in the queue`() {
        val executor = ThreadPoolExecutor(2, 10.seconds)
        var result1: Int? = null
        var result2: Int? = null
        var result3: Int? = null
        executor.execute {
            Thread.sleep(1000)
            result1 = 42
        }
        executor.execute {
            Thread.sleep(1000)
            result2 = 43
        }
        executor.execute {
            result3 = 44
        }

        executor.shutdown()
        executor.awaitTermination(10.seconds)

        assertEquals(42, result1)
        assertEquals(43, result2)
        assertEquals(44, result3)
    }

    @Test
    fun `interrupt in awaitTermination`(){
        val executor = ThreadPoolExecutor(2, 10.seconds)
        var success = false
        executor.execute {
            // Ensure thread is running for a long time to ensure that the shutdown takes a long time so awaitTermination blocks
            Thread.sleep(20000)
        }

        executor.shutdown()
        val t = thread {
            try {
                executor.awaitTermination(20.seconds)
            } catch (e: InterruptedException) {
                success = true
            }
        }

        t.interrupt()
        t.join(1000)
        if(t.isAlive) fail("Thread shouldn't be alive")
        assertTrue(success)
    }

    @Test
    fun `await termination leaves with timeout`(){
        val executor = ThreadPoolExecutor(2, 10.seconds)
        var result = false
        executor.execute {
            // Ensure thread is running for a long time to ensure that the shutdown takes a long time so awaitTermination blocks
            Thread.sleep(20000)
            result = true
        }

        executor.shutdown()
        executor.awaitTermination(1.seconds)

        assertFalse(result)
    }

    @Test
    fun `try to execute after shutdown gives RejectedExecutionException`(){
        val executor = ThreadPoolExecutor(2, 2.seconds)
        executor.shutdown()
        executor.awaitTermination(2.seconds)

        assertFailsWith<RejectedExecutionException> {
            executor.execute {
                Thread.sleep(1000)
            }
        }

    }

    @Test
    fun `shutdown immediately terminates the pool if there is no work being executed`(){
        val executor = ThreadPoolExecutor(2, 2.seconds)
        executor.shutdown()
        assertTrue(executor.awaitTermination(2.seconds))
    }

    @Test
    fun `await termination after ensuring the executor is shutdown returns true`(){
        val executor = ThreadPoolExecutor(2, 2.seconds)
        executor.shutdown()
        executor.awaitTermination(2.seconds)

        assertTrue(executor.awaitTermination(2.seconds))
    }

    @Test
    fun `trying to await termination without calling shutdown throws IllegalStateException`(){
        val executor = ThreadPoolExecutor(2, 2.seconds)
        assertFailsWith<IllegalStateException> {
            executor.awaitTermination(2.seconds)
        }
    }

    @Test
    fun `shutdown after shutdown throws IllegalStateException`(){
        val executor = ThreadPoolExecutor(2, 2.seconds)
        executor.shutdown() // shutdown immediately

        assertFailsWith<IllegalStateException> {
            executor.shutdown()
        }
    }

    @Test
    fun `throwing exception in execute does not kill a thread`(){
        val executor = ThreadPoolExecutor(1, 10.seconds)
        var thread1: Thread? = null
        var thread2: Thread? = null
        executor.execute {
            thread1 = Thread.currentThread()
            throw Exception("Test")
        }
        executor.execute {
            thread2 = Thread.currentThread()
        }

        executor.shutdown()
        executor.awaitTermination(2.seconds)

        assertSame(thread1, thread2)
    }

    @Test
    fun `thread is killed by timeout`(){
        val executor = ThreadPoolExecutor(1, 2.seconds)
        var thread1: Thread? = null
        var thread2: Thread? = null
        executor.execute {
            thread1 = Thread.currentThread()
        }
        Thread.sleep(3000)
        executor.execute {
            thread2 = Thread.currentThread()
        }

        executor.shutdown()
        executor.awaitTermination(2.seconds)

        assertNotSame(thread1, thread2)
    }

    @Test
    fun `thread is killed by timeout even when exception is thrown`(){
        val executor = ThreadPoolExecutor(1, 2.seconds)
        var thread1: Thread? = null
        var thread2: Thread? = null
        executor.execute {
            thread1 = Thread.currentThread()
            throw Exception("Test")
        }
        Thread.sleep(3000)
        executor.execute {
            thread2 = Thread.currentThread()
        }

        executor.shutdown()
        executor.awaitTermination(2.seconds)

        assertNotSame(thread1, thread2)
    }

}