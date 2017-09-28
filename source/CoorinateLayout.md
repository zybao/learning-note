http://www.jianshu.com/p/f7989a2a3ec2

# onMeasure
```java
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        prepareChildren();
        ensurePreDrawListener();

        final int paddingLeft = getPaddingLeft();
        final int paddingTop = getPaddingTop();
        final int paddingRight = getPaddingRight();
        final int paddingBottom = getPaddingBottom();
        final int layoutDirection = ViewCompat.getLayoutDirection(this);
        final boolean isRtl = layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL;
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        final int widthPadding = paddingLeft + paddingRight;
        final int heightPadding = paddingTop + paddingBottom;
        int widthUsed = getSuggestedMinimumWidth();
        int heightUsed = getSuggestedMinimumHeight();
        int childState = 0;

        final boolean applyInsets = mLastInsets != null && ViewCompat.getFitsSystemWindows(this);

        final int childCount = mDependencySortedChildren.size();
        for (int i = 0; i < childCount; i++) {
            final View child = mDependencySortedChildren.get(i);
            if (child.getVisibility() == GONE) {
                // If the child is GONE, skip...
                continue;
            }

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            int keylineWidthUsed = 0;
            if (lp.keyline >= 0 && widthMode != MeasureSpec.UNSPECIFIED) {
                final int keylinePos = getKeyline(lp.keyline);
                final int keylineGravity = GravityCompat.getAbsoluteGravity(
                        resolveKeylineGravity(lp.gravity), layoutDirection)
                        & Gravity.HORIZONTAL_GRAVITY_MASK;
                if ((keylineGravity == Gravity.LEFT && !isRtl)
                        || (keylineGravity == Gravity.RIGHT && isRtl)) {
                    keylineWidthUsed = Math.max(0, widthSize - paddingRight - keylinePos);
                } else if ((keylineGravity == Gravity.RIGHT && !isRtl)
                        || (keylineGravity == Gravity.LEFT && isRtl)) {
                    keylineWidthUsed = Math.max(0, keylinePos - paddingLeft);
                }
            }

            int childWidthMeasureSpec = widthMeasureSpec;
            int childHeightMeasureSpec = heightMeasureSpec;
            if (applyInsets && !ViewCompat.getFitsSystemWindows(child)) {
                // We're set to handle insets but this child isn't, so we will measure the
                // child as if there are no insets
                final int horizInsets = mLastInsets.getSystemWindowInsetLeft()
                        + mLastInsets.getSystemWindowInsetRight();
                final int vertInsets = mLastInsets.getSystemWindowInsetTop()
                        + mLastInsets.getSystemWindowInsetBottom();

                childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                        widthSize - horizInsets, widthMode);
                childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                        heightSize - vertInsets, heightMode);
            }

            final Behavior b = lp.getBehavior();
            if (b == null || !b.onMeasureChild(this, child, childWidthMeasureSpec, keylineWidthUsed,
                    childHeightMeasureSpec, 0)) {
                onMeasureChild(child, childWidthMeasureSpec, keylineWidthUsed,
                        childHeightMeasureSpec, 0);
            }

            widthUsed = Math.max(widthUsed, widthPadding + child.getMeasuredWidth() +
                    lp.leftMargin + lp.rightMargin);

            heightUsed = Math.max(heightUsed, heightPadding + child.getMeasuredHeight() +
                    lp.topMargin + lp.bottomMargin);
            childState = ViewCompat.combineMeasuredStates(childState,
                    ViewCompat.getMeasuredState(child));
        }

        final int width = ViewCompat.resolveSizeAndState(widthUsed, widthMeasureSpec,
                childState & ViewCompat.MEASURED_STATE_MASK);
        final int height = ViewCompat.resolveSizeAndState(heightUsed, heightMeasureSpec,
                childState << ViewCompat.MEASURED_HEIGHT_STATE_SHIFT);
        setMeasuredDimension(width, height);
    }
```
获取子 view 的 Behavior，然后判断是否为空，在根据 Behavior 去 measure 子 view。这里我们能看到子 view 的 Behavior 是保存在 LayoutParams里面的，所以这个 LayoutParams 肯定是重写的。然后我们 Behavior 一般是直接写到 xml 布局的子节点上对吧，所以可以判断子 view 的 Behavior 是在View 解析 xml 的时候，读取到 Behavior 节点，然后赋值给 LayoutParams。


# 获取app:layout_behavior等参数
CoordinatorLayout调用generateLayoutParams，获取Layout所需要的参数。

generateLayoutParams调用CoordinatorLayout.LayoutParams(design-23.2.1 CoordinatorLayout.java:2267)的构造函数，获取子布局中的参数。这些参数决定子布局在CoordinatorLayout中的布局。

