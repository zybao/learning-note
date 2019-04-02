## View 和 SurfaceView的区别:
1. View适用于主动更新的情况，而SurfaceView则适用于被动更新的情况，比如频繁刷新界面;
2. View在主线程中对页面进行刷新，而SurfaceView则开启一个子线程来对页面进行刷新;
3. View在绘图时没有实现双缓冲机制，SurfaceView在底层机制中就实现了双缓冲机制。

这摘录了一段网上对于双缓冲技术的介绍

`
双缓冲技术是游戏开发中的一个重要的技术   前面还没有显示完，程序又请求重新绘制，这样屏幕就会不停地闪烁。而双缓冲技术是把要处理的图片在内存中处理好之后，再将其显示在屏幕上。双缓冲主要是为了解决 反复局部刷屏带来的闪烁。把要画的东西先画到一个内存区域里，然后整体的一次性画出来。
`

# 使用SurfaceView
```java
public class SurfaceViewTemplate extends SurfaceView implements SurfaceHolder.Callback, Runnable { 
    public SurfaceViewTemplate(Context context) { 
        this(context, null); 
    }

    public SurfaceViewTemplate(Context context, AttributeSet attrs) { 
        this(context, attrs, 0); 
    }

    public SurfaceViewTemplate(Context context, AttributeSet attrs, int defStyleAttr) { 
        super(context, attrs, defStyleAttr); 
    } 
    
    @Override 
    public void surfaceCreated(SurfaceHolder holder) { 
        //创建 
    } 
    
    @Override 
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { 
        //改变 
    } 
    
    @Override 
    public void surfaceDestroyed(SurfaceHolder holder) { 
        //销毁 
    } 
    
    @Override public void run() { 
        //子线程 
    } 
}
```

`前面三个构造函数的写法和自定义View是相同的，接下来的三个方法分别在SurfaceView创建、改变、销毁的时候进行调用，最后的run()方法中写我们子线程中执行的绘图逻辑即可。`

## 初始化SurfaceView
```java
private SurfaceHolder mSurfaceHolder; 
//绘图的Canvas 
private Canvas mCanvas; 
//子线程标志位 
private boolean mIsDrawing; 
/** 
 * 初始化View 
 */ 
private void initView(){ 
    mSurfaceHolder = getHolder(); 
    //注册回调方法 
    mSurfaceHolder.addCallback(this); 
    //设置一些参数方便后面绘图 
    setFocusable(true); 
    setKeepScreenOn(true); 
    setFocusableInTouchMode(true); 
} 

public SurfaceViewSinFun(Context context, AttributeSet attrs, int defStyleAttr) { 
    super(context, attrs, defStyleAttr); 
    //在三个参数的构造方法中完成初始化操作 
    initView(); 
}
```

## 第一层缓冲

cpu访问内存的速度要远远快于访问屏幕的速度。如果我们需要在屏幕上绘制100个图形，有两种方案：第一种方案是每次从内存中读取一个图形然后将此图形绘制到屏幕上，这样的过程重复进行100次——因为需要访问100次内存和100次屏幕，可想而知这种方案是非常耗时的；第二种方案是每次从内存中读取一个图形并绘制到内存中的一个临时bitmap上，重复进行100次，然后再一次性将内存中绘制好的临时bitmap绘制到屏幕上——显然，这种方案可以节约大量的时间，因为只需要访问一次屏幕。这第二种方案就是双缓冲绘图中的第一层缓冲。

android系统View类的onDraw方法中已经实现了这第一层缓冲，这从我们以往的开发经验中就很容易发现：在对onDraw提供的canvas进行绘制时，并不是绘制一点就显示一点，而是onDraw方法中的绘制工作全部完成后，才一次性将绘制的全部内容显示到屏幕上。这里的canvas中的bitmap就相当于前面所说的“内存中的临时bitmap”。

## 第二层缓冲

* 要解决的问题

通常情况下，在自定义View中，所有的绘图工作都是在UI线程中进行的(onDraw(…)方法运行于UI线程)。如果要绘制的图像非常复杂，耗时较多，并且需要频繁绘制的话，那将会造成UI线程的长时间阻塞。结果是，系统无法及时对用户的点击、触摸等事件做出响应(系统处理点击、触摸等事件都是在UI线程中进行的)，会严重影响用户体验。

* 第二层缓冲

以绘制一个含有10000个图形(如圆形、矩形、线条等)的复杂图像为例，第二层缓冲指的是：并不直接在onDraw方法的canvas中绘制这10000个图形，而是(1)先将这10000个图形绘制到一个临时的canvas中，绘制完成之后，(2)再将此临时canvas中的内容(也就是一个bitmap)，通过canvas.drawBitmap绘制到onDraw方法的canvas中。

第二层缓冲的主要作用是可以减少绘图工作对UI线程的阻塞，其利用的原理就是上面的操作（2）要快于操作（1）（因为操作（2）相当于就是简单的bitmap拷贝）。

## [SurfaceTexture，TextureView, SurfaceView 和 GLSurfaceView 区别](https://juejin.im/entry/58d3807b44d90400685c7e9e)
* SurfaceView
它继承自类View，因此它本质上是一个View。但与普通View不同的是，它有自己的Surface。我们知道，一般的Activity包含的多个View会组成View hierachy的树形结构，只有最顶层的DecorView，也就是根结点视图，才是对WMS可见的。这个DecorView在WMS中有一个对应的WindowState。相应地，在SF中对应的Layer。而SurfaceView自带一个Surface，这个Surface在WMS中有自己对应的WindowState，在SF中也会有自己的Layer。

也就是说，虽然在App端它仍在View hierachy中，但在Server端（WMS和SF）中，它与宿主窗口是分离的。这样的好处是对这个Surface的渲染可以放到单独线程去做，渲染时可以有自己的GL context。这对于一些游戏、视频等性能相关的应用非常有益，因为它不会影响主线程对事件的响应。但它也有缺点，因为这个Surface不在View hierachy中，它的显示也不受View的属性控制，所以不能进行平移，缩放等变换，也不能放在其它ViewGroup中，一些View中的特性也无法使用。

* GLSurfaceView
作为SurfaceView的补充。它可以看作是SurfaceView的一种典型使用模式。在SurfaceView的基础上，它加入了EGL的管理，并自带了渲染线程。另外它定义了用户需要实现的Render接口，提供了用Strategy pattern更改具体Render行为的灵活性。作为GLSurfaceView的Client，只需要将实现了渲染函数的Renderer的实现类设置给GLSurfaceView即可。