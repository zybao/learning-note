http://blog.csdn.net/carson_ho/article/details/64904691

对于Android调用JS代码的方法有2种：
1. 通过WebView的loadUrl（）
2. 通过WebView的evaluateJavascript（）

对于JS调用Android代码的方法有3种：
1. 通过WebView的addJavascriptInterface（）进行对象映射
2. 通过 WebViewClient 的shouldOverrideUrlLoading ()方法回调拦截 url
3. 通过 WebChromeClient 的onJsAlert()、onJsConfirm()、onJsPrompt（）方法回调拦截JS对话框alert()、confirm()、prompt（） 消息


# Android如何调用H5中的方法
```html
// 文本名：javascript
<!DOCTYPE html>
<html>

   <head>
      <meta charset="utf-8">
      <title>Carson_Ho</title>

    // JS代码
     <script>
    // Android需要调用的方法
   function callJS(){
      alert("Android调用了JS的callJS方法");
   }
    </script>

   </head>

</html>
```

```java
        WebSettings webSettings = mWebView.getSettings();

        // 设置与Js交互的权限
        webSettings.setJavaScriptEnabled(true);
        // 设置允许JS弹窗
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);

        // 先载入JS代码
        // 格式规定为:file:///android_asset/文件名.html
        mWebView.loadUrl("file:///android_asset/javascript.html");

        button = (Button) findViewById(R.id.button);


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 必须另开线程进行JS方法调用(否则无法调用)
                mWebView.post(new Runnable() {
                    @Override
                    public void run() {

                        // 注意调用的JS方法名要对应上
                        // 调用javascript的callJS()方法
                        mWebView.loadUrl("javascript:callJS()");
                    }
                });

            }
        });
```  

**方式2：通过WebView的evaluateJavascript（）**
优点：该方法比第一种方法效率更高、使用更简洁。

    1. 因为该方法的执行不会使页面刷新，而第一种方法（loadUrl ）的执行则会。
    2. Android 4.4 后才可使用

```java
// 只需要将第一种方法的loadUrl()换成下面该方法即可
    mWebView.evaluateJavascript（"javascript:callJS()", new ValueCallback<String>() {
        @Override
        public void onReceiveValue(String value) {
            //此处为 js 返回的结果
        }
    });
}
```

# JS通过WebView调用 Android 代码
对于JS调用Android代码的方法有3种：
1. 通过WebView的addJavascriptInterface（）进行对象映射
2. 通过 WebViewClient 的shouldOverrideUrlLoading ()方法回调拦截 url
3. 通过 WebChromeClient 的onJsAlert()、onJsConfirm()、onJsPrompt（）方法回调拦截JS对话框alert()、confirm()、prompt（） 消息

## 方法1
```java
// 继承自Object类
public class AndroidtoJs extends Object {
    // 定义JS需要调用的方法
    // 被JS调用的方法必须加入@JavascriptInterface注解
    @JavascriptInterface
    public void hello(String msg) {
        System.out.println("JS调用了Android的hello方法");
    }
}
```

```html
<!DOCTYPE html>
<html>
   <head>
      <meta charset="utf-8">
      <title>Carson</title>  
      <script>
         function callAndroid(){
        // 由于对象映射，所以调用test对象等于调用Android映射的对象
            test.hello("js调用了android中的hello方法");
         }
      </script>
   </head>
   <body>
      //点击按钮则调用callAndroid函数
      <button type="button" id="button1" onclick="callAndroid()"></button>
   </body>
</html>
```
```java
        mWebView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = mWebView.getSettings();

        // 设置与Js交互的权限
        webSettings.setJavaScriptEnabled(true);

        // 通过addJavascriptInterface()将Java对象映射到JS对象
        //参数1：Javascript对象名
        //参数2：Java对象名
        mWebView.addJavascriptInterface(new AndroidtoJs(), "test");//AndroidtoJS类对象映射到js的test对象

        // 加载JS代码
        // 格式规定为:file:///android_asset/文件名.html
        mWebView.loadUrl("file:///android_asset/javascript.html");
```

