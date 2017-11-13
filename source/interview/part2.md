http://www.jianshu.com/p/89f19d67b348

http://www.jianshu.com/p/f0d2ed1254a9

# 阿里巴巴

    LRUCache原理

    图片加载原理

    模块化实现（好处，原因）

    JVM

    视频加密传输

    统计启动时长,标准

    如何保持应用的稳定性

    ThreadLocal 原理

    谈谈classloader

    动态布局

    热修复,插件化

    HashMap源码,SpareArray原理

    性能优化,怎么保证应用启动不卡顿

    怎么去除重复代码

    SP是进程同步的吗?有什么方法做到同步

    介绍下SurfView

    HashMap实现原理，ConcurrentHashMap 的实现原理



#    Bundle 机制

#    Handler 机制

#    android 事件传递机制

#    线程间 操作 List

#    App启动流程，从点击桌面开始

#    动态加载

#    类加载器

#    OSGI http://blog.csdn.net/xiaokui008/article/details/9662933

#    Https请求慢的解决办法，DNS，携带数据，直接访问IP

#    GC回收策略

#    画出 Android 的大体架构图

# 描述清点击 Android Studio 的 build 按钮后发生了什么

    https://www.zhihu.com/question/65289196/answer/229998562

    点击 Run 按钮，就相当于执行了一次 Gradle Task，一般来说，是assembleDebug或者assembleRelease。

    首先，点击 Run 按钮会执行一个配置，比如下图中的app就是一个配置。
    ```
    17:35:33 Executing tasks: [:app:assembleDebug]
    17:35:34 Gradle build finished in 858ms

    09/14 17:35:34: Launching app
    $ adb push /Users/didi/github/VirtualAPK/app/build/outputs/apk/app-debug.apk /data/local/tmp/com.didi.virtualapk
    $ adb shell pm install -r "/data/local/tmp/com.didi.virtualapk"
    Success

    $ adb shell am start -n "com.didi.virtualapk/com.didi.virtualapk.MainActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
    Client not ready yet..Waiting for process to come online
    Connected to process 21777 on device samsung-sm_g9500-98895a473737504e42
    ```

    结论：点击Run按钮其实依次执行了3部分内容

    * 检查项目和读取基本配置
    * Gradle Build 
    * Apk Install &  LaunchActivity 


# 大体说清一个应用程序安装到手机上时发生了什么；

# [对 Dalvik、ART 虚拟机有基本的了解](http://www.jianshu.com/p/58f817d176b7)；

    * Dalvik和JVM有啥关系？
    
    **主要区别**：
    
    Dalvik是基于寄存器的，而JVM是基于栈的。

    Dalvik运行dex文件，而JVM运行java字节码

    自Android 2.2开始，Dalvik支持JIT（just-in-time，即时编译技术）。

    优化后的Dalvik较其他标准虚拟机存在一些不同特性:　

    1. 占用更少空间　
    2. 为简化翻译，常量池只使用32位索引　　
    3.标准Java字节码实行8位堆栈指令,Dalvik使用16位指令集直接作用于局部变量。局部变量通常来自4位的“虚拟寄存器”区。这样减少了Dalvik的指令计数，提高了翻译速度。　

    当Android启动时，Dalvik VM 监视所有的程序（APK），并且创建依存关系树，为每个程序优化代码并存储在Dalvik缓存中。Dalvik第一次加载后会生成Cache文件，以提供下次快速加载，所以第一次会很慢。

    Dalvik解释器采用预先算好的Goto地址，每个指令对内存的访问都在64字节边界上对齐。这样可以节省一个指令后进行查表的时间。为了强化功能, Dalvik还提供了快速翻译器（Fast Interpreter）。

    一般来说,基于堆栈的机器必须使用指令才能从堆栈上的加载和操作数据,因此,相对基于寄存器的机器，它们需要更多的指令才能实现相同的性能。但是基于寄存器机器上的指令必须经过编码,因此,它们的指令往往更大。

    Dalvik虚拟机既不支持Java SE 也不支持Java ME类库(如：Java类,AWT和Swing都不支持)。 相反,它使用自己建立的类库（Apache Harmony Java的一个子集）。

    **什么是ART**
    
    即Android Runtime

    ART 的机制与 Dalvik 不同。在Dalvik下，应用每次运行的时候，字节码都需要通过即时编译器（just in time ，JIT）转换为机器码，这会拖慢应用的运行效率，而在ART 环境中，应用在第一次安装的时候，字节码就会预先编译成机器码，使其成为真正的本地应用。这个过程叫做预编译（AOT,Ahead-Of-Time）。这样的话，应用的启动(首次)和执行都会变得更加快速。

    ART有什么优缺点呢？

    优点：
    1. 系统性能的显著提升。
    2. 应用启动更快、运行更快、体验更流畅、触感反馈更及时。
    3. 更长的电池续航能力。
    4. 支持更低的硬件。

    缺点：
    1. 机器码占用的存储空间更大，字节码变为机器码之后，可能会增加10%-20%（不过在应用包中，可执行的代码常常只是一部分。比如最新的 Google+ APK 是 28.3 MB，但是代码只有 6.9 MB。）
    2. 应用的安装时间会变长。

    tips：现在智能手机大部分都可以让用户选择使用Dalvik还是ART模式。当然默认还是使用Dalvik模式。

    用法：设置-辅助功能-开发者选项（开发人员工具）-选择运行环境（不同的手机设置的步骤可能不一样）。




