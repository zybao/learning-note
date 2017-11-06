http://www.mamicode.com/info-detail-1802432.html

在没有Root的情况下，Android应用流量统计在6.0之前一直没有太好的办法，官方虽然提供了
TrafficStats，但其主要功能是设备启动以来流量的统计信息，和时间信息无法很好的配合。最近再看TrafficStats类时，发现说明中提到，为获取更具鲁棒性的网络历史数据，建议使用NetworkStatsManager。

本文首先简单对比下TrafficStats和NetworkStatsManager各自的限制和优缺点，然后详细说明NetworkStatsManager的用法，并给出主要代码。

# TrafficStats

Android API8提供了android.net.TrafficStats类。
通过此类能获取设备重启以来网络信息，部分函数如下所示：
```java
static long  getMobileRxBytes()  //获取通过移动数据网络收到的字节总数
static long  getMobileTxBytes()  //通过移动数据网发送的总字节数  
static long  getTotalRxBytes()  //获取设备总的接收字节数 
static long  getTotalTxBytes()  //获取设备总的发送字节数
static long  getUidRxBytes(int uid)  //获取指定uid的接收字节数  
static long  getUidTxBytes(int uid) //获取指定uid的发送字节数 
```

通过文档及上述函数可以知道，TrafficStats能够获取设备的数据流量和总的网络流量消耗（一般情况下也就得到Wi-Fi下的流量信息）；可以查询uid对应的流量信息，而uid可以通过应用的包名查询到，因此能够查询某个应用的流量统计信息（不考虑shareuid）。非常方便的是，它的使用不需要特别的权限。另一方面它也一些限制：

1. **无法获取应用的数据流量消耗**

从文档中仅能获取到指定uid的流量，但无法区分不同网络类型下的消耗
间接方法是通过监听网络切换，做好流量记录（但是要保证你的应用一直存活，且一定准确接收到网络切换信息），基本不可用。

2. **无法获取某个时间段内的流量消耗**

从API文档中看，函数参数没有与时间相关的信息。而且重要的一点是，TrafficStats类中记录的是设备重启以来的流量统计信息。因为TrafficStats 类，底层还是读取/proc/net/xt_qtaguid/stats 对内容进行解析，将得到对应的结果返回上层。

# NetworkStatsManager
在Android 6.0（API23）中新增加的类，提供网络使用历史统计信息，同时特别强调了可查询指定时间间隔内的统计信息。看看部分函数（非静态）：

```java
//查询指定网络类型在某时间间隔内的总的流量统计信息
NetworkStats.Bucket querySummaryForDevice(int networkType, String subscriberId, long startTime, long endTime)

 //查询某uid在指定网络类型和时间间隔内的流量统计信息
NetworkStats queryDetailsForUid(int networkType, String subscriberId, long startTime, long endTime, int uid)  

//查询指定网络类型在某时间间隔内的详细的流量统计信息（包括每个uid）
NetworkStats queryDetails(int networkType, String subscriberId, long startTime, long endTime) 
```

从上述函数和文档看，NetworkStatsManager类克服了TrafficStats的查询限制，而且统计信息也不再是设备重启以来的数据。但它也有自己的限制和缺点。

(1) 权限限制

NetworkStatsManager的使用需要额外的权限，”android.permission.PACKAGE_USAGE_STATS”是系统权限，需要主动引导用户开启应用的“有权查看使用情况的应用”（使用记录访问权限）权限，后面会有代码示例。

(2) 文档不完善

不好说是文档不全，还是我没找对。首先文档中没有给出类的实例对象的构造方法，一开始还是反射获取的，后来才发现可以通过获取系统服务方式得到。另外queryDetailsForUid函数中设置的时间间隔不太有用，没能及时的获取流量统计信息，而是有两个小时的时间间隔。还好可以在querySummary函数中获得。

# 代码示例
下面说说具体的使用和代码，使用前必须明确的是这里的统计信息都是在网络层以上的数据。 

1.权限设置

（1）AndroidManifest中添加权限声明
```xml
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" tools:ignore="ProtectedPermissions"/>
```

