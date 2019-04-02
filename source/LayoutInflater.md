# 获取LayoutInflater

* LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

* LayoutInflater inflater = LayoutInflater.from(context);

* 在Activity内部调用getLayoutInflater()方法

# `inflate(@LayoutRes int resource, @Nullable ViewGroup root, boolean attachToRoot)`
int resource：布局ID，也就是要解析的xml布局文件，boolean attachToRoot表示是否要添加到父布局root中去, 这里面还有个关键的参数root, 它用来表示根布局.

它主要有两个方面的作用：

* 当attachToRoot == true且root ！= null时，新解析出来的View会被add到root中去，然后将root作为结果返回;
* 当attachToRoot == false且root ！= null时，新解析的View会直接作为结果返回，而且root会为新解析的View生成LayoutParams并设置到该View中去;
* 当attachToRoot == false且root == null时，新解析的View会直接作为结果返回。



# `setContentView` 和 `inflate`的区别
setContentView()一旦调用, layout就会立刻显示UI；而inflate只会把Layout形成一个以view类实现成的对象，有需要时再用setContentView(view)显示出来。一般在activity中通过setContentView()将界面显示出来，但是如果在非activity中如何对控件布局设置操作了，这就需要LayoutInflater动态加载。