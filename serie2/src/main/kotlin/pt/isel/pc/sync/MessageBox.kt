package pt.isel.pc.sync

import java.util.concurrent.atomic.AtomicReference

/**
 * MessageBox thread-safe lock free.
 */
class MessageBox<M> {

    // Immutable approach. Downside: Object creation at each CAS even if it fails.
    private class Holder<M>(val msg: M, val lives: Int) // Can't be data class. Object identity needed.
    private val holder = AtomicReference<Holder<M>?>(null)

    fun publish(msg: M, lives: Int) {
        holder.set(Holder(msg, lives)) // Always overwrites.
    }

    fun tryConsume(): M?{
        do{
            val obsHolder = holder.get()
            if(obsHolder == null || obsHolder.lives == 0) return null
            if(holder.compareAndSet(obsHolder, Holder(obsHolder.msg, obsHolder.lives - 1)))
                return obsHolder.msg
        }while (true)
    }

}