```java
        LayoutParams(Context context, AttributeSet attrs) {
            super(context, attrs);

            ...

            mBehaviorResolved = a.hasValue(
                    R.styleable.CoordinatorLayout_Layout_layout_behavior);
            if (mBehaviorResolved) {
                mBehavior = parseBehavior(context, attrs, a.getString(
                        R.styleable.CoordinatorLayout_Layout_layout_behavior));
            }
            a.recycle();

            if (mBehavior != null) {
                // If we have a Behavior, dispatch that it has been attached
                mBehavior.onAttachedToLayoutParams(this);
            }
        }

    static Behavior parseBehavior(Context context, AttributeSet attrs, String name) {
        if (TextUtils.isEmpty(name)) {
            return null;
        }

        final String fullName;
        if (name.startsWith(".")) {
            // Relative to the app package. Prepend the app package name.
            fullName = context.getPackageName() + name;
        } else if (name.indexOf('.') >= 0) {
            // Fully qualified package name.
            fullName = name;
        } else {
            // 只有类名的，在CoordinatorLayout的包中获取预备的Behavior类
            // Assume stock behavior in this package (if we have one)
            fullName = !TextUtils.isEmpty(WIDGET_PACKAGE_NAME)
                    ? (WIDGET_PACKAGE_NAME + '.' + name)
                    : name;
        }

        // 通过反射获取Behavior实现类的构造方法
        try {
            Map<String, Constructor<Behavior>> constructors = sConstructors.get();
            if (constructors == null) {
                constructors = new HashMap<>();
                sConstructors.set(constructors);
            }
            Constructor<Behavior> c = constructors.get(fullName);
            if (c == null) {
                final Class<Behavior> clazz = (Class<Behavior>) Class.forName(fullName, true,
                        context.getClassLoader());
                c = clazz.getConstructor(CONSTRUCTOR_PARAMS);
                c.setAccessible(true);
                constructors.put(fullName, c);
            }
            // 调用两个参数的构造方法
            return c.newInstance(context, attrs);
        } catch (Exception e) {
            throw new RuntimeException("Could not inflate Behavior subclass " + fullName, e);
        }
    }
```

# Behavior的关键方法
开启一个CoordinatorLayout之后layoutDependsOn被多次调用。根据Logger的日志，可知，CoordinatorLayout.onMeasure调用的。也就是说，布局改变会触发layoutDependsOn方法。

1. 上文讲到构造函数获取XML中的app:layout_behavior属性值保存在CoordinatorLayout.LayoutParams;
2. CoordinatorLayout.hasDependencies有根据LayoutParams存储的属性值，判断是否存在带有合法依赖关系的子布局。

* 条件一：app:layout_anchor（设置子View的锚点，即以哪个控件为参照点设置位置。）已经设置。
* 条件二：至少一个子View符合dependsOn(CoordinatorLayout:2460)函数的条件

```java
        /**
         * Check if an associated child view depends on another child view of the CoordinatorLayout.
         *
         * @param parent the parent CoordinatorLayout
         * @param child the child to check
         * @param dependency the proposed dependency to check
         * @return true if child depends on dependency
         */
        boolean dependsOn(CoordinatorLayout parent, View child, View dependency) {
            return dependency == mAnchorDirectChild
                    || shouldDodge(dependency, ViewCompat.getLayoutDirection(parent))
                    || (mBehavior != null && mBehavior.layoutDependsOn(parent, child, dependency));
        }
```
# onLayout()
```java
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int layoutDirection = ViewCompat.getLayoutDirection(this);
        final int childCount = mDependencySortedChildren.size();
        for (int i = 0; i < childCount; i++) {
            final View child = mDependencySortedChildren.get(i);
            if (child.getVisibility() == GONE) {
                // If the child is GONE, skip...
                continue;
            }

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            final Behavior behavior = lp.getBehavior();

            if (behavior == null || !behavior.onLayoutChild(this, child, layoutDirection)) {
                onLayoutChild(child, layoutDirection);
            }
        }
    }
```

遍历子 view，如果 behavior.onLayoutChild()方法返回true，则不会调用 CoordinatorLayout 的 onLayouChild()方法，由此可得出结论，重写 Behavior 的 onLayoutChild 方法是用来自定义当前 View 的布局方式。

此时，布局结束，我们的 CoordinatorLayout 静态页面已经完成，接下来，我们要看的是滑动的时候，CoordinatorLayout 怎么处理。
我们来简单回顾一下 ViewGroup 的事件分发机制，首先 disPatchTouchEvent()被调用，然后调用 onInterceptTouchEvent 判断是否允许事件往下传，如果允许则丢给子 View的disPatchTouchEvent 来处理，如果不允许或者允许后子 view没有消费掉事件，则 先后调用自己的 onTouchListener 和 OnTouchEvent来消费事件。

