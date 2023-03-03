# Concurrent Programming

In this repository you will find some implementations of synchronizers and thread safe utility components, namely:
  ### Lock based (serie1)
  - [**Exchanger**](https://github.com/franciscocostalap/Synchronizers/blob/main/serie1/src/main/kotlin/pt/isel/pc/sync/Exchanger.kt): Exchanges values between two threads.

  - **Blocking Message Queue**: A blocking queue that can be used to exchange messages between threads.
    - [**Classic Aproach**](https://github.com/franciscocostalap/Synchronizers/blob/main/serie1/src/main/kotlin/pt/isel/pc/sync/BlockingMessageQueue.kt): Both enqueue and dequeue methods are blocking.
    - [**Future Implementation**](https://github.com/franciscocostalap/Synchronizers/blob/main/serie1/src/main/kotlin/pt/isel/pc/sync/BlockingMessageQueueFuture.kt): Dequeue method returns a [future](https://github.com/franciscocostalap/Synchronizers/blob/main/serie1/src/main/kotlin/pt/isel/pc/sync/FutureImpl.kt), thus is non blocking.

  - [**Thread Pool Executor**](https://github.com/franciscocostalap/Synchronizers/blob/main/serie1/src/main/kotlin/pt/isel/pc/sync/ThreadPoolExecutor.kt): 
  A thread pool that can be used to execute tasks concurrently, without the need to manage thread creation and destruction.
    

### Lock free (serie2)
  - [**Counter Modulo**](https://github.com/franciscocostalap/Synchronizers/blob/main/serie2/src/main/kotlin/pt/isel/pc/sync/CounterModulo.kt): 
  A counter that can be incremented and decremented, but only up to a given value.

  - [**Lazy**](https://github.com/franciscocostalap/Synchronizers/blob/main/serie2/src/main/kotlin/pt/isel/pc/sync/LazyLockFree.kt): 
  A lazy value that is initialized only once, and then cached.

  - [**Message Box**](https://github.com/franciscocostalap/Synchronizers/blob/main/serie2/src/main/kotlin/pt/isel/pc/sync/MessageBox.kt): 
  Stores a message that can be read an determined number of times.

  - [**Queue**](https://github.com/franciscocostalap/Synchronizers/blob/main/serie2/src/main/kotlin/pt/isel/pc/sync/QueueLockFree.kt): 
  A thread safe classic queue. Implemented using the [Lock free algorithm of Michael and Scott.](https://www.cs.rochester.edu/~scott/papers/1996_PODC_queues.pdf)


## Tests:
  Every synchronizer and component was properly tested using both unit and parameterized tests.
  The unit tests also serve as examples of how to use them properly.
### Lock based:
  - [Unit](https://github.com/isel-leic-pc/s2122-2-leic42d-problem-sets-student-franciscocostalap/tree/master/serie1/src/test/kotlin/pt/isel/pc/sync/unit)
  - [Parameterized](https://github.com/isel-leic-pc/s2122-2-leic42d-problem-sets-student-franciscocostalap/tree/master/serie1/src/test/kotlin/pt/isel/pc/sync/parameterized)


### Lock free:
  - [Unit](https://github.com/isel-leic-pc/s2122-2-leic42d-problem-sets-student-franciscocostalap/tree/master/serie2/src/test/kotlin/unit)
  - [Parameterized](https://github.com/isel-leic-pc/s2122-2-leic42d-problem-sets-student-franciscocostalap/tree/master/serie2/src/test/kotlin/parameterized)

  Made this while studying "Concurrent Programming".