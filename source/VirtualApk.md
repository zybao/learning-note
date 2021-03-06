# start Activity
```java
@Override
public void startActivity(Intent intent) {
    this.startActivity(intent, null);
}

@Override
public void startActivity(Intent intent, @Nullable Bundle options) {
    if (options != null) {
        startActivityForResult(intent, -1, options);
    } else {
        startActivityForResult(intent, -1);
    }
}

public void startActivityForResult(@RequiresPermission Intent intent, int requestCode) {
    startActivityForResult(intent, requestCode, null);
}

public void startActivityForResult(@RequiresPermission Intent intent, int requestCode,
            @Nullable Bundle options) {
    if (mParent == null) {
        options = transferSpringboardActivityOptions(options);
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
            mStartedActivity = true;
        }
        cancelInputsAndStartExitTransition(options);
    } else {
        if (options != null) {
            mParent.startActivityFromChild(this, intent, requestCode, options);
        } else {
            mParent.startActivityFromChild(this, intent, requestCode);
        }
    }
}
```

`mInstrumentation.execStartActivity()`

```java
/**
* who：正在启动该Activity的上下文
* contextThread：正在启动该Activity的上下文线程，这里为ApplicationThread
* token：正在启动该Activity的标识
* target：正在启动该Activity的Activity，也就是回调结果的Activity
*/
public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode, Bundle options) {
    ...
    int result = ActivityManagerNative.getDefault()
        .startActivity(whoThread, who.getBasePackageName(), intent,
                intent.resolveTypeIfNeeded(who.getContentResolver()),
                token, target != null ? target.mEmbeddedID : null,
                requestCode, 0, null, options);
    checkStartActivityResult(result, intent);
    ...
}
```

```java
public static void checkStartActivityResult(int res, Object intent) {
    if (res >= ActivityManager.START_SUCCESS) {
        return;
    }
    switch (res) {
        case ActivityManager.START_INTENT_NOT_RESOLVED:
        case ActivityManager.START_CLASS_NOT_FOUND:
            if (intent instanceof Intent && ((Intent)intent).getComponent() != null)
                throw new ActivityNotFoundException(
                        "Unable to find explicit activity class "
                        + ((Intent)intent).getComponent().toShortString()
                        + "; have you declared this activity in your AndroidManifest.xml?");
            throw new ActivityNotFoundException(
                    "No Activity found to handle " + intent);
        case ActivityManager.START_PERMISSION_DENIED:
            throw new SecurityException("Not allowed to start activity "
                    + intent);
        case ActivityManager.START_FORWARD_AND_REQUEST_CONFLICT:
            throw new AndroidRuntimeException(
                    "FORWARD_RESULT_FLAG used while also requesting a result");
        case ActivityManager.START_NOT_ACTIVITY:
            throw new IllegalArgumentException(
                    "PendingIntent is not an activity");
        case ActivityManager.START_NOT_VOICE_COMPATIBLE:
            throw new SecurityException(
                    "Starting under voice control not allowed for: " + intent);
        case ActivityManager.START_VOICE_NOT_ACTIVE_SESSION:
            throw new IllegalStateException(
                    "Session calling startVoiceActivity does not match active session");
        case ActivityManager.START_VOICE_HIDDEN_SESSION:
            throw new IllegalStateException(
                    "Cannot start voice activity on a hidden session");
        case ActivityManager.START_CANCELED:
            throw new AndroidRuntimeException("Activity could not be started for "
                    + intent);
        default:
            throw new AndroidRuntimeException("Unknown error code "
                    + res + " when starting " + intent);
    }
}
```

startActivity:

Activity.startActivityForResult -> Instrumentation.execStartActivity
-> ActivityManagerProxy.startActivity

AMS的startActivity
```java
@Override
public final int startActivity(IApplicationThread caller, String callingPackage,
        Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode,
        int startFlags, ProfilerInfo profilerInfo, Bundle bOptions) {
    return startActivityAsUser(caller, callingPackage, intent, resolvedType, resultTo,resulUserHandle.getCallingUserId());
}
```
## ActivityStarter.startActivityMayWait()