然后我们来根据这个顺序看 CoordinatorLayout 的事件处理顺序，首先看 disPatchTouchEvent 方法， 这个方法，没有重写，那么略过直接看 onInterceptTouchEvent 方法。
```java
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        MotionEvent cancelEvent = null;

        final int action = MotionEventCompat.getActionMasked(ev);

        // Make sure we reset in case we had missed a previous important event.
        if (action == MotionEvent.ACTION_DOWN) {
            // 重置状态
            resetTouchBehaviors();
        }

        final boolean intercepted = performIntercept(ev, TYPE_ON_INTERCEPT);

        if (cancelEvent != null) {
            cancelEvent.recycle();
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            resetTouchBehaviors();
        }

        return intercepted;
    }

    private void resetTouchBehaviors() {
        if (mBehaviorTouchView != null) {
            final Behavior b = ((LayoutParams) mBehaviorTouchView.getLayoutParams()).getBehavior();
            if (b != null) {
                final long now = SystemClock.uptimeMillis();
                final MotionEvent cancelEvent = MotionEvent.obtain(now, now,
                        MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0);
                b.onTouchEvent(this, mBehaviorTouchView, cancelEvent);
                cancelEvent.recycle();
            }
            mBehaviorTouchView = null;
        }

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            lp.resetTouchBehaviorTracking();
        }
        mDisallowInterceptReset = false;
    }
```

