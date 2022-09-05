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

  /**
   * 设置以用于支持 compareAndSet. 我们需要在这里本地实现这一点：
   * 为了允许未来的功能增强，我们不能显式地继承 AtomicInteger，不然这将是高效和有用的。
   * 因此，作为少有的弊端，我们本地使用 hotspot 内在的 API 实现。但我们这样做的时候，
   * 我们队其他 CASable 字段做同样的事情（否则可以用原子字段更新器来完成）。
   */
  private static final Unsafe unsafe = Unsafe.getUnsafe();
  private static final long stateOffset;
  private static final long headOffset;
  private static final long tailOffset;
  private static final long waitStatusOffset;
  private static final long nextOffset;

static {
        try{
        stateOffset=unsafe.objectFieldOffset
        (AbstractQueuedSynchronizer.class.getDeclaredField("state"));
        headOffset=unsafe.objectFieldOffset
        (AbstractQueuedSynchronizer.class.getDeclaredField("head"));
        tailOffset=unsafe.objectFieldOffset
        (AbstractQueuedSynchronizer.class.getDeclaredField("tail"));
        waitStatusOffset=unsafe.objectFieldOffset
        (Node.class.getDeclaredField("waitStatus"));
        nextOffset=unsafe.objectFieldOffset
        (Node.class.getDeclaredField("next"));

        }catch(Exception ex){throw new Error(ex);}
        }

/**
 * CAS head field. Used only by enq.
 */
private final boolean compareAndSetHead(Node update){
        return unsafe.compareAndSwapObject(this,headOffset,null,update);
        }

/**
 * CAS tail field. Used only by enq.
 */
private final boolean compareAndSetTail(Node expect,Node update){
        return unsafe.compareAndSwapObject(this,tailOffset,expect,update);
        }

/**
 * CAS waitStatus field of a node.
 */
private static final boolean compareAndSetWaitStatus(Node node,
        int expect,
        int update){
        return unsafe.compareAndSwapInt(node,waitStatusOffset,
        expect,update);
        }

/**
 * CAS next field of a node.
 */
private static final boolean compareAndSetNext(Node node,
        Node expect,
        Node update){
        return unsafe.compareAndSwapObject(node,nextOffset,expect,update);
        }

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

在 JDK1.5 之前，线程的阻塞和唤醒只能依赖于 `Object` 类提供的 `wait()` 、`notify()`、`notifyAll()` 方法，它们都是由 JVM
提供实现，并且使用的时候需要获取监视器锁（即需要在 `synchronized` 代码块中），没有 Java API
可以阻塞和唤醒线程。唯一可以选择的是 `Thread.suspend` 和 `Thread.resume`
，但是他们都有无法解决的竟态问题：当一个非阻塞线程在一个正准备阻塞的线程调用 `suspend` 之前调用 `resume`，`resume`操作将不起作用。

`j.u.c` 包引入了 `LockSupport` 类，其底层是基于 `Unsafe` 类的 `park()` 和 `unpark()` 方法，`LockSupport.park`
阻塞当前线程，除非或直到发出 `LockSupport.unpark`（虚假唤醒是允许的）。`park` 方法同样支持可选的相对或绝对的超时设置，以及与
JVM 的 `Thread.interrupt` 结合 —— 可通过中断来 `unpark` 一个线程。

### 3.4 条件队列

在 `AQS` 中除了同步队列外，还提供了另一种更为复杂的条件队列，而条件队列是基于 `Condition`
接口实现的，下面我们先浏览一下 `Condition` 接口的说明。

#### 3.4.1 Condition 接口

`Condition` 将 `Object` 的监视器方法（`wait`、`notify` 和 `notifyAll`） 分解到不同的对象，通过将它们与任意的 `Lock`
实现相结合，可以使每个对象具有多个等待集合。`Lock` 代替的 `synchronized` 方法和语句的使用，`Condition` 代替了 `Object`
监视器方法的使用。

`Condition`（也称为 *条件队列(condition queue)* 或 *条件变量(condition variable)*
）为线程提供了一种暂停执行（“等待”）的方法，直到另外一个线程通知说某个状态条件现在可能为 `true`
。由于对这种共享状态信息的访问会发生在多个不同线程中，所以它必须受到保护，因此需要某种形式的锁与条件相关联。等待条件提供的关键属性是它以 *
原子* 方式释放关联的锁并挂起当前线程，就像 `Object.wait` 一样。

`Condition` 实例本质上是需要绑定到锁。需要获取特定 `Lock` 实例的 `Condition` 实例，请使用其 `newCondition()` 方法。

例如，假设我们有一个支持 put 和 take 方法的有界缓冲区。如果 take
在空缓冲区上尝试获取，则线程将阻塞，知道缓冲区变得可用；如果在一个满的缓冲区上调用 `put`，则线程将阻塞，直到有空间可用。我们希望
put 线程继续等待，并且与 take
线程隔开在另一个等待集合中，以便当我们的缓冲区可用或有空间发生变化时通知对应的单个线程。这可以使用量 `Condition` 实例来实现。

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

实现注意事项：

在等待 `Condition ` 时，通常允许发生 *”虚假唤醒“*
，作为对底层平台语义的让步。这对大多数应用程序几乎没有实际影响，因为应该始终在循环中等待 `Condition`
，测试正在等待的状态谓词是否为 `true`。一个实现可以自由地消除虚假唤醒的可能性，但建议应用程序的程序员总是假设它们可以发生，因此总是在循环中等待条件唤醒。

条件等待的三种形式（可中断、不可中断和定时）在某些平台上实现的难易程度和性能特征可能不同。特别是，可能难以提供这些功能并维护特定的语义，例如排序保证。此外，中断线程的实际挂起能力可能并不总是适用所有平台。

因次，实现不需要为所有三种等待形式定义完全相同的保证或语义，也不需要支持线程实际挂起的中断。

