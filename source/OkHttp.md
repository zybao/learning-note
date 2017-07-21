# Dispatcher
* maxRequests = 64: 最大并发请求数为64
* maxRequestsPerHost = 5: 每个主机最大请求数为5
* Dispatcher: 分发者，也就是生产者（默认在主线程）
* AsyncCall: 队列中需要处理的Runnable（包装了异步回调接口）
* ExecutorService：消费者池（也就是线程池）
* Deque<readyAsyncCalls>：缓存（用数组实现，可自动扩容，无大小限制）
* Deque<runningAsyncCalls>：正在运行的任务，仅仅是用来引用正在运行的任务以判断并发量，注意它并不是消费者缓存

Dispatcher.class
在Okhttp中，构建了一个阀值为[0, Integer.MAX_VALUE]的线程池，它不保留任何最小线程数，随时创建更多的线程数，当线程空闲时只能活60秒，它使用了一个不存储元素的阻塞工作队列，一个叫做"OkHttp Dispatcher"的线程工厂。
```java
  public synchronized ExecutorService executorService() {
    if (executorService == null) {
      executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
          new SynchronousQueue<Runnable>(), Util.threadFactory("OkHttp Dispatcher", false));
    }
    return executorService;
  }
```

```java
    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters and default rejected execution handler.
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * 最小并发线程数，这里并发同时包括空闲与活动的线程，如果是0的话，空闲一段时间后所有线程将全部被销毁。
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     * 最大线程数，当任务进来时可以扩充的线程最大值，当大于了这个值就会根据丢弃处理机制来处理
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     * 当线程数大于corePoolSize时，多余的空闲线程的最大存活时间，类似于HTTP中的Keep-alive
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     * 工作队列，先进先出，可以看出并不像Picasso那样设置优先队列。
     * @param threadFactory the factory to use when the executor
     *        creates a new thread
     * 单个线程的工厂，可以打Log，设置Daemon(即当JVM退出时，线程自动结束)等
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue}
     *         or {@code threadFactory} is null
     */
    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                              TimeUnit unit, BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             threadFactory, defaultHandler);
    }
```

```java
synchronized void enqueue(AsyncCall call) {
    if (runningAsyncCalls.size() < maxRequests && runningCallsForHost(call) < maxRequestsPerHost) {
      runningAsyncCalls.add(call);
      executorService().execute(call);
    } else {
      readyAsyncCalls.add(call);
    }
}

public synchronized ExecutorService executorService() {
    if (executorService == null) {
        executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
          new SynchronousQueue<Runnable>(), Util.threadFactory("OkHttp Dispatcher", false));
    }
    return executorService;
}

 @Override 
protected void execute() {
    boolean signalledCallback = false;
    try {
        Response response = getResponseWithInterceptorChain();
        if (retryAndFollowUpInterceptor.isCanceled()) {
          signalledCallback = true;
          responseCallback.onFailure(RealCall.this, new IOException("Canceled"));
        } else {
          signalledCallback = true;
          responseCallback.onResponse(RealCall.this, response);
        }
    } catch (IOException e) {
        if (signalledCallback) {
          // Do not signal the callback twice!
          Platform.get().log(INFO, "Callback failure for " + toLoggableString(), e);
        } else {
          responseCallback.onFailure(RealCall.this, e);
        }
    } finally {
        client.dispatcher().finished(this);
      }
    }
```



## Route.java 地址封装类

  The concrete route used by a connection to reach an abstract origin server. When creating a
  connection the client has many options:
 
  <ul>
      <li><strong>HTTP proxy:</strong> a proxy server may be explicitly configured for the client.
          Otherwise the {@linkplain java.net.ProxySelector proxy selector} is used. It may return
          multiple proxies to attempt.
      <li><strong>IP address:</strong> whether connecting directly to an origin server or a proxy,
          opening a socket requires an IP address. The DNS server may return multiple IP addresses
          to attempt.
  </ul>
 
  <p>Each route is a specific selection of these options.

## Platform.java

  Access to platform-specific features.
 
  <h3>Server name indication (SNI)</h3>
 
  <p>Supported on Android 2.3+.
 
  Supported on OpenJDK 7+
 
  <h3>Session Tickets</h3>
 
  <p>Supported on Android 2.3+.
 
  <h3>Android Traffic Stats (Socket Tagging)</h3>
 
  <p>Supported on Android 4.0+.
 
  <h3>ALPN (Application Layer Protocol Negotiation)</h3>
 
  <p>Supported on Android 5.0+. The APIs were present in Android 4.4, but that implementation was
  unstable.
 
  Supported on OpenJDK 7 and 8 (via the JettyALPN-boot library).
 
  Supported on OpenJDK 9 via SSLParameters and SSLSocket features.
 
  <h3>Trust Manager Extraction</h3>
 
  <p>Supported on Android 2.3+ and OpenJDK 7+. There are no public APIs to recover the trust
  manager that was used to create an {@link SSLSocketFactory}.
 
  <h3>Android Cleartext Permit Detection</h3>
 
  <p>Supported on Android 6.0+ via {@code NetworkSecurityPolicy}.
 