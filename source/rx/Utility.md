# Delay
顾名思义，Delay操作符就是让发射数据的时机延后一段时间，这样所有的数据都会依次延后一段时间发射。在Rxjava中将其实现为Delay和DelaySubscription。不同之处在于Delay是延时数据的发射，而DelaySubscription是延时注册Subscriber。

# Do
Do操作符就是给Observable的生命周期的各个阶段加上一系列的回调监听，当Observable执行到这个阶段的时候，这些回调就会被触发。在Rxjava实现了很多的doxxx操作符。
DoOnEach可以给Observable加上这样的样一个回调：Observable每发射一个数据的时候就会触发这个回调，不仅包括onNext还包括onError和onCompleted。

# Meterialize
Meterialize操作符将OnNext/OnError/OnComplete都转化为一个Notification对象并按照原来的顺序发射出来

# Timeout
Timeout操作符给Observable加上超时时间，每发射一个数据后就重置计时器，当超过预定的时间还没有发射下一个数据，就抛出一个超时的异常。Rxjava将Timeout实现为很多不同功能的操作符，比如说超时后用一个备用的Observable继续发射数据等。