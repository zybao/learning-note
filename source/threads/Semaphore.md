信号量(Semaphore)，又被称为信号灯，在多线程环境下用于协调各个线程, 以保证它们能够正确、合理的使用公共资源。
``` java
ExecutorService exec = Executors.newCachedThreadPool();  
final Semaphore semp = new Semaphore(5);
for (int index = 0; index < 20; index++) {
    final int NO = index;  
    Runnable run = new Runnable() {  
        public void run() {  
           try {  
                //使用acquire()获取锁 
                semp.acquire();  
                System.out.println("Accessing: " + NO);  
                //睡眠1秒
                Thread.sleep(1000);  
            } catch (InterruptedException e) {  

            }  finally {
                //使用完成释放锁 
                semp.release();
            }
        }  
    };  
    exec.execute(run);  
}  
// 退出线程池 
exec.shutdown();  
```