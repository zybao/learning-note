# BinderInternal与GC
在 ActivityThread 里面，在多个地方使用到了 BinderInternal，比较好奇，研究一下它。

主要有两个地方使用到它：
```java
BinderInternal.forceGc("mem");
```
和
```java
BinderInternal.addGcWatcher(new Runnable() {
    @Override public void run() {
        if (!mSomeActivitiesChanged) {
            return;
        }
        Runtime runtime = Runtime.getRuntime();
        long dalvikMax = runtime.maxMemory();
        long dalvikUsed = runtime.totalMemory() - runtime.freeMemory();
        if (dalvikUsed > ((3*dalvikMax)/4)) {
            if (DEBUG_MEMORY_TRIM) Slog.d(TAG, "Dalvik max=" + (dalvikMax/1024)
                    + " total=" + (runtime.totalMemory()/1024)
                    + " used=" + (dalvikUsed/1024));
            mSomeActivitiesChanged = false;
            try {
                mgr.releaseSomeActivities(mAppThread);
            } catch (RemoteException e) {
            }
        }
    }
});
```
第一个好理解，就是“建议”虚拟机执行一次GC操作，比较好玩的是第二个。
```java
public class BinderInternal {
    static WeakReference<GcWatcher> sGcWatcher
            = new WeakReference<GcWatcher>(new GcWatcher());
    
    // 往这个列表里添加的 Runnable，有机会被执行
    static ArrayList<Runnable> sGcWatchers = new ArrayList<>();
    
    static Runnable[] sTmpWatchers = new Runnable[1];
    static long sLastGcTime;

    static final class GcWatcher {
        @Override
        protected void finalize() throws Throwable {
            handleGc();
            sLastGcTime = SystemClock.uptimeMillis();
            synchronized (sGcWatchers) {
                sTmpWatchers = sGcWatchers.toArray(sTmpWatchers);
            }
            for (int i=0; i<sTmpWatchers.length; i++) {
                if (sTmpWatchers[i] != null) {
                    sTmpWatchers[i].run();
                }
            }
            // 在回收的最终环节，又new一个对象，这样可以保证不死...
            sGcWatcher = new WeakReference<GcWatcher>(new GcWatcher());
        }
    }

    public static void addGcWatcher(Runnable watcher) {
        synchronized (sGcWatchers) {
            sGcWatchers.add(watcher);
        }
    }
    
    ......
```   

这里面的 sGcWatcher 是 WeakReference 类型的对象，也就是说，时不时可能会被回收的。那么它“包裹”的GCWatcher对象，也会被回收的。由于 GCWatcher 重写了 finalize这个方法，该方法在GC的最终环节会被回调，进而会执行 Runnable 的run 方法。

结合`ActivityThread` 里面的用法，可能会执行 `mgr.releaseSomeActivities(mAppThread)`;，即如果内存不够了，会“释放”一些`Activity` 对象。

所以，这里面有两点：

1. 重写 finalize 方法，在最终GC前，还有一些补救措施。
2. 如果有什么操作跟GC相关，可以模仿ActivityThread的做法，调用 BinderInternal.addGcWatcher(new Runnable() {...});，这样也可以“周期性”的执行一些操作

# [统计线上启动时间](http://www.jianshu.com/p/c967653a9468)
## 应用的主要启动流程
关于 App 启动流程的文章很多，文章底部有一些启动流程相关的参考文章，这里只列出大致流程如下：

