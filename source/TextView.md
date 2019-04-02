https://zhuanlan.zhihu.com/p/37118013

https://www.jianshu.com/p/9f7f9213bff8

# TextView 绘制流程

对于 View 以及其子类的绘制流程，无非是经过 measure 、layout、draw 三个过程。但是 TextView 的绘制又有其特殊的方式，他的绘制都交由 Layout 和TextLine 两个类以及它们的子类来完成。

当 APP 开发着敲下 setText 这个方法时，经过一系列的分析和计算之后，在TextView 中显示的内容就已经被确定。BingLayout 负责绘制单行文本，DynamicLayout 负责绘制带有 Span 的标记文本，StaticLayout 则负责绘制多行不带 Span 的纯文本。前面说到，虽然对于开发者而言 TextView 是一个单独的控件，但是如果再往细看下去，它是由一行一行的文字组合而成，经过计算，每一行显示哪些文本得以确认，而最终的显示则是由 TextLine 来进行绘制。联系到上面所说的坐标系，对于每一行文本，我们就能够理解 Baseline 等几条范围线的重要性。通过它们，TextLine 可以正确地将每一行文字绘制到屏幕正确的位置，同时可以保证行与行之间保留合适的空隙，确保用户能够获得优雅的阅读感受。
