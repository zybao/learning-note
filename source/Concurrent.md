# 并发需要注意的问题(http://blog.csdn.net/u010425776/article/details/54233279)

并发是为了提升程序的执行速度，但并不是多线程一定比单线程高效，而且并发编程容易出错。若要实现正确且高效的并发，就要在开发过程中时刻注意以下三个问题：

    * 上下文切换
    * 死锁
    * 资源限制

接下来会逐一分析这三个问题，并给出相应的解决方案


# 闭锁、同步屏障、信号量

## 闭锁CountDownLatch

若有多条线程，其中一条线程需要等到其他所有线程准备完所需的资源后才能运行，这样的情况可以使用闭锁。

CountDownLatch latch = new CountDownLatch(2);

new Thread(new Runnable(){
    public void run() {
        // 加载

        // 加载完成，闭锁
        latch.countDown();
    }
}).start();

new Thread(new Runnable(){
    public void run() {
        // 加载

        // 加载完成，闭锁
        latch.countDown();
    }
}).start();

new Thread(new Runnable(){
    public void run() {
        // 资源全部加载完成后执行
        latch.await();
    }
}).start();

## 同步屏障 CyclicBarrier

若有多条线程，他们到达屏障时将会被阻塞，只有当所有线程都到达屏障时才能打开屏障，所有线程同时执行，若有这样的需求可以使用同步屏障。此外，当屏障打开的同时还能指定执行的任务。

闭锁与同步屏障的区别

* 闭锁只会阻塞一条线程，目的是为了让该条任务线程满足条件后执行；
* 而同步屏障会阻塞所有线程，目的是为了让所有线程同时执行（实际上并不会同时执行，而是尽量把线程启动的时间间隔降为最少）。

CyclicBarrier barrier = new CyclicBarrier(3, new Runnable(){
    public void run() {
        // 所有线程准备完毕后触发此任务
    }
});

for (int i = 0; i < 3; i++) {
    new Thread(new Runnable(){
        public void run() {
            barrier.await();
        }
    }).start();
}

## 信号量 Semaphore

Semaphore s = new Semaphore(3);

for (int i = 0; i < 10; i++) {
    new Thread(new Runnable() {
        public void run() {
            s.acquire();
            // coding ...

            s.release();
        }
    })
}

# 线程的中断

什么是中断？

在Java中没有办法立即停止一条线程，然而停止线程却显得尤为重要，如取消一个耗时操作。因此，Java提供了一种用于停止线程的机制——中断。

* 中断只是一种协作机制，Java没有给中断增加任何语法，中断的过程完全需要程序员自己实现。若要中断一个线程，你需要手动调用该线程的interrupted方法，该方法也仅仅是将线程对象的中断标识设成true；接着你需要自己写代码不断地检测当前线程的标识位；如果为true，表示别的线程要求这条线程中断，此时究竟该做什么需要你自己写代码实现。
* 每个线程对象中都有一个标识，用于表示线程是否被中断；该标识位为true表示中断，为false表示未中断；
* 通过调用线程对象的interrupt方法将该线程的标识位设为true；可以在别的线程中调用，也可以在自己的线程中调用。

中断的相关API

* interrupt() 将调用者线程的中断状态设置为true
* isInterrupted()
* interrupted() 