实现需要清楚地记录每个等待方法提供的语义和保证，并且当实现确实支持线程挂起的中断时，它必须遵守此接口中定义的中断语义。

由于中断通常意味着取消，并且对中断的检查通常不常见，因此实现可以倾向于响应中断而不是正常的方法返回。即使可以证明中断发生在另一个可能已经解除阻塞线程的操作之后也是如此。一个实现应该记录这个行为。

```java
public interface Condition {

  /**
   * 使当前线程等待，直到它被 signal 或中断。
   *
   * 直到以下四种情况之一发生时，与此 Condition 关联的锁会被自动释放，并且当前线程
   * 由于线程调度会被禁用并处于休眠状态：
   * - 其他某个线程为此 Condition 调用了 signal() 方法，而当前线程恰好被选为要被唤醒的线程；
   * - 其他一些线程为此 Condition 调用了 signalAll() 方法；
   * - 其他一些线程中断当前线程，支持中断线程挂起；
   * - 发生“虚假唤醒”。
   *
   * 在所有情况下，在此方法可以返回之前，当前线程必须重新获取获取与此 Condition 关联的锁。
   * 当前线程返回时，它保证持有这个锁。
   *
   * 如果当前线程：
   * - 在进入此方法时设置其中断状态；或者，
   * - 等待过程中被中断，支持线程挂起的中断。
   *
   * 然后抛出 InterruptedException 并清除当前线程的中断状态。在第一种情况下，没有规定是否
   * 在释放锁之前进行中断判断。
   *
   * 实现注意事项：
   *
   * 调用此方法时，假定当前线程持有与此 Condition 关联的锁。由实现决定是否是这种情况，
   * 如果不是，如何响应。通常，将抛出异常（例如，IllegalMonitorStateException）并且
   * 实现必须记录该事实。
   *
   * 与响应 signal 的正常方法返回相比，实现更倾向于响应中断。在这种情况下，实现必须确保将
   * 信号量重定向到另一个等待线程（如果有的话）。
   *
   * @throws InterruptedException - 如果当前线程被中断（并且支持线程挂起的中断）
   */
  void await() throws InterruptedException;

  /**
   * 使当前线程等待，直到它被 signal。
   *
   * 直到以下三种情况之一发生时，与此 Condition 关联的锁会被自动释放，并且当前线程
   * 由于线程调度会被禁用并处于休眠状态：
   * - 其他某个线程为此 Condition 调用了 signal() 方法，而当前线程恰好被选为要被唤醒的线程；
   * - 其他一些线程为此 Condition 调用了 signalAll() 方法；
   * - 发生“虚假唤醒”。
   *
   * 在所有情况下，在此方法可以返回之前，当前线程必须重新获取获取与此 Condition 关联的锁。
   * 当前线程返回时，它保证持有这个锁。
   *
   * 如果当现场进入该方法时设置了中断状态，或者在等待过程中被中断，则继续等待直到被 signal 唤醒。
   * 当它最终从这个方法返回时，它的中断状态会依旧存在。
   *
   *
   * 实现注意事项：
   *
   * 调用此方法时，假定当前线程持有与此 Condition 关联的锁。由实现决定是否是这种情况，
   * 如果不是，如何响应。通常，将抛出异常（例如，IllegalMonitorStateException）并且
   * 实现必须记录该事实。
   *
   */
  void awaitUninterruptibly();

  /**
   * 使当前线程等待，直到它被 signal 或 中断，或者达到指定的等待时间。
   *
   * 直到以下五种情况之一发生时，与此 Condition 关联的锁会被自动释放，并且当前线程
   * 由于线程调度会被禁用并处于休眠状态：
   * - 其他某个线程为此 Condition 调用了 signal() 方法，而当前线程恰好被选为要被唤醒的线程；
   * - 其他一些线程为此 Condition 调用了 signalAll() 方法；
   * - 其他一些线程中断当前线程，支持中断线程挂起；
   * - 到达指定的等待时间；
   * - 发生“虚假唤醒”。
   *
   * 在所有情况下，在此方法可以返回之前，当前线程必须重新获取获取与此 Condition 关联的锁。
   * 当前线程返回时，它保证持有这个锁。
   *
   * 如果当前线程：
   * - 在进入此方法时设置其中断状态；或者，
   * - 等待过程中被中断，支持线程挂起的中断。
   *
   * 然后抛出 InterruptedException 并清除当前线程的中断状态。在第一种情况下，没有规定是否
   * 在释放锁之前进行中断判断。
   *
   * 在返回时提供给定的 nanosTimeout 值，该方法返回对剩余等待纳秒数的预估，如果超时，则返回
   * 小于或等于零的值。在等待返回但是等待的条件仍不成立的情况下，此值可用于确定是否重新等待以及
   * 重新等待多长时间。此方法的典型用途如以下形式：
   *
   * boolean aMethod(long timeout, TimeUnit unit) {
   *     long nanos = unit.toNanos(timeout);
   *     lock.lock();
   *     try {
   *         while (!conditionBeingWaitedFor()) {
   *             if (nanos <= 0L) 
   *                 return false;
   *             nanos = theCondition.awaitNanos(nanos);
   *         }
   *         // ...
   *     } finally {
   *         lock.unlock();
   *     }
   * }
   *
   * 设计说明：此方法需要纳秒参数，以避免报告剩余时间时出现截断错误。这种精度损失将使程序员
   * 难以确保总等待时间不会系统地短于重新等待发生时指定的时间。
   *
   * 实现注意事项：
   *
   * 调用此方法时，假定当前线程持有与此 Condition 关联的锁。由实现决定是否是这种情况，
   * 如果不是，如何响应。通常，将抛出异常（例如，IllegalMonitorStateException）并且
   * 实现必须记录该事实。
   *
   * 与响应 signal 的正常方法返回相比，实现更倾向于响应中断。在这种情况下，实现必须确保将
   * 信号量重定向到另一个等待线程（如果有的话）。
   *
   * 参数： nanosTimeout - 等待的最长时间，以纳秒为单位。
   * 返回： nanosTimeout值减去从该方法返回时等待的时间的估计值。正值表示可以用作对该方法的
   *       后续调用以完成等待所需时间的参数。小于或等于零表示没有剩余的时间。
   * @throws InterruptedException - 如果当前线程被中断（并且支持线程挂起的中断）
   */
  long awaitNanos(long nanosTimeout) throws InterruptedException;

  /**
   * 使当前线程等待，直到它被 signal 或 中断，或者达到指定的等待时间。此方法在行为上等效于：
   *     awaitNanos(unit.toNanos(time)) > 0 
   *
   * 参数： time - 等待的最长时间
   *       unit - time 参数的时间单位
   * 返回： 如果从方法返回之前已经到达指定时间，则为 false，否则为 true。
   * @throws InterruptedException - 如果当前线程被中断（并且支持线程挂起的中断）
   */
  boolean await(long time, TimeUnit unit) throws InterruptedException;

  /**
   * 使当前线程等待，直到它被 signal 或 中断，或者达到指定的等待时间。
   *
   * 直到以下五种情况之一发生时，与此 Condition 关联的锁会被自动释放，并且当前线程
   * 由于线程调度会被禁用并处于休眠状态：
   * - 其他某个线程为此 Condition 调用了 signal() 方法，而当前线程恰好被选为要被唤醒的线程；
   * - 其他一些线程为此 Condition 调用了 signalAll() 方法；
   * - 其他一些线程中断当前线程，支持中断线程挂起；
   * - 到达指定的等待时间；
   * - 发生“虚假唤醒”。
   *
   * 在所有情况下，在此方法可以返回之前，当前线程必须重新获取获取与此 Condition 关联的锁。
   * 当前线程返回时，它保证持有这个锁。
   *
   * 如果当前线程：
   * - 在进入此方法时设置其中断状态；或者，
   * - 等待过程中被中断，支持线程挂起的中断。
   *
   * 然后抛出 InterruptedException 并清除当前线程的中断状态。在第一种情况下，没有规定是否
   * 在释放锁之前进行中断判断。
   *
   * 返回值表示是否已经过了 deadline，可以如下使用：
   *
   * 实现注意事项：
   *
   * 调用此方法时，假定当前线程持有与此 Condition 关联的锁。由实现决定是否是这种情况，
   * 如果不是，如何响应。通常，将抛出异常（例如，IllegalMonitorStateException）并且
   * 实现必须记录该事实。
   *
   * 与响应 signal 的正常方法返回相比，实现更倾向于响应中断。在这种情况下，实现必须确保将
   * 信号量重定向到另一个等待线程（如果有的话）。
   * boolean aMethod(Date deadline) {
   *     boolean stillWaiting = true;
   *     lock.lock();
   *     try {
   *         while(!conditionBeingWaitedFor()) {
   *             if (!stillWaiting)
   *                 return false;
   *             stillWaiting = theCondition.awaitUntill(deadline);
   *         }
   *         // ...
   *     } finally {
   *         lock.unlock();
   *     }
   * }
   *
   * 参数： deadline - 等待的绝对时间。
   * 返回： 如果返回时已经超过最后期限，则为 false，否则为 true。
   * @throws InterruptedException - 如果当前线程被中断（并且支持线程挂起的中断）
   */
  boolean awaitUntil(Date deadline) throws InterruptedException;

  /**
   * 唤醒一个等待线程。
   *
   * 如果有任何线程在此 Condition 下等待，则选择一个用于唤醒。然后，该线程必须在从
   * await 返回之前重新获取锁。
   *
   * 实现注意事项
   *
   * 在调用此方法时，实现可能（并且通常确实）要求当前线程持有与此 Condition 关联的锁。
   * 实现必须记录此前提条件以及未持有锁时采取的任何操作。通常，会抛出 IllegalMonitorStateException。
   */
  void signal();

  /**
   * 唤醒所有等待线程。
   *
   * 如果有任何线程在此 Condition 下等待，则它们全部都会被唤醒。然后，每个线程必须在从
   * await 返回之前重新获取锁。
   *
   * 实现注意事项
   *
   * 在调用此方法时，实现可能（并且通常确实）要求当前线程持有与此 Condition 关联的锁。
   * 实现必须记录此前提条件以及未持有锁时采取的任何操作。通常，会抛出 IllegalMonitorStateException。
   */
  void signalAll();
}

```

