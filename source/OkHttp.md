# Dispatcher
* maxRequests = 64: 最大并发请求数为64
* maxRequestsPerHost = 5: 每个主机最大请求数为5
* Dispatcher: 分发者，也就是生产者（默认在主线程）
* AsyncCall: 队列中需要处理的Runnable（包装了异步回调接口）
* ExecutorService：消费者池（也就是线程池）
* Deque<readyAsyncCalls>：缓存（用数组实现，可自动扩容，无大小限制）
* Deque<runningAsyncCalls>：正在运行的任务，仅仅是用来引用正在运行的任务以判断并发量，注意它并不是消费者缓存

`Dispatcher.class`
在Okhttp中，构建了一个阀值为[0, Integer.MAX_VALUE]的线程池，它不保留任何最小线程数，随时创建更多的线程数，当线程空闲时只能活60秒，它使用了一个不存储元素的阻塞工作队列，一个叫做"OkHttp Dispatcher"的线程工厂。

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
        //执行耗时IO任务
        Response response = getResponseWithInterceptorChain();
        if (retryAndFollowUpInterceptor.isCanceled()) {
          signalledCallback = true;
          //回调，注意这里回调是在线程池中，而不是想当然的主线程回调
          responseCallback.onFailure(RealCall.this, new IOException("Canceled"));
        } else {
          signalledCallback = true;
          //回调，同上
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
        //最关键的代码
        client.dispatcher().finished(this);
      }
    }
```
当任务执行完成后，无论是否有异常，finally代码段总会被执行，也就是会调用Dispatcher的finished函数，打开源码，发现它将正在运行的任务Call从队列runningAsyncCalls中移除后，接着执行promoteCalls()函数

```java
private void promoteCalls() { 
  //如果目前是最大负荷运转，接着等 
  if (runningAsyncCalls.size() >= maxRequests) 
    return; // Already running max capacity. 
    //如果缓存等待区是空的，接着等 
  if (readyAsyncCalls.isEmpty()) 
    return; // No ready calls to promote. 
  for (Iterator<AsyncCall> i = readyAsyncCalls.iterator(); i.hasNext(); ) { 
    AsyncCall call = i.next(); 
    if (runningCallsForHost(call) < maxRequestsPerHost) { //将缓存等待区最后一个移动到运行区中，并执行 
      i.remove(); 
      runningAsyncCalls.add(call); 
      executorService().execute(call); 
    } 
    
    if (runningAsyncCalls.size() >= maxRequests) return; // Reached max capacity. 
  }
}
```

## Interceptor(拦截器)
OkHttp3的Interceptor是Request -> Response请求过程中的一个"节点"单位，通过一连串有序的Interceptor拦截器"节点"组成一条加工链，加工链中的任意一个"节点"都可以去拦截加工Request和Response。OkHttp默认提供了一套完善的Interceptor集合，当然也支持自定义一个Interceptor来实现一个上传/下载的进度更新器或者黑白名单拦截等等个性化的功能。

OkHttp默认提供了如下Interceptor：
* RetryAndFollowUpInterceptor：默认情况下位于OkHttp3加工链的首位，顾名思义，具有失败-重试机制，支持页面重定向和一些407之类的代理验证等，此外负责StreamAllocation对象的创建;
* BridgeInterceptor：桥拦截器，配置Request的Headers头信息：读取Cookie，默认启用Gzip，默认加入Keep-Alive长连接，如果不想让OkHttp3擅自使用长连接，只需在Request的Header中预设Connection字段即可;
* CacheInterceptor: 管理OkHttp3的缓存，目前仅支持GET类型的缓存，使用文件形式的Lru缓存管理策略，CacheStrategy类负责了缓存相关的策略管理;
* ConnectInterceptor：OkHttp3打开一个Socket连接的地方，OkHttp3相关的Router路由切换策略也可以从这里开始跟踪;
* CallServerInterceptor：处于OkHttp3加工链的末尾，通过HttpStream往Socket中写入Request报文信息，并回读Response报文信息.

OkHttp3的拦截器执行顺序依次是：自定义Interceptors(暂且称作A) -> RetryAndFollowUpInterceptor -> BridgeInterceptor -> CacheInterceptor -> ConnectInterceptor -> 自定义NetInterceptors(暂且称作B) -> CallServerInterceptor

1. B仅在非WebSocket情况下被调用。
2. A与B的区别是，A能拦截所有类型的请求，包括缓存命中的请求；而B仅拦截非WebSocket的情况下产生真正网络访问的请求。因此在B上做网络上传和下载进度的监听器是比较合适的。

```java
  //拦截器的责任链
  Response getResponseWithInterceptorChain() throws IOException {
    // Build a full stack of interceptors.
    List<Interceptor> interceptors = new ArrayList<>();
    interceptors.addAll(client.interceptors());
    interceptors.add(retryAndFollowUpInterceptor);
    interceptors.add(new BridgeInterceptor(client.cookieJar()));
    interceptors.add(new CacheInterceptor(client.internalCache()));
    interceptors.add(new ConnectInterceptor(client));
    if (!forWebSocket) {
      interceptors.addAll(client.networkInterceptors());
    }
    interceptors.add(new CallServerInterceptor(forWebSocket));

    Interceptor.Chain chain = new RealInterceptorChain(interceptors, null, null, null, 0,
        originalRequest, this, eventListener, client.connectTimeoutMillis(),
        client.readTimeoutMillis(), client.writeTimeoutMillis());

    return chain.proceed(originalRequest);
  }