```java
final int startActivityMayWait(IApplicationThread caller, int callingUid,
            String callingPackage, Intent intent, String resolvedType,
            IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor,
            IBinder resultTo, String resultWho, int requestCode, int startFlags,
            ProfilerInfo profilerInfo, IActivityManager.WaitResult outResult, Configuration config,
            Bundle bOptions, boolean ignoreTargetSecurity, int userId,
            IActivityContainer iContainer, TaskRecord inTask) {
    ...
    ResolveInfo rInfo = mSupervisor.resolveIntent(intent, resolvedType, userId);
    ...
    ActivityInfo aInfo = mSupervisor.resolveActivity(intent, rInfo, startFlags, profilerInfo);
    ...
    final ProcessRecord heavy = mService.mHeavyWeightProcess;
    if (heavy != null && (heavy.info.uid != aInfo.applicationInfo.uid
            || !heavy.processName.equals(aInfo.processName))) {
        ...
    }
    ...
    int res = startActivityLocked(caller, intent, ephemeralIntent, resolvedType,
            aInfo, rInfo, voiceSession, voiceInteractor,
            resultTo, resultWho, requestCode, callingPid,
            callingUid, callingPackage, realCallingPid, realCallingUid, startFlags,
            options, ignoreTargetSecurity, componentSpecified, outRecord, container,
            inTask);
    ...
}
```

# 概念
* Hook：拦截某个内部流程，在其中做某些修改，以实现自己的逻辑;
* Instrumentation：每个Activity都有一个Instrumentation对象，它是在Activity启动是被赋予的Instrumentation.ActivityResult ar = mInstrumentation.execStartActivity(); 这便是startActivityForResult的启动，同时返回启动结果;
* 占坑：声明一个不存在的Activity，如:`<activity android:name=".A$1" android:launchMode="standard"/>`
，这样启动.A$1这个Activity可以欺骗系统检测，然后再将插件Activity注入到.A$1这个坑位中

# 插件化实现原理
1. 初始化Hook住Instrumentation和ActivityThread等。通过PackageParser（插件apk包信息）、AssetManager（资源文件Resources）、ClassLoader等加载一个Apk插件;

2. 启动插件Activity：提前在主APP中占有坑位，通过替换Intent中的targetActivity，打开占坑声明的A$Activity，然后绕过AndroidManifest检测，再拦截newActivity方法中恢复targetActivity;

3. 启动插件Service：通过启动一个代理Service统一管理，拦截所有Service方法，修改为startService到代理Service，在代理Service的onStartCommond统一管理，创建/停止目标service.

## hook Instrumentation
Activity的启动是通过Instrumentation实现的，而ActivityThread有公开方法getInstrumentation. ActivityThread内部有sCurrentActivityThread静态变量, 我们可以通过反射sCurrentActivityThread拿到ActivityThread.
```java
    @UiThread
    public static Object getActivityThread(Context base) {
        if (sActivityThread == null) {
            try {
                Class<?> activityThreadClazz = Class.forName("android.app.ActivityThread");
                Object activityThread = null;
                try {
                    activityThread = ReflectUtil.getField(activityThreadClazz, null, "sCurrentActivityThread");
                } catch (Exception e) {
                    // ignored
                }
                if (activityThread == null) {
                    activityThread = ((ThreadLocal<?>) ReflectUtil.getField(activityThreadClazz, null, "sThreadLocal")).get();
                }
                sActivityThread = activityThread;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return sActivityThread;
    }
```

## hook Service
通过ActivityManagerNative的getDefault，拿到AndroidManagerService(详见启动流程图)，而VirtualApk通过自定义ActivityManagerProxy，重新生成了一个IActivityManager，然后注入回AndroidManagerService中，这样接管了系统启动、管理service等操作。

```java
    private void hookSystemServices() {
        try {
            Singleton<IActivityManager> defaultSingleton =
                    (Singleton<IActivityManager>) ReflectUtil.getField(ActivityManagerNative.class, null, "gDefault");
            IActivityManager activityManagerProxy = ActivityManagerProxy.newInstance(this, defaultSingleton.get());

            // Hook IActivityManager from ActivityManagerNative
            ReflectUtil.setField(defaultSingleton.getClass().getSuperclass(), defaultSingleton, "mInstance", activityManagerProxy);

            if (defaultSingleton.get() == activityManagerProxy) {
                this.mActivityManager = activityManagerProxy;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
```

# 加载插件Apk
```java
    public void loadPlugin(File apk) throws Exception {
        if (null == apk) {
            throw new IllegalArgumentException("error : apk is null.");
        }

        if (!apk.exists()) {
            throw new FileNotFoundException(apk.getAbsolutePath());
        }

        LoadedPlugin plugin = LoadedPlugin.create(this, this.mContext, apk);
        if (null != plugin) {
            this.mPlugins.put(plugin.getPackageName(), plugin);
            // try to invoke plugin's application
            plugin.invokeApplication();
        } else {
            throw  new RuntimeException("Can't load plugin which is invalid: " + apk.getAbsolutePath());
        }
    }
```

