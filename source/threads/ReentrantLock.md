
# [公平锁](http://wangkuiwu.github.io/2012/08/13/juc-lock03/)

## 第1部分 基本概念
本章，我们会讲解“线程获取公平锁”的原理；在讲解之前，需要了解几个基本概念。后面的内容，都是基于这些概念的；这些概念可能比较枯燥，但从这些概念中，能窥见“java锁”的一些架构，这对我们了解锁是有帮助的。
1. AQS -- 指AbstractQueuedSynchronizer类。
     AQS是java中管理“锁”的抽象类，锁的许多公共方法都是在这个类中实现。AQS是独占锁(例如，ReentrantLock)和共享锁(例如，Semaphore)的公共父类。
2. AQS锁的类别 -- 分为“独占锁”和“共享锁”两种。
     (01) 独占锁 -- 锁在一个时间点只能被一个线程锁占有。根据锁的获取机制，它又划分为“公平锁”和“非公平锁”。公平锁，是按照通过CLH等待线程按照先来先得的规则，公平的获取锁；而非公平锁，则当线程要获取锁时，它会无视CLH等待队列而直接获取锁。独占锁的典型实例子是ReentrantLock，此外，ReentrantReadWriteLock.WriteLock也是独占锁。
     (02) 共享锁 -- 能被多个线程同时拥有，能被共享的锁。JUC包中的ReentrantReadWriteLock.ReadLock，CyclicBarrier， CountDownLatch和Semaphore都是共享锁。这些锁的用途和原理，在以后的章节再详细介绍。
3. CLH队列 -- Craig, Landin, and Hagersten lock queue
     CLH队列是AQS中“等待锁”的线程队列。在多线程中，为了保护竞争资源不被多个线程同时操作而起来错误，我们常常需要通过锁来保护这些资源。在独占锁中，竞争资源在一个时间点只能被一个线程锁访问；而其它线程则需要等待。CLH就是管理这些“等待锁”的线程的队列。
     CLH是一个非阻塞的 FIFO 队列。也就是说往里面插入或移除一个节点的时候，在并发条件下不会阻塞，而是通过自旋锁和 CAS 保证节点插入和移除的原子性。
4. CAS函数 -- Compare And Swap
     CAS函数，是比较并交换函数，它是原子操作函数；即，通过CAS操作的数据都是以原子方式进行的。例如，compareAndSetHead(), compareAndSetTail(), compareAndSetNext()等函数。它们共同的特点是，这些函数所执行的动作是以原子的方式进行的。

本章是围绕“公平锁”如何获取锁而层次展开。“公平锁”涉及到的知识点比较多，但总的来说，不是特别难；如果读者能读懂AQS和ReentrantLock.java这两个类的大致意思，理解锁的原理和机制也就不成问题了。本章只是作者本人对锁的一点点理解，希望这部分知识能帮助您了解“公平锁”的获取过程，认识“锁”的框架。

第2部分 ReentrantLock数据结构

1. ReentrantLock实现了Lock接口。
2. ReentrantLock与sync是组合关系。ReentrantLock中，包含了Sync对象；而且，Sync是AQS的子类；更重要的是，Sync有两个子类
FairSync(公平锁)和NonFairSync(非公平锁)。ReentrantLock是一个独占锁，至于它到底是公平锁还是非公平锁，就取决于sync对象是"FairSync的实例"还是"NonFairSync的实例"。

