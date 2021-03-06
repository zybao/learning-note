http://dev.qq.com/topic/57ac4a0ea374c75371c08ce8

Android 进程拉活包括两个层面：

A. 提供进程优先级，降低进程被杀死的概率

B. 在进程被杀死后，进行拉活

# 1. 进程的优先级

Android 系统将尽量长时间地保持应用进程，但为了新建进程或运行更重要的进程，最终需要清除旧进程来回收内存。 为了确定保留或终止哪些进程，系统会根据进程中正在运行的组件以及这些组件的状态，将每个进程放入“重要性层次结构”中。 必要时，系统会首先消除重要性最低的进程，然后是清除重要性稍低一级的进程，依此类推，以回收系统资源。

进程的重要性，划分5级：

    前台进程(Foreground process)

    可见进程(Visible process)

    服务进程(Service process)

    后台进程(Background process)

    空进程(Empty process)

前台进程的重要性最高，依次递减，空进程的重要性最低，下面分别来阐述每种级别的进程:

## 1.1 前台进程 —— Foreground process

用户当前操作所必需的进程。通常在任意给定时间前台进程都为数不多。只有在内存不足以支持它们同时继续运行这一万不得已的情况下，系统才会终止它们。

A. 拥有用户正在交互的 Activity（已调用 onResume()）

B. 拥有某个 Service，后者绑定到用户正在交互的 Activity

C. 拥有正在“前台”运行的 Service（服务已调用 startForeground()）

D. 拥有正执行一个生命周期回调的 Service（onCreate()、onStart() 或 onDestroy()）

E. 拥有正执行其 onReceive() 方法的 BroadcastReceiver

## 1.2 可见进程 —— Visible process

没有任何前台组件、但仍会影响用户在屏幕上所见内容的进程。可见进程被视为是极其重要的进程，除非为了维持所有前台进程同时运行而必须终止，否则系统不会终止这些进程。

A. 拥有不在前台、但仍对用户可见的 Activity（已调用 onPause()）。

B. 拥有绑定到可见（或前台）Activity 的 Service

## 1.3 服务进程 —— Service process

尽管服务进程与用户所见内容没有直接关联，但是它们通常在执行一些用户关心的操作（例如，在后台播放音乐或从网络下载数据）。因此，除非内存不足以维持所有前台进程和可见进程同时运行，否则系统会让服务进程保持运行状态。

A. 正在运行 startService() 方法启动的服务，且不属于上述两个更高类别进程的进程。

## 1.4 后台进程 —— Background process

后台进程对用户体验没有直接影响，系统可能随时终止它们，以回收内存供前台进程、可见进程或服务进程使用。 通常会有很多后台进程在运行，因此它们会保存在 LRU 列表中，以确保包含用户最近查看的 Activity 的进程最后一个被终止。如果某个 Activity 正确实现了生命周期方法，并保存了其当前状态，则终止其进程不会对用户体验产生明显影响，因为当用户导航回该 Activity 时，Activity 会恢复其所有可见状态。

A. 对用户不可见的 Activity 的进程（已调用 Activity的onStop() 方法）

## 1.5. 空进程 —— Empty process

保留这种进程的的唯一目的是用作缓存，以缩短下次在其中运行组件所需的启动时间。 为使总体系统资源在进程缓存和底层内核缓存之间保持平衡，系统往往会终止这些进程。

A. 不含任何活动应用组件的进程

    详情参见：http://developer.android.com/intl/zh-cn/guide/components/processes-and-threads.html

# 2. Android 进程回收策略

Android 中对于内存的回收，主要依靠 Lowmemorykiller 来完成，是一种根据 OOM_ADJ 阈值级别触发相应力度的内存回收的机制。

Android 手机中进程被杀死可能有如下情况：
* 触发系统进程管理机制;
* 被第三方应用杀死（无Root）;
* 被第三方应用杀死（有Root）;
* 厂商杀进程功能;
* 用户主动强行停止进程.

# 3. 提升进程优先级的方案
## 3.1. 利用 Activity 提升权限
### 3.1.1. 方案设计思想

监控手机锁屏解锁事件，在屏幕锁屏时启动1个像素的 Activity，在用户解锁时将 Activity 销毁掉。注意该 Activity 需设计成用户无感知。

通过该方案，可以使进程的优先级在屏幕锁屏时间由4提升为最高优先级1。
### 3.1.2. 方案适用范围

适用场景： 本方案主要解决第三方应用及系统管理工具在检测到锁屏事件后一段时间（一般为5分钟以内）内会杀死后台进程，已达到省电的目的问题。

适用版本： 适用于所有的 Android 版本。

## 3.2. 利用 Notification 提升权限
### 3.2.1. 方案设计思想