## 启动插件Service
ActivityManagerProxy生成的IActivityManager主要拦截了Service相关启动和停止等方法，然后将其都转化为对应的startService方法，指向代理Service。因为startService方法的特性，他们最终都会在代理Service的onStartCommand中被统一处理。
```java
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("startService".equals(method.getName())) {
            try {
                return startService(proxy, method, args);
            } catch (Throwable e) {
                Log.e(TAG, "Start service error", e);
            }
        } else if ("stopService".equals(method.getName())) {
            try {
                return stopService(proxy, method, args);
            } catch (Throwable e) {
                Log.e(TAG, "Stop Service error", e);
            }
        } else if ("stopServiceToken".equals(method.getName())) {
            try {
                return stopServiceToken(proxy, method, args);
            } catch (Throwable e) {
                Log.e(TAG, "Stop service token error", e);
            }
        } else if ("bindService".equals(method.getName())) {
            try {
                return bindService(proxy, method, args);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        } else if ("unbindService".equals(method.getName())) {
            try {
                return unbindService(proxy, method, args);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        } else if ("getIntentSender".equals(method.getName())) {
            try {
                getIntentSender(method, args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("overridePendingTransition".equals(method.getName())){
            try {
                overridePendingTransition(method, args);
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        try {
            // sometimes system binder has problems.
            return method.invoke(this.mActivityManager, args);
        } catch (Throwable th) {
            Throwable c = th.getCause();
            if (c != null && c instanceof DeadObjectException) {
                // retry connect to system binder
                IBinder ams = ServiceManager.getService(Context.ACTIVITY_SERVICE);
                if (ams != null) {
                    IActivityManager am = ActivityManagerNative.asInterface(ams);
                    mActivityManager = am;
                }
            }

            Throwable cause = th;
            do {
                if (cause instanceof RemoteException) {
                    throw cause;
                }
            } while ((cause = cause.getCause()) != null);

            throw c != null ? c : th;
        }

    }
```
startService这里，主要便是提取原本目标service信息，然后转化为代理Service，发送到代理Service
```java
    private Object startService(Object proxy, Method method, Object[] args) throws Throwable {
        IApplicationThread appThread = (IApplicationThread) args[0];
        Intent target = (Intent) args[1];
        ResolveInfo resolveInfo = this.mPluginManager.resolveService(target, 0);
        if (null == resolveInfo || null == resolveInfo.serviceInfo) {
            // is host service
            return method.invoke(this.mActivityManager, args);
        }

        return startDelegateServiceForTarget(target, resolveInfo.serviceInfo, null, RemoteService.EXTRA_COMMAND_START_SERVICE);
    }

        private ComponentName startDelegateServiceForTarget(Intent target, ServiceInfo serviceInfo, Bundle extras, int command) {
        Intent wrapperIntent = wrapperTargetIntent(target, serviceInfo, extras, command);
        return mPluginManager.getHostContext().startService(wrapperIntent);
    }

    private Intent wrapperTargetIntent(Intent target, ServiceInfo serviceInfo, Bundle extras, int command) {
        // fill in service with ComponentName
        target.setComponent(new ComponentName(serviceInfo.packageName, serviceInfo.name));
        String pluginLocation = mPluginManager.getLoadedPlugin(target.getComponent()).getLocation();

        // start delegate service to run plugin service inside
        boolean local = PluginUtil.isLocalService(serviceInfo);
        Class<? extends Service> delegate = local ? LocalService.class : RemoteService.class;
        Intent intent = new Intent();
        intent.setClass(mPluginManager.getHostContext(), delegate);
        intent.putExtra(RemoteService.EXTRA_TARGET, target);
        intent.putExtra(RemoteService.EXTRA_COMMAND, command);
        intent.putExtra(RemoteService.EXTRA_PLUGIN_LOCATION, pluginLocation);
        if (extras != null) {
            intent.putExtras(extras);
        }

        return intent;
    }
```

# 资源加载

https://www.zybuluo.com/dodola/note/814116

在VirtualAPK里插件所有相关的内容都被封装到 LoadedPlugin 里，插件的加载行为一般都在这个类的构造方法的实现里,我们这里只关注与资源相关部分的代码

