* Launcher的启动流程:

Zygote进程 –> SystemServer进程 –> startOtherService方法 –> ActivityManagerService的systemReady方法 –> startHomeActivityLocked方法 –> ActivityStackSupervisor的startHomeActivity方法 –> 执行Activity的启动逻辑，执行scheduleResumeTopActivities()方法

* Android的FrameWork的启动流程

android 中运行的第一个Dalvik虚拟机叫做zygote，而这个zygote的虚拟机是如何启动的呢，是一个系统提供的一个app_process进程，这个进程会执行两个java类，一个是ZygoteInit.java，一个是SystemServer.java。当app_process进程启动时，会启动一个Dalvik虚拟机，这个虚拟机第一个执行的java类就是ZygoteInit.java，而这个类第一个做的事就是启动一个Socket服务器端口，该端口用于接收创建新进程的命令。在Socket服务器端口启动完毕后，就先后开始加载加载preload-classes和preload-resources。加载这些资源是在preloadResource（）函数中完成的，该函数调用preloadDrawable（）和preloadColorStateLists（）加载这两类资源，原理就是把这些资源读出来放到一个全局变量中，只要该类对象不被销毁，这些全局变量就会一直保存。（zygote是在app_process进程中）

当这些东西都完成后zygote进程就开始创建一个新的进程SystemService。SystemServer会开始中创建了一个Socket客户端，之后所有的Dalvik进程都将通过该Socket客户端间接被启动。然后创建一个ServiceManager，ServiceManager就开始启动各种系统服务。如，启动的Ams负责管理这个Socket客户端。如果需要启动新的APK进程时，Ams会通过该Socket客户端向zygote进程的Socket服务端发送一个启动命令，然后zygote会孵化出新的进程。

ServiceManager启动的系统服务都是以一个线程的方式存在与SystemService进程中的。

# Binder 框架
Binder机制中是有四个组件Client、Server、ServiceManager和BinderDriver。

Client、Server和ServiceManager实现在用户空间中，Binder驱动程序实现在内核空间中，Binder驱动程序和ServiceManager在Android平台中已经实现，开发者只需要在用户空间实现自己的Client和Server。

Client，Server，ServiceManager是在不同的进程中的。

Client：服务调用者，一般就是我们应用开发者。

Server: 服务提供者。

ServiceManager ：绝大多数服务都是通过ServiceManager来获取，通过它来屏蔽对其他Server的直接操作。是Binder进程间通信机制的守护进程，其进程里面有系统服务如（AMS,WMS等等）但是自定义服务去是在自己指定的进程中。

## ServiceManager

ServiceManager是在init（这个init进程也就是上文所说SystemService）进程启动过后启动，用来管理系统中的Service。

SystemService类 在其main函数里面有一个init1（args）函数，其定义为native public static void init1(String[]args);它的内部会进行一些与Dalvik虚拟机相关的初始化工作，执行完初始化工作后，其内部会调用java端的SystemServer类的init2()函数。该函数实现创建和启动ServerThread线程。在这个线程里面将一些系统服务创建后添加至ServiceManager里面

```java
    /**
     * Place a new @a service called @a name into the service
     * manager.
     *
     * @param name the name of the new service
     * @param service the service object
     */
    public static void addService(String name, IBinder service) {
        // pass
    }
```
从上面片段源码可以看出，当ServerThread 线程添加Service的时候调用addService方法，以Service名和Service对象存储进去。当要获取Service的时候就是以Service名进行获取。
```java
    /**
     * Returns a reference to a service with the given name.
     *
     * @param name the name of the service to get
     * @return a reference to the service, or <code>null</code> if the service doesn't exist
     */
    public static IBinder getService(String name) {
        return null;
    }
```

应用和service之间的通信会涉及到2次binder通信。

1. 应用向SM查询service是否存在，如果存在获得该service的代理binder，此为一次binder通信；
2. 应用通过代理binder调用service的方法，此为第二次binder通信。

当Client调用Context.bindService方法，ServiceManager就会开启一个服务，并调用此服务的onBinder方法，这个方法会返回一个IBinder对象。然后ServiceManager会收到这个IBinder对象生成一个BinderProx对象（此对象为Binder的一个内部类）通过ServiceConnection的onServiceConnected方法返回给了Client。就可以进行进程间通信了。