package pt.isel.pc.sync

import pt.isel.pc.utils.*
import java.util.LinkedList
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

/**

    Lock Based Synchronizer that supports communication between producing and
    consumer threads through messages of the generic type [T]. The communication must use the FIFO criterion (first in first
    out): given two messages placed in a queue, the first to be delivered to a consumer must be the first
    in the queue; if there are two or more consumers waiting for a message, the first to
    the first to have his request satisfied is the one who has been waiting the longest. The maximum number of
    in the queue is determined by the [capacity]

    Implemented using monitor style for enqueues and kernel style for dequeues.

 **/
class BlockingMessageQueue<T>(private val capacity: Int) {

    private val monitor = ReentrantLock()
    // Condition for enqueues
    private val hasCapacity = monitor.newCondition()


    private val messageQueue = LinkedList<T>()

    private class Consumer<T>(var value: T?=null, val condition: Condition)
    private val pendingDequeues = LinkedList<Consumer<T>>()

    private val currentCapacity
        get() = capacity - messageQueue.size


    @Throws(InterruptedException::class)
    fun tryEnqueue(messages: List<T>, timeout: Duration): Boolean {
        require(messages.size <= capacity){"The number of messages to be enqueued is greater than the capacity of the queue."}
        require(messages.isNotEmpty()){"The list of messages to be enqueued is empty."}

        monitor.withLock {
             if(pendingDequeues.isNotEmpty() && messageQueue.isEmpty()) {
                 fastDeliver(messages)
                 return true
             } // Try to fast deliver messages

            // Fast path
            if (messageQueue.size + messages.size <= capacity) {
                messageQueue.addAll(messages)
                notifyWaiters()
                return true
            }
            if(timeout.isZero) return false

            // Slow path
            val dueTime = timeout.dueTime()

            do {
                try {
                    hasCapacity awaitUntil dueTime
                    if (messageQueue.size + messages.size <= capacity) {
                        messageQueue.addAll(messages)
                        notifyWaiters()
                        return true
                    }
                    if(dueTime.isPast) return false
                }catch (e: InterruptedException){
                    if(messageQueue.size + messages.size <= capacity) {
                        messageQueue.addAll(messages)
                        notifyWaiters()
                        Thread.currentThread().interrupt()
                        return true
                    }
                    throw e
                }


            }while(true)
        }

    }

    /**
     * Delivers messages for pending [Consumer]s while there are messages to be delivered.
     */
    private fun notifyWaiters(){
        val startingCapacity = currentCapacity
        while(pendingDequeues.isNotEmpty() && messageQueue.isNotEmpty()) {
            val waiter = pendingDequeues.removeFirst()
            waiter.value = messageQueue.removeFirst()
            waiter.condition.signal()
        }
        if(startingCapacity < currentCapacity) hasCapacity.signalAll()
    }

    /**
     * Tries to deliver messages to the [Consumer]s without blocking.
     */
    private fun fastDeliver(messages: List<T>){
        val startingCapacity = currentCapacity
        var notified = 0
        while(pendingDequeues.isNotEmpty() && notified < messages.size) {
            val waiter = pendingDequeues.removeFirst()
            waiter.value = messages[notified]
            waiter.condition.signal()
            notified++
        }
        messageQueue.addAll(messages.subList(notified, messages.size)) // Add the remaining messages to the queue
        if(startingCapacity < currentCapacity) hasCapacity.signalAll()
    }

    @Throws(InterruptedException::class)
    fun tryDequeue(timeout: Duration): T? {
        monitor.withLock {
            // Fast path
            if (messageQueue.isNotEmpty() && pendingDequeues.isEmpty()) {
                // Notifies at each message consumed in the fast path
                val msg = messageQueue.removeFirst()
                hasCapacity.signalAll()
                return msg
            }
            if(timeout.isZero) return null
            // Slow path
            val dueTime = timeout.dueTime()
            val waiter = Consumer<T>(null, monitor.newCondition())
            pendingDequeues.add(waiter)
            do {
                try {
                    hasCapacity.awaitUntil(dueTime)
                    if (waiter.value != null) {
                        return waiter.value
                    }
                    if (dueTime.isPast) {
                        pendingDequeues.remove(waiter)
                        return null
                    }
                }catch (e: InterruptedException){
                    if (waiter.value != null) {
                        Thread.currentThread().interrupt()
                        return waiter.value
                    }
                    pendingDequeues.remove(waiter)
                    throw e
                }
            }while(true)
        }
    }



}

