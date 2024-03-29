/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.locks;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import sun.misc.Unsafe;

/**
 * Provides a framework for implementing blocking locks and related
 * synchronizers (semaphores, events, etc) that rely on
 * first-in-first-out (FIFO) wait queues.  This class is designed to
 * be a useful basis for most kinds of synchronizers that rely on a
 * single atomic {@code int} value to represent state. Subclasses
 * must define the protected methods that change this state, and which
 * define what that state means in terms of this object being acquired
 * or released.  Given these, the other methods in this class carry
 * out all queuing and blocking mechanics. Subclasses can maintain
 * other state fields, but only the atomically updated {@code int}
 * value manipulated using methods {@link #getState}, {@link
 * #setState} and {@link #compareAndSetState} is tracked with respect
 * to synchronization.
 *
 * <p>Subclasses should be defined as non-public internal helper
 * classes that are used to implement the synchronization properties
 * of their enclosing class.  Class
 * {@code AbstractQueuedSynchronizer} does not implement any
 * synchronization interface.  Instead it defines methods such as
 * {@link #acquireInterruptibly} that can be invoked as
 * appropriate by concrete locks and related synchronizers to
 * implement their public methods.
 *
 * <p>This class supports either or both a default <em>exclusive</em>
 * mode and a <em>shared</em> mode. When acquired in exclusive mode,
 * attempted acquires by other threads cannot succeed. Shared mode
 * acquires by multiple threads may (but need not) succeed. This class
 * does not &quot;understand&quot; these differences except in the
 * mechanical sense that when a shared mode acquire succeeds, the next
 * waiting thread (if one exists) must also determine whether it can
 * acquire as well. Threads waiting in the different modes share the
 * same FIFO queue. Usually, implementation subclasses support only
 * one of these modes, but both can come into play for example in a
 * {@link ReadWriteLock}. Subclasses that support only exclusive or
 * only shared modes need not define the methods supporting the unused mode.
 *
 * <p>This class defines a nested {@link ConditionObject} class that
 * can be used as a {@link Condition} implementation by subclasses
 * supporting exclusive mode for which method {@link
 * #isHeldExclusively} reports whether synchronization is exclusively
 * held with respect to the current thread, method {@link #release}
 * invoked with the current {@link #getState} value fully releases
 * this object, and {@link #acquire}, given this saved state value,
 * eventually restores this object to its previous acquired state.  No
 * {@code AbstractQueuedSynchronizer} method otherwise creates such a
 * condition, so if this constraint cannot be met, do not use it.  The
 * behavior of {@link ConditionObject} depends of course on the
 * semantics of its synchronizer implementation.
 *
 * <p>This class provides inspection, instrumentation, and monitoring
 * methods for the internal queue, as well as similar methods for
 * condition objects. These can be exported as desired into classes
 * using an {@code AbstractQueuedSynchronizer} for their
 * synchronization mechanics.
 *
 * <p>Serialization of this class stores only the underlying atomic
 * integer maintaining state, so deserialized objects have empty
 * thread queues. Typical subclasses requiring serializability will
 * define a {@code readObject} method that restores this to a known
 * initial state upon deserialization.
 *
 * <h3>Usage</h3>
 *
 * <p>To use this class as the basis of a synchronizer, redefine the
 * following methods, as applicable, by inspecting and/or modifying
 * the synchronization state using {@link #getState}, {@link
 * #setState} and/or {@link #compareAndSetState}:
 *
 * <ul>
 * <li> {@link #tryAcquire}
 * <li> {@link #tryRelease}
 * <li> {@link #tryAcquireShared}
 * <li> {@link #tryReleaseShared}
 * <li> {@link #isHeldExclusively}
 * </ul>
 *
 * Each of these methods by default throws {@link
 * UnsupportedOperationException}.  Implementations of these methods
 * must be internally thread-safe, and should in general be short and
 * not block. Defining these methods is the <em>only</em> supported
 * means of using this class. All other methods are declared
 * {@code final} because they cannot be independently varied.
 *
 * <p>You may also find the inherited methods from {@link
 * AbstractOwnableSynchronizer} useful to keep track of the thread
 * owning an exclusive synchronizer.  You are encouraged to use them
 * -- this enables monitoring and diagnostic tools to assist users in
 * determining which threads hold locks.
 *
 * <p>Even though this class is based on an internal FIFO queue, it
 * does not automatically enforce FIFO acquisition policies.  The core
 * of exclusive synchronization takes the form:
 *
 * <pre>
 * Acquire:
 *     while (!tryAcquire(arg)) {
 *        <em>enqueue thread if it is not already queued</em>;
 *        <em>possibly block current thread</em>;
 *     }
 *
 * Release:
 *     if (tryRelease(arg))
 *        <em>unblock the first queued thread</em>;
 * </pre>
 *
 * (Shared mode is similar but may involve cascading signals.)
 *
 * <p id="barging">Because checks in acquire are invoked before
 * enqueuing, a newly acquiring thread may <em>barge</em> ahead of
 * others that are blocked and queued.  However, you can, if desired,
 * define {@code tryAcquire} and/or {@code tryAcquireShared} to
 * disable barging by internally invoking one or more of the inspection
 * methods, thereby providing a <em>fair</em> FIFO acquisition order.
 * In particular, most fair synchronizers can define {@code tryAcquire}
 * to return {@code false} if {@link #hasQueuedPredecessors} (a method
 * specifically designed to be used by fair synchronizers) returns
 * {@code true}.  Other variations are possible.
 *
 * <p>Throughput and scalability are generally highest for the
 * default barging (also known as <em>greedy</em>,
 * <em>renouncement</em>, and <em>convoy-avoidance</em>) strategy.
 * While this is not guaranteed to be fair or starvation-free, earlier
 * queued threads are allowed to recontend before later queued
 * threads, and each recontention has an unbiased chance to succeed
 * against incoming threads.  Also, while acquires do not
 * &quot;spin&quot; in the usual sense, they may perform multiple
 * invocations of {@code tryAcquire} interspersed with other
 * computations before blocking.  This gives most of the benefits of
 * spins when exclusive synchronization is only briefly held, without
 * most of the liabilities when it isn't. If so desired, you can
 * augment this by preceding calls to acquire methods with
 * "fast-path" checks, possibly prechecking {@link #hasContended}
 * and/or {@link #hasQueuedThreads} to only do so if the synchronizer
 * is likely not to be contended.
 *
 * <p>This class provides an efficient and scalable basis for
 * synchronization in part by specializing its range of use to
 * synchronizers that can rely on {@code int} state, acquire, and
 * release parameters, and an internal FIFO wait queue. When this does
 * not suffice, you can build synchronizers from a lower level using
 * {@link java.util.concurrent.atomic atomic} classes, your own custom
 * {@link java.util.Queue} classes, and {@link LockSupport} blocking
 * support.
 *
 * <h3>Usage Examples</h3>
 *
 * <p>Here is a non-reentrant mutual exclusion lock class that uses
 * the value zero to represent the unlocked state, and one to
 * represent the locked state. While a non-reentrant lock
 * does not strictly require recording of the current owner
 * thread, this class does so anyway to make usage easier to monitor.
 * It also supports conditions and exposes
 * one of the instrumentation methods:
 *
 *  <pre> {@code
 * class Mutex implements Lock, java.io.Serializable {
 *
 *   // Our internal helper class
 *   private static class Sync extends AbstractQueuedSynchronizer {
 *     // Reports whether in locked state
 *     protected boolean isHeldExclusively() {
 *       return getState() == 1;
 *     }
 *
 *     // Acquires the lock if state is zero
 *     public boolean tryAcquire(int acquires) {
 *       assert acquires == 1; // Otherwise unused
 *       if (compareAndSetState(0, 1)) {
 *         setExclusiveOwnerThread(Thread.currentThread());
 *         return true;
 *       }
 *       return false;
 *     }
 *
 *     // Releases the lock by setting state to zero
 *     protected boolean tryRelease(int releases) {
 *       assert releases == 1; // Otherwise unused
 *       if (getState() == 0) throw new IllegalMonitorStateException();
 *       setExclusiveOwnerThread(null);
 *       setState(0);
 *       return true;
 *     }
 *
 *     // Provides a Condition
 *     Condition newCondition() { return new ConditionObject(); }
 *
 *     // Deserializes properly
 *     private void readObject(ObjectInputStream s)
 *         throws IOException, ClassNotFoundException {
 *       s.defaultReadObject();
 *       setState(0); // reset to unlocked state
 *     }
 *   }
 *
 *   // The sync object does all the hard work. We just forward to it.
 *   private final Sync sync = new Sync();
 *
 *   public void lock()                { sync.acquire(1); }
 *   public boolean tryLock()          { return sync.tryAcquire(1); }
 *   public void unlock()              { sync.release(1); }
 *   public Condition newCondition()   { return sync.newCondition(); }
 *   public boolean isLocked()         { return sync.isHeldExclusively(); }
 *   public boolean hasQueuedThreads() { return sync.hasQueuedThreads(); }
 *   public void lockInterruptibly() throws InterruptedException {
 *     sync.acquireInterruptibly(1);
 *   }
 *   public boolean tryLock(long timeout, TimeUnit unit)
 *       throws InterruptedException {
 *     return sync.tryAcquireNanos(1, unit.toNanos(timeout));
 *   }
 * }}</pre>
 *
 * <p>Here is a latch class that is like a
 * {@link java.util.concurrent.CountDownLatch CountDownLatch}
 * except that it only requires a single {@code signal} to
 * fire. Because a latch is non-exclusive, it uses the {@code shared}
 * acquire and release methods.
 *
 *  <pre> {@code
 * class BooleanLatch {
 *
 *   private static class Sync extends AbstractQueuedSynchronizer {
 *     boolean isSignalled() { return getState() != 0; }
 *
 *     protected int tryAcquireShared(int ignore) {
 *       return isSignalled() ? 1 : -1;
 *     }
 *
 *     protected boolean tryReleaseShared(int ignore) {
 *       setState(1);
 *       return true;
 *     }
 *   }
 *
 *   private final Sync sync = new Sync();
 *   public boolean isSignalled() { return sync.isSignalled(); }
 *   public void signal()         { sync.releaseShared(1); }
 *   public void await() throws InterruptedException {
 *     sync.acquireSharedInterruptibly(1);
 *   }
 * }}</pre>
 *
 * @since 1.5
 * @author Doug Lea
 */
