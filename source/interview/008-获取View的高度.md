# [获取view的高度](https://stackoverflow.com/questions/3591784/getwidth-and-getheight-of-view-returns-0/24035591#24035591)
onCreate中View.getWidth和View.getHeight无法获得一个view的高度和宽度，这是因为View组件布局要在onResume回调后完成。
## OnGlobalLayoutListener
我们可以使用getViewTreeObserver().addOnGlobalLayoutListener()来获得宽度或者高度。

OnGlobalLayoutListener 是ViewTreeObserver的内部类，当一个视图树的布局发生改变时，可以被ViewTreeObserver监听到，这是一个注册监听视图树的观察者(observer)，在视图树的全局事件改变时得到通知。ViewTreeObserver不能直接实例化，而是通过getViewTreeObserver()获得。
```java
private int mHeaderViewHeight;
private View mHeaderView;

mHeaderView.getViewTreeObserver().addOnGlobalLayoutListener(
    new OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {

            mHeaderViewHeight = mHeaderView.getHeight();
            mHeaderView.getViewTreeObserver()
                    .removeGlobalOnLayoutListener(this);
        }
});
```

但是需要注意的是OnGlobalLayoutListener可能会被多次触发，因此在得到了高度之后，要将OnGlobalLayoutListener注销掉。另外mHeaderViewHeight和mHeaderView都需要写在当前java文件类（比如Activity）的成员变量中。不能直接在onCreate中定义否则会编译不通过.

除了OnGlobalLayoutListener ，ViewTreeObserver还有如下内部类：

* `ViewTreeObserver.OnGlobalFocusChangeListener`
当在一个视图树中的焦点状态发生改变时，所要调用的回调函数的接口类

* `ViewTreeObserver.OnGlobalLayoutListener`
当在一个视图树中全局布局发生改变或者视图树中的某个视图的可视状态发生改变时，所要调用的回调函数的接口类

* `ViewTreeObserver.OnPreDrawListener`
当一个视图树将要绘制时，所要调用的回调函数的接口类

* `ViewTreeObserver.OnScrollChangedListener`
当一个视图树中的一些组件发生滚动时，所要调用的回调函数的接口类

* `ViewTreeObserver.OnTouchModeChangeListener`
当一个视图树的触摸模式发生改变时，所要调用的回调函数的接口类

## 使用 View.measure 测量 View
该方法测量的宽度和高度可能与视图绘制完成后的真实的宽度和高度不一致

```java
int width = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED); 
int height = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED); 
view.measure(width, height); view.getMeasuredWidth(); // 获取宽度 
view.getMeasuredHeight(); // 获取高度
```

## ViewTreeObserver. OnPreDrawListener
在视图将要绘制时调用该监听事件，会被调用多次，因此获取到视图的宽度和高度后要移除该监听事件
```java
view.getViewTreeObserver().addOnPreDrawListener( 
    new ViewTreeObserver.OnPreDrawListener() { 
        @Override 
        public boolean onPreDraw() { 
            view.getViewTreeObserver().removeOnPreDrawListener(this); 
            view.getWidth(); // 获取宽度 
            view.getHeight(); // 获取高度 
            return true; 
        } 
    });
```

## 重写 View 的 onSizeChanged 方法
在视图的大小发生改变时调用该方法，会被多次调用，因此获取到宽度和高度后需要考虑禁用掉代码。
该实现方法需要继承 View，且多次被调用，不建议使用。
```java
@Override 
protected void onSizeChanged(int w, int h, int oldw, int oldh) { 
    super.onSizeChanged(w, h, oldw, oldh); 
    view.getWidth(); // 获取宽度 
    view.getHeight(); // 获取高度 
}
```

## 重写 View 的 onLayout 方法
该方法会被多次调用，获取到宽度和高度后需要考虑禁用掉代码。
该实现方法需要继承 View，且多次被调用，不建议使用。
```java
@Override 
protected void onLayout(boolean changed, int l, int t, int r, int b) { 
    super.onLayout(changed, l, t, r, b); 
    view.getWidth(); // 获取宽度 
    view.getHeight(); // 获取高度 
}
```

## View.OnLayoutChangeListener (API >= 11)
在视图的 layout 改变时调用该事件，会被多次调用，因此需要在获取到视图的宽度和高度后执行 remove 方法移除该监听事件。
```java
view.addOnLayoutChangeListener( 
    new View.OnLayoutChangeListener() { 
        @Override 
        public void onLayoutChange(View v, int l, int t, int r, int b,
            int oldL, int oldT, int oldR, int oldB) { 
                view.removeOnLayoutChangeListener(this); 
                view.getWidth(); // 获取宽度 
                view.getHeight(); // 获取高度 
            } 
        });
```

## View.post() 
Runnable 对象中的方法会在 View 的 measure、layout 等事件完成后触发。
UI 事件队列会按顺序处理事件，在 setContentView() 被调用后，事件队列中会包含一个要求重新 layout 的 message，所以任何 post 到队列中的 Runnable 对象都会在 Layout 发生变化后执行。
该方法只会执行一次，且逻辑简单，建议使用。
```java
view.post(new Runnable() {

    @Override
    public void run() {
        view.getWidth(); // 获取宽度
        view.getHeight(); // 获取高度
    }
});
```

此处对Apk进行了复杂的解析、加载、合并等操作，大致流程如下：

* 解析apk的相关包信息、判断是否加载过apk;
* 创建一些插件工具类;
* 通过AssetManager创建Resource对象，平台用AssetManager创建出Resource，判断是否和宿主Apk合并资源;
* ClassLoader 根据插件APK路径创建loader，判断是否合并loader中的dex，合并nativeLIbraryDirectories;
* 将so复制到mNativeLibDir路径;
* 保存Instrumentation、Activities、Services、Providers ， 注册Broadcast等;
* 创建出Apk的Application，并call Application onCreate.
