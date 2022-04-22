package pt.isel.pc.sync.unit

import org.junit.Test
import pt.isel.pc.sync.FutureImpl
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.concurrent.thread
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlin.time.Duration.Companion.seconds as seconds

class FutureImplUnitTest {

    private fun <T> simulateTask(
        supplier: () -> T,
        failure: () -> Unit = {},
        fail: Boolean,
        cancel: Boolean = false,
        taskDuration: Duration = 200.toDuration(DurationUnit.MILLISECONDS)
    ): FutureImpl<T> {
        val future = FutureImpl<T>()
        thread {
            Thread.sleep(taskDuration.inWholeMilliseconds)
            try {
                if(fail)
                    failure()
                else if(cancel)
                    future.cancel(false)
                else
                    future.setSuccess(supplier())
            }catch (e: Throwable){
                future.setFailure(e)
            }
        }
        return future
    }

    @Test
    fun `simple future test`(){
        val future = simulateTask(supplier={ "Hello" }, fail=false)
        assertEquals("Hello", future.get())
    }

    @Test
    fun `future failure test`(){
        val future = simulateTask<String>(supplier={ "Hello" }, failure={throw IllegalStateException()}, fail=true)
        var result: String? = null
        assertFailsWith<IllegalStateException> {
            result = future.get()
        }
        assertNull(result)
    }

    @Test
    fun `future timeout test`(){
        val future = simulateTask<String>(supplier={ "Hello" }, fail=false)
        var result: String? = null
        assertFailsWith<TimeoutException> {
            result = future.get(100, TimeUnit.MILLISECONDS)
        }
        assertNull(result)
    }

    @Test
    fun `future cancellation test`(){
        val future = simulateTask<String>(supplier={ "Hello" }, fail=false, cancel=true, taskDuration=100.toDuration(DurationUnit.MILLISECONDS))
        var result: String? = null
        assertFailsWith<CancellationException> {
            result = future.get(300, TimeUnit.MILLISECONDS)
        }
        assertNull(result)
    }


    @Test
    fun `future get with zero timeout gives TimeoutException if it does not deliver instantly`(){
        val future = simulateTask<String>(supplier={ "Hello" }, fail=false, cancel=false, taskDuration=10.seconds)
        var result: String? = null
        assertFailsWith<TimeoutException> {
            result = future.get(0, TimeUnit.MILLISECONDS)
        }
        assertNull(result)
    }

    @Test
    fun `try to cancel a future that is already cancelled returns false`(){
        FutureImpl<String>().apply {
            cancel(false)
            assertFalse(cancel(false))
        }
    }

    @Test
    fun `interrupting the thread that tries to get the future throws InterruptedException`(){

        val future =
            simulateTask<String>(supplier = { "Hello" }, fail = false, cancel = false, taskDuration = 10.seconds)
        var success = false
        val t = thread {
            try {
                future.get()
            }catch (e: Throwable){
                success = true
            }
        }
        t.interrupt()
        t.join()
        assertTrue(success)
    }

    @Test
    fun `get leaves in fast path`(){
        val future = FutureImpl<String>()
        thread {
            future.setSuccess("Hello")
        }.join()
        assertEquals("Hello", future.get())
    }

    @Test
    fun `multiple thread getting from the future get the same value`(){
        val future = FutureImpl<String>()
        var v1: String? = null
        var v2: String? = null
        future.setSuccess("Hello")
        val t1 = thread {
            v1 = future.get()
        }
        val t2 = thread {
            v2 = future.get()
        }
        t1.join()
        t2.join()
        assertEquals("Hello", v1)
        assertEquals("Hello", v2)
    }

    @Test
    fun `isCancelled returns true if the future was cancelled and false if not`(){
        val future = FutureImpl<String>()
        future.cancel(false)
        assertTrue(future.isCancelled)

        val future2 = FutureImpl<String>()
        assertFalse(future2.isCancelled)
    }

    @Test
    fun `isDone returns true if the future is done and false if not`(){
        val future = FutureImpl<String>()
        assertFalse(future.isDone)
        future.setSuccess("Hello")
        assertTrue(future.isDone)
    }

    @Test
    fun `trying to setSuccess on a future that is already done throws IllegalStateException`(){
        val future = FutureImpl<String>()
        future.setSuccess("Hello")
        assertFailsWith<IllegalStateException> {
            future.setSuccess("Hello")
        }
    }

    @Test
    fun `trying to setFailure on a future that is already done throws IllegalStateException`(){
        val future = FutureImpl<String>()
        future.setFailure(IllegalStateException("Hello"))
        assertFailsWith<IllegalStateException> {
            future.setFailure(IllegalStateException("Hello"))
        }
    }


}