package pt.isel.pc.sync

import pt.isel.pc.utils.awaitUntil
import pt.isel.pc.utils.dueTime
import pt.isel.pc.utils.isPast
import pt.isel.pc.utils.isZero
import java.util.concurrent.CancellationException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Future implementation.
 *
 * Implemented using monitor style.
 */
class FutureImpl<T>: Future<T> {

    enum class State {
        NEW,
        CANCELLED,
        DONE
    }

    private val monitor = ReentrantLock()
    private val condition: Condition = monitor.newCondition()

    private var state = State.NEW
    private var result: T? = null
    private var error: Throwable? = null

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        monitor.withLock{
            if(state == State.NEW) {
                state = State.CANCELLED
                condition.signalAll()
                return true
            }
            return false
        }
    }

    @Throws(InterruptedException::class, TimeoutException::class, CancellationException::class)
    override fun get(timeout: Long, unit: TimeUnit): T {
        monitor.withLock {
            val timeoutDuration = TimeUnit.MILLISECONDS
                .convert(timeout, unit) // Convert to milliseconds to convert to Duration
                .toDuration(DurationUnit.MILLISECONDS)

            // Fast path
            if(state == State.CANCELLED) throw CancellationException()
            if(state == State.DONE)
                return result ?: throw error as Throwable

            if(timeoutDuration.isZero) throw TimeoutException("Timeout has passed")

            val dueTime = timeoutDuration.dueTime()
            while(true) {
                try {
                    condition awaitUntil dueTime
                    if(state == State.CANCELLED) throw CancellationException()
                    if(state == State.DONE) {
                        return result ?: throw error as Throwable
                    }
                    if(dueTime.isPast) throw TimeoutException("Timeout has passed")
                }catch (e: InterruptedException) {
                    // If the thread is interrupted, check if the future can be delivered then regenerate the exception
                    if(state == State.DONE){
                        Thread.currentThread().interrupt()
                        return result ?: throw error as Throwable
                    }
                    throw e
                }
            }
        }
    }

    /**
     * Fills the future with the result of the task
     */
    fun setSuccess(value: T) {
        setResult(success=value, isFailure=false)
    }

    /**
     * Fails the future with the given error
     */
    fun setFailure(failure: Throwable) {
        setResult(failure=failure, isFailure=true)
    }

    /**
     * Auxiliary function that sets the future state to done
     * by providing a success or failure value depending on the [isFailure] flag
     *
     * @param isFailure if true, the future will be set to failure, otherwise success
     * @param success the value to set the future to if the future is success
     * @param failure the error to set the future to if the future is failure
     */
    private fun setResult(success: T?=null, failure: Throwable?=null, isFailure: Boolean) {
        // Internal checks to ensure correct implementation
        require((success != null) xor (failure != null)) { "Only one of result and failure can be set" }
        require(isFailure xor  (failure == null)) { "Failure must be set if isFailure is true" }
        require(!isFailure xor (success == null)) { "Success must be set if isFailure is false" }

        monitor.withLock {
            if (isDone){
                throw IllegalStateException("Can only be used once")
            }
            state = State.DONE
            if(isFailure)
                this.error = failure
            else
                this.result = success

            condition.signalAll()
        }
    }

    /**
     * Returns `true` if this task completed.
     *
     * Completion may be due to normal termination, an exception, or
     * cancellation -- in all of these cases, this method will return
     * `true`.
     *
     * @return `true` if this task completed
     */
    override fun isDone(): Boolean {
        monitor.withLock {
            return state == State.DONE || state == State.CANCELLED
        }
    }

    /**
     * Returns `true` if this task was cancelled before it completed
     * normally.
     *
     * @return `true` if this task was cancelled before it completed
     */
    override fun isCancelled(): Boolean {
        monitor.withLock {
            return state == State.CANCELLED
        }
    }

    @Throws(InterruptedException::class, CancellationException::class)
    override fun get(): T = get(Duration.INFINITE.inWholeMilliseconds, TimeUnit.MILLISECONDS)


}