#    Android 上的 Inter-Process-Communication 跨进程通信时如何工作的；

    http://www.jianshu.com/p/b9b3051a4ff6

# App 是如何沙箱化，为什么要这么做； 
    
    http://blog.csdn.net/ljheee/article/details/53191397

    http://blog.csdn.net/jiangwei0910410003/article/details/51316688

# 权限管理系统（底层的权限是如何进行 grant 的）

    http://www.cnblogs.com/bugly/p/7344275.html

    http://blog.csdn.net/liuzhicsdn/article/details/61614632

# 进程和 Application 的生命周期；

# 系统启动流程 Zygote进程 –> SystemServer进程 –> 各种系统服务 –> 应用进程

# recycleview listview 的区别,性能

# 排序，快速排序的实现

# 树：B+树的介绍

    由于B-Tree的特性，在B-Tree中按key检索数据的算法非常直观：首先从根节点进行二分查找，如果找到则返回对应节点的data，否则对相应区间的指针指向的节点递归进行查找，直到找到节点或找到null指针，前者查找成功，后者查找失败。

    一个度为d的B-Tree，设其索引N个key，则其树高h的上限为logd((N+1)/2)，检索一个key，其查找节点个数的渐进复杂度为O(logdN)。从这点可以看出，B-Tree是一个非常有效率的索引数据结构。

    B+树：

    B-Tree有许多变种，其中最常见的是B+Tree，例如MySQL就普遍使用B+Tree实现其索引结构。

    B+树是B树的变形，它把所有的data都放在叶子结点中，只将关键字和子女指针保存于内结点，内结点完全是索引的功能。

    与B-Tree相比，B+Tree有以下不同点：

    1. 每个节点的指针上限为2d而不是2d+1。

    2. 内节点不存储data，只存储key；叶子节点存储data不存储指针。

    一般在数据库系统或文件系统中使用的B+Tree结构都在经典B+Tree的基础上进行了优化，增加了顺序访问指针。

    在B+Tree的每个叶子节点增加一个指向相邻叶子节点的指针

    例如图4中如果要查询key为从18到49的所有数据记录，当找到18后，只需顺着节点和指针顺序遍历就可以一次性访问到所有数据节点，极大提到了区间查询效率。

    为什么B树（B+树）？

    一般来说，索引本身也很大，不可能全部存储在内存中，因此索引往往以索引文件的形式存储的磁盘上。这样的话，索引查找过程中就要产生磁盘I/O消耗，相对于内存存取，I/O存取的消耗要高几个数量级，所以评价一个数据结构作为索引的优劣最重要的指标就是在查找过程中磁盘I/O操作次数的渐进复杂度。换句话说，索引的结构组织要尽量减少查找过程中磁盘I/O的存取次数。

    这涉及到磁盘存取原理、局部性原理和磁盘预读。

    先从B-Tree分析，根据B-Tree的定义，**可知检索一次最多需要访问h个节点。数据库系统的设计者巧妙利用了磁盘预读原理，将一个节点的大小设为等于一个页，这样每个节点只需要一次I/O就可以完全载入。**为了达到这个目的，在实际实现B-Tree还需要使用如下技巧：

    **每次新建节点时，直接申请一个页的空间，这样就保证一个节点物理上也存储在一个页里，加之计算机存储分配都是按页对齐的，就实现了一个node只需一次I/O。**

    **B-Tree中一次检索最多需要h-1次I/O（根节点常驻内存），渐进复杂度为O(h)=O(logdN)。一般实际应用中，出度d是非常大的数字，通常超过100，因此h非常小（通常不超过3）。**

    综上所述，用B-Tree作为索引结构效率是非常高的。

    而红黑树这种结构，h明显要深的多。由于逻辑上很近的节点（父子）物理上可能很远，无法利用局部性，所以红黑树的I/O渐进复杂度也为O(h)，效率明显比B-Tree差很多。

    至于B+Tree为什么更适合外存索引，原因和内节点出度d有关。

    由于B+Tree内节点去掉了data域，因此可以拥有更大的出度，拥有更好的性能。

