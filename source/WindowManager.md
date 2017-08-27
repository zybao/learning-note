Window是一个窗口的概念，是一个抽象类，具体实现是PhoneWindow。
通过WindowManager来创建Window。
Window的具体实现位于WindowManagerService，WindowsManager和WindowMannagerService的交互是一个IPC的过程。

* WindowManager添加一个Window的过程
```java
Button button = new Button(this);
button.setText("button");
WindowManager.LayoutParams params = new WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        0, 0, PixelFormat.TRANSPARENT);
params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
params.gravity = Gravity.LEFT | Gravity.TOP;
params.x = 100;
params.y = 300;
WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
windowManager.addView(button,params);
```

* WindowManager.LayoutParams中的flag参数表示Window的属性，下面是几个比较常用的属性
表示window不需要获取焦点，也不需要接收各种输入事件。此标记会同时启用FLAG_NOT_TOUCH_MODAL，最终事件会直接传递给下层的具有焦点的window；

在此模式下，系统会将window区域外的单击事件传递给底层的window，当前window区域内的单击事件则自己处理，一般都需要开启这个标记；

开启此模式可以让Window显示在锁屏的界面上

* TYPE参数表示Window的类型，有三种，分别是应用Window，子Window和系统Window。

应用window对应着一个Activity，子window不能独立存在，需要附属在特定的父window之上，比如Dialog就是子window。系统window是需要声明权限才能创建的window，比如Toast和系统状态栏这些都是系统window，需要声明的权限是。

* window是分层的，每个window都对应着z-ordered，层级大的会覆盖在层级小的上面，应用window的层级范围是1~99，子window的层级范围是1000~1999，系统window的层级范围是2000~2999。
[注意，应用window的层级范围并不是1~999哟]

* WindowManager继承自ViewManager，常用的只有三个方法：addView、updateView和removeView。