```java
    LoadedPlugin(PluginManager pluginManager, Context context, File apk) throws PackageParser.PackageParserException {
        //需要注意context是宿主的Context
        //apk 指的是插件的路径
        this.mResources = createResources(context, apk);
        this.mAssets = this.mResources.getAssets();
    }
        private static AssetManager createAssetManager(Context context, File apk) {
            try {
                //这里参照系统的方式生成AssetManager，并通过反射将插件的apk路径添加到AssetManager里
                //这里只适用于资源独立的情况，如果需要调用宿主资源，则需要插入到宿主的AssetManager里
                AssetManager am = AssetManager.class.newInstance();
                ReflectUtil.invoke(AssetManager.class, am, "addAssetPath", apk.getAbsolutePath());
                return am;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        @WorkerThread
        private static Resources createResources(Context context, File apk) {
            if (Constants.COMBINE_RESOURCES) {
                //如果插件资源合并到宿主里面去的情况，插件可以访问宿主的资源
                Resources resources = new ResourcesManager().createResources(context, apk.getAbsolutePath());
                ResourcesManager.hookResources(context, resources);
                return resources;
            } else {
                //插件使用独立的Resources，不与宿主有关系，无法访问到宿主的资源
                Resources hostResources = context.getResources();
                AssetManager assetManager = createAssetManager(context, apk);
                return new Resources(assetManager, hostResources.getDisplayMetrics(), hostResources.getConfiguration());
            }
        }
```

如果将宿主和插件隔离，我们只需要生成一个独立的 Resources 对象给插件使用，如果要调用宿主资源则需要将宿主的APK和插件的APK一起添加到同一个 AssetManager 里。进入到 ResourcesManager 的逻辑里

ResourcesManager.java
```java
public static synchronized Resources createResources(Context hostContext, String apk) {
        // hostContext 为宿主的Context
        Resources hostResources = hostContext.getResources();
        //获取到宿主的Resources对象
        Resources newResources = null;
        AssetManager assetManager;
        try {
            //-----begin---
            //这块的代码涉及到的内容比较多，详情见①
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                assetManager = AssetManager.class.newInstance();
                ReflectUtil.invoke(AssetManager.class, assetManager, "addAssetPath", hostContext.getApplicationInfo().sourceDir);
            } else {
                assetManager = hostResources.getAssets();
            }
            //------end----
            //------begin---
            ReflectUtil.invoke(AssetManager.class, assetManager, "addAssetPath", apk);
            List<LoadedPlugin> pluginList = PluginManager.getInstance(hostContext).getAllLoadedPlugins();
            for (LoadedPlugin plugin : pluginList) {
                ReflectUtil.invoke(AssetManager.class, assetManager, "addAssetPath", plugin.getLocation());
            }
            //------end----
            //-----begin-----
            //此处针对机型的兼容代码是可以避开的，详情见③
            if (isMiUi(hostResources)) {
                newResources = MiUiResourcesCompat.createResources(hostResources, assetManager);
            } else if (isVivo(hostResources)) {
                newResources = VivoResourcesCompat.createResources(hostContext, hostResources, assetManager);
            } else if (isNubia(hostResources)) {
                newResources = NubiaResourcesCompat.createResources(hostResources, assetManager);
            } else if (isNotRawResources(hostResources)) {
                newResources = AdaptationResourcesCompat.createResources(hostResources, assetManager);
            } else {
                // is raw android resources
                newResources = new Resources(assetManager, hostResources.getDisplayMetrics(), hostResources.getConfiguration());
            }
            //-----end-----
        } catch (Exception e) {
            e.printStackTrace();
        }
        return newResources;
    }
    public static void hookResources(Context base, Resources resources) {
        if (Build.VERSION.SDK_INT >= 24) {
            return;
        }
        try {
            ReflectUtil.setField(base.getClass(), base, "mResources", resources);
            Object loadedApk = ReflectUtil.getPackageInfo(base);
            ReflectUtil.setField(loadedApk.getClass(), loadedApk, "mResources", resources);
            Object activityThread = ReflectUtil.getActivityThread(base);
            Object resManager = ReflectUtil.getField(activityThread.getClass(), activityThread, "mResourcesManager");
            Map<Object, WeakReference<Resources>> map = (Map<Object, WeakReference<Resources>>) ReflectUtil.getField(resManager.getClass(), resManager, "mActiveResources");
            Object key = map.keySet().iterator().next();
            map.put(key, new WeakReference<>(resources));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
```

①：此处针对系统版本的区分涉及到资源加载时候的兼容性问题

由于资源做过分区，则在Android L后直接将插件包的apk地址 addAssetPath 之后就可以，但是在Android L之前，addAssetPath` 只是把补丁包加入到资源路径列表里，但是资源的解析其实是在很早的时候就已经执行完了，问题出现在这部分代码：