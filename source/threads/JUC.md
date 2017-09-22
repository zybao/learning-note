# 原子类
根据修改的数据类型，可以将JUC包中的原子操作类可以分为4类。

1. 基本类型: AtomicInteger, AtomicLong, AtomicBoolean;
2. 数组类型: AtomicIntegerArray, AtomicLongArray, AtomicReferenceArray;
3. 引用类型: AtomicReference, AtomicStampedRerence, AtomicMarkableReference;
4. 对象的属性修改类型: AtomicIntegerFieldUpdater, AtomicLongFieldUpdater, AtomicReferenceFieldUpdater 。

这些类存在的目的是对相应的数据进行原子操作。所谓原子操作，是指操作过程不会被中断，保证数据操作是以原子方式进行的。

## AtomicLong
AtomicLong是作用是对长整形进行原子操作。
在32位操作系统中，64位的long 和 double 变量由于会被JVM当作两个分离的32位来进行操作，所以不具有原子性。而使用AtomicLong能让long的操作保持原子型。

### AtomicLong API
```java
// 构造函数
AtomicLong()
// 创建值为initialValue的AtomicLong对象
AtomicLong(long initialValue)
// 以原子方式设置当前值为newValue。
final void set(long newValue) 
// 获取当前值
final long get() 
// 以原子方式将当前值减 1，并返回减1后的值。等价于“--num”
final long decrementAndGet() 
// 以原子方式将当前值减 1，并返回减1前的值。等价于“num--”
final long getAndDecrement() 
// 以原子方式将当前值加 1，并返回加1后的值。等价于“++num”
final long incrementAndGet() 
// 以原子方式将当前值加 1，并返回加1前的值。等价于“num++”
final long getAndIncrement()    
// 以原子方式将delta与当前值相加，并返回相加后的值。
final long addAndGet(long delta) 
// 以原子方式将delta添加到当前值，并返回相加前的值。
final long getAndAdd(long delta) 
// 如果当前值 == expect，则以原子方式将该值设置为update。成功返回true，否则返回false，并且不修改原值。
final boolean compareAndSet(long expect, long update)
// 以原子方式设置当前值为newValue，并返回旧值。
final long getAndSet(long newValue)
// 返回当前值对应的int值
int intValue() 
// 获取当前值对应的long值
long longValue()    
// 以 float 形式返回当前值
float floatValue()    
// 以 double 形式返回当前值
double doubleValue()    
// 最后设置为给定值。延时设置变量值，这个等价于set()方法，但是由于字段是volatile类型的，因此次字段的修改会比普通字段（非volatile字段）有稍微的性能延时（尽管可以忽略），所以如果不是想立即读取设置的新值，允许在“后台”修改值，那么此方法就很有用。如果还是难以理解，这里就类似于启动一个后台线程如执行修改新值的任务，原线程就不等待修改结果立即返回（这种解释其实是不正确的，但是可以这么理解）。
final void lazySet(long newValue)
// 如果当前值 == 预期值，则以原子方式将该设置为给定的更新值。JSR规范中说：以原子方式读取和有条件地写入变量但不 创建任何 happen-before 排序，因此不提供与除 weakCompareAndSet 目标外任何变量以前或后续读取或写入操作有关的任何保证。大意就是说调用weakCompareAndSet时并不能保证不存在happen-before的发生（也就是可能存在指令重排序导致此操作失败）。但是从Java源码来看，其实此方法并没有实现JSR规范的要求，最后效果和compareAndSet是等效的，都调用了unsafe.compareAndSwapInt()完成操作。
final boolean weakCompareAndSet(long expect, long update)
```

## AtomicLongArray
AtomicLongArray的作用则是对"长整形数组"进行原子操作。
```java
// 创建给定长度的新 AtomicLongArray。
AtomicLongArray(int length)
// 创建与给定数组具有相同长度的新 AtomicLongArray，并从给定数组复制其所有元素。
AtomicLongArray(long[] array)

// 以原子方式将给定值添加到索引 i 的元素。
long addAndGet(int i, long delta)
// 如果当前值 == 预期值，则以原子方式将该值设置为给定的更新值。
boolean compareAndSet(int i, long expect, long update)
// 以原子方式将索引 i 的元素减1。
long decrementAndGet(int i)
// 获取位置 i 的当前值。
long get(int i)
// 以原子方式将给定值与索引 i 的元素相加。
long getAndAdd(int i, long delta)
// 以原子方式将索引 i 的元素减 1。
long getAndDecrement(int i)
// 以原子方式将索引 i 的元素加 1。
long getAndIncrement(int i)
// 以原子方式将位置 i 的元素设置为给定值，并返回旧值。
long getAndSet(int i, long newValue)
// 以原子方式将索引 i 的元素加1。
long incrementAndGet(int i)
// 最终将位置 i 的元素设置为给定值。
void lazySet(int i, long newValue)
// 返回该数组的长度。
int length()
// 将位置 i 的元素设置为给定值。
void set(int i, long newValue)
// 返回数组当前值的字符串表示形式。
String toString()
// 如果当前值 == 预期值，则以原子方式将该值设置为给定的更新值。
boolean    weakCompareAndSet(int i, long expect, long update)
```