1.    通过 Launcher 启动应用时，点击应用图标后，Launcher 调用 startActivity 启动应用。
2.    Launcher Activity 最终调用 Instrumentation 的 execStartActivity 来启动应用。
3.    Instrumentation 调用 ActivityManagerProxy (ActivityManagerService 在应用进程的一个代理对象) 对象的 startActivity 方法启动 Activity。
4.    到目前为止所有过程都在 Launcher 进程里面执行，接下来 ActivityManagerProxy 对象跨进程调用 ActivityManagerService (运行在 system_server 进程)的 startActivity 方法启动应用。
5.    ActivityManagerService 的 startActivity 方法经过一系列调用，最后调用 zygoteSendArgsAndGetResult 通过 socket 发送给 zygote 进程，zygote 进程会孵化出新的应用进程。
6.    zygote 进程孵化出新的应用进程后，会执行 ActivityThread 类的 main 方法。在该方法里会先准备好 Looper 和消息队列，然后调用 attach 方法将应用进程绑定到 ActivityManagerService，然后进入 loop 循环，不断地读取消息队列里的消息，并分发消息。
7.    ActivityManagerService 保存应用进程的一个代理对象，然后 ActivityManagerService 通过代理对象通知应用进程创建入口 Activity 的实例，并执行它的生命周期函数。

总结过程就是：用户在 Launcher 程序里点击应用图标时，会通知 ActivityManagerService 启动应用的入口 Activity， ActivityManagerService 发现这个应用还未启动，则会通知 Zygote 进程孵化出应用进程，然后在这个应用进程里执行 ActivityThread 的 main 方法。应用进程接下来通知 ActivityManagerService 应用进程已启动，ActivityManagerService 保存应用进程的一个代理对象，这样 ActivityManagerService 可以通过这个代理对象控制应用进程，然后 ActivityManagerService 通知应用进程创建入口 Activity 的实例，并执行它的生命周期函数。

## 生命周期函数执行流程
上面的启动流程是 Android 提供的机制，作为开发者我们需要清楚或者至少了解其中的过程和原理，但我们并不能在这过程中做什么文章，我们能做的恰恰是从上述过程中最后一步开始，即 ActivityManagerService 通过代理对象通知应用进程创建入口 Activity 的实例，并执行它的生命周期函数开始，我们的启动时间统计以及启动速度优化也是从这里开始。下面是 Main Activity 的启动流程：
```java
-> Application 构造函数
-> Application.attachBaseContext()
-> Application.onCreate()
-> Activity 构造函数
-> Activity.setTheme()
-> Activity.onCreate()
-> Activity.onStart
-> Activity.onResume
-> Activity.onAttachedToWindow
-> Activity.onWindowFocusChanged
```
如果打 Log 记录 App 的启动时间，那么至少要记录两个点，一个起始时间点，一个结束时间点。

**起始时间点**

起始时间点比较容易记录：如果记录冷启动启动时间一般可以在 Application.attachBaseContext() 开始的位置记录起始时间点，因为在这之前 Context 还没有初始化，一般也干不了什么事情，当然这个是要视具体情况来定，其实只要保证在 App 的具体业务逻辑开始执行之前记录起始时间点即可。如果记录热启动启动时间点可以在 Activity.onRestart() 中记录起始时间点。

**结束时间点**

结束时间点理论上要选在 App 显示出第一屏界面的时候，但是在什么位置 App 显示出第一屏界面呢？网上很多文章说在 Activity 的 onResume 方法执行完成之后，Activity 就对用户可见了，实际上并不是，一个 Activity 走完onCreate onStart onResume 这几个生命周期之后，只是完成了应用自身的一些配置,比如 Activity 主题设置 window 属性的设置 View 树的建立，但是其实后面还需要各个 View 执行 measure layout draw等。所以在 OnResume 中记录结束时间点的 Log 并不准确，大家可以注意一下上面流程中最后一个函数 Activity.onWindowFocusChanged，下面是它的注释：
```java
/**
*Called when the current {@link Window} of the activity gains or loses
* focus.  This is the best indicator of whether this activity is visible
* to the user.  The default implementation clears the key tracking
* state, so should always be called.
...
*/
```

通过注释我们可以看到，这个函数是判断 activity 是否可见的最佳位置，所以我们可以在 Activity.onWindowFocusChanged 记录应用启动的结束时间点，不过需要注意的是该函数，在 Activity 焦点发生变化时就会触发，所以要做好判断，去掉不需要的情况。

# OnTrimMemory优化