```

```java
  public Response proceed(Request request, StreamAllocation streamAllocation, HttpCodec httpCodec,
      RealConnection connection) throws IOException {
    if (index >= interceptors.size()) throw new AssertionError();

    calls++;

    // If we already have a stream, confirm that the incoming request will use it.
    // 如果我们已经有一个stream。确定即将到来的request会使用它
    if (this.httpCodec != null && !this.connection.supportsUrl(request.url())) {
      throw new IllegalStateException("network interceptor " + interceptors.get(index - 1)
          + " must retain the same host and port");
    }

    // If we already have a stream, confirm that this is the only call to chain.proceed().
    // 如果我们已经有一个stream， 确定chain.proceed()唯一的call
    if (this.httpCodec != null && calls > 1) {
      throw new IllegalStateException("network interceptor " + interceptors.get(index - 1)
          + " must call proceed() exactly once");
    }

    // Call the next interceptor in the chain.
    // 调用链的下一个拦截器
    RealInterceptorChain next = new RealInterceptorChain(interceptors, streamAllocation, httpCodec,
        connection, index + 1, request, call, eventListener, connectTimeout, readTimeout,
        writeTimeout);
    Interceptor interceptor = interceptors.get(index);
    Response response = interceptor.intercept(next);

    // Confirm that the next interceptor made its required call to chain.proceed().
    if (httpCodec != null && index + 1 < interceptors.size() && next.calls != 1) {
      throw new IllegalStateException("network interceptor " + interceptor
          + " must call proceed() exactly once");
    }

    // Confirm that the intercepted response isn't null.
    if (response == null) {
      throw new NullPointerException("interceptor " + interceptor + " returned null");
    }

    if (response.body() == null) {
      throw new IllegalStateException(
          "interceptor " + interceptor + " returned a response with no body");
    }

    return response;
  }
```

## Route.java 地址封装类
OkHttp3要求每个连接都需要指明一个Router路由对象，当然这个Router路由对象可以是直连类型的，意即你不使用任何的代理服务器。当尝试使用某个路由请求失败的时候，OkHttp3会在允许请求重试的情况下通过RouterSelector切换到下个路由继续请求，并将失败的路由记录到黑名单中，这样在OkHttp3重复请求一个目标地址的时候能够优先选择成功的路由进行网络请求。

* Router：包含代理与Socket地址信息。
* RouteDatabase：记录请求失败的Router路由对象的"黑名单"。
* RouteSelector：负责指派Router路由。持有RouteDatabase对象。



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
 

 # Socket管理(StreamAllocation)
 ```java
   public Route next() throws IOException {
    // Compute the next route to attempt.
    if (!hasNextInetSocketAddress()) {
      if (!hasNextProxy()) {
        if (!hasNextPostponed()) {
          throw new NoSuchElementException();
        }
        return nextPostponed();
      }
      lastProxy = nextProxy();
    }
    lastInetSocketAddress = nextInetSocketAddress();

    Route route = new Route(address, lastProxy, lastInetSocketAddress);
    if (routeDatabase.shouldPostpone(route)) {
      postponedRoutes.add(route);
      // We will only recurse in order to skip previously failed routes. They will be tried last.
      return next();
    }

    return route;
  }
 ```

 ## Stream(流相关类)
 OkHttp3并没有直接操作Socket，而是通过okio库进行了封装，okio库的设计也是非常赞的，它的Sink对应输入流，Source对应输出流，okio已经实现了与之对应的缓冲相关的包装类，采用了Segment切片和循环链表结构实现缓冲处理，有兴趣还是可以看看okio的源码。

 * StreamAllocation：OkHttp3管理物理连接的对象，负责连接流的创建关闭等管理工作，通过池化物理连接来减少Hand-shake握手过程以提升请求效率，另外StreamAllocation通过持有RouteSelector对象切换路由。关于路由切换，这里有个场景，在Android系统中，如果你配置了代理，当代理服务器访问超时的时候，OkHttp3在进行请求重试时候会切换到下个代理或者采用无代理直连形式请求。因此并非设置了代理，OkHttp3就会"老实"的跟着你的规则走。这也是本文一开始提到的问题产生的原因。

* HttpStream： 这是一个抽象类，其子类实现了各类网络协议流格式。 HttpStream在OkHttp3中有两个实现类Http1xStream和Http2xStream，Http1xStream实现了HTTP/1.1协议流，Http2xStream则实现了HTTP/2和SPDY协议流。

* ConnectionPool：OkHttp连接池，由OkHttpClient持有该对象，ConnectionPool持有一个0核心线程数的线程池(与Executors.newCachedThreadPool()提供的线程池行为完全一样)用于清理一些超时的RealConnection连接对象，持有一个Deque对象缓存OkHttp3的RealConnection连接对象。
