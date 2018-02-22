http://wiki.jikexueyuan.com/project/opengl-es-guide/basic-geometry-definition.html

http://blog.csdn.net/column/details/apidemoopengl.html?&page=2

# GLSurfaceView
GLSurfaceView 为 android.opengl 包中核心类：

* 起到连接 OpenGL ES 与 Android 的 View 层次结构之间的桥梁作用。
* 使得 Open GL ES 库适应于 Anndroid 系统的 Activity 生命周期。
* 使得选择合适的 Frame buffer 像素格式变得容易。
* 创建和管理单独绘图线程以达到平滑动画效果。
* 提供了方便使用的调试工具来跟踪 OpenGL ES 函数调用以帮助检查错误。

GLSurfaceView.Renderer 定义了一个统一图形绘制的接口，它定义了如下三个接口函数：
``` java
// Called when the surface is created or recreated.
public void onSurfaceCreated(GL10 gl, EGLConfig config)

// Called to draw the current frame.
public void onDrawFrame(GL10 gl)

// Called when the surface changed size.
public void onSurfacn eChanged(GL10 gl, int width, int height)  
```
* onSurfaceCreated：在这个方法中主要用来设置一些绘制时不常变化的参数，比如：背景色，是否打开 z-buffer等。
* onDrawFrame：定义实际的绘图操作。
* onSurfaceChanged：如果设备支持屏幕横向和纵向切换，这个方法将发生在横向<->纵向互换时。此时可以重新设置绘制的纵横比率。

如果有需要，也可以通过函数来修改 GLSurfaceView 一些缺省设置：

* setDebugFlags(int) 设置 Debug 标志。
* setEGLConfigChooser (boolean) 选择一个 Config 接近 16bitRGB 颜色模式，可以打开或关闭深度(Depth)Buffer ,缺省为RGB_565 并打开至少有 16bit 的 depth Buffer。
* setEGLConfigChooser(EGLConfigChooser) 选择自定义 EGLConfigChooser。
* setEGLConfigChooser(int, int, int, int, int, int) 指定 red ,green, blue, alpha, depth ,stencil 支持的位数，缺省为 RGB_565 ,16 bit depth buffer。

GLSurfaceView 缺省创建为 RGB_565 颜色格式的 Surface，如果需要支持透明度，可以调用 getHolder().setFormat(PixelFormat.TRANSLUCENT)。

GLSurfaceView 的渲染模式有两种，一种是连续不断的更新屏幕，另一种为 on-demand ，只有在调用 requestRender() 在更新屏幕。 缺省为 RENDERMODE_CONTINUOUSLY 持续刷新屏幕。