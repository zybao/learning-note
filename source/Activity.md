# Launcher打开App过程：
1. Launcher向ActivityManagerService发送一个启动MainActivity的请求;
2. ActivityManagerService首先将MainActivity的相关信息保存下来，然后向Launcher发送一个使之进入中止状态的请求;
3. Launcher收到中止状态之后，就会想ActivityManagerService发送一个已进入中止状态的请求，便于ActivityManagerService继续执行启动MainActivity的操作;
4. ActivityManagerService检查用于运行MainActivity的进程，如果不存在，则启动一个新的进程;
5. 新的应用程序进程启动完成之后，就会向ActivityManagerService发送一个启动完成的请求，便于ActivityManagerService继续执行启动MainActivity的操作;
6. ActivityManagerService将第2步保存下来的MainActivity相关信息发送给新创建的进程，便于该进程启动MainActivity组件.

当我们在Launcher上点击应用程序图标时，startActivitySafely方法会被调用。需要启动的Activity信息保存在intent中，包括action、category等等。那么Launcher是如何获得intent里面的这些信息呢？首先，系统在启动时会启动一个叫做PackageManagerService的管理服务，并且通过他来安装系统中的应用程序，在这个过程中，PackageManagerService会对应用程序的配置文件AndroidManifest.xml进行解析，从而得到程序里的组件信息（包括Activity、Service、Broadcast等），然后PackageManagerService去查询所有action为“android.intent.action.MAIN”并且category为“android.intent.category.LAUNCHER”的Activity，然后为每个应用程序创建一个快捷方式图标，并把程序信息与之关联。上述代码中，Activity的启动标志位设置为“Intent.FLAG_ACTIVITY_NEW_TASK”，便于他可以在一个新的任务中启动。

* `Activity.startActivityForResult`
```java
    /**
     * Launch an activity for which you would like a result when it finished.
     * When this activity exits, your
     * onActivityResult() method will be called with the given requestCode.
     * Using a negative requestCode is the same as calling
     * {@link #startActivity} (the activity is not launched as a sub-activity).
     *
     * <p>Note that this method should only be used with Intent protocols
     * that are defined to return a result.  In other protocols (such as
     * {@link Intent#ACTION_MAIN} or {@link Intent#ACTION_VIEW}), you may
     * not get the result when you expect.  For example, if the activity you
     * are launching uses the singleTask launch mode, it will not run in your
     * task and thus you will immediately receive a cancel result.
     *
     * <p>As a special case, if you call startActivityForResult() with a requestCode
     * >= 0 during the initial onCreate(Bundle savedInstanceState)/onResume() of your
     * activity, then your window will not be displayed until a result is
     * returned back from the started activity.  This is to avoid visible
     * flickering when redirecting to another activity.
     *
     * <p>This method throws {@link android.content.ActivityNotFoundException}
     * if there was no Activity found to run the given Intent.
     *
     * @param intent The intent to start.
     * @param requestCode If >= 0, this code will be returned in
     *                    onActivityResult() when the activity exits.
     * @param options Additional options for how the Activity should be started.
     * See {@link android.content.Context#startActivity(Intent, Bundle)
     * Context.startActivity(Intent, Bundle)} for more details.
     *
     * @throws android.content.ActivityNotFoundException
     *
     * @see #startActivity
     */
    public void startActivityForResult(Intent intent, int requestCode, @Nullable Bundle options) {
        if (mParent == null) {
            Instrumentation.ActivityResult ar =
                mInstrumentation.execStartActivity(
                    this, mMainThread.getApplicationThread(), mToken, this,
                    intent, requestCode, options);
            if (ar != null) {
                mMainThread.sendActivityResult(
                    mToken, mEmbeddedID, requestCode, ar.getResultCode(),
                    ar.getResultData());
            }
            if (requestCode >= 0) {
                // If this start is requesting a result, we can avoid making
                // the activity visible until the result is received.  Setting
                // this code during onCreate(Bundle savedInstanceState) or onResume() will keep the
                // activity hidden during this time, to avoid flickering.
                // This can only be done when a result is requested because
                // that guarantees we will get information back when the
                // activity is finished, no matter what happens to it.
                mStartedActivity = true;
            }

            cancelInputsAndStartExitTransition(options);
            // TODO Consider clearing/flushing other event sources and events for child windows.
        } else {
            if (options != null) {
                mParent.startActivityFromChild(this, intent, requestCode, options);
            } else {
                // Note we want to go through this method for compatibility with
                // existing applications that may have overridden it.
                mParent.startActivityFromChild(this, intent, requestCode);
            }
        }
    }
```