OnTrimMemory 回调是 Android 4.0 之后提供的一个API，这个 API 是提供给开发者的，它的主要作用是提示开发者在系统内存不足的时候，通过处理部分资源来释放内存，从而避免被 Android 系统杀死。这样应用在下一次启动的时候，速度就会比较快。

本文通过问答的方式，从各个方面来讲解 OnTrimMemory 回调的使用过程和效果。想要开发高性能且用户体验良好的 Android 应用，那么这篇文章你不应该错过。
0. OnTrimMemory回调的作用？

OnTrimMemory是Android在4.0之后加入的一个回调，任何实现了ComponentCallbacks2接口的类都可以重写实现这个回调方法．OnTrimMemory的主要作用就是指导应用程序在不同的情况下进行自身的内存释放，以避免被系统直接杀掉，提高应用程序的用户体验.

Android系统会根据不同等级的内存使用情况，调用这个函数，并传入对应的等级：

    `TRIM_MEMORY_UI_HIDDEN` 表示应用程序的所有UI界面被隐藏了，即用户点击了Home键或者Back键导致应用的UI界面不可见．这时候应该释放一些资源．

    `TRIM_MEMORY_UI_HIDDEN`这个等级比较常用，和下面六个的关系不是很强，所以单独说．

下面三个等级是当我们的应用程序真正运行时的回调：

    `TRIM_MEMORY_RUNNING_MODERATE` 表示应用程序正常运行，并且不会被杀掉。但是目前手机的内存已经有点低了，系统可能会开始根据LRU缓存规则来去杀死进程了。

    `TRIM_MEMORY_RUNNING_LOW` 表示应用程序正常运行，并且不会被杀掉。但是目前手机的内存已经非常低了，我们应该去释放掉一些不必要的资源以提升系统的性能，同时这也会直接影响到我们应用程序的性能。

    `TRIM_MEMORY_RUNNING_CRITICAL` 表示应用程序仍然正常运行，但是系统已经根据LRU缓存规则杀掉了大部分缓存的进程了。这个时候我们应当尽可能地去释放任何不必要的资源，不然的话系统可能会继续杀掉所有缓存中的进程，并且开始杀掉一些本来应当保持运行的进程，比如说后台运行的服务。

当应用程序是缓存的，则会收到以下几种类型的回调：

    `TRIM_MEMORY_BACKGROUND` 表示手机目前内存已经很低了，系统准备开始根据LRU缓存来清理进程。这个时候我们的程序在LRU缓存列表的最近位置，是不太可能被清理掉的，但这时去释放掉一些比较容易恢复的资源能够让手机的内存变得比较充足，从而让我们的程序更长时间地保留在缓存当中，这样当用户返回我们的程序时会感觉非常顺畅，而不是经历了一次重新启动的过程。

    `TRIM_MEMORY_MODERATE` 表示手机目前内存已经很低了，并且我们的程序处于LRU缓存列表的中间位置，如果手机内存还得不到进一步释放的话，那么我们的程序就有被系统杀掉的风险了。
    TRIM_MEMORY_COMPLETE 表示手机目前内存已经很低了，并且我们的程序处于LRU缓存列表的最边缘位置，系统会最优先考虑杀掉我们的应用程序，在这个时候应当尽可能地把一切可以释放的东西都进行释放。

1. 哪些组件可以实现OnTrimMemory回调？
```java
    Application.onTrimMemory()
    Activity.onTrimMemory()
    Fragment.OnTrimMemory()
    Service.onTrimMemory()
    ContentProvider.OnTrimMemory()
```
2. OnTrimMemory回调中可以释放哪些资源？

通常在架构阶段就要考虑清楚，我们有哪些东西是要常驻内存的，有哪些是伴随界面存在的．一般情况下，有下面几种资源需要进行释放：

    缓存 缓存包括一些文件缓存，图片缓存等，在用户正常使用的时候这些缓存很有作用，但当你的应用程序UI不可见的时候，这些缓存就可以被清除以减少内存的使用．比如第三方图片库的缓存．
    一些动态生成动态添加的View．　这些动态生成和添加的View且少数情况下才使用到的View，这时候可以被释放，下次使用的时候再进行动态生成即可．比如原生桌面中，会在OnTrimMemory的TRIM_MEMORY_MODERATE等级中，释放所有AppsCustomizePagedView的资源，来保证在低内存的时候，桌面不会轻易被杀掉．

