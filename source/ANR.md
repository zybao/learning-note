 ANR一般有三种类型：

1. KeyDispatchTimeout(5 seconds) --主要类型

    按键或触摸事件在特定时间内无响应

2. BroadcastTimeout(10 seconds)

    BroadcastReceiver在特定时间内无法处理完成

3. ServiceTimeout(20 seconds) --小概率类型

    Service在特定的时间内无法处理完成

Akey or touch event was not dispatched within the specified time（按键或触摸事件在特定时间内无响应）

具体的超时时间的定义在framework下的

ActivityManagerService.java

//How long we wait until we timeout on key dispatching.

staticfinal int KEY_DISPATCHING_TIMEOUT = 5*1000

超时时间的计数一般是从按键分发给app开始。超时的原因一般有两种：

(1)当前的事件没有机会得到处理（即UI线程正在处理前一个事件，没有及时的完成或者looper被某种原因阻塞住了）

(2)当前的事件正在处理，但没有及时完成


* 如何避免KeyDispatchTimeout

1. UI线程尽量只做跟UI相关的工作

2. 耗时的工作（比如数据库操作，I/O，连接网络或者别的有可能阻碍UI线程的操作）把它放入单独的线程处理

3. 尽量用Handler来处理UIthread和别的thread之间的交互