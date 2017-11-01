OkHttp是一个高效的Http客户端，有如下的特点：
1. 支持[HTTP2/SPDY](https://leohxj.gitbooks.io/a-programmer-prepares/networks/a-simple-performance-comparison-of-https-spdy-and-http2.html)黑科技
2. socket自动选择最好路线，并支持自动重连
3. 拥有自动维护的socket连接池，减少握手次数
4. 拥有队列线程池，轻松写并发
5. 拥有Interceptors轻松处理请求与响应（比如透明GZIP压缩,LOGGING）
6. 基于Headers的缓存策略

# 主要对象
* Connections: 对JDK中的物理socket进行了引用计数封装，用来控制socket连接
* Streams: 维护HTTP的流，用来对Requset/Response进行IO操作
* Calls: HTTP请求任务封装
* StreamAllocation: 用来控制Connections/Streams的资源分配与释放


**[使用说明](http://www.cnblogs.com/whoislcj/p/5526431.html)**
```
RealCall: okHttpClient.newCall(Request) 
->
1) excute();
2) enqueue();
```



```java
  @Override public Call newCall(Request request) { // OkHttpClient
    return RealCall.newRealCall(this, request, false /* for web socket */);
  }

  static RealCall newRealCall(OkHttpClient client, Request originalRequest, boolean forWebSocket) {
    // Safely publish the Call instance to the EventListener.
    RealCall call = new RealCall(client, originalRequest, forWebSocket);
    call.eventListener = client.eventListenerFactory().create(call);
    return call;
  }

  private RealCall(OkHttpClient client, Request originalRequest, boolean forWebSocket) {
    this.client = client;
    this.originalRequest = originalRequest;
    this.forWebSocket = forWebSocket;
    this.retryAndFollowUpInterceptor = new RetryAndFollowUpInterceptor(client, forWebSocket);
  }

  // 同步
  @Override public Response execute() throws IOException {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
      executed = true;
    }
    captureCallStackTrace();
    eventListener.callStart(this);
    try {
      client.dispatcher().executed(this);
      Response result = getResponseWithInterceptorChain();
      if (result == null) throw new IOException("Canceled");
      return result;
    } catch (IOException e) {
      eventListener.callFailed(this, e);
      throw e;
    } finally {
      client.dispatcher().finished(this);
    }
  }

  // 异步
  @Override public void enqueue(Callback responseCallback) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
      executed = true;
    }
    captureCallStackTrace();
    eventListener.callStart(this);
    client.dispatcher().enqueue(new AsyncCall(responseCallback));
  }
```

`Dispatcher.class`
```java
  synchronized void executed(RealCall call) {
    runningSyncCalls.add(call);
  }

  synchronized void enqueue(AsyncCall call) {
    if (runningAsyncCalls.size() < maxRequests && runningCallsForHost(call) < maxRequestsPerHost) {
      runningAsyncCalls.add(call);
      executorService().execute(call);
    } else {
      readyAsyncCalls.add(call);
    }
  }

    /** Used by {@code AsyncCall#run} to signal completion. */
  void finished(AsyncCall call) {
    finished(runningAsyncCalls, call, true);
  }

  /** Used by {@code Call#execute} to signal completion. */
  void finished(RealCall call) {
    finished(runningSyncCalls, call, false);
  }

  private <T> void finished(Deque<T> calls, T call, boolean promoteCalls) {
    int runningCallsCount;
    Runnable idleCallback;
    synchronized (this) {
      if (!calls.remove(call)) throw new AssertionError("Call wasn't in-flight!");
      if (promoteCalls) promoteCalls();
      runningCallsCount = runningCallsCount();
      idleCallback = this.idleCallback;
    }

    if (runningCallsCount == 0 && idleCallback != null) {
      idleCallback.run();
    }
  }

  private void promoteCalls() {
    if (runningAsyncCalls.size() >= maxRequests) return; // Already running max capacity.
    if (readyAsyncCalls.isEmpty()) return; // No ready calls to promote.

    for (Iterator<AsyncCall> i = readyAsyncCalls.iterator(); i.hasNext(); ) {
      AsyncCall call = i.next();

      if (runningCallsForHost(call) < maxRequestsPerHost) {
        i.remove();
        runningAsyncCalls.add(call);
        executorService().execute(call);
      }

      if (runningAsyncCalls.size() >= maxRequests) return; // Reached max capacity.
    }
  }
```


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

### RetryAndFollowUpInterceptor 
v3.9 RealInterceptorChain 添加了call字段
```java
  @Override public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    RealInterceptorChain realChain = (RealInterceptorChain) chain;
    Call call = realChain.call();
    EventListener eventListener = realChain.eventListener();

    streamAllocation = new StreamAllocation(client.connectionPool(), createAddress(request.url()),
        call, eventListener, callStackTrace);

    int followUpCount = 0;
    Response priorResponse = null;
    while (true) {
      if (canceled) {
        streamAllocation.release();
        throw new IOException("Canceled");
      }

      Response response = null;
      boolean releaseConnection = true;
      try {
        response = realChain.proceed(request, streamAllocation, null, null);
        releaseConnection = false;
      } catch (RouteException e) {
        // The attempt to connect via a route failed. The request will not have been sent.
        // 通过路线连接失败，请求将不会再发送
        if (!recover(e.getLastConnectException(), false, request)) {
          throw e.getLastConnectException();
        }
        releaseConnection = false;
        continue;
      } catch (IOException e) {
        // An attempt to communicate with a server failed. The request may have been sent.
        // 与服务器尝试通信失败，请求不会再发送。
        boolean requestSendStarted = !(e instanceof ConnectionShutdownException);
        if (!recover(e, requestSendStarted, request)) throw e;
        releaseConnection = false;
        continue;
      } finally {
        // We're throwing an unchecked exception. Release any resources.
        // 抛出未检查的异常，释放资源
        if (releaseConnection) {
          streamAllocation.streamFailed(null);
          streamAllocation.release();
        }
      }

      // Attach the prior response if it exists. Such responses never have a body.
      // 附加上先前存在的response。这样的response从来没有body
      if (priorResponse != null) {
        response = response.newBuilder()
            .priorResponse(priorResponse.newBuilder()
                    .body(null)
                    .build())
            .build();
      }

      Request followUp = followUpRequest(response);

      if (followUp == null) {
        if (!forWebSocket) {
          streamAllocation.release();
        }
        return response;
      }

      closeQuietly(response.body());

      if (++followUpCount > MAX_FOLLOW_UPS) {
        streamAllocation.release();
        throw new ProtocolException("Too many follow-up requests: " + followUpCount);
      }

      if (followUp.body() instanceof UnrepeatableRequestBody) {
        streamAllocation.release();
        throw new HttpRetryException("Cannot retry streamed HTTP body", response.code());
      }

      if (!sameConnection(response, followUp.url())) {
        streamAllocation.release();
        streamAllocation = new StreamAllocation(client.connectionPool(),
            createAddress(followUp.url()), call, eventListener, callStackTrace);
      } else if (streamAllocation.codec() != null) {
        throw new IllegalStateException("Closing the body of " + response
            + " didn't close its backing stream. Bad interceptor?");
      }

      request = followUp;
      priorResponse = response;
    }
  }
```
主要还是做了这几个操作，首先StreamAllocation对象在这里被创建，接着调用proceed()方法执行了一次请求，并拿到一个Response报文，在followUpRequest()方法中对Response报文进行了各种判断(验证了407，判断需不需要重定向等)确定是否需要再次请求，如果需要持续请求会在followUpRequest()返回一个新的Request对象并重新请求。followUpRequest()的代码有点长，可以自行查阅源码，这里就不贴了。继续看执行的下一个拦截器BridgeInterceptor。
```java
  @Override public Response intercept(Chain chain) throws IOException {
    Request userRequest = chain.request();
    Request.Builder requestBuilder = userRequest.newBuilder();

    RequestBody body = userRequest.body();
    if (body != null) {
      MediaType contentType = body.contentType();
      if (contentType != null) {
        requestBuilder.header("Content-Type", contentType.toString());
      }

      long contentLength = body.contentLength();
      if (contentLength != -1) {
        requestBuilder.header("Content-Length", Long.toString(contentLength));
        requestBuilder.removeHeader("Transfer-Encoding");
      } else {
        requestBuilder.header("Transfer-Encoding", "chunked");
        requestBuilder.removeHeader("Content-Length");
      }
    }

    if (userRequest.header("Host") == null) {
      requestBuilder.header("Host", hostHeader(userRequest.url(), false));
    }

    if (userRequest.header("Connection") == null) {
      requestBuilder.header("Connection", "Keep-Alive");
    }

    // If we add an "Accept-Encoding: gzip" header field we're responsible for also decompressing
    // the transfer stream.
    boolean transparentGzip = false;
    if (userRequest.header("Accept-Encoding") == null && userRequest.header("Range") == null) {
      transparentGzip = true;
      requestBuilder.header("Accept-Encoding", "gzip");
    }

    List<Cookie> cookies = cookieJar.loadForRequest(userRequest.url());
    if (!cookies.isEmpty()) {
      requestBuilder.header("Cookie", cookieHeader(cookies));
    }

    if (userRequest.header("User-Agent") == null) {
      requestBuilder.header("User-Agent", Version.userAgent());
    }

    Response networkResponse = chain.proceed(requestBuilder.build());

    HttpHeaders.receiveHeaders(cookieJar, userRequest.url(), networkResponse.headers());

    Response.Builder responseBuilder = networkResponse.newBuilder()
        .request(userRequest);

    if (transparentGzip
        && "gzip".equalsIgnoreCase(networkResponse.header("Content-Encoding"))
        && HttpHeaders.hasBody(networkResponse)) {
      GzipSource responseBody = new GzipSource(networkResponse.body().source());
      Headers strippedHeaders = networkResponse.headers().newBuilder()
          .removeAll("Content-Encoding")
          .removeAll("Content-Length")
          .build();
      responseBuilder.headers(strippedHeaders);
      responseBuilder.body(new RealResponseBody(strippedHeaders, Okio.buffer(responseBody)));
    }

    return responseBuilder.build();
  }
```
可以看出来BridgeInterceptor对Request和Response报文加工的具体步骤，默认对Request报文增加了gzip头信息，并在Response报文中对gzip进行解压缩处理。另外CookieJar类也是在这里处理的。接下来就是CacheInterceptor拦截器。
```java
  @Override public Response intercept(Chain chain) throws IOException {
    Response cacheCandidate = cache != null
        ? cache.get(chain.request())
        : null;

    long now = System.currentTimeMillis();

    CacheStrategy strategy = new CacheStrategy.Factory(now, chain.request(), cacheCandidate).get();
    Request networkRequest = strategy.networkRequest;
    Response cacheResponse = strategy.cacheResponse;

    if (cache != null) {
      cache.trackResponse(strategy);
    }

    if (cacheCandidate != null && cacheResponse == null) {
      closeQuietly(cacheCandidate.body()); // The cache candidate wasn't applicable. Close it.
    }

    // If we're forbidden from using the network and the cache is insufficient, fail.
    if (networkRequest == null && cacheResponse == null) {
      return new Response.Builder()
          .request(chain.request())
          .protocol(Protocol.HTTP_1_1)
          .code(504)
          .message("Unsatisfiable Request (only-if-cached)")
          .body(Util.EMPTY_RESPONSE)
          .sentRequestAtMillis(-1L)
          .receivedResponseAtMillis(System.currentTimeMillis())
          .build();
    }

    // If we don't need the network, we're done.
    if (networkRequest == null) {
      return cacheResponse.newBuilder()
          .cacheResponse(stripBody(cacheResponse))
          .build();
    }

    Response networkResponse = null;
    try {
      networkResponse = chain.proceed(networkRequest);
    } finally {
      // If we're crashing on I/O or otherwise, don't leak the cache body.
      if (networkResponse == null && cacheCandidate != null) {
        closeQuietly(cacheCandidate.body());
      }
    }

    // If we have a cache response too, then we're doing a conditional get.
    if (cacheResponse != null) {
      if (networkResponse.code() == HTTP_NOT_MODIFIED) {
        Response response = cacheResponse.newBuilder()
            .headers(combine(cacheResponse.headers(), networkResponse.headers()))
            .sentRequestAtMillis(networkResponse.sentRequestAtMillis())
            .receivedResponseAtMillis(networkResponse.receivedResponseAtMillis())
            .cacheResponse(stripBody(cacheResponse))
            .networkResponse(stripBody(networkResponse))
            .build();
        networkResponse.body().close();

        // Update the cache after combining headers but before stripping the
        // Content-Encoding header (as performed by initContentStream()).
        cache.trackConditionalCacheHit();
        cache.update(cacheResponse, response);
        return response;
      } else {
        closeQuietly(cacheResponse.body());
      }
    }

    Response response = networkResponse.newBuilder()
        .cacheResponse(stripBody(cacheResponse))
        .networkResponse(stripBody(networkResponse))
        .build();

    if (cache != null) {
      if (HttpHeaders.hasBody(response) && CacheStrategy.isCacheable(response, networkRequest)) {
        // Offer this request to the cache.
        CacheRequest cacheRequest = cache.put(response);
        return cacheWritingResponse(cacheRequest, response);
      }

      if (HttpMethod.invalidatesCache(networkRequest.method())) {
        try {
          cache.remove(networkRequest);
        } catch (IOException ignored) {
          // The cache cannot be written.
        }
      }
    }

    return response;
  }
```
首先从cache缓存中获取一个匹配的Response报文并赋给cacheCandidate变量，cache是一个InternalCache对象，里面持有DiskLruCache这个对象，以文件流的形式存储Response报文，采用LRU原则管理这些缓存；接着使用CacheStrategy.Factory工厂类生成一个缓存策略类CacheStrategy，通过该类拿到两个关键变量networkRequest和cacheResponse，这里针对cacheCandidate、networkRequest和cacheResponse这三个变量的赋值情况依次进行了以下处理：
1. cacheCandidate不为空，cacheResponse为空，说明缓存过期，将cacheCandidate从cache中清除；
2. networkRequest和cacheResponse同时为空，说明Request要求只使用缓存，而缓存并不存在或者已经失效，直接返回504的错误报文，请求结束；
3. networkRequest为空，说明cacheResponse不为空，命中缓存，直接返回cacheResponse报文；
4. 未命中缓存，开启网络请求，继续执行下一个Interceptor拦截器。

当缓存未命中时候OkHttp3就开始执行真正的网络请求，CacheInterceptor的下一个就是ConnectInterceptor拦截器。
```java
  @Override public Response intercept(Chain chain) throws IOException {
    RealInterceptorChain realChain = (RealInterceptorChain) chain;
    Request request = realChain.request();
    StreamAllocation streamAllocation = realChain.streamAllocation();

    // We need the network to satisfy this request. Possibly for validating a conditional GET.
    boolean doExtensiveHealthChecks = !request.method().equals("GET");
    HttpCodec httpCodec = streamAllocation.newStream(client, chain, doExtensiveHealthChecks);
    RealConnection connection = streamAllocation.connection();

    return realChain.proceed(request, streamAllocation, httpCodec, connection);
  }
```
ConnectInterceptor做的事情很简单，先获取了在RetryAndFollowUpInterceptor中创建的StreamAllocation对象，接着执行streamAllocation.newStream()打开一个物理连接并返回一个HttpStream的对象，HttpStream在前文提到了是网络协议流(HTTP/1.1、HTTP/2和SPDY)的具体实现。这时候调用realChain.proceed()方法的时候，四个参数均不为空，这是最终的CallServerInterceptor拦截器了。

在ConnectInterceptor的下一个拦截器并非绝对是CallServerInterceptor，如果有自定义NetInterceptors则会被优先执行，不过绝大部分情况下CallServerInterceptor在最后也是会被调用的。
```java
  @Override public Response intercept(Chain chain) throws IOException {
    RealInterceptorChain realChain = (RealInterceptorChain) chain;
    HttpCodec httpCodec = realChain.httpStream();
    StreamAllocation streamAllocation = realChain.streamAllocation();
    RealConnection connection = (RealConnection) realChain.connection();
    Request request = realChain.request();

    long sentRequestMillis = System.currentTimeMillis();
    httpCodec.writeRequestHeaders(request);

    Response.Builder responseBuilder = null;
    if (HttpMethod.permitsRequestBody(request.method()) && request.body() != null) {
      // If there's a "Expect: 100-continue" header on the request, wait for a "HTTP/1.1 100
      // Continue" response before transmitting the request body. If we don't get that, return what
      // we did get (such as a 4xx response) without ever transmitting the request body.
      if ("100-continue".equalsIgnoreCase(request.header("Expect"))) {
        httpCodec.flushRequest();
        responseBuilder = httpCodec.readResponseHeaders(true);
      }

      if (responseBuilder == null) {
        // Write the request body if the "Expect: 100-continue" expectation was met.
        Sink requestBodyOut = httpCodec.createRequestBody(request, request.body().contentLength());
        BufferedSink bufferedRequestBody = Okio.buffer(requestBodyOut);
        request.body().writeTo(bufferedRequestBody);
        bufferedRequestBody.close();
      } else if (!connection.isMultiplexed()) {
        // If the "Expect: 100-continue" expectation wasn't met, prevent the HTTP/1 connection from
        // being reused. Otherwise we're still obligated to transmit the request body to leave the
        // connection in a consistent state.
        streamAllocation.noNewStreams();
      }
    }

    httpCodec.finishRequest();

    if (responseBuilder == null) {
      responseBuilder = httpCodec.readResponseHeaders(false);
    }

    Response response = responseBuilder
        .request(request)
        .handshake(streamAllocation.connection().handshake())
        .sentRequestAtMillis(sentRequestMillis)
        .receivedResponseAtMillis(System.currentTimeMillis())
        .build();

    int code = response.code();
    if (forWebSocket && code == 101) {
      // Connection is upgrading, but we need to ensure interceptors see a non-null response body.
      response = response.newBuilder()
          .body(Util.EMPTY_RESPONSE)
          .build();
    } else {
      response = response.newBuilder()
          .body(httpCodec.openResponseBody(response))
          .build();
    }

    if ("close".equalsIgnoreCase(response.request().header("Connection"))
        || "close".equalsIgnoreCase(response.header("Connection"))) {
      streamAllocation.noNewStreams();
    }

    if ((code == 204 || code == 205) && response.body().contentLength() > 0) {
      throw new ProtocolException(
          "HTTP " + code + " had non-zero Content-Length: " + response.body().contentLength());
    }

    return response;
  }
```
CallServerInterceptor拦截器里先调用httpStream协议流对象写入Request的Header部分，接着写入Body部分，这样就完成了Request的请求，从httpStream里回读Response报文，并根据情况读取Response的Body部分，当Response响应报文的头信息中Connection字段为close时，将streamAllocation设置成noNewStreams状态，标识其当前Connection对象不再被复用，将在流请求结束之后被回收掉。


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

# StreamAllocation
StreamAllocation 是在RetryAndFollowUpInterceptor这个拦截器中创建的，并以此通过责任链传递给下一个拦截器
而它的使用则是在ConnectionInterceptor拦截器中去与服务器建立连接。

更详细的说：建立TCP连接，处理SSL/TLS握手，完成HTTP2协商等过程在 ConnectInterceptor 中完成，具体是在StreamAllocation.newStream()。
而向网络写数据，是在CallServerInterceptor中。


* Address：描述某一个特定的服务器地址。
* Route：表示连接的线路
* ConnectionPool：连接池，所有连接的请求都保存在这里，内部由线程池维护
* RouteSelector：线路选择器，用来选择线路和自动重连
* RealConnection：用来连接到Socket链路
* HttpStream：则是Http流，它是一个接口，实现类是Http1xStream、Http2xStream。分别对应HTTP/1.1、HTTP/2和SPDY协议

# OkHttp的文件系统
OkHttp中的关键对象如下：
* FileSystem: 使用Okio对File的封装，简化了IO操作
* DiskLruCache.Editor: 添加了同步锁，并对FileSystem进行高度封装
* DiskLruCache.Entry: 维护着key对应的多个文件
* Cache.Entry: Responsejava对象与Okio流的序列化/反序列化类
* DiskLruCache: 维护着文件的创建，清理，读取。内部有清理线程池，LinkedHashMap(也就是LruCache)
* Cache: 被上级代码调用，提供透明的put/get操作，封装了缓存检查条件与DiskLruCache，开发者只用配置大小即可，不需要手动管理
* Response/Requset: OkHttp的请求与回应