最后实际上是调用mInstrumentation.execStartActivity来启动Activity，mInstrumentation类型为Instrumentation，用于监控程序和系统之间的交互操作。mInstrumentation代为执行Activity的启动操作，便于他可以监控这一个交互过程。mMainThread的类型为ActivityThread，用于描述一个应用程序进程，系统每启动一个程序都会在它里面加载一个ActivityThread的实例，并且将该实例保存在Activity的成员变量mMainThread中，而mMainThread.getApplicationThread()则用于获取其内部一个类型为ApplicationThread的本地Binder对象。mToken的类型为IBinder，他是一个Binder的代理对象，只想了ActivityManagerService中一个类型为ActivityRecord的本地Binder对象。每一个已经启动的Activity在ActivityManagerService中都有一个对应的ActivityRecord对象，用于维护Activity的运行状态及信息。

* `Instrumentation.execStartActivity`
```java
    /**
     * Execute a startActivity call made by the application.  The default 
     * implementation takes care of updating any active {@link ActivityMonitor}
     * objects and dispatches this call to the system activity manager; you can
     * override this to watch for the application to start an activity, and 
     * modify what happens when it does. 
     *
     * <p>This method returns an {@link ActivityResult} object, which you can 
     * use when intercepting application calls to avoid performing the start 
     * activity action but still return the result the application is 
     * expecting.  To do this, override this method to catch the call to start 
     * activity so that it returns a new ActivityResult containing the results 
     * you would like the application to see, and don't call up to the super 
     * class.  Note that an application is only expecting a result if 
     * <var>requestCode</var> is &gt;= 0.
     *
     * <p>This method throws {@link android.content.ActivityNotFoundException}
     * if there was no Activity found to run the given Intent.
     *
     * @param who The Context from which the activity is being started.
     * @param contextThread The main thread of the Context from which the activity
     *                      is being started.
     * @param token Internal token identifying to the system who is starting 
     *              the activity; may be null.
     * @param target Which activity is performing the start (and thus receiving 
     *               any result); may be null if this call is not being made
     *               from an activity.
     * @param intent The actual Intent to start.
     * @param requestCode Identifier for this request's result; less than zero 
     *                    if the caller is not expecting a result.
     * @param options Addition options.
     *
     * @return To force the return of a particular result, return an 
     *         ActivityResult object containing the desired data; otherwise
     *         return null.  The default implementation always returns null.
     *
     * @throws android.content.ActivityNotFoundException
     *
     * @see Activity#startActivity(Intent)
     * @see Activity#startActivityForResult(Intent, int)
     * @see Activity#startActivityFromChild
     *
     * {@hide}
     */
    public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode, Bundle options) {
        IApplicationThread whoThread = (IApplicationThread) contextThread;
        Uri referrer = target != null ? target.onProvideReferrer() : null;
        if (referrer != null) {
            intent.putExtra(Intent.EXTRA_REFERRER, referrer);
        }
        if (mActivityMonitors != null) {
            synchronized (mSync) {
                final int N = mActivityMonitors.size();
                for (int i=0; i<N; i++) {
                    final ActivityMonitor am = mActivityMonitors.get(i);
                    if (am.match(who, null, intent)) {
                        am.mHits++;
                        if (am.isBlocking()) {
                            return requestCode >= 0 ? am.getResult() : null;
                        }
                        break;
                    }
                }
            }
        }
        try {
            intent.migrateExtraStreamToClipData();
            intent.prepareToLeaveProcess();
            int result = ActivityManagerNative.getDefault()
                .startActivity(whoThread, who.getBasePackageName(), intent,
                        intent.resolveTypeIfNeeded(who.getContentResolver()),
                        token, target != null ? target.mEmbeddedID : null,
                        requestCode, 0, null, options);
            checkStartActivityResult(result, intent);
        } catch (RemoteException e) {
            throw new RuntimeException("Failure from system", e);
        }
        return null;
    }
```

通过ActivityManagerNative.getDefault()获取一个ActivityManagerService的代理对象，然后调用他的startActivity方法来通知ActivityManagerService去启动Activity。

中间还有一系列过程，跟着源码走下去，不难发现，最后，是调用ApplicationThread的scheduleLaunchActivity来进行Activity的启动。

* `Application.scheduleLaunchActivity`