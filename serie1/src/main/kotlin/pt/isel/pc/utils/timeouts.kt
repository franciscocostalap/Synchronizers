package pt.isel.pc.utils

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

inline val Duration.isZero : Boolean
    get() = this.inWholeNanoseconds == 0L

fun Duration.dueTime() =
    (System.currentTimeMillis().toDuration(DurationUnit.MILLISECONDS)  + this)
        .toLong(DurationUnit.MILLISECONDS)

inline val Long.remaining : Long
    get() = if(this == Long.MAX_VALUE) Long.MAX_VALUE
    else 0L.coerceAtLeast(this - System.currentTimeMillis())

infix fun Condition.awaitUntil(dueTime : Long) {
    this.await(dueTime.remaining, TimeUnit.MILLISECONDS)
}

inline val Long.isPast : Boolean
    get() =  this < System.currentTimeMillis()
