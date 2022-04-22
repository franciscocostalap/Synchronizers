package pt.isel.pc.sync.unit

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test
import pt.isel.pc.sync.Exchanger
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


class ExchangerUnitTests {

    @Test
    fun `simple exchange without timeout`() {
        val e = Exchanger<Int>()
        var v1: Int? = null
        var v2: Int? = null
        val t1 = Thread {
            v1 = e.exchange(1, Duration.INFINITE)
        }
        val t2 = Thread {
            v2 = e.exchange(2, Duration.INFINITE)
        }
        t1.start()
        t2.start()
        t1.join()
        t2.join()
        assertEquals(2, v1)
        assertEquals(1, v2)
    }

    @Test
    fun `simple exchange with timeout`() {
        val e = Exchanger<Int>()
        var v1: Int? = null
        var v2: Int? = null
        val t1 = Thread {
            v1 = e.exchange(1, Duration.INFINITE)
        }
        val t2 = Thread {
            v2 = e.exchange(2, Duration.INFINITE)
        }
        t1.start()
        t2.start()
        t1.join()
        t2.join()
        assertEquals(2, v1)
        assertEquals(1, v2)
    }

    @Test
    fun `timeout happens for t1 then all get null`() {
        val e = Exchanger<Int>()
        var v1: Int? = null
        var v2: Int? = null
        val t1 = Thread {
            v1 = e.exchange(1, 10.toDuration(DurationUnit.MILLISECONDS))
        }
        val t2 = Thread {
            v2 = e.exchange(2, 10.toDuration(DurationUnit.MILLISECONDS))
        }
        t1.start()
        Thread.sleep(20)
        t2.start()
        t1.join()
        t2.join()
        assertNull(v1)
        assertNull(v2)
    }

    @Test
    fun `timeout happens for t1 but other thread gets in to trade with t2 before t2's timeout`() {
        val e = Exchanger<Int>()
        var v1: Int? = null
        var v2: Int? = null
        var v3: Int? = null
        val t1 = Thread {
            v1 = e.exchange(1, 10.toDuration(DurationUnit.MILLISECONDS))
        }
        val t2 = Thread {
            v2 = e.exchange(2, 10.toDuration(DurationUnit.MILLISECONDS))
        }
        val t3 = Thread {
            v3 = e.exchange(3, 10.toDuration(DurationUnit.MILLISECONDS))
        }

        t1.start()
        Thread.sleep(20)
        t2.start()
        t3.start()
        t1.join()
        t2.join()
        assertNull(v1)
        assertEquals(3, v2)
        assertEquals(2, v3)
    }

    @Test
    fun `timeout happens for t3 after t1 and t2 exchange values`() {
        val e = Exchanger<Int>()
        var v1: Int? = null
        var v2: Int? = null
        var v3: Int? = null
        val t1 = Thread {
            v1 = e.exchange(1, 1000.toDuration(DurationUnit.MILLISECONDS))
        }
        val t2 = Thread {
            v2 = e.exchange(2, 1000.toDuration(DurationUnit.MILLISECONDS))
        }
        val t3 = Thread {
            v3 = e.exchange(3, 1000.toDuration(DurationUnit.MILLISECONDS))
        }
        t1.start()
        t2.start()
        Thread.sleep(10)
        t3.start()
        t1.join()
        t2.join()
        assertNull(v3)
        assertEquals(2, v1)
        assertEquals(1, v2)
    }

    @Test
    fun `interrupting t1 while it's waiting gives interrupted exception`() {
        val exchanger = Exchanger<Int>()
        var e: Exception? = null
        val t1 = Thread {
            try {
                exchanger.exchange(1, Duration.INFINITE)
            } catch (ex: InterruptedException) {
                e = ex
            }
        }
        t1.start()
        t1.interrupt()
        t1.join()
        assertTrue(e is InterruptedException)
    }