2.1 例子：释放不常用到的View．

代码出处：Launcher

Launcher.java:
```java
@Override
public void onTrimMemory(int level) {
    super.onTrimMemory(level);
    if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
        mAppsCustomizeTabHost.onTrimMemory();
    }
}
```
AppsCustomizeTabHost.java:
```java
public void onTrimMemory() {
    mContent.setVisibility(GONE);
    // Clear the widget pages of all their subviews - this will trigger the widget previews
    // to delete their bitmaps
    mPagedView.clearAllWidgetPages();
}
```
AppsCustomizePagedView.java:
```java
public void clearAllWidgetPages() {
    cancelAllTasks();
    int count = getChildCount();
    for (int i = 0; i < count; i++) {
        View v = getPageAt(i);
        if (v instanceof PagedViewGridLayout) {
            ((PagedViewGridLayout) v).removeAllViewsOnPage();
            mDirtyPageContent.set(i, true);
        }
    }
}
```
PagedViewGridLayout.java
```java
@Override
public void removeAllViewsOnPage() {
    removeAllViews();
    mOnLayoutListener = null;
    setLayerType(LAYER_TYPE_NONE, null);
}
```

2.2 例子: 清除缓存

代码出处：Contact
```java
@Override
public void onTrimMemory(int level) {
    if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
        // Clear the caches.  Note all pending requests will be removed too.
        clear();
    }
}
public void clear() {
    mPendingRequests.clear();
    mBitmapHolderCache.evictAll();
    mBitmapCache.evictAll();
}
```

3. OnTrimMemory和onStop的关系？

onTrimMemory()方法中的TRIM_MEMORY_UI_HIDDEN回调只有当我们程序中的所有UI组件全部不可见的时候才会触发，这和onStop()方法还是有很大区别的，因为onStop()方法只是当一个Activity完全不可见的时候就会调用，比如说用户打开了我们程序中的另一个Activity。

因此，我们可以在onStop()方法中去释放一些Activity相关的资源，比如说取消网络连接或者注销广播接收器等，但是像UI相关的资源应该一直要等到onTrimMemory(TRIM_MEMORY_UI_HIDDEN)这个回调之后才去释放，这样可以保证如果用户只是从我们程序的一个Activity回到了另外一个Activity，界面相关的资源都不需要重新加载，从而提升响应速度。

需要注意的是，onTrimMemory的TRIM_MEMORY_UI_HIDDEN 等级是在onStop方法之前调用的．

4. OnTrimMemory和OnLowMemory的关系？

在引入OnTrimMemory之前都是使用OnLowMemory回调，需要知道的是，OnLowMemory大概和OnTrimMemory中的TRIM_MEMORY_COMPLETE级别相同，如果你想兼容api<14的机器，那么可以用OnLowMemory来实现，否则你可以忽略OnLowMemory，直接使用OnTrimMemory即可．

5. 为什么要调用OnTrimMemory？

尽管系统在内存不足的时候杀进程的顺序是按照LRU Cache中从低到高来的，但是它同时也会考虑杀掉那些占用内存较高的应用来让系统更快地获得更多的内存。

所以如果你的应用占用内存较小，就可以增加不被杀掉的几率，从而快速地恢复（如果不被杀掉，启动的时候就是热启动，否则就是冷启动，其速度差在2~3倍）。

所以说在几个不同的OnTrimMemory回调中释放自己的UI资源，可以有效地提高用户体验。

6. 有哪些典型的使用场景？

6.1 常驻内存的应用

一些常驻内存的应用，比如Launcher、安全中心、电话等，在用户使用过要退出的时候，需要调用OnTrimMemory来及时释放用户使用的时候所产生的多余的内存资源：比如动态生成的View、图片缓存、Fragment等。