`Condition` 接口提供了与 JAVA 原生的监视器相同风格的 API，但是其并不依赖于 JVM 的实现，用户可以自定义实现 `Condition`
接口，提供更加强大和更加灵活的功能，`Condition` 在说明中建议和 `Lock`
共同使用，可以使每个对象具有多个等待集合，我们下面了解一下 `Lock` 接口 。

#### 3.4.2 Lock 接口

与使用 `synchronized` 方法和语句相比，`Lock`
实现提供了更广泛的锁定操作。它们允许更灵活的结构，可能具有完全不同的属性，并且可能支持多个关联的 `Condition` 对象。

`Lock` 是一种控制多线程访问共享资源的工具。通常，`Lock`
提供对共享资源的独占访问：一次只有一个线程可以获得锁，并且堆共享资源的所有访问都需要首先获取锁。但是，某些锁可能允许并发访问共享资源，例如 `ReadWriteLock`
的读锁。

`synchronized` 方法或语句的使用提供了对于每个对象关键的隐式监视器锁的访问，但强制所有锁的获取和释放必须在块结构内发生：当获取多个锁时，它们必须以相反的顺序释放，并且所有锁必须在获得它们的相同词法范围内释放。

虽然 `synchronized`
方法和语句的作用域机制让使用监视器锁编程变得更加容易，并且有助于避免许多设计锁的常见编程错误，但在某些情况下，您需要以更加灵活的方式使用锁。例如，一些遍历并发访问的数据结构的算法需要使用 `hand-over-hand`
或 `chain locking`：你获取节点 A 的锁，然后获取节点 B 的锁，然后释放 A 并获取 C，然后释放 B 并获取 D 等等。`Lock`
接口的实现通过允许在不同范围内获取和释放锁以及允许以任意顺序获取和释放多个锁，来启用此类技术。

