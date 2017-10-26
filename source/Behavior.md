http://blog.csdn.net/chengkun_123/article/details/76543667

```java
public static abstract class Behavior<V extends View> {
        //事件分发和拦截相关
        public boolean onInterceptTouchEvent(CoordinatorLayout parent, V child, MotionEvent ev) {}
        public boolean onTouchEvent(CoordinatorLayout parent, V child, MotionEvent ev) {}
        //View之间互动相关
        public boolean layoutDependsOn(CoordinatorLayout parent, V child, View dependency) {}
        public boolean onDependentViewChanged(CoordinatorLayout parent, V child, View dependency) { }
        public void onDependentViewRemoved(CoordinatorLayout parent, V child, View dependency) {
        }

      //测量和布局相关
        public boolean onMeasureChild(CoordinatorLayout parent, V child,
                int parentWidthMeasureSpec, int widthUsed,
                int parentHeightMeasureSpec, int heightUsed) { }
        public boolean onLayoutChild(CoordinatorLayout parent, V child, int layoutDirection) { }

      //嵌套滑动相关
        public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout,
                V child, View directTargetChild, View target, int nestedScrollAxes) { }
        public void onNestedScrollAccepted(CoordinatorLayout coordinatorLayout, V child,
                View directTargetChild, View target, int nestedScrollAxes) {}
        public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, V child, View target) {}
        public void onNestedScroll(CoordinatorLayout coordinatorLayout, V child, View target,
                int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {}
        public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, V child, View target,
                int dx, int dy, int[] consumed) {}
        public boolean onNestedFling(CoordinatorLayout coordinatorLayout, V child, View target,
                float velocityX, float velocityY, boolean consumed) {}
        public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout, V child, View target,
                float velocityX, float velocityY) {}

        ...
    }
```