# 图：有向无环图的解释

# [TCP/UDP的区别](http://blog.csdn.net/li_ning_/article/details/52117463)

# synchronized与Lock的区别

    http://blog.csdn.net/u012403290/article/details/64910926?locationNum=11&fps=1

# volatile

# Java线程池

# Java中对象的生命周期
在Java中，对象的生命周期包括以下几个阶段：

1.      创建阶段(Created)

在创建阶段系统通过下面的几个步骤来完成对象的创建过程

    * 为对象分配存储空间
    * 开始构造对象
    * 从超类到子类对static成员进行初始化
    * 超类成员变量按顺序初始化，递归调用超类的构造方法
    * 子类成员变量按顺序初始化，子类构造方法调用

一旦对象被创建，并被分派给某些变量赋值，这个对象的状态就切换到了应用阶段

2.      应用阶段(In Use)
    对象至少被一个强引用持有着。

3.      不可见阶段(Invisible)
    当一个对象处于不可见阶段时，说明程序本身不再持有该对象的任何强引用，虽然该这些引用仍然是存在着的。

    简单说就是程序的执行已经超出了该对象的作用域了。


4.      不可达阶段(Unreachable)
    对象处于不可达阶段是指该对象不再被任何强引用所持有。

    与“不可见阶段”相比，“不可见阶段”是指程序不再持有该对象的任何强引用，这种情况下，该对象仍可能被JVM等系统下的某些已装载的静态变量或线程或JNI等强引用持有着，这些特殊的强引用被称为”GC root”。存在着这些GC root会导致对象的内存泄露情况，无法被回收。

5.      收集阶段(Collected)
    当垃圾回收器发现该对象已经处于“不可达阶段”并且垃圾回收器已经对该对象的内存空间重新分配做好准备时，则对象进入了“收集阶段”。如果该对象已经重写了finalize()方法，则会去执行该方法的终端操作。

    这里要特别说明一下：不要重载finazlie()方法！原因有两点：

    1. 会影响JVM的对象分配与回收速度

    在分配该对象时，JVM需要在垃圾回收器上注册该对象，以便在回收时能够执行该重载方法；在该方法的执行时需要消耗CPU时间且在执行完该方法后才会重新执行回收操作，即至少需要垃圾回收器对该对象执行两次GC。

    2. 可能造成该对象的再次“复活”

    在finalize()方法中，如果有其它的强引用再次持有该对象，则会导致对象的状态由“收集阶段”又重新变为“应用阶段”。这个已经破坏了Java对象的生命周期进程，且“复活”的对象不利用后续的代码管理

6.      终结阶段(Finalized)
    当对象执行完finalize()方法后仍然处于不可达状态时，则该对象进入终结阶段。在该阶段是等待垃圾回收器对该对象空间进行回收。

7.      对象空间重分配阶段(De-allocated)
    垃圾回收器对该对象的所占用的内存空间进行回收或者再分配了，则该对象彻底消失了，称之为“对象空间重新分配阶段”。


#    类加载机制

#    双亲委派模型

#    Android事件分发机制

#    MVP模式

#    RxJava