（2）代码中主动引导用户开启权限
这里没有说明READ_PHONE_STATE的主动获取，大家根据自己的targetSdkVersion设置
```java
    private boolean hasPermissionToReadNetworkStats() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        final AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        if (mode == AppOpsManager.MODE_ALLOWED) {
            return true;
        }

        requestReadNetworkStats();
        return false;
    }

    // 打开“有权查看使用情况的应用”页面
    private void requestReadNetworkStats() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivity(intent);
    }
```

2. 查看设备和某应用的流量统计

（1）获取NetworkStatsManager示例对象
```java
NetworkStatsManager networkStatsManager = (NetworkStatsManager) getSystemService(NETWORK_STATS_SERVICE);
```

（2）查询设备总的流量统计信息
```java
NetworkStats.Bucket bucket = null;
// 获取到目前为止设备的Wi-Fi流量统计
bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_WIFI, "", 0, System.currentTimeMillis());
Log.i("Info", "Total: " + (bucket.getRxBytes() + bucket.getTxBytes()));
```

（3）查询某应用（uid）的数据流量统计信息
```java
// 获取subscriberId
TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
String subId = tm.getSubscriberId();

NetworkStats summaryStats;
long summaryRx = 0;
long summaryTx = 0;
NetworkStats.Bucket summaryBucket = new NetworkStats.Bucket();
long summaryTotal = 0;

summaryStats = networkStatsManager.querySummary(ConnectivityManager.TYPE_MOBILE, subId, getTimesMonthmorning(), System.currentTimeMillis());
do {
    summaryStats.getNextBucket(summaryBucket);
    int summaryUid = summaryBucket.getUid();
    if (uid == summaryUid) {
        summaryRx += summaryBucket.getRxBytes();
        summaryTx += summaryBucket.getTxBytes();
    }
    Log.i(MainActivity.class.getSimpleName(), "uid:" + summaryBucket.getUid() + " rx:" + summaryBucket.getRxBytes() +
" tx:" + summaryBucket.getTxBytes());
    summaryTotal += summaryBucket.getRxBytes() + summaryBucket.getTxBytes();
} while (summaryStats.hasNextBucket());
```

3. 附赠实用函数

（1）应用包名查uid
```java
NetworkStats summaryStats;
long summaryRx = 0;
long summaryTx = 0;
NetworkStats.Bucket summaryBucket = new NetworkStats.Bucket();
long summaryTotal = 0;

summaryStats = networkStatsManager.querySummary(ConnectivityManager.TYPE_MOBILE, subId, getTimesMonthmorning(), System.currentTimeMillis());
do {
    summaryStats.getNextBucket(summaryBucket);
    int summaryUid = summaryBucket.getUid();
    if (uid == summaryUid) {
        summaryRx += summaryBucket.getRxBytes();
        summaryTx += summaryBucket.getTxBytes();
    }
    Log.i(MainActivity.class.getSimpleName(), "uid:" + summaryBucket.getUid() + " rx:" + summaryBucket.getRxBytes() +
" tx:" + summaryBucket.getTxBytes());
    summaryTotal += summaryBucket.getRxBytes() + summaryBucket.getTxBytes();
} while (summaryStats.hasNextBucket());
```
（2）获得本月第一天0点时间
```java
public static long getTimesMonthMorning() {
        Calendar cal = Calendar.getInstance();
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
        return cal.getTimeInMillis();
    }
```
4. 提示无权限信息
```java
15:39:06.531 5276-5276/cn.arainfo.test.android.testapp1 E/AndroidRuntime: FATAL EXCEPTION: main
Process: cn.arainfo.test.android.testapp1, PID: 5276
java.lang.SecurityException: Network stats history of uid 10145 is forbidden for caller 10144
    at android.os.Parcel.readException(Parcel.java:1665)
    at android.os.Parcel.readException(Parcel.java:1618)
    at android.net.INetworkStatsSession$Stub$Proxy.getHistoryIntervalForUid(INetworkStatsSession.java:425)
    at android.app.usage.NetworkStats.startHistoryEnumeration(NetworkStats.java:433)
    at android.app.usage.NetworkStatsManager.queryDetailsForUidTag(NetworkStatsManager.java:254)
    at android.app.usage.NetworkStatsManager.queryDetailsForUid(NetworkStatsManager.java:219)
```