缺点：存在严重的漏洞问题，具体请看文章：[你不知道的 Android WebView 使用漏洞](http://blog.csdn.net/carson_ho/article/details/64904635)

## 方式2
```html
<!DOCTYPE html>
<html>

   <head>
      <meta charset="utf-8">
      <title>Carson_Ho</title>

     <script>
         function callAndroid(){
            /*约定的url协议为：js://webview?arg1=111&arg2=222*/
            document.location = "js://webview?arg1=111&arg2=222";
         }
      </script>
</head>

<!-- 点击按钮则调用callAndroid（）方法  -->
   <body>
     <button type="button" id="button1" onclick="callAndroid()">点击调用Android代码</button>
   </body>
</html>
```

```java
        WebSettings webSettings = mWebView.getSettings();

        // 设置与Js交互的权限
        webSettings.setJavaScriptEnabled(true);
        // 设置允许JS弹窗
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);

        // 步骤1：加载JS代码
        // 格式规定为:file:///android_asset/文件名.html
        mWebView.loadUrl("file:///android_asset/javascript.html");


// 复写WebViewClient类的shouldOverrideUrlLoading方法
mWebView.setWebViewClient(new WebViewClient() {
                                      @Override
                                      public boolean shouldOverrideUrlLoading(WebView view, String url) {

                                          // 步骤2：根据协议的参数，判断是否是所需要的url
                                          // 一般根据scheme（协议格式） & authority（协议名）判断（前两个参数）
                                          //假定传入进来的 url = "js://webview?arg1=111&arg2=222"（同时也是约定好的需要拦截的）

                                          Uri uri = Uri.parse(url);                                 
                                          // 如果url的协议 = 预先约定的 js 协议
                                          // 就解析往下解析参数
                                          if ( uri.getScheme().equals("js")) {

                                              // 如果 authority  = 预先约定协议里的 webview，即代表都符合约定的协议
                                              // 所以拦截url,下面JS开始调用Android需要的方法
                                              if (uri.getAuthority().equals("webview")) {

                                                 //  步骤3：
                                                  // 执行JS所需要调用的逻辑
                                                  System.out.println("js调用了Android的方法");
                                                  // 可以在协议上带有参数并传递到Android上
                                                  HashMap<String, String> params = new HashMap<>();
                                                  Set<String> collection = uri.getQueryParameterNames();

                                              }

                                              return true;
                                          }
                                          return super.shouldOverrideUrlLoading(view, url);
                                      }
                                  }
        );
   }
```

优点：不存在方式1的漏洞；
缺点：JS获取Android方法的返回值复杂。 

## 方式3：通过 WebChromeClient 的onJsAlert()、onJsConfirm()、onJsPrompt（）方法回调拦截JS对话框alert()、confirm()、prompt（） 消息
```html
<!DOCTYPE html>
<html>
   <head>
      <meta charset="utf-8">
      <title>Carson_Ho</title>

     <script>

    function clickprompt(){
    // 调用prompt（）
    var result=prompt("js://demo?arg1=111&arg2=222");
    alert("demo " + result);
}

      </script>
</head>

<!-- 点击按钮则调用clickprompt()  -->
   <body>
     <button type="button" id="button1" onclick="clickprompt()">点击调用Android代码</button>
   </body>
</html>
```
```java
        // 设置与Js交互的权限
        webSettings.setJavaScriptEnabled(true);
        // 设置允许JS弹窗
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);

// 先加载JS代码
        // 格式规定为:file:///android_asset/文件名.html
        mWebView.loadUrl("file:///android_asset/javascript.html");


        mWebView.setWebChromeClient(new WebChromeClient() {
                                        // 拦截输入框(原理同方式2)
                                        // 参数message:代表promt（）的内容（不是url）
                                        // 参数result:代表输入框的返回值
                                        @Override
                                        public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
                                            // 根据协议的参数，判断是否是所需要的url(原理同方式2)
                                            // 一般根据scheme（协议格式） & authority（协议名）判断（前两个参数）
                                            //假定传入进来的 url = "js://webview?arg1=111&arg2=222"（同时也是约定好的需要拦截的）

                                            Uri uri = Uri.parse(message);
                                            // 如果url的协议 = 预先约定的 js 协议
                                            // 就解析往下解析参数
                                            if ( uri.getScheme().equals("js")) {

                                                // 如果 authority  = 预先约定协议里的 webview，即代表都符合约定的协议
                                                // 所以拦截url,下面JS开始调用Android需要的方法
                                                if (uri.getAuthority().equals("webview")) {

                                                    //
                                                    // 执行JS所需要调用的逻辑
                                                    System.out.println("js调用了Android的方法");
                                                    // 可以在协议上带有参数并传递到Android上
                                                    HashMap<String, String> params = new HashMap<>();
                                                    Set<String> collection = uri.getQueryParameterNames();

                                                    //参数result:代表消息框的返回值(输入值)
                                                    result.confirm("js调用了Android的方法成功啦");
                                                }
                                                return true;
                                            }
                                            return super.onJsPrompt(view, url, message, defaultValue, result);
                                        }

// 通过alert()和confirm()拦截的原理相同，此处不作过多讲述

                                        // 拦截JS的警告框
                                        @Override
                                        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                                            return super.onJsAlert(view, url, message, result);
                                        }

                                        // 拦截JS的确认框
                                        @Override
                                        public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                                            return super.onJsConfirm(view, url, message, result);
                                        }
                                    }
        );


            }
```