    @Test
    fun `interrupting t1 while it is waiting for t2 gives interrupted exception and gives null to t2 after timeout`() {
        val exchanger = Exchanger<Int>()
        var e: Exception? = null
        var v2: Int? = 20 // ensure not null
        val t1 = Thread {
            try {
                exchanger.exchange(1, Duration.INFINITE)
            } catch (ex: InterruptedException) {
                e = ex
            }
        }
        val t2 = Thread {
            v2 = exchanger.exchange(2, 10.toDuration(DurationUnit.MILLISECONDS))
        }

        t1.start()
        t1.interrupt()
        t2.start()
        t1.join()
        t2.join()
        assertTrue(e is InterruptedException)
        assertNull(v2)
    }

    @Test fun `4 threads try to exchange values simultaneously`(){
        val exchanger = Exchanger<String>()
        val values = ConcurrentHashMap<String, String?>()

        val threads = listOf(
            Thread {
                values["t1"] = exchanger.exchange("t1", Duration.INFINITE)
            },
            Thread {
                values["t2"] = exchanger.exchange("t2", Duration.INFINITE)
            },
            Thread {
                values["t3"] = exchanger.exchange("t3", Duration.INFINITE)
            },
            Thread {
                values["t4"] = exchanger.exchange("t4", Duration.INFINITE)
            }
        )

        threads.forEach(Thread::start)
        threads.forEach(Thread::join)

        assertEquals(4, values.size)
        assertEquals(values[values["t1"]], "t1")
        assertEquals(values[values["t2"]], "t2")
        assertEquals(values[values["t3"]], "t3")
        assertEquals(values[values["t4"]], "t4")
    }

    @Test fun `8 threads try to exchange values simultaneously`(){
        val exchanger = Exchanger<String>()
        val values = ConcurrentHashMap<String, String?>()

        val threads = listOf(
            Thread {
                values["t1"] = exchanger.exchange("t1", Duration.INFINITE)
            },
            Thread {
                values["t2"] = exchanger.exchange("t2", Duration.INFINITE)
            },
            Thread {
                values["t3"] = exchanger.exchange("t3", Duration.INFINITE)
            },
            Thread {
                values["t4"] = exchanger.exchange("t4", Duration.INFINITE)
            },
            Thread {
                values["t5"] = exchanger.exchange("t5", Duration.INFINITE)
            },
            Thread {
                values["t6"] = exchanger.exchange("t6", Duration.INFINITE)
            },
            Thread {
                values["t7"] = exchanger.exchange("t7", Duration.INFINITE)
            },
            Thread {
                values["t8"] = exchanger.exchange("t8", Duration.INFINITE)
            }
        )

        threads.forEach(Thread::start)
        threads.forEach(Thread::join)

        assertEquals(8, values.size)
        assertEquals(values[values["t1"]], "t1")
        assertEquals(values[values["t2"]], "t2")
        assertEquals(values[values["t3"]], "t3")
        assertEquals(values[values["t4"]], "t4")
        assertEquals(values[values["t5"]], "t5")
        assertEquals(values[values["t6"]], "t6")
        assertEquals(values[values["t7"]], "t7")
        assertEquals(values[values["t8"]], "t8")
    }

    @Test fun `interrupting t1 does not affect post thread exchanging`(){

        val exchanger = Exchanger<Int>()
        var e: Exception? = null
        var v2: Int? = null
        var v3: Int? = null
        val t1 = Thread {
            try {
                exchanger.exchange(1, Duration.INFINITE)
            } catch (ex: InterruptedException) {
                e = ex
            }
        }
        val t2 = Thread {
            v2 = exchanger.exchange(2, 1000.toDuration(DurationUnit.MILLISECONDS))
        }
        val t3 = Thread {
            v3 = exchanger.exchange(3, 1000.toDuration(DurationUnit.MILLISECONDS))
        }

        t1.start()
        t1.interrupt()
        t2.start()
        t3.start()
        t1.join()
        t2.join()
        t3.join()
        assertTrue(e is InterruptedException)
        assertEquals(3, v2)
        assertEquals(2, v3)
    }


}