/*
        #*volatile
            当一个变量被定义成volatile之后，它将具备两项特性：第一项是保证此变量对所有线程的可见 性，这里的“可见性”是指当一条线程修改了这个变量的值，
        新值对于其他线程来说是可以立即得知 的。而普通变量并不能做到这一点，普通变量的值在线程间传递时均需要通过主内存来完成。比如， 线程A修改一个普通变量的值，
        然后向主内存进行回写，另外一条线程B在线程A回写完成了之后再对 主内存进行读取操作，新变量值才会对线程B可见。

            关于volatile变量的可见性，经常会被开发人员误解，他们会误以为下面的描述是正确的：“volatile 变量对所有线程是立即可见的，对volatile变量所有的写操作都能立
        刻反映到其他线程之中。换句话 说，volatile变量在各个线程中是一致的，所以基于volatile变量的运算在并发下是线程安全的”。这句话 的论据部分并没有错，但是由其论据并
        不能得出“基于volatile变量的运算在并发下是线程安全的”这样 的结论。volatile变量在各个线程的工作内存中是不存在一致性问题的（从物理存储的角度看，各个线程的工作
        内存中volatile变量也可以存在不一致的情况，但由于每次使用之前都要先刷新，执行引擎看 不到不一致的情况，因此可以认为不存在一致性问题），但是Java里面的运算操作符并非原子操作，
        这导致volatile变量的运算在并发下一样是不安全的。

            由于volatile变量只能保证可见性，在不符合以下两条规则的运算场景中，我们仍然要通过加锁 （使用synchronized、java.util.concurrent中的锁或原子类）来保证原子性：
                ·运算结果并不依赖变量的当前值，或者能够确保只有单一的线程修改变量的值。
                ·变量不需要与其他的状态变量共同参与不变约束

         看看Java内存模型中对volatile变量定义的特殊规则的定义。
         假定T表示 一个线程，V和W分别表示两个volatile型变量，那么在进行read、load、use、assign、store和write操作 时需要满足如下规则：
          1）只有当线程T对变量V执行的前一个动作是load的时候，线程T才能对变量V执行use动作；并且， 只有当线程T对变量V执行的后一个动作是use的时候，
           线程T才能对变量V执行load动作。线程T对变量 V的use动作可以认为是和线程T对变量V的load、read动作相关联的，必须连续且一起出现。
           这条规则要求在工作内存中，每次使用V前都必须先从主内存刷新最新的值，用于保证能看见其 他线程对变量V所做的修改。
           2）只有当线程T对变量V执行的前一个动作是assign的时候，线程T才能对变量V执行store动作；并且，只有当线程T对变量V执行的后一个动作是store的时候，
           线程T才能对变量V执行assign动作。线程 T对变量V的assign动作可以认为是和线程T对变量V的store、write动作相关联的，必须连续且一起出现。

           这条规则要求在工作内存中，每次修改V后都必须立刻同步回主内存中，用于保证其他线程可以 看到自己对变量V所做的修改。

                可见性就是指当一个线程修改了共享变量的值时，其他线程能够立即得知这个修改。上文在讲解 volatile变量的时候我们已详细讨论过这一点。
           Java内存模型是通过在变量修改后将新值同步回主内 存，在变量读取前从主内存刷新变量值这种依赖主内存作为传递媒介的方式来实现可见性的，无论是 普通变量还是volatile变量都是如此。
           普通变量与volatile变量的区别是，volatile的特殊规则保证了新值 能立即同步到主内存，以及每次使用前立即从主内存刷新。因此我们可以说volatile保证了多线程操作 时变量的可见性，
           而普通变量则不能保证这一点。

                除了volatile之外，Java还有两个关键字能实现可见性，它们是synchronized和final。同步块的可见 性是由“对一个变量执行unlock操作之前，
           必须先把此变量同步回主内存中（执行store、write操 作）”这条规则获得的。而final关键字的可见性是指：被final修饰的字段在构造器中一旦被初始化完 成，
           并且构造器没有把“this”的引用传递出去（this引用逃逸是一件很危险的事情，其他线程有可能通 过这个引用访问到“初始化了一半”的对象），
           那么在其他线程中就能看见final字段的值。如代码清单 12-7所示，变量i与j都具备可见性，它们无须同步就能被其他线程正确访问。
           public static final int i;
           public final int j;
           static { i = 0; // 省略后续动作 }
           { // 也可以选择在构造函数中初始化 j = 0; // 省略后续动作

           先行发生原则
                volatile变量规则（Volatile Variable Rule）：对一个volatile变量的写操作先行发生于后面对这个变量 的读操作，这里的“后面”同样是指时间上的先后。

·程序次序规则（Program Order Rule）：在一个线程内，按照控制流顺序，书写在前面的操作先行 发生于书写在后面的操作。注意，这里说的是控制流顺序而不是程序代码顺序，因为要考虑分支、循 环等结构。
·管程锁定规则（Monitor Lock Rule）：一个unlock操作先行发生于后面对同一个锁的lock操作。这 里必须强调的是“同一个锁”，而“后面”是指时间上的先后。
·volatile变量规则（Volatile Variable Rule）：对一个volatile变量的写操作先行发生于后面对这个变量 的读操作，这里的“后面”同样是指时间上的先后。
·线程启动规则（Thread Start Rule）：Thread对象的start()方法先行发生于此线程的每一个动作。
·线程终止规则（Thread Termination Rule）：线程中的所有操作都先行发生于对此线程的终止检 测，我们可以通过Thread::join()方法是否结束、Thread::isAlive()的返回值等手段检测线程是否已经终止 执行。
·线程中断规则（Thread Interruption Rule）：对线程interrupt()方法的调用先行发生于被中断线程 的代码检测到中断事件的发生，可以通过Thread::interrupted()方法检测到是否有中断发生。
·对象终结规则（Finalizer Rule）：一个对象的初始化完成（构造函数执行结束）先行发生于它的 finalize()方法的开始。
·传递性（Transitivity）：如果操作A先行发生于操作B，操作B先行发生于操作C，那就可以得出 操作A先行发生于操作C的结论。

           一个操作“时间上的先发生”不代表这个操作会是“先行发 生”。那如果一个操作“先行发生”，是否就能推导出这个操作必定是“时间上的先发生”呢？
                很遗憾，这 个推论也是不成立的。一个典型的例子就是多次提到的“指令重排序”，演示例子如代码清单12-10所 示。
           // 以下操作在同一个线程中执行
           int i = 1;
           int j = 2;
           代码清单12-10所示的两条赋值语句在同一个线程之中，根据程序次序规则，“int i=1”的操作先行 发生于“int j=2”，但是“int j=2”的代码完全可能先被处理器执行，
           这并不影响先行发生原则的正确性， 因为我们在这条线程之中没有办法感知到这一点。

           上面两个例子综合起来证明了一个结论：时间先后顺序与先行发生原则之间基本没有因果关系， 所以我们衡量并发安全问题的时候不要受时间顺序的干扰，一切必须以先行发生原则为准

}
 */
    /*
    #*线程
    线程是比进程更轻量级的调度执行单位，线程的引入，可以把一个进程的资源分配和 执行调度分开，各个线程既可以共享进程资源（内存地址、文件I/O等），又可以独立调度。

        内核线程的调度成本主要来自于用户态与核心态之间的状态转换，而这两种状态转换的开销主要 来自于响应中断、保护和恢复执行现场的成本。

        线程A -> 系统中断 -> 线程B
        处理器要去执行线程A的程序代码时，并不是仅有代码程序就能跑得起来，程序是数据与代码的 组合体，代码执行时还必须要有上下文数据的支撑。而这里说的“上下文”，
        以程序员的角度来看，是 方法调用过程中的各种局部的变量与资源；以线程的角度来看，是方法的调用栈中存储的各类信息； 而以操作系统和硬件的角度来看，
        则是存储在内存、缓存和寄存器中的一个个具体数值。物理硬件的 各种存储设备和寄存器是被操作系统内所有线程共享的资源，当中断发生，从线程A切换到线程B去执 行之前，
        操作系统首先要把线程A的上下文数据妥善保管好，然后把寄存器、内存分页等恢复到线程B 挂起时候的状态，这样线程B被重新激活后才能仿佛从来没有被挂起过。这种保护和恢复现场的工 作，
        免不了涉及一系列数据在各种寄存器、缓存中的来回拷贝，当然不可能是一种轻量级的操作。

    以HotSpot为例，它的每一个Java线程都是直接映射到一个操作系统原生线程来实现的，而且中间 没有额外的间接结构，所以HotSpot自己是不会去干涉线程调度的（可以设置线程优先级给操作系统提
供调度建议），全权交给底下的操作系统去处理，所以何时冻结或唤醒线程、该给线程分配多少处理 器执行时间、该把线程安排给哪个处理器核心去执行等，都是由操作系统完成的，也都是由操作系统 全权决定的。
     */

/**
 * 在计算机中，进程和线程是两个并发执行的基本概念。
 *
 * #*进程（Process）：
 * 进程是计算机中正在运行的程序的实例。它是一个独立的执行单位，拥有自己的内存空间、资源和状态。每个进程都在操作系统的管理下，通过分配系统资源来执行任务。进程之间相互独立，彼此隔离，不共享内存，通常需要进行进程间通信才能交换数据。每个进程都有一个唯一的进程标识符（PID）用于标识和管理。
 *
 * 线程（Thread）：
 * 线程是进程中的一个执行路径，是CPU调度和执行的最小单位。一个进程可以包含多个线程，共享进程的资源，如内存、文件句柄等。具有共享内存的特性使得线程之间可以更方便地进行通信和数据共享。线程的创建、撤销和切换比进程快速，且开销较小。线程之间的调度由操作系统内核完成。
 *
 * 主要区别：
 *
 * 资源占用：进程拥有自己的独立资源，线程共享进程的资源。
 * 执行开销：线程的创建、销毁和切换开销相对较小。
 * 通信与同步：进程间通信需要额外的机制，如管道、消息队列等，而线程之间可以通过共享内存直接通信。
 * 并发性：多个线程可以在同一个进程中并发执行，但不同进程之间的并发需要系统的调度和资源分配。
 * 总结来说，进程是资源分配的基本单位，线程是程序执行的最小单位。多线程可以提高程序的效率和响应能力，进程间相互独立，而线程则共享进程的资源。
 */
// TODO: 2023/2/22 总结
    /*
        1）对于非队尾节点，如果它的状态为0或PROPAGATE，那么它肯定是head。
        2）等待队列中有多个节点时，如果head的状态为0或PROPAGATE，说明head处于一种中间状态，且此时有线程刚才释放锁了。
        而对于acquire thread来说，如果检测到这种状态，说明再次acquire是极有可能获得到锁的。
     */
