http://www.jianshu.com/p/3696923aa4f7

蓝牙的功能：
* 扫描其他蓝牙设备
* 为可配对的蓝牙设备查询蓝牙适配器
* 建立RFCOMM通道（其实就是尼玛的认证）
* 通过服务搜索来链接其他的设备
* 与其他的设备进行数据传输
* 管理多个连接
蓝牙建立连接必须要求：
* 打开蓝牙
* 查找附近已配对或可用设备
* 连接设备
* 设备间数据交互

## 代码分布

`packages/apps/Bluetooth/`

蓝牙应用,主要是关于蓝牙应用协议的表现代码，包括opp、hfp、hdp、a2dp、pan等等

`frameworks/base/core/Java/android/server/`

4.2以后这个目录虽然还有，但里面代码已经转移到应用层了，就是前面那个目录，所以4.2.2上的蓝牙这里可以忽略。

`framework/base/core/java/android/bluetooth`

这个目录里的代码更像一个桥梁，里面有供java层使用一些类，也有对应的aidl文件联系C、C++部分的代码，还是挺重要的

`kernel\drivers\bluetoothBluetooth`

具体协议实现。包括hci,hid，rfcomm,sco,SDP等协议

`kernel\net\bluetooth Linux kernel`

对各种接口的Bluetoothdevice的驱动。例如：USB接口，串口等，上面kernel这两个目录有可能看不到的，但一定会有的。

`external\bluetooth\bluedroid`

官方蓝牙协议栈

`system\bluetoothBluetooth`

适配层代码，和framework那个作用类似，是串联framework与协议栈的工具。

## 关键类
`/frameworks/base/core/java/android/bluetooth/`
* BluetoothAdapter 代表本地蓝牙适配器(蓝牙发射器),是所有蓝牙交互的入口。通过它可以搜索其它蓝牙设备,查询已经配对的设备列表,通过已知的MAC地址创建BluetoothDevice,创建BluetoothServerSocket监听来自其它设备的通信。
* BluetoothDevice 代表了一个远端的蓝牙设备, 使用它请求远端蓝牙设备连接或者获取 远端蓝牙设备的名称、地址、种类和绑定状态。 (其信息是封装在 bluetoothsocket 中) 。
* BluetoothSocket 代表了一个蓝牙套接字的接口(类似于 tcp 中的套接字) ,他是应用程 序通过输入、输出流与其他蓝牙设备通信的连接点。
* BluetoothServerSocket 代表打开服务连接来监听可能到来的连接请求 (属于 server 端) , 为了连接两个蓝牙设备必须有一个设备作为服务器打开一个服务套接字。 当远端设备发起连 接连接请求的时候,并且已经连接到了的时候,Blueboothserversocket 类将会返回一个 bluetoothsocket。
* BluetoothClass 描述了一个设备的特性(profile)或该设备上的蓝牙大致可以提供哪些服务(service),但不可信。比如,设备是一个电话、计算机或手持设备;Blueboothserversocket 设备可以提供audio/telephony服务等。可以用它来进行一些UI上的提示。
* BluetoothProfile 蓝牙协议
* BluetoothHeadset 提供手机使用蓝牙耳机的支持。这既包括蓝牙耳机和免提(V1.5)模式。
* BluetoothA2dp 定义高品质的音频,可以从一个设备传输到另一个蓝牙连接。 “A2DP的”代表高级音频分配模式。
* BluetoothHealth 代表了医疗设备配置代理控制的蓝牙服务
* BluetoothHealthCallback 一个抽象类,使用实现BluetoothHealth回调。你必须扩展这个类并实现回调方法接收更新应用程序的注册状态和蓝牙通道状态的变化。
* BluetoothHealthAppConfiguration 代表一个应用程序的配置,蓝牙医疗第三方应用注册与远程蓝牙医疗设备交流。
* BluetoothProfile.ServiceListener 当他们已经连接到或从服务断开时通知BluetoothProfile IPX的客户时一个接口(即运行一个特定的配置文件,内部服务)。

`\packages\apps\Settings\src\com\android\settings\bluetooth`
* BluetoothEnabler 界面上蓝牙开启、关闭的开关就是它了，
* BluetoothSettings 主界面，用于管理配对和连接设备
* LocalBluetoothManager 提供了蓝牙API上的简单调用接口，这里只是开始。
* CachedBluetoothDevice 描述蓝牙设备的类，对BluetoothDevice的再封装
* BluetoothPairingDialog 那个配对提示的对话框

`/packages/apps/Phone/src/com/android/phone/`
BluetoothPhoneService 在phone的目录肯定和电话相关了，蓝牙接听挂断电话会用到这个

`/packages/apps/Bluetooth/src/com/android/bluetooth/`

说到这里不能不说4.2蓝牙的目录变了，在4.1及以前的代码中packages层的代码只有opp协议相关应用的代码，也就是文件传输那部分，而4.2的代码应用层的代码则丰富了许多，按具体的蓝牙应用协议来区别，分为以下文件夹（这里一并对蓝牙一些名词作个简单解释）

* btservice 这个前面AdapterService.java的描述大家应该能猜到一些，关于蓝牙基本操作的目录，一切由此开始。
    AdapterService (4.2后才有的代码)蓝牙打开、关闭、扫描、配对都会走到这里，其实更准确的说它替代了4.1之前的BluetoothService.java，原来的工作就由这个类来完成了。

* a2dp (Advanced Audio Distribution Profile)高级音频传输模式，蓝牙立体声，和蓝牙耳机听歌有关那些。
* avrcp 音频/视频远程控制配置文件，是用来听歌时暂停，上下歌曲选择的。
* hdp (Health Device Profile)蓝牙医疗设备模式，可以创建支持蓝牙的医疗设备，使用蓝牙通信的应用，例如心率监视器，血液，温度计和秤。
* hfp (Hands-free Profile)让蓝牙设备可以控制电话，如接听、挂断、拒接、语音拨号等，拒接、语音拨号要视蓝牙耳机及电话是否支持。
* pbap (Phonebook Access Profile)电话号码簿访问协议
* hid (The Human Interface Device)人机交互接口，蓝牙鼠标键盘什么的就是这个了。该协议改编自USB HID Protocol。
* opp (Object Push Profile)对象存储规范，最为常见的，文件的传输都是使用此协议。
* pan (Personal Area Network)描述了两个或更多个蓝牙设备如何构成一个即时网络，和网络有关还有串行端口功能(SPP)，拨号网络功能(DUN)

android 4.2的蓝牙应用层部分代码更丰富了，虽然有些目录还没具体代码，不过说不准哪个版本更新就有了，就像4.0添加了hdp医疗那部分一样。另外原本在framework的JNI代码也被移到packages/apps/bluetooth当中。