Android 中 Service 的优先级为4，通过 setForeground 接口可以将后台 Service 设置为前台 Service，使进程的优先级由4提升为2，从而使进程的优先级仅仅低于用户当前正在交互的进程，与可见进程优先级一致，使进程被杀死的概率大大降低。
### 3.2.2. 方案实现挑战

从 Android2.3 开始调用 setForeground 将后台 Service 设置为前台 Service 时，必须在系统的通知栏发送一条通知，也就是前台 Service 与一条可见的通知时绑定在一起的。

对于不需要常驻通知栏的应用来说，该方案虽好，但却是用户感知的，无法直接使用。
### 3.2.3. 方案挑战应对措施

通过实现一个内部 Service，在 LiveService 和其内部 Service 中同时发送具有相同 ID 的 Notification，然后将内部 Service 结束掉。随着内部 Service 的结束，Notification 将会消失，但系统优先级依然保持为2。
### 3.2.4. 方案适用范围

适用于目前已知所有版本。

# 4. 进程死后拉活的方案
## 4.1. 利用系统广播拉活
### 4.1.1. 方案设计思想
在发生特定系统事件时，系统会发出响应的广播，通过在 AndroidManifest 中“静态”注册对应的广播监听器，即可在发生响应事件时拉活。

### 4.1.2. 方案适用范围

适用于全部 Android 平台。但存在如下几个缺点：

1） 广播接收器被管理软件、系统软件通过“自启管理”等功能禁用的场景无法接收到广播，从而无法自启。

2） 系统广播事件不可控，只能保证发生事件时拉活进程，但无法保证进程挂掉后立即拉活。

因此，该方案主要作为备用手段。
## 4.2. 利用第三方应用广播拉活
### 4.2.1. 方案设计思想

该方案总的设计思想与接收系统广播类似，不同的是该方案为接收第三方 Top 应用广播。

通过反编译第三方 Top 应用，如：手机QQ、微信、支付宝、UC浏览器等，以及友盟、信鸽、个推等 SDK，找出它们外发的广播，在应用中进行监听，这样当这些应用发出广播时，就会将我们的应用拉活。
### 4.2.2. 方案适用范围

该方案的有效程度除与系统广播一样的因素外，主要受如下因素限制：

1） 反编译分析过的第三方应用的多少

2） 第三方应用的广播属于应用私有，当前版本中有效的广播，在后续版本随时就可能被移除或被改为不外发。

这些因素都影响了拉活的效果。

## 4.3. 利用系统Service机制拉活
### 4.3.1. 方案设计思想

将 Service 设置为 START_STICKY，利用系统机制在 Service 挂掉后自动拉活

### 4.3.2. 方案适用范围

如下两种情况无法拉活：

1. Service 第一次被异常杀死后会在5秒内重启，第二次被杀死会在10秒内重启，第三次会在20秒内重启，一旦在短时间内 Service 被杀死达到5次，则系统不再拉起。

2. 进程被取得 Root 权限的管理工具或系统工具通过 forestop 停止掉，无法重启。

## 4.4. 利用Native进程拉活

### 4.4.1. 方案设计思想

**主要思想：** 利用 Linux 中的 fork 机制创建 Native 进程，在 Native 进程中监控主进程的存活，当主进程挂掉后，在 Native 进程中立即对主进程进行拉活。

**主要原理：** 在 Android 中所有进程和系统组件的生命周期受 ActivityManagerService 的统一管理。而且，通过 Linux 的 fork 机制创建的进程为纯 Linux 进程，其生命周期不受 Android 的管理。

## 4.5. 利用 JobScheduler 机制拉活
### 4.5.1. 方案设计思想

Android5.0 以后系统对 Native 进程等加强了管理，Native 拉活方式失效。系统在 Android5.0 以上版本提供了 JobScheduler 接口，系统会定时调用该进程以使应用进行一些逻辑操作。

在本项目中，我对 JobScheduler 进行了进一步封装，兼容 Android5.0 以下版本。封装后 JobScheduler 接口的使用如下：

# 5. 其他有效拉活方案

经研究发现还有其他一些系统拉活措施可以使用，但在使用时需要用户授权，用户感知比较强烈。

这些方案包括：

    利用系统通知管理权限进行拉活

    利用辅助功能拉活，将应用加入厂商或管理软件白名单。

这些方案需要结合具体产品特性来搞。

上面所有解释这些方案都是考虑的无 Root 的情况。

其他还有一些技术之外的措施，比如说应用内 Push 通道的选择：

    国外版应用：接入 Google 的 GCM。

    国内版应用：根据终端不同，在小米手机（包括 MIUI）接入小米推送、华为手机接入华为推送；其他手机可以考虑接入腾讯信鸽或极光推送与小米推送做 A/B Test。
