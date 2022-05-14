package unit

import org.junit.Test
import pt.isel.pc.sync.CounterModulo
import pt.isel.pc.sync.LazyLockFree
import kotlin.concurrent.thread
import kotlin.test.assertEquals


class LazyUnitTests {


    @Test
    fun `single thread lazy works`(){
        var count = 1
        val lazy = LazyLockFree { count-- }


        assertEquals(1, lazy.value)
        assertEquals(0, count)

        // Getting the value again should not trigger the computation
        // Count should remain at 0
        assertEquals(1, lazy.value)
        assertEquals(0, count)

    }

    @Test
    fun `two threads getting lazy should not trigger the computation twice`(){
        val counter = CounterModulo(2)
        var v1: Int? = null
        var v2: Int? = null

        val lazy = LazyLockFree {
            counter.increment()
            10
        }

        val t1 = thread {
            v1 = lazy.value
        }

        val t2 = thread {
            v2 = lazy.value
        }

        t1.join()
        t2.join()

        assertEquals(1, counter.value)
        assertEquals(10, v1)
        assertEquals(10, v2)
    }

    @Test
    fun `computation takes time so the second thread waits for it's completion without trying to compute it again`(){

        val counter = CounterModulo(2) // Here to ensure that the computation is executed only once
        var v1: Int? = null
        var v2: Int? = null

        val lazy = LazyLockFree {
            counter.increment()
            Thread.sleep(1000) // takes more than 1 second to complete
            10
        }

        val t1 = thread {
            v1 = lazy.value
        }

        val t2 = thread {
            v2 = lazy.value
        }

        t1.join()
        t2.join()

        assertEquals(1, counter.value)
        assertEquals(10, v1)
        assertEquals(10, v2)
    }

}