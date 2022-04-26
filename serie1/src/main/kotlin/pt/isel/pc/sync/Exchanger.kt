package pt.isel.pc.sync


import pt.isel.pc.utils.awaitUntil
import pt.isel.pc.utils.dueTime
import pt.isel.pc.utils.isPast
import pt.isel.pc.utils.isZero
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

/**

    This synchronizer supports the exchange of information between pairs of threads.
    Threads that use this synchronizer manifest their willingness to initiate an exchange by invoking the exchange method,
    specifying the object they want to deliver to the partner thread (value)
    and the limit duration of the wait for the exchange (timeout)

    Implemented as a lock-based synchronizer using the kernel style approach.
    Makes one exchange at a time.

 */
class Exchanger<T> {

    private val monitor = ReentrantLock()
    private val condition: Condition = monitor.newCondition()

    /**
     * Holder that holds the values making part of the exchange
     */
    private inner class ExchangerHolder(var v1: T?, var v2:T?)
    private var exchangeHolder: ExchangerHolder? = null

    @Throws(InterruptedException::class)
    fun exchange(value: T, timeout: Duration): T?{
        monitor.withLock {

            /**
             * Second thread always takes the fast path
             */
            if(exchangeHolder?.v1 != null){
                val v = exchangeHolder!!.v1
                exchangeHolder!!.v2 = value
                condition.signal()
                exchangeHolder = null
                return v
            }
            if(timeout.isZero) return null

            // Wait path
            val scopeHolder = ExchangerHolder(value, null)
            exchangeHolder = scopeHolder
            val dueTime = timeout.dueTime()

            while(true){
                try {
                    condition awaitUntil dueTime
                    // If the partner thread has already delivered the value, return it
                    if (scopeHolder.v2 != null) {
                        return scopeHolder.v2
                    }
                    if (dueTime.isPast) {
                        // remove waiter if timeout was reached so other exchange can take place
                        exchangeHolder = null
                        return null
                    }
                }catch (e: InterruptedException){
                    if(scopeHolder.v2 != null) {
                        Thread.currentThread().interrupt()
                        return scopeHolder.v2
                    }
                    // remove waiter if the thread was interrupted so other exchange can take place
                    exchangeHolder = null
                    throw e
                }
            }
        }
    }

}