随着这种灵活性的增加，额外的责任也随之而来。块结构锁定的缺失消除了 `synchronized` 方法和语句发生的锁定和自动释放。在大多数情况下，应使用以下语句：

```java
Lock l=...;
        l.lock();
        try{
        // access the resource protected by this lock
        }finally{
        l.unlock;
        }
```

当锁定和解锁发生在不同范围内时，必须注意确保所有在持有锁时执行的代码都受到 `try-finally` 或 `try-catch` 的保护，以确保在必要时释放锁。

`Lock` 实现通过提供非阻塞获取锁定方式（`tryLock()`）、获取可中断锁的尝试（`lockInterruptibly()`
，以及获取锁的尝试）、还提供了超过使用 `synchronized` 方法和语句的附加功能 —— 可以超时（`tryLock(long, Timeunit)`）。

`Lock` 类还可以提供与隐式监视器锁完全不同的行为和语义，例如保证排序、不可重入使用或死锁检测。如果实现提供了这种专门的语义，那么实现必须用文档记录这些语义。

请注意，`Lock` 实例只是普通对象，它们本身可以用作 `synchronized` 语句中的目标。获取 `Lock`
实例的监视器锁与调用该实例的任何 `lock() `
方法没有指定关系。建议为避免混淆，除非在它们自己的实现中，否则不要以这种方式使用 `Lock` 实例。

除非另有说明，否则任何参数传递 `null` 将导致 `NullPointerException`。

**内存同步**：

