package pt.isel.pc.sync

import java.util.concurrent.atomic.AtomicReference

/**
 * * A thread-safe lock free queue using MichaelScott's algorithm.
 *
 * Reference: https://www.cs.rochester.edu/~scott/papers/1996_PODC_queues.pdf
 *
 * @param T the type of the elements in the stack.
 */
class QueueLockFree<T> {

    private class Node<T>(
        val value: T? = null,
        val next: AtomicReference<Node<T>?> = AtomicReference(null)
    )

    private val head: AtomicReference<Node<T>>
    private val tail: AtomicReference<Node<T>>

    init {
        val dummy = Node<T>()
        head = AtomicReference(dummy)
        tail = AtomicReference(dummy)
    }

    /**
     * Enqueues an element to the queue.
     * @param value the element to be enqueued.
     */
    fun enqueue(value: T){
        val newNode = Node(value)
        do {
            val obsTail = tail.get()
            val obsNext = obsTail.next.get()
            if (obsTail != tail.get()) continue  // Check tail consistency
            if (obsNext == null) { // Check if tail is pointing to the end of the queue.
                if (obsTail.next.compareAndSet(obsNext, newNode)) { // try to insert the new node
                    tail.compareAndSet(obsTail, newNode) // and adjust tail accordingly
                    return
                }
            } else {
                tail.compareAndSet(obsTail, obsNext) // Tail fell behind, advance it and retry
            }
        }
        while(true)
    }


    fun take(): T?{

        do {
            val obsHead = head.get()
            val obsNext = obsHead.next.get()
            if(obsHead == head.get()) {
                if(obsNext == null) return null
                if(head.compareAndSet(obsHead, obsNext))
                    return obsNext.value
            }
        }while (true)

    }

}
