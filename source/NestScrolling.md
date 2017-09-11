NestedScrolling机制能够让 父view 和 子view 在滚动时进行配合，其基本流程如下：

1. 当 子view 开始滚动之前，可以通知 父view，让其先于自己进行滚动;

2. 子view 自己进行滚动

3. 子view 滚动之后，还可以通知 父view 继续滚动

要实现这样的交互，父View 需要实现 NestedScrollingParent接口，而 子View 需要实现NestedScrollingChild接口。

在这套交互机制中，child 是动作的发起者，parent 只是接受回调并作出响应。

另外：父view 和 子view 并不需要是直接的父子关系，即如果 "parent1 包含 parent2，parent2 包含child”，则 parent1 和child 仍能通过 NestedScrolling机制 进行交互。

主要接口
* NestedScrollingChild
* NestedScrollingParent

帮助类
* NestedScrollingChildHelper
* NestedScrollingParentHelper

interface NestedScrollingChild
```java
    public void setNestedScrollingEnabled(boolean enabled);
    public void setNestedScrollingEnabled(boolean enabled);
    public boolean startNestedScroll(int axes);
    public void stopNestedScroll();
    public boolean hasNestedScrollingParent();
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed,
            int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow);
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow);
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed);
    public boolean dispatchNestedPreFling(float velocityX, float velocityY);
```

interface NestedScrollingParent
```java
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes);
    public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes);
    public void onStopNestedScroll(View target);
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed,
            int dxUnconsumed, int dyUnconsumed);
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed);
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed);
    public boolean onNestedPreFling(View target, float velocityX, float velocityY);
    public int getNestedScrollAxes();
```

嵌套滚动的流程概括如下:
1. 调用 child 的 startNestedScroll() 来发起嵌套滚动流程(实质是寻找能够配合 child 进行嵌套滚动的 parent)。parent 的 onStartNestedScroll() 会被回调，如果此方法返回 true，则 onNestedScrollAccepted() 也会被回调。

2. child 每次滚动前，可以先询问 parent 是否要滚动，即调用 dispatchNestedPreScroll()，这会回调到 parent 的 onNestedPreScroll()，parent 可以在这个回调中先于 child 滚动。

3. disdispatchNestedPreScroll() 之后，child可 以进行自己的滚动操作。

4. child 滚动以后，可以调用 dispatchNestedScroll()，会回调到 parent 的 onNestedScroll()，在这里 paren t可以进行后于 child 的滚动。

5. 滚动结束，调用 stopNestedScroll()。

当调用NestedScrollingChild中的方法时，NestedScrollingParent中与之相对应的方法就会被回调。方法之间的具体对应关系如下：
|子(发起者)|父(被回调)|
|---------|---------|
|startNestedScroll | onStartNestedScroll、onNestedScrollAccepted|
|dispatchNestedPreScroll | onNestedPreScroll|
|dispatchNestedScroll | onNestedScroll|
|stopNestedScroll | onStopNestedScroll|
|...|...|