6.2 有后台Service运行的应用

这些应用不是常驻内存的，意味着可以被任务管理器杀掉，但是在某些场景下用户不会去杀。
这类应用包括：音乐、下载等。用户退出UI界面后，音乐还在继续播放，下载程序还在运行。这时候音乐应该释放部分UI资源和Cache。

# 快速切换到主线程更新UI的几种方法 
## 方法一： view.post(Runnable action)

假如该方法是在子线程中
```java
textView.post(new Runnable() {
        @Override
        public void run() {
            textView.setText("更新textView");
            //还可以更新其他的控件
            imageView.setBackgroundResource(R.drawable.update);
        }
    });
```

这是view自带的方法，比较简单，如果你的子线程里可以得到要更新的view的话，可以用此方法进行更新。

view还有一个方法view.postDelayed(Runnable action, long delayMillis)用来延迟发送。

## 方法二： activity.runOnUiThread(Runnable action)

假如该方法是在子线程中

注意：context 对象要是 主线程中的MainActivity，这样强转才可以。
```java
public void updateUI(final Context context) {
        ((MainActivity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //此时已在主线程中，可以更新UI了
            }
        });
    }
```
如果没有上下文（context），试试下面的方法：
1.用view.getContext()可以得到上下文。
2.跳过context直接用new Activity().runOnUiThread(Runnable action)来切换到主线程

## 方法三： Handler机制

首先在主线程中定义Handler，Handler mainHandler = new Handler();（必须要在主线程中定义才能操作主线程，如果想在其他地方定义声明时要这样写Handler mainHandler = new Handler(Looper.getMainLooper())，来获取主线程的 Looper 和 Queue ）

获取到 Handler 后就很简单了，用handler.post(Runnable r)方法把消息处理放在该 handler 依附的消息队列中（也就是主线程消息队列）。

（1）：假如该方法是在子线程中
```java
    Handler mainHandler = new Handler(Looper.getMainLooper());
    mainHandler.post(new Runnable() {
        @Override
        public void run() {
            //已在主线程中，可以更新UI
        }
    });
```
Handler还有下面的方法：
1.postAtTime(Runnable r, long uptimeMillis); //在某一时刻发送消息
2.postAtDelayed(Runnable r, long delayMillis); //延迟delayMillis毫秒再发送消息

（2）: 假设在主线程中
```java
    Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case 0:
                    //更新UI等
                    break;
                case 1:
                     //更新UI等
                    break;
                default:
                    break;
            }
        }
    }
```
之后可以把 mainHandler 当做参数传递在各个类之间，当需要更新UI时，可以调用sendMessage一系列方法来执行handleMessage里的操作。

假设现在在子线程了
```java
    /**
      *获取消息，尽量用obtainMessage()方法，查看源码发现，该方法节省内存。
      *不提倡用Messenger msg=new Messenger()这种方法，每次都去创建一个对象，肯定不节省内存啦！
      *至于为什么该方法还存在，估计还是有存在的必要吧。（留作以后深入研究）
      */
    Message msg = myHandler.obtainMessage();
    msg.what = 0; //消息标识
    myHandler.sendMessage(msg); //发送消息
```

如上代码，只是发送了个消息标识，并没有传其他参数。
如果想传递参数，可以这样：
```java
      msg.what = 1;  //消息标识
      msg.arg1=2;   //存放整形数据，如果携带数据简单，优先使用arg1和arg2，比Bundle更节省内存。
      msg.arg2=3;   //存放整形数据
      Bundle bundle=new Bundle();
      bundle.putString("dd","adfasd");
      bundle.putInt("love",5);
      msg.setData(bundle);
      msg.obj=bundle;   //用来存放Object类型的任意对象
      myHandler.sendMessage(msg); //发送消息   
```
总结： msg.obj它的功能比较强大一下，至于它和利用Bundle传递数据，那个会效率高一些，更节省内存一些。个人认为：从传递数据的复杂程度看，由简单到复杂依次使用，arg1， setData(), obj。会比较好一些。