## 第3部分 参考代码
ReentranLock.java
AQS(AbstractQueuedSynchronizer.java)
由于源码太多，这里就不粘贴出源码了，感兴趣的读者可以自行查看。
第4部分 获取公平锁(基于JDK1.7.0_40)
通过前面“Java多线程系列--“JUC锁”02之 互斥锁ReentrantLock”的“示例1”，我们知道，获取锁是通过lock()函数。下面，我们以lock()对获取公平锁的过程进行展开。
1. lock()
lock()在ReentrantLock.java的FairSync类中实现，它的源码如下：
final void lock() {
    acquire(1);
}
说明：“当前线程”实际上是通过acquire(1)获取锁的。
这里说明一下“1”的含义，它是设置“锁的状态”的参数。对于“独占锁”而言，锁处于可获取状态时，它的状态值是0；锁被线程初次获取到了，它的状态值就变成了1。
由于ReentrantLock(公平锁/非公平锁)是可重入锁，所以“独占锁”可以被单个线程多此获取，每获取1次就将锁的状态+1。也就是说，初次获取锁时，通过acquire(1)将锁的状态值设为1；再次获取锁时，将锁的状态值设为2；依次类推...这就是为什么获取锁时，传入的参数是1的原因了。
可重入就是指锁可以被单个线程多次获取。
2. acquire()
acquire()在AQS中实现的，它的源码如下：
public final void acquire(int arg) {
    if (!tryAcquire(arg) &&
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}
(01) “当前线程”首先通过tryAcquire()尝试获取锁。获取成功的话，直接返回；尝试失败的话，进入到等待队列排序等待(前面还有可能有需要线程在等待该锁)。
(02) “当前线程”尝试失败的情况下，先通过addWaiter(Node.EXCLUSIVE)来将“当前线程”加入到"CLH队列(非阻塞的FIFO队列)"末尾。CLH队列就是线程等待队列。
(03) 再执行完addWaiter(Node.EXCLUSIVE)之后，会调用acquireQueued()来获取锁。由于此时ReentrantLock是公平锁，它会根据公平性原则来获取锁。
(04) “当前线程”在执行acquireQueued()时，会进入到CLH队列中休眠等待，直到获取锁了才返回！如果“当前线程”在休眠等待过程中被中断过，acquireQueued会返回true，此时"当前线程"会调用selfInterrupt()来自己给自己产生一个中断。至于为什么要自己给自己产生一个中断，后面再介绍。
上面是对acquire()的概括性说明。下面，我们将该函数分为4部分来逐步解析。
一. tryAcquire()
二. addWaiter()
三. acquireQueued()
四. selfInterrupt()
第4.1部分 tryAcquire()
1. tryAcquire()
公平锁的tryAcquire()在ReentrantLock.java的FairSync类中实现，源码如下：
protected final boolean tryAcquire(int acquires) {
    // 获取“当前线程”
    final Thread current = Thread.currentThread();
    // 获取“独占锁”的状态
    int c = getState();
    // c=0意味着“锁没有被任何线程锁拥有”，
    if (c == 0) {
        // 若“锁没有被任何线程锁拥有”，
        // 则判断“当前线程”是不是CLH队列中的第一个线程线程，
        // 若是的话，则获取该锁，设置锁的状态，并切设置锁的拥有者为“当前线程”。
        if (!hasQueuedPredecessors() &&
            compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    }
    else if (current == getExclusiveOwnerThread()) {
        // 如果“独占锁”的拥有者已经为“当前线程”，
        // 则将更新锁的状态。
        int nextc = c + acquires;
        if (nextc < 0)
            throw new Error("Maximum lock count exceeded");
        setState(nextc);
        return true;
    }
    return false;
}
说明：根据代码，我们可以分析出，tryAcquire()的作用就是尝试去获取锁。注意，这里只是尝试！
尝试成功的话，返回true；尝试失败的话，返回false，后续再通过其它办法来获取该锁。后面我们会说明，在尝试失败的情况下，是如何一步步获取锁的。
2. hasQueuedPredecessors()
hasQueuedPredecessors()在AQS中实现，源码如下：
public final boolean hasQueuedPredecessors() {
    Node t = tail; 
    Node h = head;
    Node s;
    return h != t &&
        ((s = h.next) == null || s.thread != Thread.currentThread());
}
说明： 通过代码，能分析出，hasQueuedPredecessors() 是通过判断"当前线程"是不是在CLH队列的队首，来返回AQS中是不是有比“当前线程”等待更久的线程。下面对head、tail和Node进行说明。
3. Node的源码
Node就是CLH队列的节点。Node在AQS中实现，它的数据结构如下：
private transient volatile Node head;    // CLH队列的队首
private transient volatile Node tail;    // CLH队列的队尾

// CLH队列的节点
static final class Node {
    static final Node SHARED = new Node();
    static final Node EXCLUSIVE = null;

    // 线程已被取消，对应的waitStatus的值
    static final int CANCELLED =  1;
    // “当前线程的后继线程需要被unpark(唤醒)”，对应的waitStatus的值。
    // 一般发生情况是：当前线程的后继线程处于阻塞状态，而当前线程被release或cancel掉，因此需要唤醒当前线程的后继线程。
    static final int SIGNAL    = -1;
    // 线程(处在Condition休眠状态)在等待Condition唤醒，对应的waitStatus的值
    static final int CONDITION = -2;
    // (共享锁)其它线程获取到“共享锁”，对应的waitStatus的值
    static final int PROPAGATE = -3;

    // waitStatus为“CANCELLED, SIGNAL, CONDITION, PROPAGATE”时分别表示不同状态，
    // 若waitStatus=0，则意味着当前线程不属于上面的任何一种状态。
    volatile int waitStatus;

    // 前一节点
    volatile Node prev;

    // 后一节点
    volatile Node next;

    // 节点所对应的线程
    volatile Thread thread;

    // nextWaiter是“区别当前CLH队列是 ‘独占锁’队列 还是 ‘共享锁’队列 的标记”
    // 若nextWaiter=SHARED，则CLH队列是“独占锁”队列；
    // 若nextWaiter=EXCLUSIVE，(即nextWaiter=null)，则CLH队列是“共享锁”队列。
    Node nextWaiter;

    // “共享锁”则返回true，“独占锁”则返回false。
    final boolean isShared() {
        return nextWaiter == SHARED;
    }

    // 返回前一节点
    final Node predecessor() throws NullPointerException {
        Node p = prev;
        if (p == null)
            throw new NullPointerException();
        else
            return p;
    }

    Node() {    // Used to establish initial head or SHARED marker
    }

    // 构造函数。thread是节点所对应的线程，mode是用来表示thread的锁是“独占锁”还是“共享锁”。
    Node(Thread thread, Node mode) {     // Used by addWaiter
        this.nextWaiter = mode;
        this.thread = thread;
    }

    // 构造函数。thread是节点所对应的线程，waitStatus是线程的等待状态。
    Node(Thread thread, int waitStatus) { // Used by Condition
        this.waitStatus = waitStatus;
        this.thread = thread;
    }
}
说明：
Node是CLH队列的节点，代表“等待锁的线程队列”。
(01) 每个Node都会一个线程对应。
(02) 每个Node会通过prev和next分别指向上一个节点和下一个节点，这分别代表上一个等待线程和下一个等待线程。
(03) Node通过waitStatus保存线程的等待状态。
(04) Node通过nextWaiter来区分线程是“独占锁”线程还是“共享锁”线程。如果是“独占锁”线程，则nextWaiter的值为EXCLUSIVE；如果是“共享锁”线程，则nextWaiter的值是SHARED。
4. compareAndSetState()
compareAndSetState()在AQS中实现。它的源码如下：
protected final boolean compareAndSetState(int expect, int update) {
    return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
}
说明： compareAndSwapInt() 是sun.misc.Unsafe类中的一个本地方法。对此，我们需要了解的是 compareAndSetState(expect, update) 是以原子的方式操作当前线程；若当前线程的状态为expect，则设置它的状态为update。
5. setExclusiveOwnerThread()
setExclusiveOwnerThread()在AbstractOwnableSynchronizer.java中实现，它的源码如下：
// exclusiveOwnerThread是当前拥有“独占锁”的线程
private transient Thread exclusiveOwnerThread;
protected final void setExclusiveOwnerThread(Thread t) {
    exclusiveOwnerThread = t;
}
说明：setExclusiveOwnerThread()的作用就是，设置线程t为当前拥有“独占锁”的线程。
6. getState(), setState()
getState()和setState()都在AQS中实现，源码如下：
// 锁的状态
private volatile int state;
// 设置锁的状态
protected final void setState(int newState) {
    state = newState;
}
// 获取锁的状态
protected final int getState() {
    return state;
}
说明：state表示锁的状态，对于“独占锁”而已，state=0表示锁是可获取状态(即，锁没有被任何线程锁持有)。由于java中的独占锁是可重入的，state的值可以>1。
小结：tryAcquire()的作用就是让“当前线程”尝试获取锁。获取成功返回true，失败则返回false。
第4.2部分 addWaiter(Node.EXCLUSIVE)
addWaiter(Node.EXCLUSIVE)的作用是，创建“当前线程”的Node节点，且Node中记录“当前线程”对应的锁是“独占锁”类型，并且将该节点添加到CLH队列的末尾。
1.addWaiter()
addWaiter()在AQS中实现，源码如下：
private Node addWaiter(Node mode) {
    // 新建一个Node节点，节点对应的线程是“当前线程”，“当前线程”的锁的模型是mode。
    Node node = new Node(Thread.currentThread(), mode);
    Node pred = tail;
    // 若CLH队列不为空，则将“当前线程”添加到CLH队列末尾
    if (pred != null) {
        node.prev = pred;
        if (compareAndSetTail(pred, node)) {
            pred.next = node;
            return node;
        }
    }
    // 若CLH队列为空，则调用enq()新建CLH队列，然后再将“当前线程”添加到CLH队列中。
    enq(node);
    return node;
}
说明：对于“公平锁”而言，addWaiter(Node.EXCLUSIVE)会首先创建一个Node节点，节点的类型是“独占锁”(Node.EXCLUSIVE)类型。然后，再将该节点添加到CLH队列的末尾。
2. compareAndSetTail()
compareAndSetTail()在AQS中实现，源码如下：
private final boolean compareAndSetTail(Node expect, Node update) {
    return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
}
说明：compareAndSetTail也属于CAS函数，也是通过“本地方法”实现的。compareAndSetTail(expect, update)会以原子的方式进行操作，它的作用是判断CLH队列的队尾是不是为expect，是的话，就将队尾设为update。
3. enq()
enq()在AQS中实现，源码如下：
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
说明： enq()的作用很简单。如果CLH队列为空，则新建一个CLH表头；然后将node添加到CLH末尾。否则，直接将node添加到CLH末尾。
小结：addWaiter()的作用，就是将当前线程添加到CLH队列中。这就意味着将当前线程添加到等待获取“锁”的等待线程队列中了。
第4.3部分 acquireQueued()
前面，我们已经将当前线程添加到CLH队列中了。而acquireQueued()的作用就是逐步的去执行CLH队列的线程，如果当前线程获取到了锁，则返回；否则，当前线程进行休眠，直到唤醒并重新获取锁了才返回。下面，我们看看acquireQueued()的具体流程。
1. acquireQueued()
acquireQueued()在AQS中实现，源码如下：
final boolean acquireQueued(final Node node, int arg) {
    boolean failed = true;
    try {
        // interrupted表示在CLH队列的调度中，
        // “当前线程”在休眠时，有没有被中断过。
        boolean interrupted = false;
        for (;;) {
            // 获取上一个节点。
            // node是“当前线程”对应的节点，这里就意味着“获取上一个等待锁的线程”。
            final Node p = node.predecessor();
            if (p == head && tryAcquire(arg)) {
                setHead(node);
                p.next = null; // help GC
                failed = false;
                return interrupted;
            }
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}
说明：acquireQueued()的目的是从队列中获取锁。
2. shouldParkAfterFailedAcquire()
shouldParkAfterFailedAcquire()在AQS中实现，源码如下：
// 返回“当前线程是否应该阻塞”
private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
    // 前继节点的状态
    int ws = pred.waitStatus;
    // 如果前继节点是SIGNAL状态，则意味这当前线程需要被unpark唤醒。此时，返回true。
    if (ws == Node.SIGNAL)
        return true;
    // 如果前继节点是“取消”状态，则设置 “当前节点”的 “当前前继节点”  为  “‘原前继节点’的前继节点”。
    if (ws > 0) {
        do {
            node.prev = pred = pred.prev;
        } while (pred.waitStatus > 0);
        pred.next = node;
    } else {
        // 如果前继节点为“0”或者“共享锁”状态，则设置前继节点为SIGNAL状态。
        compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
    }
    return false;
}
说明：
(01) 关于waitStatus请参考下表(中扩号内为waitStatus的值)，更多关于waitStatus的内容，可以参考前面的Node类的介绍。
CANCELLED[1]  -- 当前线程已被取消
SIGNAL[-1]    -- “当前线程的后继线程需要被unpark(唤醒)”。一般发生情况是：当前线程的后继线程处于阻塞状态，而当前线程被release或cancel掉，因此需要唤醒当前线程的后继线程。
CONDITION[-2] -- 当前线程(处在Condition休眠状态)在等待Condition唤醒
PROPAGATE[-3] -- (共享锁)其它线程获取到“共享锁”
[0]           -- 当前线程不属于上面的任何一种状态。
(02) shouldParkAfterFailedAcquire()通过以下规则，判断“当前线程”是否需要被阻塞。
规则1：如果前继节点状态为SIGNAL，表明当前节点需要被unpark(唤醒)，此时则返回true。
规则2：如果前继节点状态为CANCELLED(ws>0)，说明前继节点已经被取消，则通过先前回溯找到一个有效(非CANCELLED状态)的节点，并返回false。
规则3：如果前继节点状态为非SIGNAL、非CANCELLED，则设置前继的状态为SIGNAL，并返回false。
如果“规则1”发生，即“前继节点是SIGNAL”状态，则意味着“当前线程”需要被阻塞。接下来会调用parkAndCheckInterrupt()阻塞当前线程，直到当前先被唤醒才从parkAndCheckInterrupt()中返回。
3. parkAndCheckInterrupt())
parkAndCheckInterrupt()在AQS中实现，源码如下：
private final boolean parkAndCheckInterrupt() {
    // 通过LockSupport的park()阻塞“当前线程”。
    LockSupport.park(this);
    // 返回线程的中断状态。
    return Thread.interrupted();
}
说明：parkAndCheckInterrupt()的作用是阻塞当前线程，并且返回“线程被唤醒之后”的中断状态。
它会先通过LockSupport.park()阻塞“当前线程”，然后通过Thread.interrupted()返回线程的中断状态。
这里介绍一下线程被阻塞之后如何唤醒。一般有2种情况：
第1种情况：unpark()唤醒。“前继节点对应的线程”使用完锁之后，通过unpark()方式唤醒当前线程。
第2种情况：中断唤醒。其它线程通过interrupt()中断当前线程。
补充：LockSupport()中的park(),unpark()的作用 和 Object中的wait(),notify()作用类似，是阻塞/唤醒。
它们的用法不同，park(),unpark()是轻量级的，而wait(),notify()是必须先通过Synchronized获取同步锁。
关于LockSupport，我们会在之后的章节再专门进行介绍！
4. 再次tryAcquire()
了解了shouldParkAfterFailedAcquire()和parkAndCheckInterrupt()函数之后。我们接着分析acquireQueued()的for循环部分。
final Node p = node.predecessor();
if (p == head && tryAcquire(arg)) {
    setHead(node);
    p.next = null; // help GC
    failed = false;
    return interrupted;
}
说明：
(01) 通过node.predecessor()获取前继节点。predecessor()就是返回node的前继节点，若对此有疑惑可以查看下面关于Node类的介绍。
(02) p == head && tryAcquire(arg)
     首先，判断“前继节点”是不是CHL表头。如果是的话，则通过tryAcquire()尝试获取锁。
     其实，这样做的目的是为了“让当前线程获取锁”，但是为什么需要先判断p==head呢？理解这个对理解“公平锁”的机制很重要，因为这么做的原因就是为了保证公平性！
     (a) 前面，我们在shouldParkAfterFailedAcquire()我们判断“当前线程”是否需要阻塞；
     (b) 接着，“当前线程”阻塞的话，会调用parkAndCheckInterrupt()来阻塞线程。当线程被解除阻塞的时候，我们会返回线程的中断状态。而线程被解决阻塞，可能是由于“线程被中断”，也可能是由于“其它线程调用了该线程的unpark()函数”。
     (c) 再回到p==head这里。如果当前线程是因为其它线程调用了unpark()函数而被唤醒，那么唤醒它的线程，应该是它的前继节点所对应的线程(关于这一点，后面在“释放锁”的过程中会看到)。 OK，是前继节点调用unpark()唤醒了当前线程！
     此时，再来理解p==head就很简单了：当前继节点是CLH队列的头节点，并且它释放锁之后；就轮到当前节点获取锁了。然后，当前节点通过tryAcquire()获取锁；获取成功的话，通过setHead(node)设置当前节点为头节点，并返回。
总之，如果“前继节点调用unpark()唤醒了当前线程”并且“前继节点是CLH表头”，此时就是满足p==head，也就是符合公平性原则的。否则，如果当前线程是因为“线程被中断”而唤醒，那么显然就不是公平了。这就是为什么说p==head就是保证公平性！
小结：acquireQueued()的作用就是“当前线程”会根据公平性原则进行阻塞等待，直到获取锁为止；并且返回当前线程在等待过程中有没有并中断过。
第4.4部分 selfInterrupt()
selfInterrupt()是AQS中实现，源码如下：
private static void selfInterrupt() {
    Thread.currentThread().interrupt();
}
说明：selfInterrupt()的代码很简单，就是“当前线程”自己产生一个中断。但是，为什么需要这么做呢？
这必须结合acquireQueued()进行分析。如果在acquireQueued()中，当前线程被中断过，则执行selfInterrupt()；否则不会执行。
在acquireQueued()中，即使是线程在阻塞状态被中断唤醒而获取到cpu执行权利；但是，如果该线程的前面还有其它等待锁的线程，根据公平性原则，该线程依然无法获取到锁。它会再次阻塞！ 该线程再次阻塞，直到该线程被它的前面等待锁的线程锁唤醒；线程才会获取锁，然后“真正执行起来”！
也就是说，在该线程“成功获取锁并真正执行起来”之前，它的中断会被忽略并且中断标记会被清除！ 因为在parkAndCheckInterrupt()中，我们线程的中断状态时调用了Thread.interrupted()。该函数不同于Thread的isInterrupted()函数，isInterrupted()仅仅返回中断状态，而interrupted()在返回当前中断状态之后，还会清除中断状态。 正因为之前的中断状态被清除了，所以这里需要调用selfInterrupt()重新产生一个中断！
小结：selfInterrupt()的作用就是当前线程自己产生一个中断。
总结
再回过头看看acquire()函数，它最终的目的是获取锁！
public final void acquire(int arg) {
    if (!tryAcquire(arg) &&
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}
(01) 先是通过tryAcquire()尝试获取锁。获取成功的话，直接返回；尝试失败的话，再通过acquireQueued()获取锁。
(02) 尝试失败的情况下，会先通过addWaiter()来将“当前线程”加入到"CLH队列"末尾；然后调用acquireQueued()，在CLH队列中排序等待获取锁，在此过程中，线程处于休眠状态。直到获取锁了才返回。 如果在休眠等待过程中被中断过，则调用selfInterrupt()来自己产生一个中断。