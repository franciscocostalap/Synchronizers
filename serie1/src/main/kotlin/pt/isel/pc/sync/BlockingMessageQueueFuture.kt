package pt.isel.pc.sync

import pt.isel.pc.utils.awaitUntil
import pt.isel.pc.utils.dueTime
import pt.isel.pc.utils.isPast
import pt.isel.pc.utils.isZero
import java.util.LinkedList
import java.util.concurrent.Future
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

/**
 * Enqueues implemented using monitor style.
 * Dequeues implemented using a future.
 */
class BlockingMessageQueueFuture<T>(private val capacity: Int){

    private val monitor = ReentrantLock()
    private val hasCapacity = monitor.newCondition()

    private val currentCapacity
        get() = capacity - messageQueue.size

    private val pendingDequeues = LinkedList<FutureImpl<T>>()
    private val messageQueue = LinkedList<T>()


    @Throws(InterruptedException::class)
    fun tryEnqueue(messages: List<T>, timeout: Duration): Boolean {
        require(messages.size <= capacity){"The number of messages to be enqueued is greater than the capacity of the queue."}
        require(messages.isNotEmpty()){"The list of messages to be enqueued is empty."}
        monitor.withLock {
            if(pendingDequeues.isNotEmpty() && messageQueue.isEmpty()) {
                fastDeliver(messages)
                return true
            } // Try to deliver without enqueuing

            // Fast path
            if (messageQueue.size + messages.size <= capacity) {
                messageQueue.addAll(messages)
                fillFutures()
                return true
            }

            if(timeout.isZero) return false

            // Slow path
            val dueTime = timeout.dueTime()

            do {
                try{
                    hasCapacity awaitUntil dueTime
                    if (messageQueue.size + messages.size <= capacity) {
                        messageQueue.addAll(messages)
                        fillFutures()
                        return true
                    }
                    if(dueTime.isPast) return false

                }catch (e: InterruptedException){
                    if (messageQueue.size + messages.size <= capacity) {
                        messageQueue.addAll(messages)
                        fillFutures()
                        Thread.currentThread().interrupt()
                        return true
                    }
                    throw e
                }
            }while(true)

        }

    }

    /**
     * Delivers messages for pending [FutureImpl]s while there are messages to be delivered.
     */
    private fun fillFutures(){
        val startingCapacity = currentCapacity
        while(pendingDequeues.isNotEmpty() && messageQueue.isNotEmpty()) {
            val future = pendingDequeues.removeFirst()
            future.setSuccess(messageQueue.removeFirst())
        }
        if(startingCapacity < currentCapacity) hasCapacity.signalAll()
    }

    /**
     * Tries to deliver messages to the [FutureImpl]s without enqueue.
     */
    private fun fastDeliver(messages: List<T>){
        val startingCapacity = currentCapacity
        var notified = 0
        while(pendingDequeues.isNotEmpty() && notified < messages.size) {
            val future = pendingDequeues.removeFirst()
            future.setSuccess(messages[notified])
            notified++
        }
        messageQueue.addAll(messages.subList(notified, messages.size)) // Add the remaining messages to the queue
        if(startingCapacity < currentCapacity) hasCapacity.signalAll()
    }

    /**
     * Tries to dequeue a message from the queue.
     * Returns a future that will be completed when a message is available.
     */
    fun tryDequeue(): Future<T> {
        monitor.withLock{
            val future = FutureImpl<T>()
            if(pendingDequeues.isEmpty() && messageQueue.isNotEmpty()) {
                future.setSuccess(messageQueue.removeFirst())
                hasCapacity.signalAll()
                return future
            }
            pendingDequeues.add(future)
            return future
        }
    }

}

