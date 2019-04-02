OkHttp使用：
新建一个OkHttpClient
```java
OkHttpClient: new OkHttpClient.Builder()
                .readTimeout(...)
                .connectionTimeOut(...)
                .build();
```
发送请求：
```java
 Call: okHttpClient.newCall(Request)
```
实际上发送请求是调用RealCall中的方法, 如源码:
```java
  @Override public Call newCall(Request request) {
    return RealCall.newRealCall(this, request, false /* for web socket */);
  }
```

同步请求调用Call的execute方法，异步请求调用enqueue方法，参数是CallBack.
如下所示：
```java
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

  private void captureCallStackTrace() {
    Object callStackTrace = Platform.get().getStackTraceForCloseable("response.body().close()");
    retryAndFollowUpInterceptor.setCallStackTrace(callStackTrace);
  }

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

1. 我们先看同步请求:
这里`client.dispatcher().executed(this)`调用了Dispatcher中的方法，主要是将请求放入到同步请求集合中：
```java
  synchronized void executed(RealCall call) {
    runningSyncCalls.add(call);
  }
```
接下来调用下面的方法：
```java
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

这个过程主要是进行OkHttp的拦截器操作。OkHttp3的拦截器是Request -> Response请求过程中的一个"节点"单位，通过一连串有序的Interceptor拦截器"节点"组成一条加工链，加工链中的任意一个"节点"都可以去拦截加工Request和Response。OkHttp默认提供了一套完善的Interceptor集合，当然也支持自定义一个Interceptor来实现一个上传/下载的进度更新器或者黑白名单拦截等等个性化的功能。

OkHttp默认提供了如下Interceptor：

    RetryAndFollowUpInterceptor：默认情况下位于OkHttp3加工链的首位，顾名思义，具有失败-重试机制，支持页面重定向和一些407之类的代理验证等，此外负责StreamAllocation对象的创建;
    BridgeInterceptor：桥拦截器，配置Request的Headers头信息：读取Cookie，默认启用Gzip，默认加入Keep-Alive长连接，如果不想让OkHttp3擅自使用长连接，只需在Request的Header中预设Connection字段即可;
    CacheInterceptor: 管理OkHttp3的缓存，目前仅支持GET类型的缓存，使用文件形式的Lru缓存管理策略，CacheStrategy类负责了缓存相关的策略管理;
    ConnectInterceptor：OkHttp3打开一个Socket连接的地方，OkHttp3相关的Router路由切换策略也可以从这里开始跟踪;
    CallServerInterceptor：处于OkHttp3加工链的末尾，通过HttpStream往Socket中写入Request报文信息，并回读Response报文信息.

OkHttp3的拦截器执行顺序依次是：自定义Interceptors(暂且称作A) -> RetryAndFollowUpInterceptor -> BridgeInterceptor -> CacheInterceptor -> ConnectInterceptor -> 自定义NetInterceptors(暂且称作B) -> CallServerInterceptor

https://blog.csdn.net/hello2mao/article/details/53201974

https://www.jianshu.com/p/f7972c30fc52