#    抽象类和接口的区别

#    集合 Set实现 Hash 怎么防止碰撞

#    JVM 内存区域 开线程影响哪块内存

#    垃圾收集机制 对象创建，新生代与老年代

#    二叉树 深度遍历与广度遍历

* B树、B+树

    http://www.cnblogs.com/vincently/p/4526560.html

*    消息机制

*    进程调度

    https://www.2cto.com/kf/201606/517284.html

    http://gityuan.com/2016/08/07/android-adj/

*    进程与线程

*    死锁

*    进程状态

*    JVM内存模型

*    并发集合了解哪些

*    ConCurrentHashMap实现

* CAS介绍

    http://www.jianshu.com/p/fb6e91b013cc

* 开启线程的三种方式,run()和start()方法区别

    Java 线程一直是一个比较容易困扰的地方,首先，我们来认识下怎样生存线程。
    
    认识 Thread 和 Runnable

    java中实现多线程有两种途径：继承Thread类或者实现Runnable接口。Runnable是接口，建议用接口的方式生成线程，因为接口可以实现多继承，况且Runnable只有一个run方法，很适合继承。在使用Thread的时候只需继承Thread，并且new一个实例出来，调用 start()方法即可以启动一个线程。
```java
    Thread Test = new Thread();

    Test.start();
```
    
    在使用Runnable的时候需要先new一个实现Runnable的实例，之后启动Thread即可。
    
    ```java
    Test impelements Runnable;

    Test t = new Test();

    Thread test = new Thread(t);

    test.start();
    ```

    总结：Thread和Runnable是实现java多线程的2种方式，runable是接口，thread是类，建议使用runable实现 java多线程，不管如何，最终都需要通过thread.start()来使线程处于可运行状态。
    下面我们来谈谈本文重点，start（）和run（）方法的区别
    1） start：

    用start方法来启动线程，真正实现了多线程运行，这时无需等待run方法体代码执行完毕而直接继续执行下面的代码。通过调用Thread类的 start()方法来启动一个线程，这时此线程处于就绪（可运行）状态，并没有运行，一旦得到cpu时间片，就开始执行run()方法，这里方法 run()称为线程体，它包含了要执行的这个线程的内容，Run方法运行结束，此线程随即终止。

    2） run：

    run()方法只是类的一个普通方法而已，如果直接调用Run方法，程序中依然只有主线程这一个线程，其程序执行路径还是只有一条，还是要顺序执行，还是要等待run方法体执行完毕后才可继续执行下面的代码，这样就没有达到写线程的目的。

    总结：调用start方法方可启动线程，而run方法只是thread的一个普通方法调用，还是在主线程里执行。

    这两个方法应该都比较熟悉，把需要并行处理的代码放在run()方法中，start()方法启动线程将自动调用 run()方法，这是由jvm的内存机制规定的。并且run()方法必须是public访问权限，返回值类型为void.

* 线程池

* 常用数据结构简介

* 判断环（猜测应该是链表环）

    http://www.cnblogs.com/chengyeliang/p/4454290.html

* 排序，堆排序实现

* 链表反转
```java
// 非递归
    public ListNode reverseList(ListNode head) {
        ListNode prev = null;
        while(head!=null){
            ListNode tmp = head.next;
            head.next = prev;
            prev = head;
            head = tmp;
        }
        return prev;
    }
    
    // 递归
    public ListNode reverseList(ListNode head) {
        if(head==null||head.next ==null)
            return head;
        ListNode prev = reverseList(head.next);
        head.next.next = head;
        head.next = null;
        return prev;
    }
```

腾讯

    synchronized用法

    volatile用法

    动态权限适配方案，权限组的概念

    网络请求缓存处理，okhttp如何处理网络缓存的

    图片加载库相关，bitmap如何处理大图，如一张30M的大图，如何预防OOM

#    进程保活

    http://www.jianshu.com/p/1da4541b70ad

    http://www.sohu.com/a/68299349_355140

    listview图片加载错乱的原理和解决方案

    https相关，如何验证证书的合法性，https中哪里用了对称加密，哪里用了非对称加密，对加密算法（如RSA）等是否有了解

