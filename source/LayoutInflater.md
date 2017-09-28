# 获取LayoutInflater

* LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

* LayoutInflater inflater = LayoutInflater.from(context);

* 在Activity内部调用getLayoutInflater()方法

# `setContentView` 和 `inflate`的区别
setContentView()一旦调用, layout就会立刻显示UI；而inflate只会把Layout形成一个以view类实现成的对象，有需要时再用setContentView(view)显示出来。一般在activity中通过setContentView()将界面显示出来，但是如果在非activity中如何对控件布局设置操作了，这就需要LayoutInflater动态加载。