当然可以用简化方法sendEmptyMessage(int what)来减少不必要的代码，这样写：
```java
    myHandler.sendEmptyMessage(0); //其实内部实现还是和上面一样
```

发送消息的其他方法有：
```java
endEmptyMessageAtTime(int what, long uptimeMillis); //定时发送空消息
sendEmptyMessageDelayed(int what, long delayMillis); //延时发送空消息
sendMessageAtTime(Message msg, long uptimeMillis); //定时发送消息
sendMessageDelayed(Message msg, long delayMillis); //延时发送消息
sendMessageAtFrontOfQueue(Message msg); //最先处理消息（慎用）
```

方法四： 使用AsyncTask
```java
/**
  * 该类中方法的执行顺序依次为：onPreExecute, doInBackground, onPostExecute
  */
    private class MyAsyncTask extends AsyncTask<String, Integer, String> {
        /**
         * 主线程中执行
         * 在execute()被调用后首先执行
         * 一般用来在执行后台任务前对UI做一些标记
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            System.out.println("MyAsyncTask.onPreExecute");
        }

        /**
         * 子线程中执行，执行一些耗时操作，关键方法
         * 在执行过程中可以调用publishProgress(Progress... values)来更新进度信息。
         */
        @Override
        protected String doInBackground(String... params) {
            System.out.println("MyAsyncTask.doInBackground");
            //只是模拟了耗时操作
            int count = 0;
            for (int i = 0; i < 10; i++) {
                try {
                    count++;
                    publishProgress((count % 100) * 10);
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // publishProgress((int) ((count / (float) total) * 100));
            return "耗时操作执行完毕";
        }

        /**
         * 主线程中执行
         * 在调用publishProgress(Progress... values)时，此方法被执行，直接将进度信息更新到UI组件中
         */
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressBar.setProgress(values[0]);
            textView.setText("loading..." + values[0] + "%");
            System.out.println("MyAsyncTask.onProgressUpdate");
        }

        /**
         * 在主线程中，当后台操作结束时，此方法将会被调用
         * 计算结果将做为参数传递到此方法中，直接将结果显示到UI组件上。
         */
        @Override
        protected void onPostExecute(String aVoid) {
            super.onPostExecute(aVoid);
            System.out.println("MyAsyncTask.onPostExecute aVoid=" + aVoid);
            textView.setText(aVoid);
        }


        /**
         * 主线程中执行
         * 当异步任务取消后的，会回调该函数。在该方法内可以更新UI
         */
        @Override
        protected void onCancelled() {
            super.onCancelled();
            System.out.println("MyAsyncTask.onCancelled");
            progressBar.setProgress(0);
            textView.setText("0");
        }

        @Override
        protected void onCancelled(String s) {
            super.onCancelled(s);
        }
    }
```
注意：doInBackground方法是在子线程中，所以，我们在这个方法里面执行耗时操作。同时，由于其返回结果会传递到onPostExecute方法中，而onPostExecute方法工作在UI线程，这样我们就在这个方法里面更新ui，达到了异步更新ui的目的。

# `invalidate()` 与 `requestLayout()`的区别

https://juejin.im/entry/59e46ce16fb9a04511702dd0

```java
/**
* Invalidate the whole view. If the view is visible,
* {@link #onDraw(android.graphics.Canvas)} will be called at some point in
* the future.
* <p>
* This must be called from a UI thread. To call from a non-UI thread, call
* {@link #postInvalidate()}.
*/
```