滴滴

    MVP

    广播（动态注册和静态注册区别，有序广播和标准广播）

    service生命周期

    handler实现机制（很多细节需要关注：如线程如何建立和退出消息循环等等）

    多线程（关于AsyncTask缺陷引发的思考）

    数据库数据迁移问题

    设计模式相关（例如Android中哪里使用了观察者模式，单例模式相关）

    x个苹果，一天只能吃一个、两个、或者三个，问多少天可以吃完

    TCP与UDP区别与应用（三次握手和四次挥手）涉及到部分细节（如client如何确定自己发送的消息被server收到） HTTP相关 提到过Websocket 问了WebSocket相关以及与socket的区别

    是否熟悉Android jni开发，jni如何调用java层代码

    进程间通信的方式

    java注解

    计算一个view的嵌套层级

    项目组件化的理解

    多线程断点续传原理

    Android系统为什么会设计ContentProvider，进程共享和线程安全问题

    jvm相关

    Android相关优化（如内存优化、网络优化、布局优化、电量优化、业务优化）

    EventBus实现原理

美团

    static synchronized 方法的多线程访问和作用，同一个类里面两个synchronized方法，两个线程同时访问的问题

    内部类和静态内部类和匿名内部类，以及项目中的应用

    handler发消息给子线程，looper怎么启动

    View事件传递

    activity栈

    封装view的时候怎么知道view的大小

    arraylist和linkedlist的区别，以及应用场景

    怎么启动service，service和activity怎么进行数据交互

    下拉状态栏是不是影响activity的生命周期，如果在onStop的时候做了网络请求，onResume的时候怎么恢复

    view渲染

今日头条

    数据结构中堆的概念，堆排序

    死锁的概念，怎么避免死锁

    ReentrantLock 、synchronized和volatile（n面）

    HashMap

    singleTask启动模式

    用到的一些开源框架，介绍一个看过源码的，内部实现过程。

    消息机制实现

    ReentrantLock的内部实现

    App启动崩溃异常捕捉

    事件传递机制的介绍

    ListView的优化

    二叉树，给出根节点和目标节点，找出从根节点到目标节点的路径

    模式MVP，MVC介绍

    断点续传的实现

    集合的接口和具体实现类，介绍

    TreeMap具体实现

    synchronized与ReentrantLock

    手写生产者/消费者模式

    逻辑地址与物理地址，为什么使用逻辑地址

    一个无序，不重复数组，输出N个元素，使得N个元素的和相加为M，给出时间复杂度、空间复杂度。手写算法

    .Android进程分类

    前台切换到后台，然后再回到前台，Activity生命周期回调方法。弹出Dialog，生命值周期回调方法。

    Activity的启动模式

爱奇艺

    RxJava的功能与原理实现

    RecycleView的使用，原理，RecycleView优化

    ANR的原因

    四大组件

    Service的开启方式

    Activity与Service通信的方式

    Activity之间的通信方式

    HashMap的实现，与HashSet的区别

    JVM内存模型，内存区域

    Java中同步使用的关键字，死锁

    MVP模式

    Java设计模式，观察者模式

    Activity与Fragment之间生命周期比较

    广播的使用场景

 百度

    Bitmap 使用时候注意什么？

    Oom 是否可以try catch ？

    内存泄露如何产生？

    适配器模式，装饰者模式，外观模式的异同？

    ANR 如何产生？

    String buffer 与string builder 的区别？

    如何保证线程安全？

    java四中引用

    Jni 用过么？

    多进程场景遇见过么？

    关于handler，在任何地方new handler 都是什么线程下

    sqlite升级，增加字段的语句

    bitmap recycler 相关

    强引用置为null，会不会被回收？

    glide 使用什么缓存？

    Glide 内存缓存如何控制大小？

* 如何保证多线程读写文件的安全？

    http://blog.csdn.net/rlanffy/article/details/26622521

携程

    Activity启动模式

    广播的使用方式，场景

    App中唤醒其他进程的实现方式

    AndroidManifest的作用与理解

    List,Set,Map的区别

    HashSet与HashMap怎么判断集合元素重复

    Java中内存区域与垃圾回收机制

    EventBus作用，实现方式，代替EventBus的方式

    Android中开启摄像头的主要步骤

