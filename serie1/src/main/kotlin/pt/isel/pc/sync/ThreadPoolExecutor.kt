package pt.isel.pc.sync

import pt.isel.pc.utils.awaitUntil
import pt.isel.pc.utils.dueTime
import pt.isel.pc.utils.isPast
import java.util.*
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.time.Duration

/**
 * A thread pool that executes tasks in a FIFO queue.
 * This lock based synchronizer is implemented using monitor style.
 * Does not matter which thread executes the task, as long as the task is executed.
 */
class ThreadPoolExecutor(
    private val maxThreadPoolSize: Int, // max number of threads
    private val keepAliveTime: Duration, // time to keep alive a thread
) {

    companion object{
        private const val THREAD_POOL_NOT_RUNNING = "Thread pool is not running already"
        private const val THREAD_POOL_STILL_ALIVE = "Thread pool is still running"
    }

    private enum class State { RUNNING, TERMINATING, SHUTDOWN }
    private var state = State.RUNNING

    private val monitor: Lock = ReentrantLock()

    /**
     * Condition used by worker threads to wait for [keepAliveTime]  a task to be executed
     */
    private val workerCondition: Condition = monitor.newCondition()

    // condition to wait for pool shutdown
    private val awaitTermination: Condition by lazy { monitor.newCondition() }

    // Number of threads alive
    private var poolSize = 0

    // number of threads that are currently running a task
    private var workingThreads = 0
        get() = monitor.withLock { field }
        private set(value) = monitor.withLock { field = value }

    // queue of tasks to be executed
    private val workItems = LinkedList<Runnable>()


    init {
        require(maxThreadPoolSize > 0) { "maxThreadPoolSize must be greater than 0" }
        require(keepAliveTime > Duration.ZERO) { "keepAliveTime must be greater than 0" }
    }

    @Throws(RejectedExecutionException::class)
    fun execute(runnable: Runnable) {
        monitor.withLock{
            if(state != State.RUNNING)
                throw RejectedExecutionException("ThreadPool is shutdown")

            if(poolSize < maxThreadPoolSize && poolSize == workingThreads){
                poolSize++
                thread(name = "Worker-$poolSize") { workingLoop(runnable) }
            } else {
                workItems.add(runnable)
                workerCondition.signal()
            }
        }
    }

    private fun Runnable.safeExecute() {
        try {
            run()
        } catch (e: Exception) {
            // Safely ignore exceptions
        }
    }


    private fun workingLoop(runnable: Runnable){
        var current: Runnable? = runnable
        do {
            current?.let { notNullableRunnable ->
                workingThreads++ // Getter and setter are synchronized
                notNullableRunnable.safeExecute()
                current = null
            }
            monitor.withLock {
                val updatedKeepAliveStamp = keepAliveTime.dueTime()
                if (state == State.TERMINATING && workItems.isEmpty()) {
                    // If there are no more work items kill the thread and decrement the pool size
                    poolSize--
                    if(poolSize == 0) {
                        // If the thread was the last thread alive perform the shutdown signalling the awaitTermination condition
                        state = State.SHUTDOWN
                        awaitTermination.signalAll()
                    }
                    return
                }
                if (workItems.isNotEmpty()) {
                    current = workItems.removeFirst()
                }
                if(state == State.RUNNING && current == null){
                    // Interrupt never happens since the pool is creating the threads
                    workingThreads--
                    workerCondition awaitUntil updatedKeepAliveStamp
                }
                if (updatedKeepAliveStamp.isPast) {
                    poolSize--
                    return
                }
            }
        }while (true)
    }


    fun shutdown(){
        monitor.withLock {
            if(state == State.RUNNING) {
                if(poolSize == 0) {
                    state = State.SHUTDOWN
                }
                else{
                    state = State.TERMINATING
                    workerCondition.signalAll()
                }
            }
            else
                throw IllegalStateException(THREAD_POOL_NOT_RUNNING)
        }
    }


    @Throws(InterruptedException::class)
    fun awaitTermination(timeout: Duration): Boolean {
        monitor.withLock {
            return when(state){
                State.RUNNING -> throw IllegalStateException(THREAD_POOL_STILL_ALIVE)
                State.SHUTDOWN -> true
                State.TERMINATING -> {
                    val dueTime = timeout.dueTime()
                    try {
                        awaitTermination awaitUntil dueTime
                        if(state == State.SHUTDOWN) return true
                        if(dueTime.isPast) return false
                    } catch (e: InterruptedException) {
                        if(state == State.SHUTDOWN){
                            Thread.currentThread().interrupt()
                            return true
                        }
                        throw e
                    }

                    throw InternalError("Not Expected here")
                }
            }
        }
    }

}



