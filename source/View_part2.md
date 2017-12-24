# [setWillNotDraw(false)](http://souly.cn/%E6%8A%80%E6%9C%AF%E5%8D%9A%E6%96%87/2015/07/22/setWillNotDraw%E5%92%8CsetDescendantFocusability%E8%AF%A6%E8%A7%A3/)

如果我们想要重写LinearLayout的onDraw的话，我们也可以在其构造方法中调用setWillNotDraw方法。 在ViewGroup初始他时，它调用了一个私有方法：initViewGroup，它里面会有一句setFlags(WILL_NOT_DRAW, DRAW_MASK); 相当于调用了setWillNotDraw(true)，所以说，对于ViewGroup，它就认为是透明的了。如果我们想要重写onDraw，就需要调用setWillNotDraw(false)