```java
void invalidateInternal(int l, int t, int r, int b, boolean invalidateCache,
            boolean fullInvalidate) {
        if (mGhostView != null) {
            mGhostView.invalidate(true);
            return;
        }
      //这里判断该子View是否可见或者是否处于动画中
        if (skipInvalidate()) {
            return;
        }
         //根据View的标记位来判断该子View是否需要重绘，假如View没有任何变化，那么就不需要重绘,这里很重要一定要注意，通过标志位来决定是否需要重新绘制
        if ((mPrivateFlags & (PFLAG_DRAWN | PFLAG_HAS_BOUNDS)) == (PFLAG_DRAWN | PFLAG_HAS_BOUNDS)
                || (invalidateCache && (mPrivateFlags & PFLAG_DRAWING_CACHE_VALID) == PFLAG_DRAWING_CACHE_VALID)
                || (mPrivateFlags & PFLAG_INVALIDATED) != PFLAG_INVALIDATED
                || (fullInvalidate && isOpaque() != mLastIsOpaque)) {
            if (fullInvalidate) {
                mLastIsOpaque = isOpaque();
                mPrivateFlags &= ~PFLAG_DRAWN;
            }

            mPrivateFlags |= PFLAG_DIRTY;

            if (invalidateCache) {
                mPrivateFlags |= PFLAG_INVALIDATED;
                mPrivateFlags &= ~PFLAG_DRAWING_CACHE_VALID;
            }

            // Propagate the damage rectangle to the parent view.
            final AttachInfo ai = mAttachInfo;
            final ViewParent p = mParent;
            //当父容器不为null把需要重绘的区域传递给父容器
            if (p != null && ai != null && l < r && t < b) {
                final Rect damage = ai.mTmpInvalRect;
                damage.set(l, t, r, b);
                //注意这行代码
                p.invalidateChild(this, damage);
            }

            // Damage the entire projection receiver, if necessary.
            if (mBackground != null && mBackground.isProjected()) {
                final View receiver = getProjectionReceiver();
                if (receiver != null) {
                    receiver.damageInParent();
                }
            }
        }
    }
```

意思就是当整个view可见的时候，会使整个view无效，会间接调用到onDraw方法，并且只能在UI线程中调用，如果要在非UI线程中调用推荐使用postInvalidate。 首先来看下 mParent.invalidateChild方法。在view中invalidateChild是个抽象方法，那我们进入ViewGroup来看下他的invalidateChild。

* ViewGroup#invalidateChild

```java
   @Override
    public final void invalidateChild(View child, final Rect dirty) {
        final AttachInfo attachInfo = mAttachInfo;
        ……
        ViewParent parent = this;
            do {
                View view = null;
                // If the parent is dirty opaque or not dirty, mark it dirty with the opaque
                // flag coming from the child that initiated the invalidate
                if (view != null) {
                    if ((view.mViewFlags & FADING_EDGE_MASK) != 0 &&
                            view.getSolidColor() == 0) {
                        opaqueFlag = PFLAG_DIRTY;
                    }
                    if ((view.mPrivateFlags & PFLAG_DIRTY_MASK) != PFLAG_DIRTY) {
                        view.mPrivateFlags = (view.mPrivateFlags & ~PFLAG_DIRTY_MASK) | opaqueFlag;
                    }
                }
                //注意这行代码  //调用ViewGrup的invalidateChildInParent，如果已经达到最顶层view,则调用ViewRootImpl
            //的invalidateChildInParent。
                parent = parent.invalidateChildInParent(location, dirty);
              ……
            } while (parent != null);
        }
    }
```

* invalidate：首先会进行本身的标志位设置，看看是否需要进行mease、layout、draw操作，需要需要会进入ViewRootImpl#invalidateChildInParent，最终进入到performTraversals执行遍历绘制操作。
* postInvalidate:postInvalidate比invalidate多了一步在ViewRootImpl的线程切换，然后下面的流程跟invalidate一摸一样。

其实invalidate相比requestLayout就是在进入ViewRootImpl之前进行了一系列的标致位设置。防止view进行一系列不必要的测量和布局，当然draw流程是两者都需要进行的。然后进入ViewRootImpl的函数调用流程是这样的：
scheduleTraversals -> doTraversal -> performTraversals，其中scheduleTraversals到doTraversal是通过Handler机制进行调用的。所以，当我们在编码时，当明确的知道没有进行控件的大小和布局改变的时候调用invalidate可以减少不必要的测量和布局，提升性能。