所有 `Lock` 实现*必须*
强制执行与内置监视器锁提供的相同的内存同步语义。如 [《The Java Language Specification (17.4 Memory Model) 》](https://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.4)
中所述：

- 成功的 *Lock* 动作与成功的 `lock()` 操作具有相同的内存同步效果。
- 成功的 *Unlock* 动作与成功的 `unlock()` 操作具有相同的内存同步效果。

不成功的 lock 和 unlock 操作，以及重入 lock/unlock 操作，不需要任何内存同步效果。

**实现注意事项**：

三种形式的锁获取（可中断、不可中断和超时）可能在它们的性能特征、顺序保证或其他实现质量方面有所不同。此外，中断 *正在进行*
的锁获取的能力在给定的 `Lock`
类中可能不可用。因此，实现不需要为所有的三种形式的锁获取给定完全相同的保证或语义，也不需要支持正在进行的锁获取的中断。实现需要清楚地记录每个锁定方法提供的语义和保证。它们必须遵守此接口中定义的中断语义，一直吃获取锁的中断：完全或仅在方法入口上。

由于中断通常意味着取消，并且对中断的检查通常不常见，因此实现可以倾向于响应中断而不是正常的方法返回。即使可以证明在另一个操作可能已解除阻塞线程之后发生中断也是如此。实现应该用文档记录这个行为。

```java
public interface Lock {

  /**
   * 获取锁。
   *
   * 如果锁不可用，则当前线程处于线程调度的目的，将被禁用并处于休眠状态，直到获得锁为止。
   *
   * 实现注意事项
   *
   * Lock 实现可能能够检测到锁的错误使用，例如会导致死锁的调用，并且在这种情况下可能会抛出
   * （未经检查）的异常。该 Lock 实现必须描述和记录情况以及异常类型。
   */
  void lock();

  /**
   * 除非当前线程被中断，否则获取锁。
   *
   * 如果可用，则获取锁并立即返回。
   *
   * 如果锁不可用，则当前线程处于线程调度的目的，将被禁用并处于休眠状态，直到发生以下两种情况之一：
   * - 锁被当前线程获取；
   * - 其他一些线程中断当前线程，当前线程支持获取锁的中断。
   *
   * 如果当前线程：
   * - 在进入此方法时设置其中断状态；
   * - 获取锁时中断，并支持获取锁中断。
   *
   * 然后会抛出 InterruptedException 并清除当前线程的中断状态。
   *
   *
   * 实现注意事项
   *
   * 在某些实现中中断锁获取的能力可能是无法实现的，并且如果可能的话会是一个非常昂贵的操作。
   * 程序员应该意识到可能是这种情况，并详细记录和描述这种情况。。
   *
   * 与正常方法返回相比，实现更倾向于响应中断。
   *
   * Lock 实现可能能够检测到锁的错误使用，例如会导致死锁的调用，并且在这种情况下可能会抛出
   * （未经检查的）异常。该 Lock 实现必须详细记录情况和异常类型。
   *
   * @throws InterruptedException - 如果当前线程在获取锁时被中断（并且支持获取锁的中断）
   */
  void lockInterruptibly() throws InterruptedException;

  /**
   * 仅当调用时是空闲的，才获取到锁。
   *
   * 如果锁可用，则获取锁并立即返回 true。如果锁不可用，则此方法立即返回 false。
   *
   * 该方法的典型用法是：
   *
   * Lock lock = ...;
   * if (lock.tryLock()) {
   *     try {
   *         // manipulate protected state
   *     } finally {
   *         lock.unlock();
   *     }
   * } else {
   *     // perform alternative actions
   * }
   *
   * 这种方法确保锁在获得的情况下才解锁，并且在未获得的时候不进行解锁操作。
   *
   * 返回： 如果获得了锁返回 true，否则为 false。
   */
  boolean tryLock();

  /**
   * 如果在给定的等待时间内锁空闲并且当前线程没有被中断，则获取锁。
   *
   * 如果锁可用，则获取锁并立即返回 true。如果锁不可用，则当前线程处于线程调度的目的，
   * 将被禁用并处于休眠状态，直到发生以下三种情况之一：
   * - 锁被当前线程获取；
   * - 其他一些线程中断当前线程，当前线程支持获取锁的中断；
   * - 指定的等待时间已过。
   *
   * 如果获得锁，则返回 true。
   *
   * 如果当前线程：
   * - 在进入此方法时设置其为中断状态；或
   * - 获取锁时中断，并支持获取锁中断。
   *
   * 然后会抛出 InterruptedException 并清除当前线程的中断状态。
   *
   * 如果经过了指定的等待时间，则返回 false。如果时间小于或等于 0，则该方法不会等待。
   *
   * 实现注意事项
   *
   * 在某些实现中中断锁获取的能力可能是无法实现的，并且如果可能的话会是一个非常昂贵的操作。
   * 程序员应该意识到可能是这种情况，并详细记录和描述这种情况。。
   *
   * 与正常方法返回相比，实现更倾向于响应中断。
   *
   * Lock 实现可能能够检测到锁的错误使用，例如会导致死锁的调用，并且在这种情况下可能会抛出
   * （未经检查的）异常。该 Lock 实现必须详细记录情况和异常类型。
   *
   * 参数： time - 等待锁的最长时间
   *       unit - time 参数的时间单位
   * 返回： 如果获得了锁，返回 true；如果在获得锁之前超过了等待时间，返回 false
   * @throws InterruptedException - 如果当前线程在获取锁时被中断（并且支持获取锁的中断）
   */
  boolean tryLock(long time, TimeUnit unit) throws InterruptedException;

  /**
   * 释放锁。
   *
   * 实现注意事项
   *
   * Lock 实现通常会对哪个线程可以释放锁施加限制（通常只有锁的持有者可以释放它），
   * 并且如果违反限制可能会抛出（未经检查的）异常。该 Lock 实现必须详细记录情况和异常类型。
   */
  void unlock();

  /**
   * 返回绑定到此 Lock 实例的新 Condition 实例。
   *
   * 在等待条件之前，锁必须由当前线程持有。调用 Condition.await() 将在等待之前自动释放
   * 锁，并在等待返回之前重新获取锁。
   *
   * 实现注意事项
   *
   * Condition 实例的确切操作取决于 Lock 实现，并且必须由该实现描述。
   *
   *
   * 返回：此 Lock 实例的新 Condition 实例
   * @throws UnsupportedOperationException - 如果 Lock 实现不支持 Condition
   */
  Condition newCondition();
}
```

#### 3.4.3 AQS 中的 ConditionObject

在 `AQS` 内部也存在这 `Condition` 接口的实现类，即 `ConditionObject`，它是 `AQS`的共有内部类，并且它是 `Lock`
实现的基础。`ConditionObject` 提供的条件队列的入队的方法如下：

```java
public class ConditionObject implements Condition, java.io.Serializable {
  private static final long serialVersionUID = 1173984872572414699L;
  /** 条件队列的第一个节点 */
  private transient Node firstWaiter;
  /** 条件队列的最后一个节点 */
  private transient Node lastWaiter;

  /**
   * Creates a new {@code ConditionObject} instance.
   */
  public ConditionObject() {
  }

  // 内部方法

  /**
   * 为等待队列添加一个新的等待节点
   * @return 新的等待节点
   */
  private Node addConditionWaiter() {
    // 本地变量保存 lastWaiter
    Node t = lastWaiter;
    // 如果 lastWaiter 不为条件等待状态，则说明 lastWaiter 是取消状态，清理
    if (t != null && t.waitStatus != Node.CONDITION) {
      // 解除所有取消的等待节点的连接
      unlinkCancelledWaiters();
      t = lastWaiter;
    }
    // 创建当前线程的新节点，类型为 CONDITION
    Node node = new Node(Thread.currentThread(), Node.CONDITION);
    // 在首次创建 Condition 时，lastWaiter 为 null，则把当前节点设置为 firstWaiter 
    if (t == null)
      firstWaiter = node;
    else
      // lastWaiter 不为空，则连接新节点
      t.nextWaiter = node;
    // 当前新增节点为 lastWaiter
    lastWaiter = node;
    return node;
  }

  /**
   * 从条件队列中取消连接已取消的等待节点。仅在持有锁时调用。当前方法会在条件等待期间
   * 发生取消时被调用，并且在 lastWaiter 已被取消时插入新的等待节点时调用。需要这种
   * 方法来避免在没有 signal 的情况下保留垃圾。因此，即使它可能需要完全遍历，它也只有
   * 在没有被 signal 的情况下发生超时或取消时才发挥作用。它遍历所有节点，而不是在特定
   * 目标处停止以取消连接到垃圾节点的所有指针，因此不会在取消风暴期间进行多次重新遍历。
   *
   * 简单来说，此方法就是更新队列，移除所有 CANCELLED 的节点，期间会 firstWaiter 和
   * lastWaiter 的引用
   */
  private void unlinkCancelledWaiters() {
    // 保存当前的 firstWaiter 
    Node t = firstWaiter;
    // 跟踪节点，用于最后找到 lastWaiter
    Node trail = null;
    while (t != null) {
      // 从 firstWaiter 开始往后遍历
      Node next = t.nextWaiter;
      // 当前节点不是 CONDITION，那么就是 CANCELLED
      if (t.waitStatus != Node.CONDITION) {
        // 取消当前节点的引用
        t.nextWaiter = null;
        // trail 为空，说明当前还未遇到第一个 CONDITION 状态的节点
        if (trail == null)
          // 将 firstWaiter 暂时设置为 下个节点
          firstWaiter = next;
        else
          // 将 next 链接到追踪节点
          trail.nextWaiter = next;
        // 遍历结束
        if (next == null)
          // lastWaiter 即 trail 的最后一个节点
          lastWaiter = trail;
      } else
        // CONDITION 节点，记录当前节点
        trail = t;
      // 更新当前节点为 next
      t = next;
    }
  }

  // 公共方法

  /**
   * 将等待时间最长的线程（如果存在）从该条件队列转换到拥有锁的等待队列。
   *
   * @throws IllegalMonitorStateException 如果 isHeldExclusively 返回 false
   */
  public final void signal() {
    // 当前同步器持有的线程是否是当前线程
    if (!isHeldExclusively())
      throw new IllegalMonitorStateException();
    // 等待时间最长的就是第一个入队的 fistWaiter
    Node first = firstWaiter;
    if (first != null)
      // 唤醒节点
      doSignal(first);
  }

  /**
   * 将所有线程从该条件等待队列转换到拥有锁的等待队列。
   *
   * @throws IllegalMonitorStateException 如果 isHeldExclusively 返回 false
   */
  public final void signalAll() {
    // // 当前同步器持有的线程是否是当前线程
    if (!isHeldExclusively())
      throw new IllegalMonitorStateException();
    Node first = firstWaiter;
    if (first != null)
      // 唤醒所有节点
      doSignalAll(first);
  }

  /**
   * 实现非中断的条件等待。
   *
   * 1. 保存 getStatus() 返回的锁定状态。
   * 2. 使用保存的状态作为参数调用 release()，如果失败抛出 IllegalMonitorStateException。
   * 3. 阻塞直到 signal。
   * 4. 将保存的状态作为参数调用特定版本的 acquire() 来重新获取锁。
   */
  public final void awaitUninterruptibly() {
    // 添加新的等待节点
    Node node = addConditionWaiter();
    // release 当前 AQS 的所有资源，并返回资源的 state
    int savedState = fullyRelease(node);
    // 是否中断
    boolean interrupted = false;
    // 判断当前节点是否是同步队列节点，理论上新增的应当是不在同步队列，当被唤醒时，如果加锁成功则会在同步队列
    while (!isOnSyncQueue(node)) {
      // 阻塞当前节点
      LockSupport.park(this);
      // 判断当前线程是否中断
      if (Thread.interrupted())
        interrupted = true;
    }
    // 如果当前线程被中断，或在加锁过程中中断，则对当前线程进行中断操作
    if (acquireQueued(node, savedState) || interrupted)
      selfInterrupt();
  }

  /**
   * 删除并转换节点，直到命中未取消的节点或 null。从 signal 中分离出来部分是为了
   * 编译器内联没有等待节点的情况。
   *
   * @param first (非空) 条件队列中的第一个节点
   */
  private void doSignal(Node first) {
    do {
      // 第一个节点的 nextWaiter 为空，说明目前只有一个等待节点
      if ((firstWaiter = first.nextWaiter) == null)
        lastWaiter = null;
      // 将当前处理节点从条件队列移除
      first.nextWaiter = null;
      // 转换当前节点
    } while (!transferForSignal(first) &&
            // 转换失败，此时的 firstWaiter 是 first 的 nextWaiter 节点
            (first = firstWaiter) != null);
  }

  /**
   * 移除并转换所有节点
   * @param first (非空) 条件队列中的第一个节点
   */
  private void doSignalAll(Node first) {
    // 全部转换，则将 lastWaiter 和 firstWaiter 置空
    lastWaiter = firstWaiter = null;
    do {
      // 获取下一个等待节点
      Node next = first.nextWaiter;
      // 下一个等待节点移除
      first.nextWaiter = null;
      // 处理当前节点
      transferForSignal(first);
      // 更新下个节点为处理节点
      first = next;
    } while (first != null);
  }
  // 暂时不展示其他方法
}
```

我们在观察 `ConditionObject` 类后可以发现，所有的 `await` 方法及其变体都会调用 `addConditionWaiter()`
方法，将阻塞线程添加到添加队列中。我们下面演示一下条件队列入队的情况下，假设存在两个线程 `thread-1` 和 `thread-2`
需要阻塞入队，首先是 `thread-1` 入队：

![thread-1-enq](/Users/wenbo.zhang/Desktop/images/condition-queue-thread-1-enq.png)

在 `thread-1` 入队后等待过程中，`thread-2` 入队：

![thread-2-enq](/Users/wenbo.zhang/Desktop/images/condition-queue-thread-2-enq.png)

之后线程入队就如上面操作一样，只需修改 lastWaiter 和 nextWaiter 指向新节点即可，其基本原理暂时介绍到这里，后面我们会根据源码详细介绍。

### 四、AQS 的独占与共享

在 `AQS` 的设计中，为我们保留的扩展的能力，我们可以使用 `ConditionObject` 和 `AQS`
去实现共享资源的独占和共享，就和 `ReadWriteLock` 一样，下面我们根据 `AQS` 的源码来解析这两种模式是如何实现的。

#### 4.1 独占模式

独占模式：意味着同一时刻，共享资源只有唯一的单个节点可以获取访问，此时获取到锁的节点的线程是独享的，获取到锁的线程也就从阻塞状态可以继续运行，而同步队列的其他节点则需要继续阻塞。

独占模式的实现主要由 `AQS` 在初始化时， `status` 值来确定允许申请资源的数量上限，而对共享资源的获取和释放主要由以下方法进行操作：

- `acquire(int)` ：获取 int 数量的资源，也就是原子修改 `status`。
- `acquireInterruptibly(int)`：获取 int 数量的资源，可以响应线程中断。
- `tryAcquireNanos(int, long)` ：在指定 long 时间内，获取 int 数量的资源。
- `release(int)` ：释放 int 数量的资源。

##### 4.1.1 acquire

下面我们根据源码，了解一下独占模式是如何运行的，首先是 `acquire`：

```java
/**
 * 以独占模式获取锁，忽略中断。  通过调用至少一次 tryAcquire() 方法来实现，成功就返回。
 * 否则线程排队，调用 tryAcquire() 成功之前，可能重复阻塞和解除阻塞。此方法可用于实现
 * Lock.lock()。
 *
 * 参数：arg - acquire 参数。这个值被传递给 tryAcquire，你可以用此代表你喜欢的任何东西。
 */
public final void acquire(int arg){
        // 只有当加锁成功或以独占类型节点入队（同步队列，非条件队列）成功时返回，
        if(!tryAcquire(arg)&&
        // 加锁失败，则进行入队操作
        acquireQueued(addWaiter(Node.EXCLUSIVE),arg))
        // 加锁失败，入队失败，则中断线程
        selfInterrupt();
        }

/**
 * 尝试以独占模式 acquire。此方法应查询对象的状态，判断是否允许以独占模式获取它。
 *
 * 此方法始终由执行 acquire 的线程调用。如果此方法报告失败，且该线程尚未入队，
 * 则 acquire 方法可以将该线程排队，知道某个其他线程 release 并 signal。这
 * 可用于实现 Lock.tryLock 方法。
 *
 * 默认实现抛出 UnsupportedOperationException 。
 *
 * 参数：arg - acquire 参数.。该值始终是传递给 acquire 方法的值，或者是在进入条件等待时
 保存的值。该值可以表示你喜欢的任何东西。
 * 返回：如果成功，返回 true。成功后，该对象已 acquire。
 * @throws IllegalMonitorStateException  如果获取会将此同步器置于非法状态。
 *                                       必须以一致的方式抛出此异常，同步才能正常工作。
 * @throws UnsupportedOperationException 如果不支持独占模式
 */
protected boolean tryAcquire(int arg){
        throw new UnsupportedOperationException();
        }


/**
 * 为当前线程和给定模式创建节点并入队节点。
 *
 * 参数：mode - Node.EXCLUSIVE 用于独占，Node.SHARED 用于共享
 * 返回：新节点
 */
private Node addWaiter(Node mode){
        // 创建当前线程和模式的新节点，此时 waitStatus 为 0
        Node node=new Node(Thread.currentThread(),mode);
        // 先尝试直接入队，当且仅当 tail 不为空时，直接将当前节点追加到 tail 后面
        Node pred=tail;
        if(pred!=null){
        // 当前节点的前驱节点为 pred
        node.prev=pred;
        // 原子修改 tail 为当前节点
        if(compareAndSetTail(pred,node)){
        // pred 的后继节点指向当前节点
        pred.next=node;
        return node;
        }
        }
        // tail 为空，或入队失败，则进行自旋 enq 入队
        enq(node);
        return node;
        }

/**
 * 将节点插入队列，必要时进行初始化。
 * 参数： node - 插入的节点
 * 返回： 节点的前驱节点
 */
private Node enq(final Node node){
        // 自旋进行插入操作
        for(;;){
        // 获取队列的 tail
        Node t=tail;
        // t 为空，说明队尾没有节点，说明还没有初始化
        if(t==null){ // Must initialize
        // 初始化操作，创建 head 节点
        if(compareAndSetHead(new Node()))
        // 将 tail 也指向 head
        tail=head;
        }else{
        // 将队尾指向当前节点的前驱节点
        node.prev=t;
        // 设置当前节点为队尾
        if(compareAndSetTail(t,node)){
        // 设置 t 的后继节点为当前节点
        t.next=node;
        return t;
        }
        }
        }
        }


/**
 * 以独占模式且不中断，acquire 队列中的线程。由 condition 的 wait 和 acquire 方法使用。
 *
 * 参数：node - 节点
 *      arg - acquire 参数
 * 返回：如果在等待时被中断，返回 true
 */
final boolean acquireQueued(final Node node,int arg){
        // acquire 是否失败
        boolean failed=true;
        try{
        // 是否中断
        boolean interrupted=false;
        // 自旋尝试获取资源，每次自旋都会调用 tryAcquire 尝试获取资源，获取资源失败，则进入阻塞状态
        // 成功则跳出自旋
        for(;;){
// 当前新入队节点的前驱节点
final Node p=node.predecessor();
        // 前驱节点为头节点时，尝试获取资源。
        if(p==head&&tryAcquire(arg)){
        // 获取资源成功，将当前节点设置为头结点
        setHead(node);
        // 断开前一个节点的链接，帮助 GC
        p.next=null; // help GC
        // 获取成功
        failed=false;
        // 返回是否中断
        return interrupted;
        }
        // 判断在 acquire 失败后是否需要阻塞当前节点中的线程
        if(shouldParkAfterFailedAcquire(p,node)&&
        parkAndCheckInterrupt())
        interrupted=true;
        }
        }finally{
        if(failed)
        cancelAcquire(node);
        }
        }

/**
 * 检查并更新 acquire 失败的节点的状态。如果线程应该阻塞，则返回 true。
 * 这是所有循环 acquire 获取资源的主要 signal 控制方法。要求 pred == node.prev。
 *
 * 参数：pred - 节点的前驱节点持有的状态
 *      node - 当前节点
 * 返回：如果线程应该阻塞，返回 true。
 */
private static boolean shouldParkAfterFailedAcquire(Node pred,Node node){
        // 前驱节点的等待状态
        int ws=pred.waitStatus;
        // 前驱结点状态为 SIGNAL，说明当前节点可以阻塞，pred 在完成后需要调用 release
        if(ws==Node.SIGNAL)
        /*
         * 前驱节点状态设置为 Node.SIGNAL，等待被 release 调用释放，后继节点可以安全地进入阻塞。
         */
        return true;
        if(ws>0){
        /*
         * 前驱节点为 CANCELLED，尝试把所有 CANCELLED 的前驱节点移除，找到一个
         * 非取消的前驱节点。
         */
        do{
        node.prev=pred=pred.prev;
        }while(pred.waitStatus>0);
        pred.next=node;
        }else{
        /*
         * waitStatus 为 0 或 PROPAGATE.  表示我们需要一个 signal，
         * 而不是阻塞。调用者需要重试以确保在阻塞前无法 acquire。
         */
        compareAndSetWaitStatus(pred,ws,Node.SIGNAL);
        }
        return false;
        }

/**
 * park 后检查是否中断的便捷方法
 *
 * 返回：如果中断，返回true
 */
private final boolean parkAndCheckInterrupt(){
        // park 当前线程
        LockSupport.park(this);
        // 判断是否中断
        return Thread.interrupted();
        }


/**
 * 将队列 head 设置为 node，从而使之前的节点出队。仅由 acquire 方法调用。
 * 为了 GC 和抑制不必要的 signal 和遍历，同时也清空无用的字段。
 *
 * 参数：node - 节点
 */
private void setHead(Node node){
        head=node;
        node.thread=null;
        node.prev=null;
        }
```

依旧使用上面的例子，当 `thread-1` 入队时，此时队列为空，需要初始化一个空节点，之后将调用 `addWaiter()` 将  `thread-1` 入队：

![aqs-thread-1-enq](/Users/wenbo.zhang/Desktop/images/AQS-thread-1-enq.png)

此时，在 `thread-1` 等待过程中，将 `thread-2` 进行入队操作：

![aqs-thread-2-enq](/Users/wenbo.zhang/Desktop/images/AQS-thread-2-enq.png)

以上就是 `tryAcquire` 失败后的入队逻辑，可以看到，在节点进行入队时，会修改前驱节点的 waitStatus，当前驱节点 `release`
时，会进行哪些操作呢？下面我们对 `release` 操作进行解析。

##### 4.1.2 release

在独占模式中，`release()` 用来释放资源，下面我们根据源码来解读 `AQS` 如何进行释放操作。

```java
/**
 * 释放独占模式。如果 tryRelease 返回 true，则通过解锁一个或多个线程实现。此方法可以
 * 用来实现方法 Lock.unlock.
 *
 * 参数：arg - release 参数。这个值被传递给 tryRelease，你可以用它表示任何你喜欢的东西。
 * 返回：tryRelease 返回的值
 */
public final boolean release(int arg){
        // 尝试释放资源
        if(tryRelease(arg)){
        Node h=head;
        // head 不为空，且 waitStatus 不为 0 的情况下，唤醒后继节点
        if(h!=null&&h.waitStatus!=0)
        // 后继节点解除阻塞
        unparkSuccessor(h);
        return true;
        }
        return false;
        }

/**
 * 尝试设置状态，以体现独占模式下的 release。
 *
 * 该方法总是由执行 release 的线程调用。
 *
 * 默认实现抛出 UnsupportedOperationException。
 *
 * 参数：arg - release 参数。此值始终是传递给 release 方法的值，或者是进入条件等待时的
 *            当前状态值。该值是未解释的，可以表示任何你想要的内容。
 *        uninterpreted and can represent anything you like.
 * 返回：如果当前对象现在完全释放，则返回 true，以便任何等待的线程都可以尝试 acquire；否则 false。
 * @throws IllegalMonitorStateException - 如果 release 会将此同步器置于非法状态。
 *                                        必须以一致的方式抛出此异常，同步器才能正常工作。
 * @throws UnsupportedOperationException - 如果不支持独占模式
 */
protected boolean tryRelease(int arg){
        throw new UnsupportedOperationException();
        }

/**
 * 如果节点存在后继节点，则唤醒后继节点。
 *
 * 参数：node - 节点
 */
private void unparkSuccessor(Node node){
        /*
         * 如果状态为负数（即可能需要 singal），尝试 clear 以等待 signal。
         * 允许失败或等待线程更改状态。
         */
        int ws=node.waitStatus;
        if(ws< 0)
        // 将当前节点的 waitStatus 置为 0
        compareAndSetWaitStatus(node,ws,0);

        /*
         * 当前线程的后继节点 unpark ，通常只是下一个节点。但如果下个节点为空或
         * 已经取消，则从 tail 向后遍历以找到实际未取消的后继节点。
         */
        Node s=node.next;
        // 后继节点为空，或后继节点是 CANCELLED
        if(s==null||s.waitStatus>0){
        s=null;
        // 从 tail 开始，向 head 遍历，找到最接近 当前节点的不为空且未取消的节点
        for(Node t=tail;t!=null&&t!=node;t=t.prev)
        if(t.waitStatus<=0)
        s=t;
        }
        // 找到之后，unpark 节点线程阻塞状态
        if(s!=null)
        LockSupport.unpark(s.thread);
        }
```

当 `release` 操作成功 `unpark` 一个线程后，该线程在通过 `acquireQueued` 进行 `tryAcquire`
成功后，就会将头结点设置为当前节点，并将之前的头结点以及线程字段置空，以方便 GC 回收，`thread-1` 获取到锁在执行过程中，状态如下：

![aqs-thread-1-release](/Users/wenbo.zhang/Desktop/images/AQS-thread-1-release.png)

`thread-1` 执行完成后，对 `thread-2` 进行 unpark 后，状态如下：

![aqs-thread-2-release](/Users/wenbo.zhang/Desktop/images/AQS-thread-2-release.png)

##### 4.1.3 acquireInterruptibly

下面我们对 `acquire` 的变体，即带有响应中断版本的 `acquireInterruptibly` 方法进行解析：

```java
/**
 * Acquires in exclusive mode, aborting if interrupted.
 * Implemented by first checking interrupt status, then invoking
 * at least once {@link #tryAcquire}, returning on
 * success.  Otherwise the thread is queued, possibly repeatedly
 * blocking and unblocking, invoking {@link #tryAcquire}
 * until success or the thread is interrupted.  This method can be
 * used to implement method {@link Lock#lockInterruptibly}.
 *
 * @param arg the acquire argument.  This value is conveyed to
 *        {@link #tryAcquire} but is otherwise uninterpreted and
 *        can represent anything you like.
 * @throws InterruptedException if the current thread is interrupted
 */
public final void acquireInterruptibly(int arg)
        throws InterruptedException{
        if(Thread.interrupted())
        throw new InterruptedException();
        if(!tryAcquire(arg))
        doAcquireInterruptibly(arg);
        }
```



