package unit

import org.junit.Test
import pt.isel.pc.sync.CounterModulo
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CounterModuloUnitTests {


    @Test
    fun `instantiating CounterModulo with modulo 0 throws because modulo is exclusive, excluding 0 there aren't any numbers`() {
        assertFailsWith<IllegalArgumentException> {
            CounterModulo(0)
        }
    }

    @Test
    fun `should return 0 when modulo is 1 because modulo is exclusive`() {

        val counter = CounterModulo(1)

        repeat(1000){
            assertEquals(0, counter.increment())
            assertEquals(0, counter.value)
            assertEquals(0, counter.decrement())
            assertEquals(0, counter.value)
        }

    }

    @Test
    fun `incrementing with modulo 9 and counter at 8 should return 0`() {

        val counter = CounterModulo(9)


        repeat(8) {
            counter.increment() // Increments the counter 8 times
        }

        assertEquals(0, counter.increment())
        assertEquals(0, counter.value)
    }

    @Test
    fun `decrementing with modulo 9 and counter at 0 should return 8`() {
        val counter = CounterModulo(9)

        assertEquals(8, counter.decrement())
        assertEquals(8, counter.value)
    }


}