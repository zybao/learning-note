## View 和 SurfaceView的区别:
1. View适用于主动更新的情况，而SurfaceView则适用于被动更新的情况，比如频繁刷新界面;
2. View在主线程中对页面进行刷新，而SurfaceView则开启一个子线程来对页面进行刷新;
3. View在绘图时没有实现双缓冲机制，SurfaceView在底层机制中就实现了双缓冲机制。

这摘录了一段网上对于双缓冲技术的介绍

`
双缓冲技术是游戏开发中的一个重要的技术。当一个动画争先显示时，程序又在改变它，前面还没有显示完，程序又请求重新绘制，这样屏幕就会不停地闪烁。而双缓冲技术是把要处理的图片在内存中处理好之后，再将其显示在屏幕上。双缓冲主要是为了解决 反复局部刷屏带来的闪烁。把要画的东西先画到一个内存区域里，然后整体的一次性画出来。
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