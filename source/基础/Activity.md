

手机应用的大多数情况下我们只能在手机上看到一个程序的一个界面，用户除了通过程序界面上的功能按钮来在不同的窗体间切换，还可以通过Back键和 Home键来返回上一个窗口，而用户使用Back或者Home的时机是非常不确定的，任何时候用户都可以使用Home或Back来强行切换当前的界面。

HOME键的执行顺序：onPause->onStop->onRestart->onStart->onResume

BACK键的顺序： onPause->onStop->onDestroy->onCreate->onStart->onResume

onPause不要做太耗时的工作

    各种方法的详解
    1. void onCreate(Bundle savedInstanceState) 

    当Activity被第首次加载时执行。我们新启动一个程序的时候其主窗体的onCreate事件就会被执行。如果Activity被销毁后 （onDestroy后），再重新加载进Task时，其onCreate事件也会被重新执行。注意这里的参数 savedInstanceState（Bundle类型是一个键值对集合，大家可以看成是.Net中的Dictionary）是一个很有用的设计，由于 前面已经说到的手机应用的特殊性，一个Activity很可能被强制交换到后台（交换到后台就是指该窗体不再对用户可见，但实际上又还是存在于某个 Task中的，比如一个新的Activity压入了当前的Task从而“遮盖”住了当前的 Activity，或者用户按了Home键回到桌面，又或者其他重要事件发生导致新的Activity出现在当前Activity之上，比如来电界面）， 而如果此后用户在一段时间内没有重新查看该窗体（Android通过长按Home键可以选择最近运行的6个程序，或者用户直接再次点击程序的运行图标，如 果窗体所在的Task和进程没有被系统销毁，则不用重新加载Process, Task和Task中的Activity，直接重新显示Task顶部的Activity，这就称之为重新查看某个程序的窗体），该窗体连同其所在的 Task和Process则可能已经被系统自动销毁了，此时如果再次查看该窗体，则要重新执行 onCreate事件初始化窗体。而这个时候我们可能希望用户继续上次打开该窗体时的操作状态进行操作，而不是一切从头开始。例如用户在编辑短信时突然来 电，接完电话后用户又去做了一些其他的事情，比如保存来电号码到联系人，而没有立即回到短信编辑界面，导致了短信编辑界面被销毁，当用户重新进入短信程序 时他可能希望继续上次的编辑。这种情况我们就可以覆写Activity的void onSaveInstanceState(Bundle outState)事件，通过向outState中写入一些我们需要在窗体销毁前保存的状态或信息，这样在窗体重新执行onCreate的时候，则会通过 savedInstanceState将之前保存的信息传递进来，此时我们就可以有选择的利用这些信息来初始化窗体，而不是一切从头开始。 

    2. void onStart() 

    onCreate事件之后执行。或者当前窗体被交换到后台后，在用户重新查看窗体前已经过去了一段时间，窗体已经执行了onStop事件，但是窗 体和其所在进程并没有被销毁，用户再次重新查看窗体时会执行onRestart事件，之后会跳过onCreate事件，直接执行窗体的onStart事 件。 

    3. void onResume() 

    onStart事件之后执行。或者当前窗体被交换到后台后，在用户重新查看窗体时，窗体还没有被销毁，也没有执行过onStop事件（窗体还继续存在于Task中），则会跳过窗体的onCreate和onStart事件，直接执行onResume事件。 

    4. void onPause() 

    窗体被交换到后台时执行。 

    5. void onStop() 

    onPause事件之后执行。如果一段时间内用户还没有重新查看该窗体，则该窗体的onStop事件将会被执行；或者用户直接按了Back键，将该窗体从当前Task中移除，也会执行该窗体的onStop事件。 

    6. void onRestart() 

    onStop事件执行后，如果窗体和其所在的进程没有被系统销毁，此时用户又重新查看该窗体，则会执行窗体的onRestart事件，onRestart事件后会跳过窗体的onCreate事件直接执行onStart事件。 

    7. void onDestroy() 

    Activity被销毁的时候执行。在窗体的onStop事件之后，如果没有再次查看该窗体，Activity则会被销毁。 

    最后用一个实际的例子来说明Activity的各个生命周期。假设有一个程序由2个Activity A和B组成，A是这个程序的启动界面。当用户启动程序时，Process和默认的Task分别被创建，接着A被压入到当前的Task中，依次执行了 onCreate, onStart, onResume事件被呈现给了用户；此时用户选择A中的某个功能开启界面B，界面B被压入当前Task遮盖住了A，A的onPause事件执行，B的 onCreate, onStart, onResume事件执行，呈现了界面B给用户；用户在界面B操作完成后，使用Back键回到界面A，界面B不再可见，界面B的onPause, onStop, onDestroy执行，A的onResume事件被执行，呈现界面A给用户。此时突然来电，界面A的onPause事件被执行，电话接听界面被呈现给用 户，用户接听完电话后，又按了Home键回到桌面，打开另一个程序“联系人”，添加了联系人信息又做了一些其他的操作，此时界面A不再可见，其 onStop事件被执行，但并没有被销毁。此后用户重新从菜单中点击了我们的程序，由于A和其所在的进程和Task并没有被销毁，A的onRestart 和onStart事件被执行，接着A的onResume事件被执行，A又被呈现给了用户。用户这次使用完后，按Back键返回到桌面，A的 onPause, onStop被执行，随后A的onDestroy被执行，由于当前Task中已经没有任何Activity，A所在的Process的重要程度被降到很 低，很快A所在的Process被系统结束
    常见的例子

 

情形一、一个单独的Activity的正常的生命过程是这样的：onCreate->onStart->onPause->onStop->onDestroy。例如：运行一个Activity，进行了一些简单操作（不涉及页面的跳转等），然后按返回键结束。

 

 

 

情形二、有两个Activity（a和b），一开始显示a，然后由a启动b，然后在由b回到a，这时候a的生命过程应该是怎么样的呢（a被b完全遮盖）？

 

a经历的过程为onCreate->onStart->onResume->onPause->onStop->onRestart->onStart->onResume。这个过程说明了图中，如果Activity完全被其他界面遮挡时，进入后台，并没有完全销毁,而是停留在onStop状态，当再次进入a时，onRestart->onStart->onResume，又重新恢复。

 

 

 

情形三、基本情形同二一样，不过此时a被b部分遮盖（比如给b添加个对话框主题 android:theme="@android:style/Theme.Dialog"）

 

a经历的过程是：onCreate->onStart->onResume->onPause->onResume

 

所以当Activity被部分遮挡时，Activity进入onPause，并没有进入onStop，从Activity2返回后，执行了onResume

 

 

 

情形四、 打开程序，启动a，点击a，启动AlertDialog，按返回键从AlertDialog返回。

 

a经历的过程是：onCreate->onStart->onResume

 

当启动和退出Dialog时，Activity的状态始终未变，可见，Dialog实际上属于Acitivity内部的界面，不会影响Acitivty的生命周期。