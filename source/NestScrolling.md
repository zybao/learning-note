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

```java
    public boolean startNestedScroll(int axes) {
        if (hasNestedScrollingParent()) {
            // Already in progress
            return true;
        }
        if (isNestedScrollingEnabled()) {
            ViewParent p = mView.getParent();
            View child = mView;
            while (p != null) {
                if (ViewParentCompat.onStartNestedScroll(p, child, mView, axes)) {
                    mNestedScrollingParent = p;
                    ViewParentCompat.onNestedScrollAccepted(p, child, mView, axes);
                    return true;
                }
                if (p instanceof View) {
                    child = (View) p;
                }
                p = p.getParent();
            }
        }
        return false;
    }
```
去寻找NestedScrollingParent，然后回调onStartNestedScroll和onNestedScrollAccepted。

```java
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        if (isNestedScrollingEnabled() && mNestedScrollingParent != null) {
            if (dx != 0 || dy != 0) {
                int startX = 0;
                int startY = 0;
                if (offsetInWindow != null) {
                    mView.getLocationInWindow(offsetInWindow);
                    startX = offsetInWindow[0];
                    startY = offsetInWindow[1];
                }

                if (consumed == null) {
                    if (mTempNestedScrollConsumed == null) {
                        mTempNestedScrollConsumed = new int[2];
                    }
                    consumed = mTempNestedScrollConsumed;
                }
                consumed[0] = 0;
                consumed[1] = 0;
                ViewParentCompat.onNestedPreScroll(mNestedScrollingParent, mView, dx, dy, consumed);

                if (offsetInWindow != null) {
                    mView.getLocationInWindow(offsetInWindow);
                    offsetInWindow[0] -= startX;
                    offsetInWindow[1] -= startY;
                }
                return consumed[0] != 0 || consumed[1] != 0;
            } else if (offsetInWindow != null) {
                offsetInWindow[0] = 0;
                offsetInWindow[1] = 0;
            }
        }
        return false;
    }
```
dispatchNestedPreScroll中会回调onNestedPreScroll方法，内部的scrollByInternal中还会回调onNestedScroll方法。