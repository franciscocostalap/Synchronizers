package parameterized

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import pt.isel.pc.sync.CounterModulo
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.fail

@RunWith(Parameterized::class)
class CounterModuloParameterizedTests(val modulo: Int, val nThreads: Int) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Modulo {0}, Threads {1}")
        fun data(): Collection<Array<Any>> {

            val maxModulo = 1000
            val MAX_CORES = Runtime.getRuntime().availableProcessors()

            val data = mutableListOf<Array<Any>>()

            for (i in 1..maxModulo) {
                for(cores in 1..MAX_CORES) {
                    data.add(arrayOf(i, cores))
                }
            }

            return data
        }
    }

    private val ITERATIONS = 1000

    @Test
    fun `multi thread counter modulo test `() {
        val counter = CounterModulo(moduloValue = modulo)
        val iterationCounter = AtomicInteger(nThreads * ITERATIONS) // Asserts number of iterations made
        val pool = Executors.newFixedThreadPool(nThreads)

        (1..nThreads/2).map {
            pool.execute{
                repeat(ITERATIONS) {
                    iterationCounter.safeDecrement()
                    counter.increment()
                }
            }
        }

        (1..nThreads/2).map {
            pool.execute{
                repeat(ITERATIONS) {
                    iterationCounter.safeIncrement()
                    counter.decrement()
                }
            }
        }

        pool.shutdown()
        pool.awaitTermination(10000, TimeUnit.MILLISECONDS)

        if(!pool.isTerminated) fail("Thread pool hasn't shutdown")

        // Asserts all iterations were done
        assertEquals(iterationCounter.get(), nThreads * ITERATIONS)

        /**
         *  If all increases and decreases took place, the counter should be 0
         *  as the same amount of increases and decreases are made
         */
        assertEquals(0, counter.value)
    }

}

private fun AtomicInteger.safeIncrement() {
    do {
        val obs = get()
    } while (!compareAndSet(obs, obs + 1))
}

private fun AtomicInteger.safeDecrement() {
    do {
        val observed = get()
    } while (!compareAndSet(observed, observed - 1))
}