网易

    集合

    concurrenthashmap

    volatile

    synchronized与Lock

    Java线程池

    wait/notify

    NIO

    垃圾收集器

    Activity生命周期

* AlertDialog,popupWindow,Activity区别

Activity像一个工匠（控制单元），Window像窗户（承载模型），View像窗花（显示视图） LayoutInflater像剪刀，Xml配置像窗花图纸。

在Activity中调用attach，创建了一个Window
创建的window是其子类PhoneWindow，在attach中创建PhoneWindow
在Activity中调用setContentView(R.layout.xxx)
其中实际上是调用的getWindow().setContentView()
调用PhoneWindow中的setContentView方法
创建ParentView：  作为ViewGroup的子类，实际是创建的DecorView(作为FramLayout的子类）
将指定的R.layout.xxx进行填充 通过布局填充器进行填充【其中的parent指的就是DecorView】
调用到ViewGroup
调用ViewGroup的removeAllView()，先将所有的view移除掉
添加新的view：addView()



小米

    String 为什么要设计成不可变的？

    fragment 各种情况下的生命周期

    Activity 上有 Dialog 的时候按 home 键时的生命周期

    横竖屏切换的时候，Activity 各种情况下的生命周期

    Application 和 Activity 的 context 对象的区别

    序列化的作用，以及 Android 两种序列化的区别。

    List 和 Map 的实现方式以及存储方式。

    静态内部类的设计意图。

    线程如何关闭，以及如何防止线程的内存泄漏

360

    软引用、弱引用区别

    垃圾回收

    多线程：怎么用、有什么问题要注意；Android线程有没有上限，然后提到线程池的上限

    JVM

    锁

    OOM，内存泄漏

    ANR怎么分析解决

    LinearLayout、RelativeLayout、FrameLayout的特性、使用场景

    如何实现Fragment的滑动

    ViewPager使用细节，如何设置成每次只初始化当前的Fragment，其他的不初始化

    ListView重用的是什么

    进程间通信的机制

    AIDL机制

    AsyncTask机制

    如何取消AsyncTask

    序列化

    Android为什么引入Parcelable

    有没有尝试简化Parcelable的使用

    AIDL机制

    项目：拉活怎么做的

    应用安装过程

某海外直播公司

    线程和进程的区别？

    为什么要有线程，而不是仅仅用进程？

    算法判断单链表成环与否？

    如何实现线程同步？

    hashmap数据结构？

    arraylist 与 linkedlist 异同？

    object类的equal 和hashcode 方法重写，为什么？

    hashmap如何put数据（从hashmap源码角度讲解）？

    简述IPC？

    fragment之间传递数据的方式？

    简述tcp四次挥手?

    threadlocal原理

    内存泄漏的可能原因？

* 用IDE如何分析内存泄漏？

    http://www.jianshu.com/p/216b03c22bb8

    OOM的可能原因？

    线程死锁的4个条件？

    差值器&估值器

    简述消息机制相关

    进程间通信方式？

    Binder相关？

    触摸事件的分发？

    简述Activity启动全部过程？

    okhttp源码？

    RxJava简介及其源码解读？

    性能优化如何分析systrace？

    广播的分类？

    点击事件被拦截，但是相传到下面的view，如何操作？

    Glide源码？

    ActicityThread相关？

 *   volatile的原理
    
    http://www.cnblogs.com/chenssy/p/6379280.html

    synchronize的原理

    lock原理

    翻转一个单项链表

    string to integer

    合并多个单有序链表（假设都是递增的）

其他公司

    四大组件

    Android中数据存储方式

    微信主页面的实现方式

    微信上消息小红点的原理

    两个不重复的数组集合中，求共同的元素。

    上一问扩展，海量数据，内存中放不下，怎么求出。

    Java中String的了解。

    ArrayList与LinkedList区别

    堆排序过程，时间复杂度，空间复杂度

    快速排序的时间复杂度，空间复杂度

    RxJava的作用，与平时使用的异步操作来比，优势

    Android消息机制原理

    Binder机制介绍

    为什么不能在子线程更新UI

    JVM内存模型

    Android中进程内存的分配，能不能自己分配定额内存

    垃圾回收机制与调用System.gc()区别

    Android事件分发机制

    断点续传的实现

    RxJava的作用，优缺点

集合框架
http://tengj.top/2016/04/12/javajhtotal/

**1. 二叉搜索树**:(Binary Search Tree又名：二叉查找树,二叉排序树)它或者是一棵空树,或者是具有下列性质的二叉树： 若它的左子树不空,则左子树上所有结点的值均小于它的根结点的值；若它的右子树不空,则右子树上所有结点的值均大于它的根结点的值；它的左、右子树也分别为二叉搜索树。

**2. RBT红黑树**

**二叉搜索树**:(Binary Search Tree又名：二叉查找树，二叉排序树)它或者是一棵空树,或者是具有下列性质的二叉树： 若它的左子树不空,则左子树上所有结点的值均小于它的根结点的值；若它的右子树不空,则右子树上所有结点的值均大于它的根结点的值；它的左、右子树也分别为二叉搜索树。

红黑树是一棵二叉搜索树，它在每个结点上增加一个存储位来表示结点的颜色，可以是RED或BLACK。通过对任何一条从根到叶子的简单路径上各个结点的颜色进行约束，红黑树没有一条路径会比其他路径长出2倍，所以红黑树是近似平衡的，使得红黑树的查找、插入、删除等操作的时间复杂度最坏为O(log n)，但需要注意到在红黑树上执行插入或删除后将不在满足红黑树性质，恢复红黑树的属性需要少量(O(log
n))的颜色变更(实际是非常快速的)和不超过三次树旋转(对于插入操作是两次)。虽然插入和删除很复杂，但操作时间仍可以保持为 O(log n) 次。具体如何保证？引出红黑树的5个性质。

红黑树的5个性质：满足以下五个性质的二叉搜索树

1. 每个结点或是红色的或是黑色的
2. 根结点是黑色的
3. 每个叶结点是黑色的
4. 如果一个结点是红色的,则它的两个子结点是黑色的
5. 对于每个结点,从该结点到其后代叶结点的简单路径上,均包含相同数目的黑色结点

插入操作：

由于性质的约束，插入的结点都是红色的。插入时性质1、3始终保持。破坏性质2当且仅当当前插入结点为根节点。变一下颜色即可。如果是破坏性质4或5，则需要旋转和变色来继续满足红黑树的性质。下面说一说插入的几种情况，约定当前插入结点为N，其父结点为P，叔叔为U，祖父为G

情形1：树空，直接插入违反性质1，将红色改黑。

情形2：N的父结点为黑，不必修改，直接插入

从情形3开始的情形假定N结点的父结点P为红色，所以存在G，并且G为黑色。且N存在一个叔叔结点U，尽管U可能为叶结点。

情形3：P为红，U为红（G结点一定存在且为黑）这里不论P是G的左孩子还是右孩子；不论N是P的左孩子还是右孩子。

首先把P、U改黑，G改红，并以G作为一个新插入的红结点重新进行各种情况的检查，若一路检索至根节点还未结束，则将根结点变黑。

情形4：P为红，U为黑或不存在（G结点一定存在且为黑），且P为G的左孩子，N为P的左孩子（或者P为G的右孩子，N为P的右孩子，保证同向的）。
P、G右旋并将P、G变相反色。因为P取代之前黑G的位置，所以P变黑可以理解，而G变红是为了不违反性质5。

情形5：P为红，U为黑或不存在，且P为G的左孩子，N为P的右孩子（或P为G的右孩子，N为P的左孩子，保证是反向的），对N，P进行一次左旋转换为情形4

删除操作比插入复杂一些，但最多不超过三次旋转可以让红黑树恢复平衡。

其他

- 黑高从某个结点x出发(不含x)到达一个叶结点的任意一条简单路径上的黑色结点个数称为该结点的黑高。红黑树的黑高为其根结点的黑高。
- 一个具有n个内部结点的红黑树的高度h<=2lg(n+1)
- 结点的属性(五元组):color key left right p
- 动态集合操作最坏时间复杂度为O(lgn)


https://blog.dreamtobe.cn/2016/03/09/oo_architecture/

http://www.iigrowing.cn/android-shi-jian-de-chu-li-mo-xing-he-duo-xian-cheng-chu-li.html

http://www.cnblogs.com/fly-fish/p/4942066.html