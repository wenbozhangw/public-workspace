# 理解同步器框架AbstractQueuedSynchronizer

## 一、背景

Java 在 1.5 版本引入了 `java.util.concurrent`包，用以支持并发编程，降低并发编程的复杂性；而其中大部分的同步器（例如 `lock`, `barriers` 等等）都是基于 `AbstractQueuedSynchronizer` 类，一般我们成为 `AQS`。`java.util.concurrent.locks.AbstractQueuedSynchronizer` 出自 `Doug Lea` 带佬，他的 [个人博客](https://gee.cs.oswego.edu/) 上有一篇相关论文 [《The java.util.concurrent Synchronizer Framework》](https://gee.cs.oswego.edu/dl/papers/aqs.pdf)，在我们深入研究 `AQS` 之前，有必要拜读一下该论文，翻译见笔者的另一篇博客[《The java.util.concurrent Synchronizer Framework》原文翻译][../todo] 之后结合相关源码实现进行分析。

## 二、AQS概述

`AQS` 是 `j.u.c` 包中用来构建同步组件（例如 `ReentrantLock`、`Semaphore`）的基础框架。从实现上来看，`AQS` 提供了原子的同步状态管理、线程的阻塞及唤醒以及存储队列管理模型。基于 `AQS` 提供的强大功能，我们可以很简单的构建属于自己的同步器组件。同时，`AQS` 也提供了任务取消、阻塞超时以及`conditionObject`提供的管程风格的 `await/signal/signalAll`操作。并且根据所需策略的不同，`AQS` 还提供了`公平`/`非公平`、`独占`/`共享` 等特性。

## 三、同步器框架原理

`AQS` 框架用来实现加锁和解锁的核心是基于 `acquire` 和 `release` 方法，通过这两个方法，从而去进行原子操作修改同步器状态变量，从而实现对共享资源的并发访问。在 《The java.util.concurrent Synchronizer Framework》 原文中有提到 `AQS` 的这两个核心操作实现的伪代码：

`acquire` 操作如下：

```
while(synchronization state does not allow acquire) {
  enqueue current thread if not already queued;
  possibly block current thread;
}
dequeue current thread if it was queued;
```

简单翻译一下：

```
while(同步器状态获取失败) {
  if (当前线程未进入等待队列) {
    将当前线程入队；
  }
  可能尝试阻塞当前线程;
}
if (如果当前线程已经入队) {
  当前线程出队;
}
```

`release` 操作如下：

```
update synchronization state;
if(state may permit a blocked thread to acquire) 
  unblock one or more queued threads;
```

简单翻译一下：

```
更新同步器状态;
if (同步器状态可以允许一个阻塞线程获取) {
  解除一个或多个队列线程的阻塞状态;
}
```

可以看到 `AQS` 的核心思想是，如果请求资源空闲（即同步器状态修改成功），将共享资源设置为锁定状态；如果共享资源被占用（即同步器状态修改失败），就需要对当前线程进行入队操作，之后通过阻塞等待唤醒机制来保证锁的分配。这个队列机制主要是通过 CLH 队列的变体来实现的。我们会在下文中对 CLH 队列进行讲述。

### 3.1同步器状态

`AQS` 类内部定义了一个`volatile`修饰的 `32` 位 `int` 类型的 `state` 变量用于维护同步器的状态：

```java
    /**
     * 同步状态值
     */
    private volatile int state;

    /**
     * 返回同步状态的当前值。
     * 该操作的内存语义为{@code volatile} 读。
     * @return 当前同步状态值
     */
    protected final int getState() {
        return state;
    }

    /**
     * 设置同步状态的值。
     * 该操作具有 {@code volatile} 写的内存语义。
     * @param newState 新状态值
     */
    protected final void setState(int newState) {
        state = newState;
    }

    /**
     * 如果当前状态值等于预期值，则自动将同步状态设置为
     * 给定的更新值。该操作具有 {@code volatile} 读写
     * 的内存语义。
     *
     * @param expect 期望值
     * @param update 新值
     * @return 如果成功，返回{@code true}. 返回 false 表示实际值
     *	       与期望值不相等。
     */
    protected final boolean compareAndSetState(int expect, int update) {
        // 见下面的内部设置来支持这一点
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }

```

同步器的状态 `state` 在不同的实现中会有不同的作用和意义，需要结合具体的使用进行分析（比如说 `ReentrantReadWriteLock` 中 `state` 的前 16 位记录读锁的数量（共享），后 16 位记录写锁的数量（独占））。另外，我们可以看到，上面关于 `state` 的几个方法都是 `final` 修饰的，说明子类无法重写它们。我们可以通过修改 `state` 字段来表示同步状态加锁的过程。

### 3.2 CLH 队列

`CLH`队列：`Craig、Landin and Hagersten`队列，基础的 CLH 对列是一个单向链表，而 `AQS` 中是用的队列是 CLH 队列的变体——虚拟双向队列（FIFO），因此，该框架是不支持基于优先级的同步。使用同步队列的原因是，它是一种不需要使用低级锁来构造非阻塞数据结构。

CLH 队列实际上并不是很像队列，因为它的入队和出队操作都与其用途（作为锁）紧密相关。他通过两个字段 `tail` 和 `head` 来存取，同时这两个字段支持原子更新，两者在初始化时都指向的空节点。

![][]

当一个新节点通过原子操作入队：

```java
do {
  pred = tail;
} while (!tail.compareAndSet(pred, node));
```

同时， 每个节点的 `release` 状态都保存在其前驱结点中。因此，当前节点可以通过自旋，直到前驱节点释放锁（但是，从实际上来看，过度的自旋会带来大量的 CPU 性能损耗）：

```
while (pred.status != RELEASED); // spin
```

自旋后的出队操作只需将 `head` 字段指向刚刚得到锁的节点：

```
head = node
```

CLH 的优点是：它的入队和出队操作是快速的、无锁的、无阻塞的（即使在竞争的情况下，也只会有一个线程赢得插入的机会，从而能进行下去）。检测是否有线程在等待也很快（只需要检测 `head` 和 `tail` 是否相等）；同时，`release` 是分散的，避免了一些不必要的内存竞争。

`AQS`中的等待队列是 CLH 锁队列的变体。CLH 锁通常用于自旋锁。但是在 `AQS` 中将其作为阻塞同步器，但是根据其基本思想，即在其节点的前驱节点中保存有关线程的控制信息。每个节点的“状态”字段跟踪线程是否应该阻塞。节点在其前驱节点释放时发出 `signal` 。否则，队列中的每个节点都是持有单个线程的特定通知器。状态字段不用于控制线程是否持有锁。同时，线程可能会尝试获取队列中的第一个节点，但其并不保证一定成功，所以当前释放的竞争线程可能会重新被阻塞（如果没有获取到锁）。

`AQS`使用的 CLH 变体中的 "prev" 连接（指向前驱节点）主要用于处理取消。如果一个节点被取消，它的后继节点（通常）需要重新连接到一个未取消的前驱节点。

`AQS` 使用 CLH 的 "next" 连接（指向后继节点）来实现阻塞机制。每个节点的线程 id 保存在自身中，因此前驱节点通过遍历 next 连接来确定它是哪个线程来通知下一个节点的唤醒。设置当前节点的 next （后继节点）时，必须避免与新入队的节点竞争。当节点的后继节点为空时，通过从队列的 `tail` 向后检查来解决这个问题。（"next" 本来就是一种优化，通常情况下是不需要向后扫描的。）

CLH 队列需要一个虚拟头节点。但是我们不会在构建时创建它们，因为如果从不存在竞争，那将是浪费精力。相反，在第一次出现竞争时构造节点并设置 `head` 和 `tail` 指针。

`Condition` 等待队列中的阻塞线程使用的是相同的 `Node` 结构，但是提供了另一个链表来存放，因此 `Condition` 等待队列的实现会更加复杂。

关于 CLH 队列的实现如下：

```java
    static final class Node {
        /** 标记节点处于共享模式下的等待 */
        static final Node SHARED = new Node();
        /** 标记节点处于独占模式下的等待 */
        static final Node EXCLUSIVE = null;

        /** waitStatus 值，表示线程已经取消 */
        static final int CANCELLED =  1;
        /** waitStatus 值，表示后继线程需要唤醒 */
        static final int SIGNAL    = -1;
        /** waitStatus 值，表示线程需要等待条件 */
        static final int CONDITION = -2;
        /**
         * waitStatus 值，指示下一个 acquireShared 应该无条件传播
         */
        static final int PROPAGATE = -3;

        /**
         * Status 字段, 仅取以下值:
         *   SIGNAL:     该节点的后继节点被（或即将）阻塞（通过 park），因此当前
         *               节点在释放或取消时必须解除其后继节点的 park。为了避免
         *               竞争，acquire 方法必须首先表明它们需要 signal，然后重试
         *               原子获取，然后在失败时阻塞。
         *   CANCELLED:  由于超时或中断，该节点被取消。节点永远不会离开这个状态。
         *				 特别的是，被取消的节点的线程永远不会再次阻塞。
         *   CONDITION:  此节点当前位于条件队列中。在转换之前不会用作同步队列节点，
         *               此时状态将设置为 0。（此处使用此值与该字段的其他用途无关，但简化的机制）
         *   PROPAGATE:  releaseShared 应该传播到其他节点。这是在 doReleaseShared 中设置 
         *               的（仅针对头结点），以确保传播继续进行，即使其他操作已经介入。
         *   0:          以上都不是
         *
         * 这些值按数字排列以简化使用。非负值意味着节点不需要发出 signal。
         * 因此，大多数代码不需要检查特定值，只需检查 sign 即可。
         *
         * 对于正常同步节点，该字段初始化为 0，对于 CONDITION 节点，该字段初始化为 CONDITION。
         * 它使用 CAS 进行修改（或者在可能的情况下，无条件的 volatile 写入）
         */
        volatile int waitStatus;

        /**
         * 链接到当前节点/线程的前驱节点，用于检查 waitStatus。在入队时分配，
         * 并且仅在出队时设置为 null（为了 GC）。此外，在前驱节点取消时，我们短路，同时
         * 找到一个未取消的前驱节点（该前驱节点不会不存在），因为头结点不会被取消：一个节点
         * 只有在 acquire 成功时才会成为头结点。被取消的线程永远不会获取成功，同时
         * 一个线程只能取消自己，无法取消任何其他节点。
         */
        volatile Node prev;

        /**
         * 链接到当前节点/线程的后继节点，用于在 release 时 unpark 操作。在入队时分配，
         * 前驱节点取消时，会进行绕过调整，在出队时清空（为了 GC）。enq 操作在建立链接之前
         * 不会给前驱节点的 next 字段赋值，因此看到 next 字段为 null，并不一定意味着该节点在
         * 队尾。然而，如果 next 字段看起来是 null，我们可以从 tail 扫描 prev 节点，从而
         * 进行双重检查。取消的节点的 next 字段被设置为指向节点自身，而不是 null，
         * 从而使 isOnSyncQueue 的工作更容易。
         */
        volatile Node next;

        /**
         * 将 thread 放入当前节点。构造时初始化，使用后清空。
         */
        volatile Thread thread;

        /**
         * 链接到等待条件的下一个节点，或特定的 SHARED 值。因为条件队列只有在
         * 独占模式下被访问，所以我们只需要一个简单的链接队列来保存等待条件的节点。
         * 然后，它们被转移到队列中进行重新 acquire。因为条件只能是独占的，所以
         * 我们通过使用特殊值来保存特殊值，以表示共享模式。
         */
        Node nextWaiter;

        /**
         * 如果节点在共享模式下等待，则返回true。
         */
        final boolean isShared() {
            return nextWaiter == SHARED;
        }

        /**
         * 返回上一个节点，如果为空则抛出NullPointerException。
         * 当前驱节点不能为空时使用。null 检查可以省略，但它的存在是为了帮助 VM。
         *
         * @return 当前节点的前驱节点
         */
        final Node predecessor() throws NullPointerException {
            Node p = prev;
            if (p == null)
                throw new NullPointerException();
            else
                return p;
        }

        Node() {    // Used to establish initial head or SHARED marker
        }

        Node(Thread thread, Node mode) {     // Used by addWaiter
            this.nextWaiter = mode;
            this.thread = thread;
        }

        Node(Thread thread, int waitStatus) { // Used by Condition
            this.waitStatus = waitStatus;
            this.thread = thread;
        }
    }

    /**
     * 等待队列的头部，延迟初始化。除此之外，只能通过 setHead 方法进行修改，
     * 注意：如果 head 存在，它的 waitStatus 保证不会被 CANCELLED。
     */
    private transient volatile Node head;

    /**
     * 等待队列的尾部，延迟初始化。仅通过方法 enq 修改以添加新的等待节点。
     */
    private transient volatile Node tail;

```

下面介绍一下 `Node` 类中的几个属性：

- `waitStatus`：当前 `Node` 的等待状态，共有五个可选值：
  - `0`：初始值，当前节点如果没有指定初始值，则默认为 `0`。
  - `CANCELLED(1)`：表示当前节点因为超时或线程中断被取消。当节点被取消后，不会再转换为其他状态，被取消的节点的线程实例也不会阻塞。
  - `SIGNAL(-1)`：表示当前节点的后继节点通过 `park()` 被阻塞，当前节点释放或取消时，必须 `unpark()` 它的后继节点。
  - `CONDITION(2)`：表示当前节点是条件队列中的一个节点，当它转换为同步队列中节点时，`waitStatus` 会被重新设置为 `0`。
  - `PROPAGATE(3)`：当节点为头结点，调用 `doReleaseShared()` 时，确保 `releaseShared()` 可以传播到其他节点。
- `prev`：当前节点的前驱节点，用于检查 `waitStatus`。当前驱节点被取消时，通过 `prev` 找到一个未取消的前驱节点。
- `next`：当前节点的后继节点，当节点被取消或释放时，用于 `unpark` 取消后继节点的阻塞（会自动绕过取消的后继节点）。
- `thread`：当前节点持有的线程实例引用。
- `nextWaiter`：下一个等待节点，可能的取值有下面的几种情况：
  - 当前实例为独占模式时，取值为 `Node.EXCLUSIVE` （即 `null`）。
  - 当前实例为共享模式时，取值为 `Node.SHARED`。
  - 非上面两种情况时，代表条件队列中当前节点的下一个等待节点。

### 3.3 阻塞

在 JDK1.5 之前，线程的阻塞和唤醒只能依赖于 `Object` 类提供的 `wait()` 、`notify()`、`notifyAll()` 方法，它们都是由 JVM 提供实现，并且使用的时候需要获取监视器锁（即需要在 `synchronized` 代码块中），没有 Java API 可以阻塞和唤醒线程。唯一可以选择的是 `Thread.suspend` 和 `Thread.resume`，但是他们都有无法解决的竟态问题：当一个非阻塞线程在一个正准备阻塞的线程调用 `suspend` 之前调用 `resume`，`resume`操作将不起作用。

`j.u.c` 包引入了 `LockSupport` 类，其底层是基于 `Unsafe` 类的 `park()` 和 `unpark()` 方法，`LockSupport.park` 阻塞当前线程，除非或直到发出 `LockSupport.unpark`（虚假唤醒是允许的）。`park` 方法同样支持可选的相对或绝对的超时设置，以及与 JVM 的 `Thread.interrupt` 结合 —— 可通过中断来 `unpark` 一个线程。

### 3.4 条件队列

在 `AQS` 中除了同步队列外，还提供了另一种更为复杂的条件队列，而条件队列是基于 `Condition` 接口实现的，下面我们先浏览一下 `Condition` 接口的说明：

`Condition` 将 `Object` 的监视器方法（`wait`、`notify` 和 `notifyAll`） 分解到不同的对象，通过将它们与任意的 `Lock` 实现相结合，可以使每个对象具有多个等待集合。`Lock` 代替的 `synchronized` 方法和语句的使用，`Condition` 代替了 `Object` 监视器方法的使用。

`Condition`（也称为 *条件队列(condition queue)* 或 *条件变量(condition variable)*）为线程提供了一种暂停执行（“等待”）的方法，直到另外一个线程通知说某个状态条件现在可能为 `true`。由于对这种共享状态信息的访问会发生在多个不同线程中，所以它必须受到保护，因此需要某种形式的锁与条件相关联。等待条件提供的关键属性是它以 *原子* 方式释放关联的锁并挂起当前线程，就像 `Object.wait` 一样。

`Condition` 实例本质上是需要绑定到锁。需要获取特定 `Lock` 实例的 `Condition` 实例，请使用其 `newCondition()` 方法。

例如，假设我们有一个支持 put 和 take 方法的有界缓冲区。如果 take 在空缓冲区上尝试获取，则线程将阻塞，知道缓冲区变得可用；如果在一个满的缓冲区上调用 `put`，则线程将阻塞，直到有空间可用。我们希望 put 线程继续等待，并且与 take 线程隔开在另一个等待集合中，以便当我们的缓冲区可用或有空间发生变化时通知对应的单个线程。这可以使用量 `Condition` 实例来实现。

```java
class BoundedBuffer {
    final Lock lock = new ReentrantLock();
    final Condition notFull = lock.newCondition();
    final Condition notEmpty = lock.newCondition();
    
    final Object[] items = new Object[100];
    int putptr, takeptr, count;
    
    public void put(Object x) throws InterruptedException {
        lock.lock();
        try {
            while (count == items.length) 
                notFull.await();
            items[putptr] = x;
            if (++putptr == items.length) putptr = 0;
            ++count;
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }
    
    public Object take() throws InterrputedException {
        lock.lock();
        try {
            while (count == 0) 
                notEmpty.await();
            Object x = items[takeptr];
            if (++takeptr == items.length) takeptr = 0;
            --count;
            notFull.signal();
            return x;
        } finally {
            lock.unlock;
        }
    }
}
```

(`java.util.concurrent.ArrayBlockingQueue` 类提供了这个功能，所以没有理由使用这个实例类。)

`Condition` 的实现可以提供与 `object` 监视器方法不同的行为和语义，例如保证通知的顺序，或者在执行通知时不需要持有锁。如果实现提供了这种专门的语义，那么实现必须记录这些语义。

请注意，`Condition` 实例只是普通对象，它们本身可以用作 `synchronized` 语句中的目标，并且可以调用它们自己的监视器 `wait` 和 `notification` 方法。获取 `Condition` 实例的监视器锁，或使用其监视器方法，与获取和该 `Condition` 关联的 `Lock` 或使用其 `wait()` 和 `signal()` 方法没有指定关系。为避免混淆，建议不要以这种方式使用 `Condition` 实例，除非在它们自己的实现中。

除非另有说明，否则为任何参数传递 `null` 值将导致 `NullPointerException`。

实施注意事项：

在等待 `Condition ` 时，通常允许发生 *”虚假唤醒“*，作为对底层平台语义的让步。这对大多数应用程序几乎没有实际影响，因为应该始终在循环中等待 `Condition`，测试正在等待的状态谓词是否为 `true`。一个实现可以自由地消除虚假唤醒的可能性，但建议应用程序的程序员总是假设它们可以发生，因此总是在循环中等待条件唤醒。

条件等待的三种形式（可中断、不可中断和定时）在某些平台上实现的难易程度和性能特征可能不同。特别是，可能难以提供这些功能并维护特定的语义，例如排序保证。此外，中断线程的实际挂起能力可能并不总是适用所有平台。

因次，实现不需要为所有三种等待形式定义完全相同的保证或语义，也不需要支持线程实际挂起的中断。

实现需要清楚地记录每个等待方法提供的语义和保证，并且当实现确实支持线程挂起的中断时，它必须遵守此接口中定义的中断语义。

由于中断通常意味着取消，并且对中断的检查通常不常见，因此实现可以倾向于响应中断而不是正常的方法返回。即使可以证明中断发生在另一个可能已经解除阻塞线程的操作之后也是如此。一个实现应该记录这个行为。

```java
public interface Condition {

    /**
     * Causes the current thread to wait until it is signalled or
     * {@linkplain Thread#interrupt interrupted}.
     *
     * <p>The lock associated with this {@code Condition} is atomically
     * released and the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until <em>one</em> of four things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #signal} method for this
     * {@code Condition} and the current thread happens to be chosen as the
     * thread to be awakened; or
     * <li>Some other thread invokes the {@link #signalAll} method for this
     * {@code Condition}; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of thread suspension is supported; or
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.
     * </ul>
     *
     * <p>In all cases, before this method can return the current thread must
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * and interruption of thread suspension is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared. It is not specified, in the first
     * case, whether or not the test for interruption occurs before the lock
     * is released.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>The current thread is assumed to hold the lock associated with this
     * {@code Condition} when this method is called.
     * It is up to the implementation to determine if this is
     * the case and if not, how to respond. Typically, an exception will be
     * thrown (such as {@link IllegalMonitorStateException}) and the
     * implementation must document that fact.
     *
     * <p>An implementation can favor responding to an interrupt over normal
     * method return in response to a signal. In that case the implementation
     * must ensure that the signal is redirected to another waiting thread, if
     * there is one.
     *
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    void await() throws InterruptedException;

    /**
     * Causes the current thread to wait until it is signalled.
     *
     * <p>The lock associated with this condition is atomically
     * released and the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until <em>one</em> of three things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #signal} method for this
     * {@code Condition} and the current thread happens to be chosen as the
     * thread to be awakened; or
     * <li>Some other thread invokes the {@link #signalAll} method for this
     * {@code Condition}; or
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.
     * </ul>
     *
     * <p>In all cases, before this method can return the current thread must
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     *
     * <p>If the current thread's interrupted status is set when it enters
     * this method, or it is {@linkplain Thread#interrupt interrupted}
     * while waiting, it will continue to wait until signalled. When it finally
     * returns from this method its interrupted status will still
     * be set.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>The current thread is assumed to hold the lock associated with this
     * {@code Condition} when this method is called.
     * It is up to the implementation to determine if this is
     * the case and if not, how to respond. Typically, an exception will be
     * thrown (such as {@link IllegalMonitorStateException}) and the
     * implementation must document that fact.
     */
    void awaitUninterruptibly();

    /**
     * Causes the current thread to wait until it is signalled or interrupted,
     * or the specified waiting time elapses.
     *
     * <p>The lock associated with this condition is atomically
     * released and the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until <em>one</em> of five things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #signal} method for this
     * {@code Condition} and the current thread happens to be chosen as the
     * thread to be awakened; or
     * <li>Some other thread invokes the {@link #signalAll} method for this
     * {@code Condition}; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of thread suspension is supported; or
     * <li>The specified waiting time elapses; or
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.
     * </ul>
     *
     * <p>In all cases, before this method can return the current thread must
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * and interruption of thread suspension is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared. It is not specified, in the first
     * case, whether or not the test for interruption occurs before the lock
     * is released.
     *
     * <p>The method returns an estimate of the number of nanoseconds
     * remaining to wait given the supplied {@code nanosTimeout}
     * value upon return, or a value less than or equal to zero if it
     * timed out. This value can be used to determine whether and how
     * long to re-wait in cases where the wait returns but an awaited
     * condition still does not hold. Typical uses of this method take
     * the following form:
     *
     *  <pre> {@code
     * boolean aMethod(long timeout, TimeUnit unit) {
     *   long nanos = unit.toNanos(timeout);
     *   lock.lock();
     *   try {
     *     while (!conditionBeingWaitedFor()) {
     *       if (nanos <= 0L)
     *         return false;
     *       nanos = theCondition.awaitNanos(nanos);
     *     }
     *     // ...
     *   } finally {
     *     lock.unlock();
     *   }
     * }}</pre>
     *
     * <p>Design note: This method requires a nanosecond argument so
     * as to avoid truncation errors in reporting remaining times.
     * Such precision loss would make it difficult for programmers to
     * ensure that total waiting times are not systematically shorter
     * than specified when re-waits occur.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>The current thread is assumed to hold the lock associated with this
     * {@code Condition} when this method is called.
     * It is up to the implementation to determine if this is
     * the case and if not, how to respond. Typically, an exception will be
     * thrown (such as {@link IllegalMonitorStateException}) and the
     * implementation must document that fact.
     *
     * <p>An implementation can favor responding to an interrupt over normal
     * method return in response to a signal, or over indicating the elapse
     * of the specified waiting time. In either case the implementation
     * must ensure that the signal is redirected to another waiting thread, if
     * there is one.
     *
     * @param nanosTimeout the maximum time to wait, in nanoseconds
     * @return an estimate of the {@code nanosTimeout} value minus
     *         the time spent waiting upon return from this method.
     *         A positive value may be used as the argument to a
     *         subsequent call to this method to finish waiting out
     *         the desired time.  A value less than or equal to zero
     *         indicates that no time remains.
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    long awaitNanos(long nanosTimeout) throws InterruptedException;

    /**
     * Causes the current thread to wait until it is signalled or interrupted,
     * or the specified waiting time elapses. This method is behaviorally
     * equivalent to:
     *  <pre> {@code awaitNanos(unit.toNanos(time)) > 0}</pre>
     *
     * @param time the maximum time to wait
     * @param unit the time unit of the {@code time} argument
     * @return {@code false} if the waiting time detectably elapsed
     *         before return from the method, else {@code true}
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    boolean await(long time, TimeUnit unit) throws InterruptedException;

    /**
     * Causes the current thread to wait until it is signalled or interrupted,
     * or the specified deadline elapses.
     *
     * <p>The lock associated with this condition is atomically
     * released and the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until <em>one</em> of five things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #signal} method for this
     * {@code Condition} and the current thread happens to be chosen as the
     * thread to be awakened; or
     * <li>Some other thread invokes the {@link #signalAll} method for this
     * {@code Condition}; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of thread suspension is supported; or
     * <li>The specified deadline elapses; or
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.
     * </ul>
     *
     * <p>In all cases, before this method can return the current thread must
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     *
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * and interruption of thread suspension is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared. It is not specified, in the first
     * case, whether or not the test for interruption occurs before the lock
     * is released.
     *
     *
     * <p>The return value indicates whether the deadline has elapsed,
     * which can be used as follows:
     *  <pre> {@code
     * boolean aMethod(Date deadline) {
     *   boolean stillWaiting = true;
     *   lock.lock();
     *   try {
     *     while (!conditionBeingWaitedFor()) {
     *       if (!stillWaiting)
     *         return false;
     *       stillWaiting = theCondition.awaitUntil(deadline);
     *     }
     *     // ...
     *   } finally {
     *     lock.unlock();
     *   }
     * }}</pre>
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>The current thread is assumed to hold the lock associated with this
     * {@code Condition} when this method is called.
     * It is up to the implementation to determine if this is
     * the case and if not, how to respond. Typically, an exception will be
     * thrown (such as {@link IllegalMonitorStateException}) and the
     * implementation must document that fact.
     *
     * <p>An implementation can favor responding to an interrupt over normal
     * method return in response to a signal, or over indicating the passing
     * of the specified deadline. In either case the implementation
     * must ensure that the signal is redirected to another waiting thread, if
     * there is one.
     *
     * @param deadline the absolute time to wait until
     * @return {@code false} if the deadline has elapsed upon return, else
     *         {@code true}
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    boolean awaitUntil(Date deadline) throws InterruptedException;

    /**
     * Wakes up one waiting thread.
     *
     * <p>If any threads are waiting on this condition then one
     * is selected for waking up. That thread must then re-acquire the
     * lock before returning from {@code await}.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>An implementation may (and typically does) require that the
     * current thread hold the lock associated with this {@code
     * Condition} when this method is called. Implementations must
     * document this precondition and any actions taken if the lock is
     * not held. Typically, an exception such as {@link
     * IllegalMonitorStateException} will be thrown.
     */
    void signal();

    /**
     * Wakes up all waiting threads.
     *
     * <p>If any threads are waiting on this condition then they are
     * all woken up. Each thread must re-acquire the lock before it can
     * return from {@code await}.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>An implementation may (and typically does) require that the
     * current thread hold the lock associated with this {@code
     * Condition} when this method is called. Implementations must
     * document this precondition and any actions taken if the lock is
     * not held. Typically, an exception such as {@link
     * IllegalMonitorStateException} will be thrown.
     */
    void signalAll();
}

```