```java
    private boolean performIntercept(MotionEvent ev, final int type) {
        boolean intercepted = false;
        boolean newBlock = false;

        MotionEvent cancelEvent = null;

        final int action = MotionEventCompat.getActionMasked(ev);

        final List<View> topmostChildList = mTempList1;
        getTopSortedChildren(topmostChildList);

        // Let topmost child views inspect first
        final int childCount = topmostChildList.size();
        for (int i = 0; i < childCount; i++) {
            final View child = topmostChildList.get(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            final Behavior b = lp.getBehavior();

            if ((intercepted || newBlock) && action != MotionEvent.ACTION_DOWN) {
                // Cancel all behaviors beneath the one that intercepted.
                // If the event is "down" then we don't have anything to cancel yet.
                if (b != null) {
                    if (cancelEvent == null) {
                        final long now = SystemClock.uptimeMillis();
                        cancelEvent = MotionEvent.obtain(now, now,
                                MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0);
                    }
                    switch (type) {
                        case TYPE_ON_INTERCEPT:
                            b.onInterceptTouchEvent(this, child, cancelEvent);
                            break;
                        case TYPE_ON_TOUCH:
                            b.onTouchEvent(this, child, cancelEvent);
                            break;
                    }
                }
                continue;
            }

            if (!intercepted && b != null) {
                switch (type) {
                    case TYPE_ON_INTERCEPT:
                        intercepted = b.onInterceptTouchEvent(this, child, ev);
                        break;
                    case TYPE_ON_TOUCH:
                        intercepted = b.onTouchEvent(this, child, ev);
                        break;
                }
                if (intercepted) {
                    mBehaviorTouchView = child;
                }
            }

            // Don't keep going if we're not allowing interaction below this.
            // Setting newBlock will make sure we cancel the rest of the behaviors.
            final boolean wasBlocking = lp.didBlockInteraction();
            final boolean isBlocking = lp.isBlockingInteractionBelow(this, child);
            newBlock = isBlocking && !wasBlocking;
            if (isBlocking && !newBlock) {
                // Stop here since we don't have anything more to cancel - we already did
                // when the behavior first started blocking things below this point.
                break;
            }
        }

        topmostChildList.clear();

        return intercepted;
    }
```
遍历所有子 View，调用了符合条件的 view 的 Behavior.onInterceptTouchEvent/onTouchEvent方法 然后我们来看 onTouchEvent 方法
```java
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean handled = false;
        boolean cancelSuper = false;
        MotionEvent cancelEvent = null;

        final int action = MotionEventCompat.getActionMasked(ev);

        if (mBehaviorTouchView != null || (cancelSuper = performIntercept(ev, TYPE_ON_TOUCH))) {
            // Safe since performIntercept guarantees that
            // mBehaviorTouchView != null if it returns true
            final LayoutParams lp = (LayoutParams) mBehaviorTouchView.getLayoutParams();
            final Behavior b = lp.getBehavior();
            if (b != null) {
                handled = b.onTouchEvent(this, mBehaviorTouchView, ev);
            }
        }

        // Keep the super implementation correct
        if (mBehaviorTouchView == null) {
            handled |= super.onTouchEvent(ev);
        } else if (cancelSuper) {
            if (cancelEvent == null) {
                final long now = SystemClock.uptimeMillis();
                cancelEvent = MotionEvent.obtain(now, now,
                        MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0);
            }
            super.onTouchEvent(cancelEvent);
        }

        if (!handled && action == MotionEvent.ACTION_DOWN) {

        }

        if (cancelEvent != null) {
            cancelEvent.recycle();
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            resetTouchBehaviors();
        }

        return handled;
    }
```
重点在if (mBehaviorTouchView != null || (cancelSuper = performIntercept(ev, TYPE_ON_TOUCH))这句话上，如果先前有子 view 的Behavior 的 onInterceptTouchEvent 返回了 true，则直接调用这个子 view 的 Behavior 的 onTouchEvent。否则就继续走一遍performIntercept(ev, TYPE_ON_TOUCH)，即：执行所有含有 Behavior 的子 view 的 Behavior.onTouchEvent方法。

最终都是调用了 Behavior 的 onInterceptTouchEvent 和 onTouchEvent 方法，然后各种条件判断就是什么时候调用这两个方法。

* onInterceptTouchEvent
1. 在 CoordinatorLayout 的 onInterceptTouchEvent 方法中杯调用 。
2. 调用顺序：按照 CoordinatorLayout 中 child 的添加倒叙进行调用
3. 运行原理：
如果此方法在 down 事件返回 true，那么它后面的 view 的 Behavior 都执行不到此方法；并且执行 onTouchEvent 事件的时候只会执行此 view 的 Behavior 的 onTouchEvent 方法。
如果不是 down 事件返回 true，那么它后面的 view 的 Behavior 的 onInterceptTouchEvent 方法都会执行，但还是只执行第一个 view 的 Behavior 的 onTouchEvent 方法
如果所有的 view 的 Behavior 的onInterceptTouchEvent 方法都没有返回 true，那么在 CoordinatorLayout 的 onTouchEvent 方法内会回调所有 child 的 Behavior 的 onTouchEvent 方法
4. CoordinatorLayout 的 onInterceptTouchEvent 默认返回 false，返回值由child 的 Behavior 的 onInterceptTouchEvent 方法决定

* onTouchEvent
1. 在 CoordinatorLayout 的 onTouchEvent 方法中被调用
2. 调用顺序：同上
3. 在上面 onInterceptTouchEvent 提到的所有 Behavior 的 onTouchEvent 都返回 false 的情况下，会遍历所有 child 的此方法，但是只要有一个 Behavior 的此方法返回 true，那么后面的所有 child 的此方法都不会执行
4. CoordinatorLayout 的 onTouchEvent默认返回super.onTouchEvent()，如果有 child 的 Behavior 的此方法返回 true，则返回 true。

然后再来说一下嵌套滑动把，我们都知道 CoordinatorLayout 的内嵌套滑动只能用 NestedScrollView 和 RecyclerView，至于为什么呢。我相信很多人肯定点开过 NestedScrollView 和 RecyclerView 的源码，细心的同学肯定会发现这两个类都实现了NestedScrollingChild接口，而我们的 CoordinatorLayout 则实现了NestedScrollingParent的接口。这两个接口不是这篇文章的重点，我简单说一下，CoordinatorLayout 的内嵌滑动事件都是被它的子NestedScrollingChild实现类处理的。而子View 在滑动的时候，会调用NestedScrollingParent的方法，于是 CoordinatorLayout 再NestedScrollingParent的实现方法中，调用了 Behavior 的对应方法。

了解一下NestedScrollingParent和NestedScrollingChild的嵌套滚动机制。简单点说，就是 child（RecycleView） 在滚动的时候调用了 parent（CoordinatorLayout） 的 对应方法，而我们的 Behavior，则是在 parent 的回调方法中，处理了其他child 的伴随变化。

# Behavior
```java 
public boolean layoutDependsOn(CoordinatorLayout parent, V child, View dependency)
```

确定所提供的child 是否有另一个特点兄弟 View 的依赖

在一个CoordinatorLayout 布局里面，这个方法最少会被调用一次，如果对于一个给定的 child 和依赖返回 true，则父CoordinatorLayout 将：

1. 在被依赖的view Layout 发生改变后，这个 child 也会重新 layout。
2. 当依赖关系视图的布局或位置变化时，会调用 onDependentViewChange


```java 
public boolean onDependentViewChanged(CoordinatorLayout parent, V child, View dependency)
```

响应依赖 view 变化的方法

无论是依赖 view的尺寸、大小或者位置发生改变，这个方法都会被调用，一个 Behavior 可以使用此方法来适当的更新响应child
view 的依赖关系由layoutDependsOn 或者child 设置了another属性来确定。

如果 Behavior 改变了 child 的大小或位置，它应该返回 true，默认返回 false。