## AtomicReference
AtomicReference是作用是对"对象"进行原子操作。
```java
// 使用 null 初始值创建新的 AtomicReference。
AtomicReference()
// 使用给定的初始值创建新的 AtomicReference。
AtomicReference(V initialValue)
// 如果当前值 == 预期值，则以原子方式将该值设置为给定的更新值。
boolean compareAndSet(V expect, V update)
// 获取当前值。
V get()
// 以原子方式设置为给定值，并返回旧值。
V getAndSet(V newValue)
// 最终设置为给定值。
void lazySet(V newValue)
// 设置为给定值。
void set(V newValue)
// 返回当前值的字符串表示形式。
String toString()
// 如果当前值 == 预期值，则以原子方式将该值设置为给定的更新值。
boolean weakCompareAndSet(V expect, V update)
```

## AtomicLongFieldUpdater
AtomicLongFieldUpdater可以对指定"类的 'volatile long'类型的成员"进行原子更新。它是基于反射原理实现的。
```java
// 受保护的无操作构造方法，供子类使用。
protected AtomicLongFieldUpdater()
// 以原子方式将给定值添加到此更新器管理的给定对象的字段的当前值。
long addAndGet(T obj, long delta)
// 如果当前值 == 预期值，则以原子方式将此更新器所管理的给定对象的字段设置为给定的更新值。
abstract boolean compareAndSet(T obj, long expect, long update)
// 以原子方式将此更新器管理的给定对象字段当前值减 1。
long decrementAndGet(T obj)
// 获取此更新器管理的在给定对象的字段中保持的当前值。
abstract long get(T obj)
// 以原子方式将给定值添加到此更新器管理的给定对象的字段的当前值。
long getAndAdd(T obj, long delta)
// 以原子方式将此更新器管理的给定对象字段当前值减 1。
long getAndDecrement(T obj)
// 以原子方式将此更新器管理的给定对象字段的当前值加 1。
long getAndIncrement(T obj)
// 将此更新器管理的给定对象的字段以原子方式设置为给定值，并返回旧值。
long getAndSet(T obj, long newValue)
// 以原子方式将此更新器管理的给定对象字段当前值加 1。
long incrementAndGet(T obj)
// 最后将此更新器管理的给定对象的字段设置为给定更新值。
abstract void lazySet(T obj, long newValue)
// 为对象创建并返回一个具有给定字段的更新器。
static <U> AtomicLongFieldUpdater<U> newUpdater(Class<U> tclass, String fieldName)
// 将此更新器管理的给定对象的字段设置为给定更新值。
abstract void set(T obj, long newValue)
// 如果当前值 == 预期值，则以原子方式将此更新器所管理的给定对象的字段设置为给定的更新值。
abstract boolean weakCompareAndSet(T obj, long expect, long update)
```

# [锁](http://www.cnblogs.com/skywang12345/p/3496098.html)
根据锁的添加到Java中的时间，Java中的锁，可以分为"同步锁"和"JUC包中的锁"。

* 同步锁
即通过synchronized关键字来进行同步，实现对竞争资源的互斥访问的锁。Java 1.0版本中就已经支持同步锁了。
同步锁的原理是，对于每一个对象，有且仅有一个同步锁；不同的线程能共同访问该同步锁。但是，在同一个时间点，该同步锁能且只能被一个线程获取到。这样，获取到同步锁的线程就能进行CPU调度，从而在CPU上执行；而没有获取到同步锁的线程，必须进行等待，直到获取到同步锁之后才能继续运行。这就是，多线程通过同步锁进行同步的原理！

* JUC中的锁
相比同步锁，JUC包中的锁的功能更加强大，它为锁提供了一个框架，该框架允许更灵活地使用锁，只是它的用法更难罢了。

JUC包中的锁，包括：Lock接口，ReadWriteLock接口，LockSupport阻塞原语，Condition条件，AbstractOwnableSynchronizer/AbstractQueuedSynchronizer/AbstractQueuedLongSynchronizer三个抽象类，ReentrantLock独占锁，ReentrantReadWriteLock读写锁。由于CountDownLatch，CyclicBarrier和Semaphore也是通过AQS来实现的；因此，我也将它们归纳到锁的框架中进行介绍。