public abstract class AbstractQueuedSynchronizer
    extends AbstractOwnableSynchronizer
    implements java.io.Serializable {

    private static final long serialVersionUID = 7373984972572414691L;

    /**
     * Creates a new {@code AbstractQueuedSynchronizer} instance
     * with initial synchronization state of zero.
     */
    protected AbstractQueuedSynchronizer() { }

    /**
     * Wait queue node class.
     *
     * <p>The wait queue is a variant of a "CLH" (Craig, Landin, and
     * Hagersten) lock queue. CLH locks are normally used for
     * spinlocks.  We instead use them for blocking synchronizers, but
     * use the same basic tactic of holding some of the control
     * information about a thread in the predecessor of its node.  A
     * "status" field in each node keeps track of whether a thread
     * should block.  A node is signalled when its predecessor
     * releases.  Each node of the queue otherwise serves as a
     * specific-notification-style monitor holding a single waiting
     * thread. The status field does NOT control whether threads are
     * granted locks etc though.  A thread may try to acquire if it is
     * first in the queue. But being first does not guarantee success;
     * it only gives the right to contend.  So the currently released
     * contender thread may need to rewait.
     *
     * <p>To enqueue into a CLH lock, you atomically splice it in as new
     * tail. To dequeue, you just set the head field.
     * <pre>
     *      +------+  prev +-----+       +-----+
     * head |      | <---- |     | <---- |     |  tail
     *      +------+       +-----+       +-----+
     * </pre>
     *
     * <p>Insertion into a CLH queue requires only a single atomic
     * operation on "tail", so there is a simple atomic point of
     * demarcation from unqueued to queued. Similarly, dequeuing
     * involves only updating the "head". However, it takes a bit
     * more work for nodes to determine who their successors are,
     * in part to deal with possible cancellation due to timeouts
     * and interrupts.
     *
     * <p>The "prev" links (not used in original CLH locks), are mainly
     * needed to handle cancellation. If a node is cancelled, its
     * successor is (normally) relinked to a non-cancelled
     * predecessor. For explanation of similar mechanics in the case
     * of spin locks, see the papers by Scott and Scherer at
     * http://www.cs.rochester.edu/u/scott/synchronization/
     *
     * <p>We also use "next" links to implement blocking mechanics.
     * The thread id for each node is kept in its own node, so a
     * predecessor signals the next node to wake up by traversing
     * next link to determine which thread it is.  Determination of
     * successor must avoid races with newly queued nodes to set
     * the "next" fields of their predecessors.  This is solved
     * when necessary by checking backwards from the atomically
     * updated "tail" when a node's successor appears to be null.
     * (Or, said differently, the next-links are an optimization
     * so that we don't usually need a backward scan.)
     *
     * <p>Cancellation introduces some conservatism to the basic
     * algorithms.  Since we must poll for cancellation of other
     * nodes, we can miss noticing whether a cancelled node is
     * ahead or behind us. This is dealt with by always unparking
     * successors upon cancellation, allowing them to stabilize on
     * a new predecessor, unless we can identify an uncancelled
     * predecessor who will carry this responsibility.
     *
     * <p>CLH queues need a dummy header node to get started. But
     * we don't create them on construction, because it would be wasted
     * effort if there is never contention. Instead, the node
     * is constructed and head and tail pointers are set upon first
     * contention.
     *
     * <p>Threads waiting on Conditions use the same nodes, but
     * use an additional link. Conditions only need to link nodes
     * in simple (non-concurrent) linked queues because they are
     * only accessed when exclusively held.  Upon await, a node is
     * inserted into a condition queue.  Upon signal, the node is
     * transferred to the main queue.  A special value of status
     * field is used to mark which queue a node is on.
     *
     * <p>Thanks go to Dave Dice, Mark Moir, Victor Luchangco, Bill
     * Scherer and Michael Scott, along with members of JSR-166
     * expert group, for helpful ideas, discussions, and critiques
     * on the design of this class.
     */
    /*
    总结：
    1 如果只有一个线程进来不会初始化CHL同步得带队列
    2 第二个线程来加锁失败(竞争)时，会帮头节点初始化一个节点，并将自己的节点挂在头节点后面
    3 每个等待的节点会将上一个节点的状态改为SIGNAL(-1)，用来标志改节点后面的节点才可以被唤醒
    4 在释放锁的时候如果头节点的状态为不为0，会先将其设置为0。然后唤醒下一个节点
    5 节点入队和唤醒的时候都会跳过ws>0 即CANCELLED(取消)的节点
    6 头节点一定表示当前获取锁的线程节点。

     */
    static final class Node {
        /** Marker to indicate a node is waiting in shared mode */
        // 标记节点以共享模式阻塞
        static final Node SHARED = new Node();
        /** Marker to indicate a node is waiting in exclusive mode */
        // 标记节点以独占形式阻塞
        static final Node EXCLUSIVE = null;

        /** waitStatus value to indicate thread has cancelled */
        // CANCELLED：表示由于超时或者中断，当前节点被取消。被取消后，当前节点的状态将不再发生任何变化。
        // 节点被取消
        static final int CANCELLED =  1;
        /** waitStatus value to indicate successor's thread needs unparking */
        // SIGNAL:表示后继节点等待获取同步状态，当前节点释放同步状态、中断、取消则唤醒其后继节点，以继续获取同步状态。
        // 后继节点等待被唤醒
        static final int SIGNAL    = -1;
        /** waitStatus value to indicate thread is waiting on condition */
        // 表示当前节点在等待condition
        // 节点在“条件”上等待
        static final int CONDITION = -2;
        /**
         * waitStatus value to indicate the next acquireShared should
         * unconditionally propagate
         */
        // 传播：表示下一次获取共享同步状态将被无条件传递下去
        // 下一次获取共享同步状态将被无条件传递下去
        static final int PROPAGATE = -3;

        /**
         * Status field, taking on only the values:
         *   SIGNAL:     The successor of this node is (or will soon be)
         *               blocked (via park), so the current node must
         *               unpark its successor when it releases or
         *               cancels. To avoid races, acquire methods must
         *               first indicate they need a signal,
         *               then retry the atomic acquire, and then,
         *               on failure, block.
         *   CANCELLED:  This node is cancelled due to timeout or interrupt.
         *               Nodes never leave this state. In particular,
         *               a thread with cancelled node never again blocks.
         *   CONDITION:  This node is currently on a condition queue.
         *               It will not be used as a sync queue node
         *               until transferred, at which time the status
         *               will be set to 0. (Use of this value here has
         *               nothing to do with the other uses of the
         *               field, but simplifies mechanics.)
         *   PROPAGATE:  A releaseShared should be propagated to other
         *               nodes. This is set (for head node only) in
         *               doReleaseShared to ensure propagation
         *               continues, even if other operations have
         *               since intervened.
         *   0:          None of the above
         *
         * The values are arranged numerically to simplify use.
         * Non-negative values mean that a node doesn't need to
         * signal. So, most code doesn't need to check for particular
         * values, just for sign.
         *
         * The field is initialized to 0 for normal sync nodes, and
         * CONDITION for condition nodes.  It is modified using CAS
         * (or when possible, unconditional volatile writes).
         */
        // 节点在队列中的状态，默认初始值为0
        /**
         * 1)当一个线程的节点刚刚加入到等待队列中时，它的初始 waitStatus 值会被设置为 0。
         *
         * 2)如果一个节点的前驱节点释放了锁或者被取消，那么该节点的 waitStatus 将被设置为 0，表示它可以尝试获取锁或者被唤醒。
         */
        volatile int waitStatus;

        /**
         * Link to predecessor node that current node/thread relies on
         * for checking waitStatus. Assigned during enqueuing, and nulled
         * out (for sake of GC) only upon dequeuing.  Also, upon
         * cancellation of a predecessor, we short-circuit while
         * finding a non-cancelled one, which will always exist
         * because the head node is never cancelled: A node becomes
         * head only as a result of successful acquire. A
         * cancelled thread never succeeds in acquiring, and a thread only
         * cancels itself, not any other node.
         */
        // 上一个节点
        volatile Node prev;

        /**
         * Link to the successor node that the current node/thread
         * unparks upon release. Assigned during enqueuing, adjusted
         * when bypassing cancelled predecessors, and nulled out (for
         * sake of GC) when dequeued.  The enq operation does not
         * assign next field of a predecessor until after attachment,
         * so seeing a null next field does not necessarily mean that
         * node is at end of queue. However, if a next field appears
         * to be null, we can scan prev's from the tail to
         * double-check.  The next field of cancelled nodes is set to
         * point to the node itself instead of null, to make life
         * easier for isOnSyncQueue.
         */
        // 下一个节点
        volatile Node next;

        /**
         * The thread that enqueued this node.  Initialized on
         * construction and nulled out after use.
         */
        // 获取锁的线程
        volatile Thread thread;

        /**
         * Link to next node waiting on condition, or the special
         * value SHARED.  Because condition queues are accessed only
         * when holding in exclusive mode, we just need a simple
         * linked queue to hold nodes while they are waiting on
         * conditions. They are then transferred to the queue to
         * re-acquire. And because conditions can only be exclusive,
         * we save a field by using special value to indicate shared
         * mode.
         */
        Node nextWaiter;

        /**
         * Returns true if node is waiting in shared mode.
         */
        final boolean isShared() {
            return nextWaiter == SHARED;
        }

        /**
         * Returns previous node, or throws NullPointerException if null.
         * Use when predecessor cannot be null.  The null check could
         * be elided, but is present to help the VM.
         *
         * @return the predecessor of this node
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
     * Head of the wait queue, lazily initialized.  Except for
     * initialization, it is modified only via method setHead.  Note:
     * If head exists, its waitStatus is guaranteed not to be
     * CANCELLED.
     */
    // 等待队列的头，被惰性地初始化。除了初始化之外，它只通过方法setHead进行修改。注意:如果head存在，它的等待状态保证不会被取消。
    /*
    head的状态为0属于一种中间状态，因为：

        1）如果head的后继唤醒后能获得锁，那么head的后继就会成为新head，只要新head不为tail，那么新head的状态一般都为SIGNAL。
        2）如果head的后继唤醒后不能获得锁，那么这个线程又会执行shouldParkAfterFailedAcquire，再把head的状态置为SIGNAL。

        最重要的，head的状态为0，代表有人刚释放了锁。
     */
    private transient volatile Node head;

    /**
     * Tail of the wait queue, lazily initialized.  Modified only via
     * method enq to add new wait node.
     */
    // 等待队列的尾部，延迟初始化。仅通过enq()方法修改以添加新的等待节点。
    private transient volatile Node tail;

    /**
     * The synchronization state.
     */
    // 同步状态
    /*
    对于独占锁，state变量即可以理解为锁。当state为0时，表示锁空闲，当前线程可以获得锁；当state为1时，表示锁被其它线程占有，当前线程无法获取锁（假如不允许重入）。
    那么当前线程可以被构造为Node节点，加入到队列中、并阻塞。当持有锁的线程释放锁之后，将state变量重新设置为0，并唤醒阻塞的线程，继续获得锁。
    */
    private volatile int state;

    /**
     * Returns the current value of synchronization state.
     * This operation has memory semantics of a {@code volatile} read.
     * @return current state value
     */
    // 获取同步状态值，该方法具有volatile读的内存语义

    protected final int getState() {
        return state;
    }

    /**
     * Sets the value of synchronization state.
     * This operation has memory semantics of a {@code volatile} write.
     * @param newState the new state value
     */
    // 设置同步状态值，该方法具有volatile写的内存语义

    protected final void setState(int newState) {
        state = newState;
    }

    /**
     * Atomically sets synchronization state to the given updated
     * value if the current state value equals the expected value.
     * This operation has memory semantics of a {@code volatile} read
     * and write.
     *
     * @param expect the expected value
     * @param update the new value
     * @return {@code true} if successful. False return indicates that the actual
     *         value was not equal to the expected value.
     */
    // 如果当前状态值等于预期值，则自动将同步状态设置为给定的更新值。
    // 该方法兼顾volatile读、写的内存语义
    protected final boolean compareAndSetState(int expect, int update) {
        // See below for intrinsics setup to support this
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }

    // Queuing utilities

    /**
     * The number of nanoseconds for which it is faster to spin
     * rather than to use timed park. A rough estimate suffices
     * to improve responsiveness with very short timeouts.
     */
    /**
     * 旋转比使用定时停车更快的纳秒数。粗略的估计足以在极短的超时时间内提高响应能力。
     */
    static final long spinForTimeoutThreshold = 1000L;

    /**
     * Inserts node into queue, initializing if necessary. See picture above.
     * @param node the node to insert
     * @return node's predecessor
     */
    // 节点入队全量方法
    private Node enq(final Node node) {
        for (;;) {
            Node t = tail;
            if (t == null) { // Must initialize
                if (compareAndSetHead(new Node()))
                    tail = head;
            } else {
                node.prev = t;
                if (compareAndSetTail(t, node)) {
                    t.next = node;
                    return t;
                }
            }
        }
    }

    /**
     * Creates and enqueues node for current thread and given mode.
     *
     * @param mode Node.EXCLUSIVE for exclusive, Node.SHARED for shared
     * @return the new node
     */
    // 若addWaiter()方法得以被执行，说明锁被其它线程持有，则将当前获取锁的线程构造成Node节点加入到同步队列中。
    /*
    这种情况是最常见的，比如现在AQS的等待队列中有很多node正在等待，当前线程现在刚执行完毕addWaiter（node刚成为新队尾），
    然后现在开始执行获取锁的死循环（独占锁对应的是acquireQueued里的死循环，共享锁对应的是doAcquireShared的死循环），
    此时node的前驱，也就是旧队尾的状态肯定还是0（也就是默认初始化的值），然后死循环执行两次，
    第一次执行shouldParkAfterFailedAcquire自然会检测到前驱状态为0，然后将0设置为SIGNAL；
    第二次执行shouldParkAfterFailedAcquire，直接返回true
     */
    private Node addWaiter(Node mode) {
        // 将当前线程构造为Node节点
        Node node = new Node(Thread.currentThread(), mode);
        // Try the fast path of enq; backup to full enq on failure
        // // 尝试快速在队列尾部添加
        Node pred = tail;
        if (pred != null) {
            node.prev = pred;
            if (compareAndSetTail(pred, node)) {
                pred.next = node;
                return node;
            }
        }
        // 快速添加失败，调用全量方法将节点加入队列
        enq(node);
        return node;
    }

    /**
     * Sets head of queue to be node, thus dequeuing. Called only by
     * acquire methods.  Also nulls out unused fields for sake of GC
     * and to suppress unnecessary signals and traversals.
     *
     * @param node the node
     */
    private void setHead(Node node) {
        head = node;
        node.thread = null;
        node.prev = null;
    }

    /**
     * Wakes up node's successor, if one exists.
     *
     * @param node the node
     */
    // 唤醒后继节点
    /*
    1.如果节点的waitStatus值小于0，则更新为0。todo 这一步的目的不太清楚
    2.回溯，保证队列节点的合法性，防止节点状态为取消、节点为空的情况
    3.唤醒第一个可用的后继节点
     */
    private void unparkSuccessor(Node node) {
        /*
         * If status is negative (i.e., possibly needing signal) try
         * to clear in anticipation of signalling.  It is OK if this
         * fails or if status is changed by waiting thread.
         */
        // 此处的node表示的是 head 结点
        int ws = node.waitStatus;
        // 如果节点的waitStatus值小于0，则更新为0
        if (ws < 0)
            compareAndSetWaitStatus(node, ws, 0);

        /*
         * Thread to unpark is held in successor, which is normally
         * just the next node.  But if cancelled or apparently null,
         * traverse backwards from tail to find the actual
         * non-cancelled successor.
         */
        // s 表示头结点的后继结点，之前 获取锁失败的时候 讲到要将该线程结点加入到等待队列的尾部，
        // 但是 如果等待队列还未初始化，则new 一个空的Node 来表示head结点，所以head结点的后续结点才是真正的等待被唤醒的线程结点。
        Node s = node.next;
        // 回溯，保证后继节点及其状态的合法性
        // 后继结点为null 或者 waitStatus > 0表示该线程的任务被取消
        if (s == null || s.waitStatus > 0) {
            s = null;
            for (Node t = tail; t != null && t != node; t = t.prev)
                if (t.waitStatus <= 0)
                    s = t;
        }
        // 唤醒后继节点
        if (s != null)
            LockSupport.unpark(s.thread);
    }

    /**
     * Release action for shared mode -- signals successor and ensures
     * propagation. (Note: For exclusive mode, release just amounts
     * to calling unparkSuccessor of head if it needs signal.)
     */
    // 共享模式下唤醒队列中的线程
    /*
    head状态为0的情况
        1)如果等待队列中只有一个dummy node（它的状态为0），那么head也是tail，且head的状态为0。
        2)等待队列中当前只有一个dummy node（它的状态为0），acquire thread获取锁失败了（无论独占还是共享），将当前线程包装成node放到队列中，此时队列中有两个node，但当前线程还没来得及执行shouldParkAfterFailedAcquire。
        3)此时队列中有多个node，有线程刚释放了锁，刚执行了unparkSuccessor里的if (ws < 0) compareAndSetWaitStatus(node, ws, 0);把head的状态设置为了0，然后唤醒head后继线程，head后继线程获取锁成功，直到head后继线程将自己设置为AQS的新head的这段时间里，head的状态为0。

     */
    private void doReleaseShared() {
        /*
         * Ensure that a release propagates, even if there are other
         * in-progress acquires/releases.  This proceeds in the usual
         * way of trying to unparkSuccessor of head if it needs
         * signal. But if it does not, status is set to PROPAGATE to
         * ensure that upon release, propagation continues.
         * Additionally, we must loop in case a new node is added
         * while we are doing this. Also, unlike other uses of
         * unparkSuccessor, we need to know if CAS to reset status
         * fails, if so rechecking.
         */
        for (;;) {
            Node h = head;
            // h==null，说明队列是空的
            // head==tail，说明队列中没有节点等待唤醒
            if (h != null && h != tail) {
                // 表示等待队列中有等待的线程结点
                int ws = h.waitStatus;
                // 节点状态=SIGNAL，说明后继节点在等待被唤醒
                if (ws == Node.SIGNAL) {
                    // 通过cas操作 将 头结点的 waitStatus置为0
                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                        continue;            // loop to recheck cases
                    // 唤醒后继节点，后继节点唤醒竞争到资源后，会调用setHeadAndPropagate()
                    // 如果还有剩余资源可以，会继续唤醒后继节点，因此当前线程没有必要将节点全部唤醒
                    unparkSuccessor(h);// 唤醒头结点的后续等待结点
                }
                /*
                如果状态为0，说明h的后继所代表的线程已经被唤醒或即将被唤醒，并且这个中间状态即将消失，
                要么由于acquire thread获取锁失败再次设置head为SIGNAL并再次阻塞，
                要么由于acquire thread获取锁成功而将自己（head后继）设置为新head并且只要head后继不是队尾，那么新head肯定为SIGNAL。
                所以设置这种中间状态的head的status为PROPAGATE，让其status又变成负数，这样可能被唤醒线程检测到。
                 */
                else if (ws == 0 &&
                         !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))  // 下一次获取共享同步状态将被无条件传递下去
                    continue;                // loop on failed CAS
            }
            /*
            如果h!=head，说明唤醒的后继节点已经竞争到资源，并将head指向它了，说明可能还有资源可用，
            后面的节点还有被唤醒的机会，因此自旋重试。
            */
            /*
            （只有cas操作成功后，才会到这里）
            head变化一定是因为：acquire thread被唤醒，之后它成功获取锁，然后setHead设置了新head
            h == head：保证了，只要在某个循环的过程中有线程刚获取了锁且设置了新head，就会再次循环。
            目的当然是为了再次执行unparkSuccessor(h)，即唤醒队列中第一个等待的线程。
             */
            if (h == head)                   // loop if head changed
                break;
        }
    }

    /**
     * Sets head of queue, and checks if successor may be waiting
     * in shared mode, if so propagating if either propagate > 0 or
     * PROPAGATE status was set.
     *
     * @param node the node
     * @param propagate the return value from a tryAcquireShared
     */
    private void setHeadAndPropagate(Node node, int propagate) {
        Node h = head; // Record old head for check below
        setHead(node);
        /*
         * Try to signal next queued node if:
         *   Propagation was indicated by caller,
         *     or was recorded (as h.waitStatus either before
         *     or after setHead) by a previous operation
         *     (note: this uses sign-check of waitStatus because
         *      PROPAGATE status may transition to SIGNAL.)
         * and
         *   The next node is waiting in shared mode,
         *     or we don't know, because it appears null
         *
         * The conservatism in both of these checks may cause
         * unnecessary wake-ups, but only when there are multiple
         * racing acquires/releases, so most need signals now or soon
         * anyway.
         */
        if (propagate > 0 || h == null || h.waitStatus < 0 ||
            (h = head) == null || h.waitStatus < 0) {
            Node s = node.next;
            if (s == null || s.isShared()) // 头节点的下一个是 SHARED 节点（共享同步状态）
                doReleaseShared();
        }
    }

    // Utilities for various versions of acquire

    /**
     * Cancels an ongoing attempt to acquire.
     *
     * @param node the node
     */
    private void cancelAcquire(Node node) {
        // Ignore if node doesn't exist
        if (node == null)
            return;

        node.thread = null;

        // Skip cancelled predecessors
        Node pred = node.prev;
        while (pred.waitStatus > 0) // 找到当前节点上一个不是取消状态的节点
            node.prev = pred = pred.prev;

        // predNext is the apparent node to unsplice. CASes below will
        // fail if not, in which case, we lost race vs another cancel
        // or signal, so no further action is necessary.
        Node predNext = pred.next; // 非取消状态节点的下一个节点

        // Can use unconditional write instead of CAS here.
        // After this atomic step, other Nodes can skip past us.
        // Before, we are free of interference from other threads.
        node.waitStatus = Node.CANCELLED; // 当前节点状态变为取消

        // If we are the tail, remove ourselves.
        if (node == tail && compareAndSetTail(node, pred)) { // 当前节点是尾节点 然后吧找到当前节点上一个不是取消状态的节点赋值作为尾节点
            compareAndSetNext(pred, predNext, null); // 尾节点的下一个赋值null
        } else { // 当前节点不是尾节点
            // If successor needs signal, try to set pred's next-link
            // so it will get one. Otherwise wake it up to propagate.
            int ws;
            if (pred != head && //上一个节点不是头节点
                ((ws = pred.waitStatus) == Node.SIGNAL || // 状态是待唤醒
                 (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) && // 非取消状态 且 不是待唤醒状态就改为待唤醒状态
                pred.thread != null) {
                Node next = node.next; // 当前节点的下一个节点
                if (next != null && next.waitStatus <= 0)// 当前节点的下一个节点不为空 且 状态为非取消
                    compareAndSetNext(pred, predNext, next); // 连接到上一个非取消节点的next
            } else {//上一个节点 是头节点 或 (状态是非待唤醒 且 修改上一个状态为非唤醒状态失败) 或 持有节点线程为空
                unparkSuccessor(node); // 唤醒后继节点？？？
            }

            node.next = node; // help GC
        }
    }

    /**
     * Checks and updates status for a node that failed to acquire.
     * Returns true if thread should block. This is the main signal
     * control in all acquire loops.  Requires that pred == node.prev.
     *
     * @param pred node's predecessor holding status
     * @param node the node
     * @return {@code true} if thread should block
     */
    // 要作用是 判断是否需要中断 和 删除已经取消的线程结点
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {

        // 注意Node的waitStatus字段我们在上面创建Node的时候并没有指定 ，默认值是0
        // waitStatus 的4种状态
        //static final int CANCELLED =  1;    // 取消任务
        //static final int SIGNAL    = -1;  //等待被唤醒
        //static final int CONDITION = -2;   //条件锁使用
        //static final int PROPAGATE = -3; //共享锁时使用

        // 获取前驱节点的状态
        int ws = pred.waitStatus;
        /**
         * 如果前置结点的状态是 SIGNAL 等待被唤醒，那么直接返回true，
         * 表示当前线程对应的结点需要被阻塞；因为前面的线程还在等待被唤醒，更轮不到你了，直接等待阻塞吧
         */
        // 前驱节点的状态为SIGNAL，则后继节点应被阻塞，返回true
        if (ws == Node.SIGNAL)
            /*
             * This node has already set status asking a release
             * to signal it, so it can safely park.
             */
        /*
         * 前驱节点已经设置了SIGNAL，闹钟已经设好，现在我可以安心睡觉（阻塞）了。
         * 如果前驱变成了head，并且head的代表线程exclusiveOwnerThread释放了锁，
         * 就会来根据这个SIGNAL来唤醒自己
         */
        //该节点已经设置了状态，要求释放发出信号，以便它可以安全地停车。
            return true;
        // 前驱节点状态为CANCELLED，则清除队列中已经被取消的节点
        if (ws > 0) {
            /*
             * Predecessor was cancelled. Skip over predecessors and
             * indicate retry.
             */
            /*
             *ws > 0 表示该结点对应的线程已经取消任务了，那么循环遍历删除 已取消任务的结点，就是双向链表的删除
             */
            /*
             * 发现传入的前驱的状态大于0，即CANCELLED。说明前驱节点已经因为超时或响应了中断，
             * 而取消了自己。所以需要跨越掉这些CANCELLED节点，直到找到一个<=0的节点
             */
            do {
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            pred.next = node;
        } else {
            // 运行至此，前驱节点的状态要么为初始状态（0）、要么为PROPAGATE。
            // 此时，将前驱节点的状态改为 SIGNAL，以便在下一轮自旋中阻塞当前节点
            /*
            PROPAGATE：只能在使用共享锁的时候出现，并且只可能设置在head上。
             */
            /*
             * waitStatus must be 0 or PROPAGATE.  Indicate that we
             * need a signal, but don't park yet.  Caller will need to
             * retry to make sure it cannot acquire before parking.
             */
            // 设置前置结点的 waitStatus 为SIGNAL ，等待被唤醒
            /*
            检测到0后，一定要设置成SIGNAL的原因：
                设置前驱状态为SIGNAL，以便当前线程阻塞后，前驱能根据SIGNAL状态来唤醒自己。（唤醒head后继的条件）

            设置成SIGNAL后会返回false的原因：
                返回false后，下一次循环开始，会重新获取node的前驱，前驱如果就是head，那么还会重新尝试获取锁。这次重新尝试是有必要的，某些场景下，重新尝试获取锁会成功。

             */
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
        }
        /*
           return false 的意义：当前线程对应的结点的前置结点已经设置为SIGNAL 等待被唤醒，
           但是不直接返回true，如果返回true就直接要阻塞当前线程啦，返回false，
           那么之前 acquireQueued 方法的 无限循环 会再次判断 当前线程的前置结点是否是 head结点，
           如果是的话就尝试获取锁。这样做的目的就是 ：Java 的线程是映射到操作系统原生线程之上的，
           如果要阻塞或唤醒一个线程就需要操作系统的帮忙，这就要从用户态转换到核心态，而状态的切换是需要花费很多CPU时间的。
         */

        return false;
    }

    /**
     * Convenience method to interrupt current thread.
     */
    static void selfInterrupt() {
        Thread.currentThread().interrupt();
    }

    /**
     * Convenience method to park and then check if interrupted
     *
     * @return {@code true} if interrupted
     */
    // 阻塞当前节点，清除并返回当前线程中断标识
    private final boolean parkAndCheckInterrupt() {
        // 阻塞当前线程
        LockSupport.park(this);
        // 清除并返回线程中断标识
        return Thread.interrupted();
    }

    /*
     * Various flavors of acquire, varying in exclusive/shared and
     * control modes.  Each is mostly the same, but annoyingly
     * different.  Only a little bit of factoring is possible due to
     * interactions of exception mechanics (including ensuring that we
     * cancel if tryAcquire throws exception) and other control, at
     * least not without hurting performance too much.
     */

    /**
     * Acquires in exclusive uninterruptible mode for thread already in
     * queue. Used by condition wait methods as well as acquire.
     *
     * @param node the node
     * @param arg the acquire argument
     * @return {@code true} if interrupted while waiting
     */
    // 加入到同步队列中的节点，将以“自旋”的方式继续尝试获取同步状态。
    /*
    假设T1持有锁且不释放，T2再次获取锁，则acquireQueued()方法将被调用。因为T2的前驱节点即为头节点，其符合出队条件。那么再次调用tryAcquire()方法获取锁，因为T1不释放锁，tryAcquire()返回false，所以shouldParkAfterFailedAcquire()方法将被调用。

    进入到shouldParkAfterFailedAcquire()方法后（注意该方法参数一个是前驱节点、一个是当前节点），前驱节点的状态为0，将前驱节点状态改为SIGNAL，返回false。

    因为acquireQueued()方法以自旋的方式进行，所以上面的步骤被重复，当获取同步状态再次进入shouldParkAfterFailedAcquire()之后，前驱节点的状态已经变为SIGNAL，而且两次通过tryAcquire()方法获取同步状态均失败，则当前线程应被阻塞。

    如果此时T3也来获取锁，则重复上面的步骤，当然此时的T3是不符合出队条件的，总而言之，通过acquireQueued()方法自旋调用，加上shouldParkAfterFailedAcquire()方法，保证了在必要时候将前驱节点的状态设置为SIGNAL。
     */
    final boolean acquireQueued(final Node node, int arg) {
        /*
        第一个if，判断前驱节点是否为head节点（保证队列“先进先出”的原则）
            是，尝试获取同步状态
                获取成功，返回true
                获取失败，进行第二个if判断
            否，进行第二个if判断
        第二个if，执行到此，要么前驱节点非头节点，不符合出队原则；要么前驱节点是头结点，但获取同步状态失败（持有锁的线程依然未释放锁）
            判断当前节点尝试获取同步状态失败后是否应当阻塞、并更新节点状态
                是，阻塞当前节点，清除、并记录当前线程中断标识
                否，自旋，进行下一轮判断
         */
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {
                // 获取前驱节点
                final Node p = node.predecessor();
                // 为保证队列“先进先出”的特性。当前驱节点为头节点时，可尝试获取同步状态，其它节点依然阻塞
                // 注意：若某个节点的前驱节点为头节点，则说明它是队列中的第一个节点，符合“先进先出”原则
                /*
                做if (p == head && tryAcquire(arg))判断两次，是有好处的。
                    1）在获取共享锁的死循环，每次都会重新获取node的前驱（node.predecessor()）
                    2）在新尾加入队列后，从head到新tail之间的node都取消掉了，使得新tail的前驱变成了head，
                    说不定此时获取锁也能成功呢，所以这第二次if (p == head && tryAcquire(arg))判断是很有必要的。
                 */
                if (p == head && tryAcquire(arg)) {
                    // 获取同步状态成功，更新头结点
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return interrupted;
                }
                // 若前驱节点非头结点、或是头节点但获取同步状态失败，则阻塞当前节点
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            if (failed) // 当前获取锁的node异常，唤醒下一个非取消节点
                cancelAcquire(node);
        }
    }

    /**
     * Acquires in exclusive interruptible mode.
     * @param arg the acquire argument
     */
    private void doAcquireInterruptibly(int arg)
        throws InterruptedException {
        final Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return;
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    // 响应中断
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * Acquires in exclusive timed mode.
     *
     * @param arg the acquire argument
     * @param nanosTimeout max wait time
     * @return {@code true} if acquired
     */
    // 以超时模式获取同步状态
    private boolean doAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        // 超时
        if (nanosTimeout <= 0L)
            return false;
        // 计算超时到期时间
        final long deadline = System.nanoTime() + nanosTimeout;
        // 线程构造为AQS节点、入队
        final Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                // 符合出队条件，再次获取同步状态
                if (p == head && tryAcquire(arg)) {
                    // 获取同步状态成功，重新设置头节点、清空释放同步状态的节点
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return true;
                }
                nanosTimeout = deadline - System.nanoTime();
                // 超时
                if (nanosTimeout <= 0L)
                    return false;
                // spinForTimeoutThreshold：自旋超时阈值，1000纳秒（1秒=1000毫秒；1毫秒=1000微秒；1微秒=1000纳秒）
                // 如果nanosTimeout大于spinForTimeoutThreshold，则将线程阻塞nanosTimeout纳秒
                if (shouldParkAfterFailedAcquire(p, node) &&
                    nanosTimeout > spinForTimeoutThreshold)
                    // 阻塞nanosTimeout纳秒，到期唤醒后，再次以自旋的形式获取锁
                    LockSupport.parkNanos(this, nanosTimeout);
                // 响应中断
                if (Thread.interrupted())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * Acquires in shared uninterruptible mode.
     * @param arg the acquire argument
     */
    private void doAcquireShared(int arg) {
        // 加入等待队列
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {
                final Node p = node.predecessor(); // 获取当前节点在等待队列的上一个节点
                if (p == head) { // 如果上一个节点是头节点（头节点的下一个才是获取锁的线程），即当前线程
                    int r = tryAcquireShared(arg); // 尝试获取锁
                    if (r >= 0) {// 当前线程获取锁成功
                        setHeadAndPropagate(node, r); // 把node作为头节点，然后再唤醒当前节点下一个节点 下一次获取共享同步状态将被无条件传递下去
                        p.next = null; // help GC
                        if (interrupted) // 是否要中断当前线程
                            selfInterrupt();
                        failed = false;
                        return;
                    }
                }
                // 当前线程节点的上一个不是head 或 当前线程获取锁失败
                if (shouldParkAfterFailedAcquire(p, node) &&//判断是否需要中断 和 删除已经取消的线程结点
                    parkAndCheckInterrupt()) // 阻塞当前线程
                    interrupted = true;
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * Acquires in shared interruptible mode.
     * 以共享可中断模式采集
     * @param arg the acquire argument
     */
    private void doAcquireSharedInterruptibly(int arg)
        throws InterruptedException {
        // 创建一个和当前线程绑定的Node节点，并添加到队尾
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;//是否竞争失败
        try {
            for (;;) {
                // 获取当前节点的前驱节点
                final Node p = node.predecessor();
                // 如果前驱节点是头节点，自己就有资格去竞争了
                if (p == head) {
                    // 当前节点前一个是头节点
                    // 尝试获取锁（CountDownLatch:状态为零则返回 1 ）
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        /*
                        竞争成功，将当前节点设为head。
                        因为是共享模式，如果还有剩余资源可用，需要唤醒后继节点。
                        */
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        failed = false;
                        return;
                    }
                }
                    /*
                    竞争失败，或自己压根就没资格去竞争，则判断是否需要Park。
                    Park的前提条件:前驱节点的waitStatus为SIGNAL
                    */
                if (shouldParkAfterFailedAcquire(p, node) &&
                        // 需要Park，则LockSupport.park()挂起线程
                        parkAndCheckInterrupt())
                    // 如果线程发生中断了，则抛异常
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                // 竞争失败后取消竞争了，将节点状态设为CANCELLED
                cancelAcquire(node);
        }
    }

    /**
     * Acquires in shared timed mode.
     *
     * @param arg the acquire argument
     * @param nanosTimeout max wait time
     * @return {@code true} if acquired
     */
    private boolean doAcquireSharedNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        // 不用挂起
        if (nanosTimeout <= 0L)
            return false;
        // 计算到期时间
        final long deadline = System.nanoTime() + nanosTimeout;
        // 创建节点并入队
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        // 获取到资源了
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        failed = false;
                        return true;
                    }
                }
                // 计算应该被挂起的时间
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L)
                    return false;
                // 判断竞争失败后是否需要被Park
                if (shouldParkAfterFailedAcquire(p, node) &&
                    nanosTimeout > spinForTimeoutThreshold)
                    // 需要，则Park给定时间，到期自动唤醒
                    LockSupport.parkNanos(this, nanosTimeout);
                if (Thread.interrupted())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                // 最终没有获取到资源，则退出竞争
                cancelAcquire(node);
        }
    }

    // Main exported methods

    /**
     * Attempts to acquire in exclusive mode. This method should query
     * if the state of the object permits it to be acquired in the
     * exclusive mode, and if so to acquire it.
     *
     * <p>This method is always invoked by the thread performing
     * acquire.  If this method reports failure, the acquire method
     * may queue the thread, if it is not already queued, until it is
     * signalled by a release from some other thread. This can be used
     * to implement method {@link Lock#tryLock()}.
     *
     * <p>The default
     * implementation throws {@link UnsupportedOperationException}.
     *
     * @param arg the acquire argument. This value is always the one
     *        passed to an acquire method, or is the value saved on entry
     *        to a condition wait.  The value is otherwise uninterpreted
     *        and can represent anything you like.
     * @return {@code true} if successful. Upon success, this object has
     *         been acquired.
     * @throws IllegalMonitorStateException if acquiring would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if exclusive mode is not supported
     */
    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to set the state to reflect a release in exclusive
     * mode.
     *
     * <p>This method is always invoked by the thread performing release.
     *
     * <p>The default implementation throws
     * {@link UnsupportedOperationException}.
     *
     * @param arg the release argument. This value is always the one
     *        passed to a release method, or the current state value upon
     *        entry to a condition wait.  The value is otherwise
     *        uninterpreted and can represent anything you like.
     * @return {@code true} if this object is now in a fully released
     *         state, so that any waiting threads may attempt to acquire;
     *         and {@code false} otherwise.
     * @throws IllegalMonitorStateException if releasing would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if exclusive mode is not supported
     */
    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to acquire in shared mode. This method should query if
     * the state of the object permits it to be acquired in the shared
     * mode, and if so to acquire it.
     *
     * <p>This method is always invoked by the thread performing
     * acquire.  If this method reports failure, the acquire method
     * may queue the thread, if it is not already queued, until it is
     * signalled by a release from some other thread.
     *
     * <p>The default implementation throws {@link
     * UnsupportedOperationException}.
     *
     * @param arg the acquire argument. This value is always the one
     *        passed to an acquire method, or is the value saved on entry
     *        to a condition wait.  The value is otherwise uninterpreted
     *        and can represent anything you like.
     * @return a negative value on failure; zero if acquisition in shared
     *         mode succeeded but no subsequent shared-mode acquire can
     *         succeed; and a positive value if acquisition in shared
     *         mode succeeded and subsequent shared-mode acquires might
     *         also succeed, in which case a subsequent waiting thread
     *         must check availability. (Support for three different
     *         return values enables this method to be used in contexts
     *         where acquires only sometimes act exclusively.)  Upon
     *         success, this object has been acquired.
     * @throws IllegalMonitorStateException if acquiring would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if shared mode is not supported
     */
    protected int tryAcquireShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to set the state to reflect a release in shared mode.
     *
     * <p>This method is always invoked by the thread performing release.
     *
     * <p>The default implementation throws
     * {@link UnsupportedOperationException}.
     *
     * @param arg the release argument. This value is always the one
     *        passed to a release method, or the current state value upon
     *        entry to a condition wait.  The value is otherwise
     *        uninterpreted and can represent anything you like.
     * @return {@code true} if this release of shared mode may permit a
     *         waiting acquire (shared or exclusive) to succeed; and
     *         {@code false} otherwise
     * @throws IllegalMonitorStateException if releasing would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if shared mode is not supported
     */
    protected boolean tryReleaseShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns {@code true} if synchronization is held exclusively with
     * respect to the current (calling) thread.  This method is invoked
     * upon each call to a non-waiting {@link ConditionObject} method.
     * (Waiting methods instead invoke {@link #release}.)
     *
     * <p>The default implementation throws {@link
     * UnsupportedOperationException}. This method is invoked
     * internally only within {@link ConditionObject} methods, so need
     * not be defined if conditions are not used.
     *
     * @return {@code true} if synchronization is held exclusively;
     *         {@code false} otherwise
     * @throws UnsupportedOperationException if conditions are not supported
     */
    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }

    /**
     * Acquires in exclusive mode, ignoring interrupts.  Implemented
     * by invoking at least once {@link #tryAcquire},
     * returning on success.  Otherwise the thread is queued, possibly
     * repeatedly blocking and unblocking, invoking {@link
     * #tryAcquire} until success.  This method can be used
     * to implement method {@link Lock#lock}.
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     */
    /*
    一、以独占形式获取锁
    二、忽略中断
        1.假如T1进入lock()方法后即被其它线程中断，lock()方法不会抛出InterruptedException异常
        2.acquire()在方法执行过程中会清除、并记录线程是否被中断
            1）否，不做任何操作
            2）是，调用selfInterrupt()中断线程，此时已经获取到同步状态，线程得以继续被执行，线程调用者可以自行决定是否中断继续中断该线程。
    三、至少调用一次tryAcquire()方法尝试快速获取同步状态（该方法不会阻塞）
        1.成功，返回
        2.失败
            1）调用addWaiter()方法将线程构造为Node节点加入同步队列
            2）阻塞线程直至获取到同步状态，在此期间，线程可能会反复被唤醒、阻塞，直至再次通过tryAcquire()方法获取到同步状态为止
     */
    public final void acquire(int arg) {
        // 尝试快速获取同步状态，若失败，则阻塞该线程直至获取到同步状态
        if (!tryAcquire(arg) &&
            acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            // 若线程在获取同步状态的过程中曾被其它线程中断，则再次中断该线程
            // 注意：acquireQueued返回值标识该线程在阻塞期间是否被中断，而不是是否成功获取到同步状态
            selfInterrupt();
    }

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
            throws InterruptedException {
        // 快速检查线程是否被中断
        if (Thread.interrupted())
            throw new InterruptedException();
        // 调用tryAcquire()方法获取同步状态。注意：该方法不响应中断
        if (!tryAcquire(arg))
            // 以响应中断的模式获取同步状态
            doAcquireInterruptibly(arg);
    }

    /**
     * Attempts to acquire in exclusive mode, aborting if interrupted,
     * and failing if the given timeout elapses.  Implemented by first
     * checking interrupt status, then invoking at least once {@link
     * #tryAcquire}, returning on success.  Otherwise, the thread is
     * queued, possibly repeatedly blocking and unblocking, invoking
     * {@link #tryAcquire} until success or the thread is interrupted
     * or the timeout elapses.  This method can be used to implement
     * method {@link Lock#tryLock(long, TimeUnit)}.
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     * @param nanosTimeout the maximum number of nanoseconds to wait
     * @return {@code true} if acquired; {@code false} if timed out
     * @throws InterruptedException if the current thread is interrupted
     */
    public final boolean tryAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        // 响应中断
        if (Thread.interrupted())
            throw new InterruptedException();
        // 调用tryAcquire()方法尝试快速获取同步状态
        // 若tryAcquire()方法未能获取到同步状态，则调用doAcquireNanos以带超时阻塞的形式再次获取同步状态
        return tryAcquire(arg) ||
            doAcquireNanos(arg, nanosTimeout);
    }

    /**
     * Releases in exclusive mode.  Implemented by unblocking one or
     * more threads if {@link #tryRelease} returns true.
     * This method can be used to implement method {@link Lock#unlock}.
     *
     * @param arg the release argument.  This value is conveyed to
     *        {@link #tryRelease} but is otherwise uninterpreted and
     *        can represent anything you like.
     * @return the value returned from {@link #tryRelease}
     */
    public final boolean release(int arg) {
        // 释放同步状态
        if (tryRelease(arg)) {
            // 唤醒后继节点
            Node h = head;
            if (h != null && h.waitStatus != 0)
                unparkSuccessor(h);
            return true;
        }
        return false;
    }

    /**
     * Acquires in shared mode, ignoring interrupts.  Implemented by
     * first invoking at least once {@link #tryAcquireShared},
     * returning on success.  Otherwise the thread is queued, possibly
     * repeatedly blocking and unblocking, invoking {@link
     * #tryAcquireShared} until success.
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquireShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     */
    public final void acquireShared(int arg) {
        if (tryAcquireShared(arg) < 0)
            // 尝试获取锁不成功，进入循环等待获取锁
            doAcquireShared(arg); // 执行完，说明当前线程获取了锁
    }

    /**
     * Acquires in shared mode, aborting if interrupted.  Implemented
     * by first checking interrupt status, then invoking at least once
     * {@link #tryAcquireShared}, returning on success.  Otherwise the
     * thread is queued, possibly repeatedly blocking and unblocking,
     * invoking {@link #tryAcquireShared} until success or the thread
     * is interrupted.
     * @param arg the acquire argument.
     * This value is conveyed to {@link #tryAcquireShared} but is
     * otherwise uninterpreted and can represent anything
     * you like.
     * @throws InterruptedException if the current thread is interrupted
     */
    // 共享模式下，响应中断的方式获取资源
    public final void acquireSharedInterruptibly(int arg)
            throws InterruptedException {
        // 如果线程发生中断，抛异常
        if (Thread.interrupted())
            throw new InterruptedException();
        // 尝试获取共享资源
        if (tryAcquireShared(arg) < 0)
            // 获取不到则入队并Park
            doAcquireSharedInterruptibly(arg);
    }

    /**
     * Attempts to acquire in shared mode, aborting if interrupted, and
     * failing if the given timeout elapses.  Implemented by first
     * checking interrupt status, then invoking at least once {@link
     * #tryAcquireShared}, returning on success.  Otherwise, the
     * thread is queued, possibly repeatedly blocking and unblocking,
     * invoking {@link #tryAcquireShared} until success or the thread
     * is interrupted or the timeout elapses.
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquireShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     * @param nanosTimeout the maximum number of nanoseconds to wait
     * @return {@code true} if acquired; {@code false} if timed out
     * @throws InterruptedException if the current thread is interrupted
     */
    public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        return tryAcquireShared(arg) >= 0 ||
            doAcquireSharedNanos(arg, nanosTimeout);
    }

    /**
     * Releases in shared mode.  Implemented by unblocking one or more
     * threads if {@link #tryReleaseShared} returns true.
     *
     * @param arg the release argument.  This value is conveyed to
     *        {@link #tryReleaseShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     * @return the value returned from {@link #tryReleaseShared}
     */
    public final boolean releaseShared(int arg) {
        if (tryReleaseShared(arg)) {
            doReleaseShared();
            return true;
        }
        return false;
    }

    // Queue inspection methods

    /**
     * Queries whether any threads are waiting to acquire. Note that
     * because cancellations due to interrupts and timeouts may occur
     * at any time, a {@code true} return does not guarantee that any
     * other thread will ever acquire.
     *
     * <p>In this implementation, this operation returns in
     * constant time.
     *
     * @return {@code true} if there may be other threads waiting to acquire
     */
    public final boolean hasQueuedThreads() {
        return head != tail;
    }

    /**
     * Queries whether any threads have ever contended to acquire this
     * synchronizer; that is if an acquire method has ever blocked.
     *
     * <p>In this implementation, this operation returns in
     * constant time.
     *
     * @return {@code true} if there has ever been contention
     */
    public final boolean hasContended() {
        return head != null;
    }

    /**
     * Returns the first (longest-waiting) thread in the queue, or
     * {@code null} if no threads are currently queued.
     *
     * <p>In this implementation, this operation normally returns in
     * constant time, but may iterate upon contention if other threads are
     * concurrently modifying the queue.
     *
     * @return the first (longest-waiting) thread in the queue, or
     *         {@code null} if no threads are currently queued
     */
    public final Thread getFirstQueuedThread() {
        // handle only fast path, else relay
        return (head == tail) ? null : fullGetFirstQueuedThread();
    }

    /**
     * Version of getFirstQueuedThread called when fastpath fails
     */
    private Thread fullGetFirstQueuedThread() {
        /*
         * The first node is normally head.next. Try to get its
         * thread field, ensuring consistent reads: If thread
         * field is nulled out or s.prev is no longer head, then
         * some other thread(s) concurrently performed setHead in
         * between some of our reads. We try this twice before
         * resorting to traversal.
         */
        Node h, s;
        Thread st;
        if (((h = head) != null && (s = h.next) != null &&
             s.prev == head && (st = s.thread) != null) ||
            ((h = head) != null && (s = h.next) != null &&
             s.prev == head && (st = s.thread) != null))
            return st;

        /*
         * Head's next field might not have been set yet, or may have
         * been unset after setHead. So we must check to see if tail
         * is actually first node. If not, we continue on, safely
         * traversing from tail back to head to find first,
         * guaranteeing termination.
         */

        Node t = tail;
        Thread firstThread = null;
        while (t != null && t != head) {
            Thread tt = t.thread;
            if (tt != null)
                firstThread = tt;
            t = t.prev;
        }
        return firstThread;
    }

    /**
     * Returns true if the given thread is currently queued.
     *
     * <p>This implementation traverses the queue to determine
     * presence of the given thread.
     *
     * @param thread the thread
     * @return {@code true} if the given thread is on the queue
     * @throws NullPointerException if the thread is null
     */
    public final boolean isQueued(Thread thread) {
        if (thread == null)
            throw new NullPointerException();
        for (Node p = tail; p != null; p = p.prev)
            if (p.thread == thread)
                return true;
        return false;
    }

    /**
     * Returns {@code true} if the apparent first queued thread, if one
     * exists, is waiting in exclusive mode.  If this method returns
     * {@code true}, and the current thread is attempting to acquire in
     * shared mode (that is, this method is invoked from {@link
     * #tryAcquireShared}) then it is guaranteed that the current thread
     * is not the first queued thread.  Used only as a heuristic in
     * ReentrantReadWriteLock.
     */
    /**
     * 如果明显的第一个排队线程（如果存在）正在以独占模式等待，则返回 true。
     * 如果此方法返回 true，并且当前线程正在尝试以共享模式获取（即，从 tryAcquireShared 调用此方法），
     * 则可以保证当前线程不是第一个排队的线程。仅在 ReentrantReadWriteLock 中用作启发式。
     */
    final boolean apparentlyFirstQueuedIsExclusive() {
        Node h, s;
        return (h = head) != null &&
            (s = h.next)  != null &&
            !s.isShared()         &&
            s.thread != null;
    }

    /**
     * Queries whether any threads have been waiting to acquire longer
     * than the current thread.
     *
     * <p>An invocation of this method is equivalent to (but may be
     * more efficient than):
     *  <pre> {@code
     * getFirstQueuedThread() != Thread.currentThread() &&
     * hasQueuedThreads()}</pre>
     *
     * <p>Note that because cancellations due to interrupts and
     * timeouts may occur at any time, a {@code true} return does not
     * guarantee that some other thread will acquire before the current
     * thread.  Likewise, it is possible for another thread to win a
     * race to enqueue after this method has returned {@code false},
     * due to the queue being empty.
     *
     * <p>This method is designed to be used by a fair synchronizer to
     * avoid <a href="AbstractQueuedSynchronizer#barging">barging</a>.
     * Such a synchronizer's {@link #tryAcquire} method should return
     * {@code false}, and its {@link #tryAcquireShared} method should
     * return a negative value, if this method returns {@code true}
     * (unless this is a reentrant acquire).  For example, the {@code
     * tryAcquire} method for a fair, reentrant, exclusive mode
     * synchronizer might look like this:
     *
     *  <pre> {@code
     * protected boolean tryAcquire(int arg) {
     *   if (isHeldExclusively()) {
     *     // A reentrant acquire; increment hold count
     *     return true;
     *   } else if (hasQueuedPredecessors()) {
     *     return false;
     *   } else {
     *     // try to acquire normally
     *   }
     * }}</pre>
     *
     * @return {@code true} if there is a queued thread preceding the
     *         current thread, and {@code false} if the current thread
     *         is at the head of the queue or the queue is empty
     * @since 1.7
     */
    // 公平锁使用：判断当前的线程是不是位于同步队列的首位，不是就是返回true，否就返回false。
    public final boolean hasQueuedPredecessors() {
        // The correctness of this depends on head being initialized
        // before tail and on head.next being accurate if the current
        // thread is first in queue.
        Node t = tail; // Read fields in reverse initialization order
        Node h = head;
        Node s;
        return h != t &&
            ((s = h.next) == null || s.thread != Thread.currentThread());
    }


    // Instrumentation and monitoring methods

    /**
     * Returns an estimate of the number of threads waiting to
     * acquire.  The value is only an estimate because the number of
     * threads may change dynamically while this method traverses
     * internal data structures.  This method is designed for use in
     * monitoring system state, not for synchronization
     * control.
     *
     * @return the estimated number of threads waiting to acquire
     */
    public final int getQueueLength() {
        int n = 0;
        for (Node p = tail; p != null; p = p.prev) {
            if (p.thread != null)
                ++n;
        }
        return n;
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire.  Because the actual set of threads may change
     * dynamically while constructing this result, the returned
     * collection is only a best-effort estimate.  The elements of the
     * returned collection are in no particular order.  This method is
     * designed to facilitate construction of subclasses that provide
     * more extensive monitoring facilities.
     *
     * @return the collection of threads
     */
    public final Collection<Thread> getQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            Thread t = p.thread;
            if (t != null)
                list.add(t);
        }
        return list;
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire in exclusive mode. This has the same properties
     * as {@link #getQueuedThreads} except that it only returns
     * those threads waiting due to an exclusive acquire.
     *
     * @return the collection of threads
     */
    public final Collection<Thread> getExclusiveQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            if (!p.isShared()) {
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire in shared mode. This has the same properties
     * as {@link #getQueuedThreads} except that it only returns
     * those threads waiting due to a shared acquire.
     *
     * @return the collection of threads
     */
    public final Collection<Thread> getSharedQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            if (p.isShared()) {
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }

    /**
     * Returns a string identifying this synchronizer, as well as its state.
     * The state, in brackets, includes the String {@code "State ="}
     * followed by the current value of {@link #getState}, and either
     * {@code "nonempty"} or {@code "empty"} depending on whether the
     * queue is empty.
     *
     * @return a string identifying this synchronizer, as well as its state
     */
    public String toString() {
        int s = getState();
        String q  = hasQueuedThreads() ? "non" : "";
        return super.toString() +
            "[State = " + s + ", " + q + "empty queue]";
    }


    // Internal support methods for Conditions

    /**
     * Returns true if a node, always one that was initially placed on
     * a condition queue, is now waiting to reacquire on sync queue.
     * @param node the node
     * @return true if is reacquiring
     */
    // 判断调用condition.await的线程是否在同步队列
    final boolean isOnSyncQueue(Node node) {
        //如果waitStatus为CONDITION 或者prev为null一定不在同步队列
        // 因为condition等待队列为单项列表 且没有pre节点 只有nextWaiter
        if (node.waitStatus == Node.CONDITION || node.prev == null)
            return false;
        if (node.next != null) // If has successor, it must be on queue
            return true;
        /*
         * node.prev can be non-null, but not yet on queue because
         * the CAS to place it on queue can fail. So we have to
         * traverse from tail to make sure it actually made it.  It
         * will always be near the tail in calls to this method, and
         * unless the CAS failed (which is unlikely), it will be
         * there, so we hardly ever traverse much.
         */
        return findNodeFromTail(node);
    }

    /**
     * Returns true if node is on sync queue by searching backwards from tail.
     * Called only when needed by isOnSyncQueue.
     * @return true if present
     */
    //从后往前遍历，是因为添加到AQS队列是先设置prev连接
    private boolean findNodeFromTail(Node node) {
        Node t = tail;
        for (;;) {
            if (t == node)
                //遍历到，直接返回
                return true;
            if (t == null)
                //遍历完之后 没有则返回false
                return false;
            t = t.prev;
        }
    }

    /**
     * Transfers a node from a condition queue onto sync queue.
     * Returns true if successful.
     * @param node the node
     * @return true if successfully transferred (else the node was
     * cancelled before signal)
     */
    final boolean transferForSignal(Node node) {
        /*
         * If cannot change waitStatus, the node has been cancelled.
         */
        if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
            return false;

        /*
         * Splice onto queue and try to set waitStatus of predecessor to
         * indicate that thread is (probably) waiting. If cancelled or
         * attempt to set waitStatus fails, wake up to resync (in which
         * case the waitStatus can be transiently and harmlessly wrong).
         */
        Node p = enq(node);
        int ws = p.waitStatus;
        if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
            LockSupport.unpark(node.thread);
        return true;
    }

    /**
     * Transfers node, if necessary, to sync queue after a cancelled wait.
     * Returns true if thread was cancelled before being signalled.
     *
     * @param node the node
     * @return true if cancelled before the node was signalled
     */
    /*
            该方法的返回值代表当前线程是否在park的时候被中断唤醒，如果为 true 表示中断在signal调用之前，signal还未执行，
            那么这个时候会根据await的语义，在await时遇到中断需要抛出interruptedException，返回true就是告诉
            checkInterruptWhileWaiting返回THROW_IE(-1)。
            如果返回 false，否则表示signal已经执行过了，只需要重新响应中断即可
    */
    final boolean transferAfterCancelledWait(Node node) {
        //cas设置节点awaitState为0，如果设置失败则表示线程cancel
        //使用 cas 修改节点状态，如果还能修改成功，说明线程被唤醒时，signal还没有被调用。
        //这里有一个知识点，就是线程被唤醒，并不一定是在 java 层面执行了locksupport.unpark，也可能是调用了线程的 interrupt()方法，这个方法会更新一个中断标识，并且会唤醒处于阻塞状态下的线程。
        if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) {
            // 没有修改加入阻塞队列
            enq(node);
            return true;
        }
        /*
         * If we lost out to a signal(), then we can't proceed
         * until it finishes its enq().  Cancelling during an
         * incomplete transfer is both rare and transient, so just
         * spin.
         */
        //如果cas失败，则判断当前node是否已经在AQS队列上，如果不在，则让给其他线程执行
        //当node被触发了signal方法时，node就会被加到aqs队列上
        while (!isOnSyncQueue(node))
            Thread.yield();
        return false;
    }

    /**
     * Invokes release with current state value; returns saved state.
     * Cancels node and throws exception on failure.
     * @param node the condition node for this wait
     * @return previous sync state
     */
    final int fullyRelease(Node node) {
        boolean failed = true;
        try {
            int savedState = getState();
            //释放方法同lock.unlock()一样
            //不同的是，这里不管重入了几次都是一次释放
            if (release(savedState)) {
                failed = false;
                return savedState;
            } else {
                throw new IllegalMonitorStateException();
            }
        } finally {
            if (failed)
                node.waitStatus = Node.CANCELLED;
        }
    }

    // Instrumentation methods for conditions

    /**
     * Queries whether the given ConditionObject
     * uses this synchronizer as its lock.
     *
     * @param condition the condition
     * @return {@code true} if owned
     * @throws NullPointerException if the condition is null
     */
    public final boolean owns(ConditionObject condition) {
        return condition.isOwnedBy(this);
    }

    /**
     * Queries whether any threads are waiting on the given condition
     * associated with this synchronizer. Note that because timeouts
     * and interrupts may occur at any time, a {@code true} return
     * does not guarantee that a future {@code signal} will awaken
     * any threads.  This method is designed primarily for use in
     * monitoring of the system state.
     *
     * @param condition the condition
     * @return {@code true} if there are any waiting threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
     */
    public final boolean hasWaiters(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.hasWaiters();
    }

    /**
     * Returns an estimate of the number of threads waiting on the
     * given condition associated with this synchronizer. Note that
     * because timeouts and interrupts may occur at any time, the
     * estimate serves only as an upper bound on the actual number of
     * waiters.  This method is designed for use in monitoring of the
     * system state, not for synchronization control.
     *
     * @param condition the condition
     * @return the estimated number of waiting threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
     */
    public final int getWaitQueueLength(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.getWaitQueueLength();
    }

    /**
     * Returns a collection containing those threads that may be
     * waiting on the given condition associated with this
     * synchronizer.  Because the actual set of threads may change
     * dynamically while constructing this result, the returned
     * collection is only a best-effort estimate. The elements of the
     * returned collection are in no particular order.
     *
     * @param condition the condition
     * @return the collection of threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
     */
    public final Collection<Thread> getWaitingThreads(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.getWaitingThreads();
    }

    /**
     * Condition implementation for a {@link
     * AbstractQueuedSynchronizer} serving as the basis of a {@link
     * Lock} implementation.
     *
     * <p>Method documentation for this class describes mechanics,
     * not behavioral specifications from the point of view of Lock
     * and Condition users. Exported versions of this class will in
     * general need to be accompanied by documentation describing
     * condition semantics that rely on those of the associated
     * {@code AbstractQueuedSynchronizer}.
     *
     * <p>This class is Serializable, but all fields are transient,
     * so deserialized conditions have no waiters.
     */
    public class ConditionObject implements Condition, java.io.Serializable {
        private static final long serialVersionUID = 1173984872572414699L;
        /** First node of condition queue. */
        private transient Node firstWaiter;
        /** Last node of condition queue. */
        private transient Node lastWaiter;

        /**
         * Creates a new {@code ConditionObject} instance.
         */
        public ConditionObject() { }

        // Internal methods

        /**
         * Adds a new waiter to wait queue.
         * @return its new wait node
         */
        //创建一个新的节点，节点的状态是condition，数据结构为单向链表
        private Node addConditionWaiter() {
            Node t = lastWaiter;
            // If lastWaiter is cancelled, clean out.
            //如果lastWaiter节点为cancel状态，则清理掉
            if (t != null && t.waitStatus != Node.CONDITION) {
                unlinkCancelledWaiters();
                t = lastWaiter;
            }
            //第一次进来的时候 lastwaiter为null
            //创建一个新的节点，节点的状态是condition，数据结构为链表 传当前线程
            Node node = new Node(Thread.currentThread(), Node.CONDITION);
            //lastWaiter 设置firstWaiter为node
            if (t == null)
                firstWaiter = node;
            else
                t.nextWaiter = node;
            lastWaiter = node;
            return node;
        }

        /**
         * Removes and transfers nodes until hit non-cancelled one or
         * null. Split out from signal in part to encourage compilers
         * to inline the case of no waiters.
         * @param first (non-null) the first node on condition queue
         */
        private void doSignal(Node first) {
            do {
                if ( (firstWaiter = first.nextWaiter) == null)
                    lastWaiter = null;
                first.nextWaiter = null;
            } while (!transferForSignal(first) &&
                     (first = firstWaiter) != null);
        }

        /**
         * Removes and transfers all nodes.
         * @param first (non-null) the first node on condition queue
         */
        private void doSignalAll(Node first) {
            lastWaiter = firstWaiter = null;
            do {
                Node next = first.nextWaiter;
                first.nextWaiter = null;
                transferForSignal(first);
                first = next;
            } while (first != null);
        }

        /**
         * Unlinks cancelled waiter nodes from condition queue.
         * Called only while holding lock. This is called when
         * cancellation occurred during condition wait, and upon
         * insertion of a new waiter when lastWaiter is seen to have
         * been cancelled. This method is needed to avoid garbage
         * retention in the absence of signals. So even though it may
         * require a full traversal, it comes into play only when
         * timeouts or cancellations occur in the absence of
         * signals. It traverses all nodes rather than stopping at a
         * particular target to unlink all pointers to garbage nodes
         * without requiring many re-traversals during cancellation
         * storms.
         */
        // 已经不是条件状态等待节点的断开连接
        // 重新赋值 队列第一个 和 最后一个节点
        private void unlinkCancelledWaiters() {
            Node t = firstWaiter;
            // 记录节点
            Node trail = null;
            while (t != null) {
                Node next = t.nextWaiter;
                if (t.waitStatus != Node.CONDITION) {// 当前节点不是条件等待状态
                    t.nextWaiter = null;// 赋值为空
                    if (trail == null)
                        // 说明是此前循环都不是等待状态节点
                        firstWaiter = next;
                    else
                        trail.nextWaiter = next;
                    if (next == null)
                        lastWaiter = trail;
                }
                else
                    trail = t;// 当前节点是条件等待状态，记录节点赋值当前节点
                t = next;
            }
        }

        // public methods

        /**
         * Moves the longest-waiting thread, if one exists, from the
         * wait queue for this condition to the wait queue for the
         * owning lock.
         *
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        public final void signal() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            Node first = firstWaiter;
            if (first != null)
                doSignal(first);
        }

        /**
         * Moves all threads from the wait queue for this condition to
         * the wait queue for the owning lock.
         *
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        public final void signalAll() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            Node first = firstWaiter;
            if (first != null)
                doSignalAll(first);
        }

        /**
         * Implements uninterruptible condition wait.
         * <ol>
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * </ol>
         */
        public final void awaitUninterruptibly() {
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            boolean interrupted = false;
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this);
                if (Thread.interrupted())
                    interrupted = true;
            }
            if (acquireQueued(node, savedState) || interrupted)
                selfInterrupt();
        }

        /*
         * For interruptible waits, we need to track whether to throw
         * InterruptedException, if interrupted while blocked on
         * condition, versus reinterrupt current thread, if
         * interrupted while blocked waiting to re-acquire.
         */

        /** Mode meaning to reinterrupt on exit from wait */
        private static final int REINTERRUPT =  1;
        /** Mode meaning to throw InterruptedException on exit from wait */
        //模式意味着在退出等待时抛出中断异常
        private static final int THROW_IE    = -1;

        /**
         * Checks for interrupt, returning THROW_IE if interrupted
         * before signalled, REINTERRUPT if after signalled, or
         * 0 if not interrupted.
         */
        //检查中断，如果在发出信号之前中断，则返回THROW_IE，如果在发出信号后返回重新中断，如果未中断则返回 0。
        private int checkInterruptWhileWaiting(Node node) {
            return Thread.interrupted() ?
                    // 如果当前线程再park 期间被中断过 进入取消等待后的转移transferAfterCancelledWait
                    // 如果节点条件等待状态被修改过，就重新中断 返回1 ， 否则抛出异常 返回 -1
                (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) : //REINTERRUPT ：重新中断
                0;
        }

        /**
         * Throws InterruptedException, reinterrupts current thread, or
         * does nothing, depending on mode.
         */
        private void reportInterruptAfterWait(int interruptMode)
            throws InterruptedException {
            if (interruptMode == THROW_IE)
                throw new InterruptedException();
            else if (interruptMode == REINTERRUPT)
                selfInterrupt();
        }

        /**
         * Implements interruptible condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled or interrupted.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * </ol>
         */
        public final void await() throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            // 创建一个新的节点，节点的状态是condition，数据结构为链表
            Node node = addConditionWaiter();
            //释放当前的锁，得到锁的状态，并唤醒AQS队列中的一个线程
            //并且缓存当前线程的state 后续唤醒时继续设置，考虑重入锁
            int savedState = fullyRelease(node);
            int interruptMode = 0;
            //判断节点是否在同步队列 如果不在同步队列 则阻塞线程

            while (!isOnSyncQueue(node)) {
                // 阻塞当前线程
                LockSupport.park(this);
                // 判断是否抛异常或重新中断
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    // 线程等待被终止过 结束循环
                    break;

            }
            //当线程被唤醒后 继续执行以下逻辑 唤醒线程
            //当这个线程醒来,会尝试拿锁,当acquireQueued返回false就是拿到锁了.
            //interruptMode != THROW_IE -> 表示这个线程被中断,但signal执行了enq方法让其入队了.
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)// -1
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null) // clean up if cancelled
                //如果 node 的下一个等待者不是 null, 则进行清理,清理 Condition 队列上的节点
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                // 如果线程被中断了,需要根据transferAfterCancelledWait的返回结果判断怎么处理
                reportInterruptAfterWait(interruptMode);
        }

        /**
         * Implements timed condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * </ol>
         */
        public final long awaitNanos(long nanosTimeout)
                throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return deadline - System.nanoTime();
        }

        /**
         * Implements absolute timed condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * <li> If timed out while blocked in step 4, return false, else true.
         * </ol>
         */
        public final boolean awaitUntil(Date deadline)
                throws InterruptedException {
            long abstime = deadline.getTime();
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (System.currentTimeMillis() > abstime) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                LockSupport.parkUntil(this, abstime);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return !timedout;
        }

        /**
         * Implements timed condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * <li> If timed out while blocked in step 4, return false, else true.
         * </ol>
         */
        public final boolean await(long time, TimeUnit unit)
                throws InterruptedException {
            long nanosTimeout = unit.toNanos(time);
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return !timedout;
        }

        //  support for instrumentation

        /**
         * Returns true if this condition was created by the given
         * synchronization object.
         *
         * @return {@code true} if owned
         */
        final boolean isOwnedBy(AbstractQueuedSynchronizer sync) {
            return sync == AbstractQueuedSynchronizer.this;
        }

        /**
         * Queries whether any threads are waiting on this condition.
         * Implements {@link AbstractQueuedSynchronizer#hasWaiters(ConditionObject)}.
         *
         * @return {@code true} if there are any waiting threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        protected final boolean hasWaiters() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION)
                    return true;
            }
            return false;
        }

        /**
         * Returns an estimate of the number of threads waiting on
         * this condition.
         * Implements {@link AbstractQueuedSynchronizer#getWaitQueueLength(ConditionObject)}.
         *
         * @return the estimated number of waiting threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        protected final int getWaitQueueLength() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            int n = 0;
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION)
                    ++n;
            }
            return n;
        }

        /**
         * Returns a collection containing those threads that may be
         * waiting on this Condition.
         * Implements {@link AbstractQueuedSynchronizer#getWaitingThreads(ConditionObject)}.
         *
         * @return the collection of threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        protected final Collection<Thread> getWaitingThreads() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            ArrayList<Thread> list = new ArrayList<Thread>();
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION) {
                    Thread t = w.thread;
                    if (t != null)
                        list.add(t);
                }
            }
            return list;
        }
    }

    /**
     * Setup to support compareAndSet. We need to natively implement
     * this here: For the sake of permitting future enhancements, we
     * cannot explicitly subclass AtomicInteger, which would be
     * efficient and useful otherwise. So, as the lesser of evils, we
     * natively implement using hotspot intrinsics API. And while we
     * are at it, we do the same for other CASable fields (which could
     * otherwise be done with atomic field updaters).
     */
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long stateOffset;
    private static final long headOffset;
    private static final long tailOffset;
    private static final long waitStatusOffset;
    private static final long nextOffset;

    static {
        try {
            stateOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("state"));
            headOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("head"));
            tailOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("tail"));
            waitStatusOffset = unsafe.objectFieldOffset
                (Node.class.getDeclaredField("waitStatus"));
            nextOffset = unsafe.objectFieldOffset
                (Node.class.getDeclaredField("next"));

        } catch (Exception ex) { throw new Error(ex); }
    }

    /**
     * CAS head field. Used only by enq.
     */
    private final boolean compareAndSetHead(Node update) {
        return unsafe.compareAndSwapObject(this, headOffset, null, update);
    }

    /**
     * CAS tail field. Used only by enq.
     */
    private final boolean compareAndSetTail(Node expect, Node update) {
        return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
    }

    /**
     * CAS waitStatus field of a node.
     */
    private static final boolean compareAndSetWaitStatus(Node node,
                                                         int expect,
                                                         int update) {
        return unsafe.compareAndSwapInt(node, waitStatusOffset,
                                        expect, update);
    }

    /**
     * CAS next field of a node.
     */
    private static final boolean compareAndSetNext(Node node,
                                                   Node expect,
                                                   Node update) {
        return unsafe.compareAndSwapObject(node, nextOffset, expect, update);
    }
}
