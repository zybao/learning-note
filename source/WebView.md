```java
WebView mWebview= (WebView)findViewById(R.id.wv1);
WebSettings wSet = wView.getSettings();   
wSet.setJavaScriptEnabled(true);
mWebview.loadUrl("http://www.google.com");
```

* loadUrl, loadData, loadDataWithBase 的区别

打开网页时不调用系统浏览器， 而是在本WebView中显示：
```java
mWebView.setWebViewClient(new WebViewClient(){
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
          view.loadUrl(url);
         return true;
      }
  });
```

* WebChromeClient和WebChromeClient区别

WebViewClient主要帮助WebView处理各种通知、请求事件的，比如：

    onLoadResource
    onPageStart
    onPageFinish
    onReceiveError
    onReceivedHttpAuthRequest

WebChromeClient主要辅助WebView处理JavaScript的对话框、网站图标、网站title、加载进度等比如

    onCloseWindow(关闭WebView)
    onCreateWindow()
    onJsAlert (WebView上alert无效，需要定制WebChromeClient处理弹出)
    onJsPrompt
    onJsConfirm
    onProgressChanged
    onReceivedIcononReceivedTitle


看上去他们有很多不同，实际使用的话，如果你的WebView只是用来处理一些html的页面内容，只用WebViewClient就行了，如果需要更丰富的处理效果，比如js、进度条等，就要用到WebChromeClient。

## 缓存及一些设定
```java
mWebview.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY); 
mWebview.getSettings().setJavaScriptEnabled(true); 
mWebview.getSettings().setDomStorageEnabled(true); 
mWebview.getSettings().setAppCacheMaxSize(1024 * 1024 * 8); 
String appCachePath = mContext.getApplicationContext().getCacheDir() 
.getAbsolutePath(); 
mWebview.getSettings().setAppCachePath(appCachePath); 
mWebview.getSettings().setAllowFileAccess(true); 
mWebview.getSettings().setAppCacheEnabled(true);
```

* Android的WebView有五种缓存模式

1. LOAD_CACHE_ONLY //不使用网络，只读取本地缓存数据

2. LOAD_DEFAULT //根据cache-control决定是否从网络上取数据。

3. LOAD_CACHE_NORMAL //API level 17中已经废弃, 从API level 11开始作用同LOAD_DEFAULT模式

4. LOAD_NO_CACHE //不使用缓存，只从网络获取数据

5. LOAD_CACHE_ELSE_NETWORK //只要本地有，无论是否过期，或者no-cache，都使用缓存中的数据

* 缓存路径

/data/data/包名/cache/ /data/data/包名/database/webview.db /data/data/包名/database/webviewCache.db

* 设置缓存模式
```java
    mWebSetting.setLoadWithOverviewMode(true);
    mWebSetting.setDomStorageEnabled(true);
    mWebSetting.setAppCacheMaxSize(1024 * 1024 * 8);//设置缓存大小
    //设置缓存路径
    appCacheDir = Environment.getExternalStorageDirectory().getPath() + "/xxx/cache";
    File fileSD = new File(appCacheDir);
    if (!fileSD.exists()) {
        fileSD.mkdir();
    }
    mWebSetting.setAppCachePath(appCacheDir);
    mWebSetting.setAllowContentAccess(true);
    mWebSetting.setAppCacheEnabled(true);
    if (CheckHasNet.isNetWorkOk(context)) {
        //有网络网络加载
        mWebSetting.setCacheMode(WebSettings.LOAD_DEFAULT);
    } else {
        //无网时本地缓存加载
        mWebSetting.setCacheMode(WebSettings.LOAD_CACHE_ONLY);
    }
```

* 清除缓存
```java
    public void clearWebViewCache(){ 
        //清理Webview缓存数据库 
        try { 
            deleteDatabase("webview.db");  
            deleteDatabase("webviewCache.db"); 
        } catch (Exception e) { 
            e.printStackTrace(); 
        } 

        //WebView 缓存文件 
        File appCacheDir = new File(getFilesDir().getAbsolutePath()+APP_CACAHE_DIRNAME); 
        Log.e(TAG, "appCacheDir path="+appCacheDir.getAbsolutePath()); 

        File webviewCacheDir = new File(getCacheDir().getAbsolutePath()+"/webviewCache"); 
        Log.e(TAG, "webviewCacheDir path="+webviewCacheDir.getAbsolutePath()); 

        //删除webview 缓存目录 
        if(webviewCacheDir.exists()){ 
            deleteFile(webviewCacheDir); 
        } 
        //删除webview 缓存 缓存目录 
        if(appCacheDir.exists()){ 
            deleteFile(appCacheDir); 
        } 
    }
```

# [js交互](https://zhuanlan.zhihu.com/p/27588089)
Java调用JavaScript的方法很简单，只需要执行如下的代码即可：
```mWebView.loadUrl("javascript:toast()");```

JavaScript调用Java代码

HTML代码中脚本

html的脚本代码，跟上面java调用Javascipt的html一样，这里你只需要关注如下标签onclick即可，

```js
button class="but" type="submit" value="显示一个土司" onclick="javaObject.showToast('张小月，如果永远都忘不了你，那该怎么办呢？')">登录</button>
```
其中javaObject就是android中暴露出来的Java对象名字。
