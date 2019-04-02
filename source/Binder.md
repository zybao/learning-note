http://blog.csdn.net/happylishang/article/details/51784834

https://www.jianshu.com/p/adaa1a39a274

Binder是Android系统中大量使用的IPC（Inter-process communication，进程间通讯）机制。无论是应用程序对系统服务的请求，还是应用程序自身提供对外服务，都需要使用到Binder。

在Unix/Linux环境下，传统的IPC机制包括：

* 管道
* 消息队列
* 共享内存
* 信号量
* Socket
等。

Binder相较于传统IPC来说更适合于Android系统，具体原因的包括如下三点：

* Binder本身是C/S架构的，这一点更符合Android系统的架构
* 性能上更有优势：管道，消息队列，Socket的通讯都需要两次数据拷贝，而Binder只需要一次。要知道，对于系统底层的IPC形式，少一次数据拷贝，对整体性能的影响是非常之大的
* 安全性更好：传统IPC形式，无法得到对方的身份标识（UID/GID)，而在使用Binder IPC时，这些身份标示是跟随调用过程而自动传递的。Server端很容易就可以知道Client端的身份，非常便于做安全检查

# 整体架构

Binder整体架构如下所示：
<div align=center>
<img src="imges/Binder_Architecture.png" width = "80%" alt="图片名称" align=center />
</div>