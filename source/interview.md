* Explain Activity and Fragment lifecycle. (Complete diagram [GitHub](https://github.com/xxv/android-lifecycle), simplified diagram for [Activity](https://developer.android.com/guide/components/activities/activity-lifecycle.html#alc), [Fragment](https://developer.android.com/guide/components/fragments.html#Lifecycle))
* Tell all the Android application components.
* Service vs IntentService. [StackOverflow](https://stackoverflow.com/a/15772151/5153275)
* What is the structure of an Android Application?
* How to persist data in an Android app?
* How would you perform a long-running operation in an application?
* How would you communicate between two Fragments?
* Explain Android notification system?
* How can two distinct Android apps interact?
* What is Fragment?
#  Why is it recommended to use only the default constructor to create a fragment? [StackOverflow](https://stackoverflow.com/a/16042750/2809326)

The reason why you should be passing parameters through bundle is because when the system restores a fragment (e.g on config change), it will automatically restore your bundle.

The callbacks like onCreate or onCreateView should read the parameters from the bundle - this way you are guaranteed to restore the state of the fragment correctly to the same state the fragment was initialised with (note this state can be different from the onSaveInstanceState bundle that is passed to the onCreate/onCreateView)

The recommendation of using the static newInstance() method is just a recommendation. You can use a non default constructor but make sure you populate the initialisation parameters in the bundle inside the body of that constructor. And read those parameters in the onCreate() or onCreateView() methods.

# Why Bundle class is used for data passing and why cannot we use simple Map data structure

* Bundle内部是由ArrayMap实现的，ArrayMap的内部实现是两个数组，一个int数组是存储对象数据对应下标，一个对象数组保存key和value，内部使用二分法对key进行排序，所以在添加、删除、查找数据的时候，都会使用二分法查找，只适合于小数据量操作，如果在数据量比较大的情况下，那么它的性能将退化。而HashMap内部则是数组+链表结构，所以在数据量较少的时候，HashMap的Entry Array比ArrayMap占用更多的内存。因为使用Bundle的场景大多数为小数据量，我没见过在两个Activity之间传递10个以上数据的场景，所以相比之下，在这种情况下使用ArrayMap保存数据，在操作速度和内存占用上都具有优势，因此使用Bundle来传递数据，可以保证更快的速度和更少的内存占用。
* 另外一个原因，则是在Android中如果使用Intent来携带数据的话，需要数据是基本类型或者是可序列化类型，HashMap使用Serializable进行序列化，而Bundle则是使用Parcelable进行序列化。而在Android平台中，更推荐使用Parcelable实现序列化，虽然写法复杂，但是开销更小，所以为了更加快速的进行数据的序列化和反序列化，系统封装了Bundle类，方便我们进行数据的传输。

参考资料：[Android内存优化](http://www.codes51.com/article/detail_163576.html)

# What is Dialog in Android?
# What is View in Android?
# Can you create custom views? How?
# What are ViewGroups and how they are different from the Views?
# What is the difference between a fragment and an activity? Explain the relationship between the two.
# What is the difference between Serializable and Parcelable? Which is the best approach in Android?
# What are "launch modes"? [Mindorks](https://blog.mindorks.com/android-activity-launchmode-explained-cbc6cf996802)
# What are Intents? [StackOverflow](https://stackoverflow.com/questions/6578051/what-is-an-intent-in-android)
# What is an Implicit Intent?
# What is an Explicit Intent?
* What is an `AsyncTask`?
* What is a `BroadcastReceiver`? [StackOverflow](https://stackoverflow.com/questions/5296987/what-is-broadcastreceiver-and-when-we-use-it)
* What is a `LocalBroadcastManager`? [Developer Android](https://developer.android.com/reference/android/support/v4/content/LocalBroadcastManager.html)
* What is a JobScheduler? [Vogella](http://www.vogella.com/tutorials/AndroidTaskScheduling/article.html)
* What is DDMS and what can you do with it?
* What is the support library? Why was it introduced?[MartianCraft](http://martiancraft.com/blog/2015/06/android-support-library/)
* What is a ContentProvider and what is it typically used for?
* What is Android Data Binding? [Developer Android](https://developer.android.com/topic/libraries/data-binding/index.html)
* What are Android Architecture Components? [Developer Android](https://developer.android.com/topic/libraries/architecture/index.html)
* What is ADB?
* What is ANR? How can the ANR be prevented?
* What is `AndroidManifest.xml`?
* Describe how broadcasts and intents work to be able to pass messages around your app?
* How do you handle `Bitmaps` in Android as it takes too much memory?
* What are different ways to store data in your Android app?
* What is the Dalvik Virtual Machine?
* What is the relationship between the life cycle of an AsyncTask and an Activity? What problems can this result in? How can these problems be avoided?
* What is the function of an intent filter?
* What is a Sticky Intent? [AndroidInterview](http://www.androidinterview.com/what-is-a-sticky-intent/)
* What is AIDL? Enumerate the steps in creating a bounded service through AIDL.
* What are the different protection levels in permission?
* How would you preserve Activity state during a screen rotation? [StackOverflow](https://stackoverflow.com/questions/3915952/how-to-save-state-during-orientation-change-in-android-if-the-state-is-made-of-m)
* Relative Layout vs Linear Layout.
* How to implement XML namespaces?
* Difference between `View.GONE` and `View.INVISIBLE`?
* What is the difference between a regular bitmap and a nine-patch image?
* Tell about the bitmap pool. [Mindorks](https://blog.mindorks.com/how-to-use-bitmap-pool-in-android-56c71a55533c)
* How to avoid memory leaks in Android?
* What are widgets on Home-Screen in Android?
* What is AAPT?
* How do you find memory leaks in Android applications?
* How do you troubleshoot a crashing application?
* Why should you avoid to run non-ui code on the main thread?
* How did you support different types of resolutions?
* What is Doze? What about App Standby?
* What can you use for background processing in Android?
* What is ORM? How does it work?
* What is a Loader?
* What is the NDK and why is it useful?
* What is the StrictMode? [Mindorks](https://blog.mindorks.com/use-strictmode-to-find-things-you-did-by-accident-in-android-development-4cf0e7c8d997)
* What is Lint? What is it used for?
* What is a `SurfaceView`?
* What is the difference between `ListView` and `RecyclerView`?
* What is the ViewHolder pattern? Why should we use it?
* What is a PendingIntent?
* Can you manually call the Garbage collector?
* What is the best way to update the screen periodically?
* What are the different types of Broadcasts?
* Have you developed widgets? Describe. [Mindorks](https://blog.mindorks.com/android-widgets-ad3d166458d3)
* What is Context? How is it used? [Medium](https://medium.com/p/understanding-context-in-android-application-330913e32514)
* Do you know what is the view tree? How can you optimize its depth?
* What is the `onTrimMemory` method?
* Is it possible to run an Android app in multiple processes? How?
* How does the OutOfMemory happens?
* What is a `spannable`?
* What is `overdraw`? [Developer Android](https://developer.android.com/topic/performance/rendering/overdraw.html)
* What is renderscript? [Mindorks](https://blog.mindorks.com/comparing-android-ndk-and-renderscript-1a718c01f6fe)
* What are the differences between Dalvik and ART?
* FlatBuffers vs JSON. [Mindorks](https://blog.mindorks.com/why-consider-flatbuffer-over-json-2e4aa8d4ed07)
* What are Annotations? [Mindorks](https://blog.mindorks.com/creating-custom-annotations-in-android-a855c5b43ed9), [Link](https://blog.mindorks.com/improve-your-android-coding-through-annotations-26b3273c137a)
* Tell about Constraint Layout [Mindorks](https://blog.mindorks.com/using-constraint-layout-in-android-531e68019cd)
* `HashMap`, `ArrayMap` and `SparseArray` [Mindorks](https://blog.mindorks.com/android-app-optimization-using-arraymap-and-sparsearray-f2b4e2e3dc47)
* Explain Looper, Handler and HandlerThread. [Mindorks](https://blog.mindorks.com/android-core-looper-handler-and-handlerthread-bd54d69fe91a)
* How to reduce battery usage in an android application? [Mindorks](https://blog.mindorks.com/battery-optimization-for-android-apps-f4ef6170ff70)
* What is `SnapHelper`? [Mindorks](https://blog.mindorks.com/using-snaphelper-in-recyclerview-fc616b6833e8)
* How to handle multi-touch in android [GitHub](https://arjun-sna.github.io/android/2016/07/20/multi-touch-android/)