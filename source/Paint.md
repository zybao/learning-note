# setXfermode

## 禁用GPU硬件加速方法
* 在AndroidManifest.xml文件为application标签添加如下的属性即可为整个应用程序开启/关闭硬件加速：
```xml
    <application android:hardwareAccelerated="true" ...>   
```
* 在Activity 标签下使用 hardwareAccelerated 属性开启或关闭硬件加速：
```java
    <activity android:hardwareAccelerated="false" />  
```
* 在Window 层级使用如下代码开启硬件加速：(Window层级不支持关闭硬件加速)
```java
    getWindow().setFlags(
    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,  
    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED); 
```

* View 级别如下关闭硬件加速：（view 层级上不支持开启硬件加速）
```java
    setLayerType(View.LAYER_TYPE_SOFTWARE, null);    
```
或者使用android:layerType=”software”来关闭硬件加速

## setXfermode(Xfermode xfermode)之AvoidXfermode
```java
public class Xfermode {

    protected void finalize() throws Throwable {
        try {
            finalizer(native_instance);
            native_instance = 0;
        } finally {
            super.finalize();
        }
    }

    private static native void finalizer(long native_instance);

    long native_instance;
}

public class PorterDuffXfermode extends Xfermode {
    /**
     * @hide
     */
    public final PorterDuff.Mode mode;

    /**
     * Create an xfermode that uses the specified porter-duff mode.
     *
     * @param mode           The porter-duff mode that is applied
     */
    public PorterDuffXfermode(PorterDuff.Mode mode) {
        this.mode = mode;
        native_instance = nativeCreateXfermode(mode.nativeInt);
    }
    
    private static native long nativeCreateXfermode(int mode);
}
```

在使用Xfermode时，为了保险起见，我们需要做两件事:
* 禁用硬件加速
* 使用离屏绘制
```java
int layerID = canvas.saveLayer(0,0,width,height,mPaint,Canvas.ALL_SAVE_FLAG);  
  
//TODO 核心绘制代码  
  
//还原图层  
canvas.restoreToCount(layerID); 
```

### PorterDuffXfermode
PorterDuff.Mode表示混合模式，枚举值有18个，表示各种图形混合模式, 有：
```java
Mode.CLEAR;
Mode.SRC;
Mode.DST;
Mode.SRC_OVER;
Mode.DST_OVER;
Mode.SRC_IN;
Mode.DST_IN;
Mode.SRC_OUT;
Mode.DST_OUT;
Mode.SRC_ATOP;
Mode.DST_ATOP;
Mode.XOR;
Mode.DARKEN;
Mode.LIGHTEN;
Mode.MULTIPLY;
Mode.SCREEN;
Mode.ADD;
Mode.OVERLAY;
```