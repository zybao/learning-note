https://juejin.im/entry/586a12c5128fe10057037fba


* RecyclerViewDataObserver 数据观察器
* Recycler View循环复用系统，核心部件
* SavedState RecyclerView状态
* AdapterHelper 适配器更新
* ChildHelper 管理子View
* ViewInfoStore 存储子VIEW的动画信息
* Adapter 数据适配器
* LayoutManager 负责子VIEW的布局，核心部件
* ItemAnimator Item动画
* ViewFlinger 快速滑动管理
* NestedScrollingChildHelper 管理子VIEW嵌套滑动


```java
public RecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, CLIP_TO_PADDING_ATTR, defStyle, 0);
            mClipToPadding = a.getBoolean(0, true);
            a.recycle();
        } else {
            mClipToPadding = true;
        }
        setScrollContainer(true);
        setFocusableInTouchMode(true);

        final ViewConfiguration vc = ViewConfiguration.get(context);
        mTouchSlop = vc.getScaledTouchSlop();
        mScaledHorizontalScrollFactor =
                ViewConfigurationCompat.getScaledHorizontalScrollFactor(vc, context);
        mScaledVerticalScrollFactor =
                ViewConfigurationCompat.getScaledVerticalScrollFactor(vc, context);
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        setWillNotDraw(getOverScrollMode() == View.OVER_SCROLL_NEVER);

        mItemAnimator.setListener(mItemAnimatorListener);
        initAdapterManager();
        initChildrenHelper();
        // If not explicitly specified this view is important for accessibility.
        if (ViewCompat.getImportantForAccessibility(this)
                == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            ViewCompat.setImportantForAccessibility(this,
                    ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }
        mAccessibilityManager = (AccessibilityManager) getContext()
                .getSystemService(Context.ACCESSIBILITY_SERVICE);
        setAccessibilityDelegateCompat(new RecyclerViewAccessibilityDelegate(this));
        // Create the layoutManager if specified.

        boolean nestedScrollingEnabled = true;

        if (attrs != null) {
            int defStyleRes = 0;
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RecyclerView,
                    defStyle, defStyleRes);
            String layoutManagerName = a.getString(R.styleable.RecyclerView_layoutManager);
            int descendantFocusability = a.getInt(
                    R.styleable.RecyclerView_android_descendantFocusability, -1);
            if (descendantFocusability == -1) {
                setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            }
            mEnableFastScroller = a.getBoolean(R.styleable.RecyclerView_fastScrollEnabled, false);
            if (mEnableFastScroller) {
                StateListDrawable verticalThumbDrawable = (StateListDrawable) a
                        .getDrawable(R.styleable.RecyclerView_fastScrollVerticalThumbDrawable);
                Drawable verticalTrackDrawable = a
                        .getDrawable(R.styleable.RecyclerView_fastScrollVerticalTrackDrawable);
                StateListDrawable horizontalThumbDrawable = (StateListDrawable) a
                        .getDrawable(R.styleable.RecyclerView_fastScrollHorizontalThumbDrawable);
                Drawable horizontalTrackDrawable = a
                        .getDrawable(R.styleable.RecyclerView_fastScrollHorizontalTrackDrawable);
                initFastScroller(verticalThumbDrawable, verticalTrackDrawable,
                        horizontalThumbDrawable, horizontalTrackDrawable);
            }
            a.recycle();
            createLayoutManager(context, layoutManagerName, attrs, defStyle, defStyleRes);

            if (Build.VERSION.SDK_INT >= 21) {
                a = context.obtainStyledAttributes(attrs, NESTED_SCROLLING_ATTRS,
                        defStyle, defStyleRes);
                nestedScrollingEnabled = a.getBoolean(0, true);
                a.recycle();
            }
        } else {
            setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        }

        // Re-set whether nested scrolling is enabled so that it is set on all API levels
        setNestedScrollingEnabled(nestedScrollingEnabled);
    }

```

代码进行了一系列的初始化工作,关键是createLayoutManager,创建了一个布局管理器

```java
    private void createLayoutManager(Context context, String className, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        if (className != null) {
            className = className.trim();
            if (!className.isEmpty()) {
                className = getFullClassName(context, className);
                try {
                    ClassLoader classLoader;
                    if (isInEditMode()) {
                        // Stupid layoutlib cannot handle simple class loaders.
                        classLoader = this.getClass().getClassLoader();
                    } else {
                        classLoader = context.getClassLoader();
                    }
                    Class<? extends LayoutManager> layoutManagerClass =
                            classLoader.loadClass(className).asSubclass(LayoutManager.class);
                    Constructor<? extends LayoutManager> constructor;
                    Object[] constructorArgs = null;
                    try {
                        constructor = layoutManagerClass
                                .getConstructor(LAYOUT_MANAGER_CONSTRUCTOR_SIGNATURE);
                        constructorArgs = new Object[]{context, attrs, defStyleAttr, defStyleRes};
                    } catch (NoSuchMethodException e) {
                        try {
                            constructor = layoutManagerClass.getConstructor();
                        } catch (NoSuchMethodException e1) {
                            e1.initCause(e);
                            throw new IllegalStateException(attrs.getPositionDescription()
                                    + ": Error creating LayoutManager " + className, e1);
                        }
                    }
                    constructor.setAccessible(true);
                    setLayoutManager(constructor.newInstance(constructorArgs));
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(attrs.getPositionDescription()
                            + ": Unable to find LayoutManager " + className, e);
                } catch (InvocationTargetException e) {
                    throw new IllegalStateException(attrs.getPositionDescription()
                            + ": Could not instantiate the LayoutManager: " + className, e);
                } catch (InstantiationException e) {
                    throw new IllegalStateException(attrs.getPositionDescription()
                            + ": Could not instantiate the LayoutManager: " + className, e);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(attrs.getPositionDescription()
                            + ": Cannot access non-public constructor " + className, e);
                } catch (ClassCastException e) {
                    throw new IllegalStateException(attrs.getPositionDescription()
                            + ": Class is not a LayoutManager " + className, e);
                }
            }
        }
    }
```

如果在布局文件里面设置了布局管理器的类型，那么这里会通过反射的方式实例化出对应的布局管理器。最后将实例化出的布局管理器设置到当前的RecyclerView

```java
    /**
     * Set the {@link LayoutManager} that this RecyclerView will use.
     *
     * <p>In contrast to other adapter-backed views such as {@link android.widget.ListView}
     * or {@link android.widget.GridView}, RecyclerView allows client code to provide custom
     * layout arrangements for child views. These arrangements are controlled by the
     * {@link LayoutManager}. A LayoutManager must be provided for RecyclerView to function.</p>
     *
     * <p>Several default strategies are provided for common uses such as lists and grids.</p>
     *
     * @param layout LayoutManager to use
     */
    public void setLayoutManager(LayoutManager layout) {
        if (layout == mLayout) {
            return;
        }
        stopScroll();
        // TODO We should do this switch a dispatchLayout pass and animate children. There is a good
        // chance that LayoutManagers will re-use views.
        if (mLayout != null) {
            // end all running animations
            if (mItemAnimator != null) {
                mItemAnimator.endAnimations();
            }
            mLayout.removeAndRecycleAllViews(mRecycler);
            mLayout.removeAndRecycleScrapInt(mRecycler);
            mRecycler.clear();

            if (mIsAttached) {
                mLayout.dispatchDetachedFromWindow(this, mRecycler);
            }
            mLayout.setRecyclerView(null);
            mLayout = null;
        } else {
            mRecycler.clear();
        }
        // this is just a defensive measure for faulty item animators.
        mChildHelper.removeAllViewsUnfiltered();
        mLayout = layout;
        if (layout != null) {
            if (layout.mRecyclerView != null) {
                throw new IllegalArgumentException("LayoutManager " + layout
                        + " is already attached to a RecyclerView:"
                        + layout.mRecyclerView.exceptionLabel());
            }
            mLayout.setRecyclerView(this);
            if (mIsAttached) {
                mLayout.dispatchAttachedToWindow(this);
            }
        }
        mRecycler.updateViewCacheSize();
        requestLayout();
    }
```

设置布局管理器之前会先清空所有之前的缓存VIEW。最后通知VIEW刷新,requestLayout可见要绘制了

```java
    @Override
    public void requestLayout() {
        if (mEatRequestLayout == 0 && !mLayoutFrozen) {
            super.requestLayout();
        } else {
            mLayoutRequestEaten = true;
        }
    }
```

调用View#requestLayout

```java
    /**
     * Call this when something has changed which has invalidated the
     * layout of this view. This will schedule a layout pass of the view
     * tree. This should not be called while the view hierarchy is currently in a layout
     * pass ({@link #isInLayout()}. If layout is happening, the request may be honored at the
     * end of the current layout pass (and then layout will run again) or after the current
     * frame is drawn and the next layout occurs.
     *
     * <p>Subclasses which override this method should call the superclass method to
     * handle possible request-during-layout errors correctly.</p>
     */
    @CallSuper
    public void requestLayout() {
        if (mMeasureCache != null) mMeasureCache.clear();

        if (mAttachInfo != null && mAttachInfo.mViewRequestingLayout == null) {
            // Only trigger request-during-layout logic if this is the view requesting it,
            // not the views in its parent hierarchy
            ViewRootImpl viewRoot = getViewRootImpl();
            if (viewRoot != null && viewRoot.isInLayout()) {
                if (!viewRoot.requestLayoutDuringLayout(this)) {
                    return;
                }
            }
            mAttachInfo.mViewRequestingLayout = this;
        }

        mPrivateFlags |= PFLAG_FORCE_LAYOUT;
        mPrivateFlags |= PFLAG_INVALIDATED;

        if (mParent != null && !mParent.isLayoutRequested()) {
            mParent.requestLayout();
        }
        if (mAttachInfo != null && mAttachInfo.mViewRequestingLayout == this) {
            mAttachInfo.mViewRequestingLayout = null;
        }
    }
```
在requestLayout方法中，首先先判断当前View树是否正在布局流程，接着为当前子View设置标记位，该标记位的作用就是标记了当前的View是需要进行重新布局的，接着调用mParent.requestLayout方法，这个十分重要，因为这里是向父容器请求布局，即调用父容器的requestLayout方法，为父容器添加PFLAG_FORCE_LAYOUT标记位，而父容器又会调用它的父容器的requestLayout方法，即requestLayout事件层层向上传递，直到DecorView，即根View，而根View又会传递给ViewRootImpl，也即是说子View的requestLayout事件，最终会被ViewRootImpl接收并得到处理。纵观这个向上传递的流程，其实是采用了责任链模式，即不断向上传递该事件，直到找到能处理该事件的上级，在这里，只有ViewRootImpl能够处理requestLayout事件。

最终调用到recyclerview的onMeature

recyclerView.setLayoutManager(layoutManager)就是桥接模式的体现，因为layoutManager的实现可以有多种,即桥接模式具体实现化逻辑ConcreteImplementor

* ListView功能 `recyclerView.setLayoutManager(new LinearLayoutManager(this))`;
* GridView功能 `recyclerView.setLayoutManager(new GridLayoutManager(this,3))`;
* 瀑布流形式功能 `recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2,StaggeredGridLayoutManager.VERTICAL))`;
* 横向ListView的功能 `recyclerView.setLayoutManager(new LinearLayoutManager(this)); layoutManager.setOrientation(…)`;

```java
public LinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
    setOrientation(orientation);
    setReverseLayout(reverseLayout);
    setAutoMeasureEnabled(true);
}
public StaggeredGridLayoutManager(Context context, AttributeSet attrs, int defStyleAttr,
        int defStyleRes) {
    Properties properties = getProperties(context, attrs, defStyleAttr, defStyleRes);
    setOrientation(properties.orientation);
    setSpanCount(properties.spanCount);
    setReverseLayout(properties.reverseLayout);
    setAutoMeasureEnabled(mGapStrategy != GAP_HANDLING_NONE);
    mLayoutState = new LayoutState();
    createOrientationHelpers();
}
GridLayoutManager继承LinearLayoutManager
```

可见其初始化时候会设置AutoMeasurEnabled,前面说过，RecyclerView会将测量与布局交给LayoutManager来做，并且LayoutManager有一个叫做mAutoMeasure的属性，这个属性用来控制LayoutManager是否开启自动测量，开启自动测量的话布局就交由RecyclerView使用一套默认的测量机制，否则，自定义的LayoutManager需要重写onMeasure来处理自身的测量工作。RecyclerView目前提供的几种LayoutManager都开启了自动测量，所以这里我们关注一下自动测量部分的逻辑：

```java
    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        if (mLayout == null) {
            defaultOnMeasure(widthSpec, heightSpec);
            return;
        }
        if (mLayout.mAutoMeasure) {
            final int widthMode = MeasureSpec.getMode(widthSpec);
            final int heightMode = MeasureSpec.getMode(heightSpec);
            final boolean skipMeasure = widthMode == MeasureSpec.EXACTLY
                    && heightMode == MeasureSpec.EXACTLY;
            mLayout.onMeasure(mRecycler, mState, widthSpec, heightSpec);
            if (skipMeasure || mAdapter == null) {
                return;
            }
            if (mState.mLayoutStep == State.STEP_START) {
                dispatchLayoutStep1();
            }
            // set dimensions in 2nd step. Pre-layout should happen with old dimensions for
            // consistency
            mLayout.setMeasureSpecs(widthSpec, heightSpec);
            mState.mIsMeasuring = true;
            dispatchLayoutStep2();

            // now we can get the width and height from the children.
            mLayout.setMeasuredDimensionFromChildren(widthSpec, heightSpec);

            // if RecyclerView has non-exact width and height and if there is at least one child
            // which also has non-exact width & height, we have to re-measure.
            if (mLayout.shouldMeasureTwice()) {
                mLayout.setMeasureSpecs(
                        MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
                mState.mIsMeasuring = true;
                dispatchLayoutStep2();
                // now we can get the width and height from the children.
                mLayout.setMeasuredDimensionFromChildren(widthSpec, heightSpec);
            }
        } else {
            if (mHasFixedSize) {
                mLayout.onMeasure(mRecycler, mState, widthSpec, heightSpec);
                return;
            }
            // custom onMeasure
            if (mAdapterUpdateDuringMeasure) {
                eatRequestLayout();
                onEnterLayoutOrScroll();
                processAdapterUpdatesAndSetAnimationFlags();
                onExitLayoutOrScroll();

                if (mState.mRunPredictiveAnimations) {
                    mState.mInPreLayout = true;
                } else {
                    // consume remaining updates to provide a consistent state with the layout pass.
                    mAdapterHelper.consumeUpdatesInOnePass();
                    mState.mInPreLayout = false;
                }
                mAdapterUpdateDuringMeasure = false;
                resumeRequestLayout(false);
            } else if (mState.mRunPredictiveAnimations) {
                // If mAdapterUpdateDuringMeasure is false and mRunPredictiveAnimations is true:
                // this means there is already an onMeasure() call performed to handle the pending
                // adapter change, two onMeasure() calls can happen if RV is a child of LinearLayout
                // with layout_width=MATCH_PARENT. RV cannot call LM.onMeasure() second time
                // because getViewForPosition() will crash when LM uses a child to measure.
                setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight());
                return;
            }

            if (mAdapter != null) {
                mState.mItemCount = mAdapter.getItemCount();
            } else {
                mState.mItemCount = 0;
            }
            eatRequestLayout();
            mLayout.onMeasure(mRecycler, mState, widthSpec, heightSpec);
            resumeRequestLayout(false);
            mState.mInPreLayout = false; // clear
        }
    }
```

自动测量的原理如下:当RecyclerView的宽高都为EXACTLY时，可以直接设置对应的宽高，然后返回，结束测量.

整个mLayout.mAutoMeasure就是在做前两步的布局，可见RecylerView的measure与layout是紧密相关的，所以我们来赶快瞧一瞧RecyclerView是如何layout的。

```java
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        TraceCompat.beginSection(TRACE_ON_LAYOUT_TAG);
        dispatchLayout();
        TraceCompat.endSection();
        mFirstLayoutComplete = true;
    }

    /**
     * Wrapper around layoutChildren() that handles animating changes caused by layout.
     * Animations work on the assumption that there are five different kinds of items
     * in play:
     * PERSISTENT: items are visible before and after layout
     * REMOVED: items were visible before layout and were removed by the app
     * ADDED: items did not exist before layout and were added by the app
     * DISAPPEARING: items exist in the data set before/after, but changed from
     * visible to non-visible in the process of layout (they were moved off
     * screen as a side-effect of other changes)
     * APPEARING: items exist in the data set before/after, but changed from
     * non-visible to visible in the process of layout (they were moved on
     * screen as a side-effect of other changes)
     * The overall approach figures out what items exist before/after layout and
     * infers one of the five above states for each of the items. Then the animations
     * are set up accordingly:
     * PERSISTENT views are animated via
     * {@link ItemAnimator#animatePersistence(ViewHolder, ItemHolderInfo, ItemHolderInfo)}
     * DISAPPEARING views are animated via
     * {@link ItemAnimator#animateDisappearance(ViewHolder, ItemHolderInfo, ItemHolderInfo)}
     * APPEARING views are animated via
     * {@link ItemAnimator#animateAppearance(ViewHolder, ItemHolderInfo, ItemHolderInfo)}
     * and changed views are animated via
     * {@link ItemAnimator#animateChange(ViewHolder, ViewHolder, ItemHolderInfo, ItemHolderInfo)}.
     */
    void dispatchLayout() {
        if (mAdapter == null) {
            Log.e(TAG, "No adapter attached; skipping layout");
            // leave the state in START
            return;
        }
        if (mLayout == null) {
            Log.e(TAG, "No layout manager attached; skipping layout");
            // leave the state in START
            return;
        }
        mState.mIsMeasuring = false;
        if (mState.mLayoutStep == State.STEP_START) {
            dispatchLayoutStep1();
            mLayout.setExactMeasureSpecsFrom(this);
            dispatchLayoutStep2();
        } else if (mAdapterHelper.hasUpdates() || mLayout.getWidth() != getWidth()
                || mLayout.getHeight() != getHeight()) {
            // First 2 steps are done in onMeasure but looks like we have to run again due to
            // changed size.
            mLayout.setExactMeasureSpecsFrom(this);
            dispatchLayoutStep2();
        } else {
            // always make sure we sync them (to ensure mode is exact)
            mLayout.setExactMeasureSpecsFrom(this);
        }
        dispatchLayoutStep3();
    }
```

通过查看dispatchLayout的代码正好验证了我们前文关于RecyclerView的layout三步走原则，如果在onMeasure中已经完成了step1与step2，则只会执行step3，否则三步会依次触发。接下来我们一步一步的进行分析

## `dispatchLayoutStep1`

The first step of a layout where we;
 - process adapter updates
 - decide which animation should run
 - save information about current views
 - If necessary, run predictive layout and save its information

step的第一步目的就是在记录View的状态，首先遍历当前所有的View依次进行处理，mItemAnimator会根据每个View的信息封装成一个ItemHolderInfo，这个ItemHolderInfo中主要包含的就是当前View的位置状态等。然后ItemHolderInfo 就被存入mViewInfoStore中,由代码可见被存在ArrayMap和LongSparseArray中,其是对HashMap的android优化,是用两个数组来完成存储,arraymap的key可以是任意值,SparseArray的key只能为int,其核心是折半查找。

注意这里调用的是mViewInfoStore的addToPreLayout方法，我们追进：

```java
    /**
     * Adds the item information to the prelayout tracking
     * @param holder The ViewHolder whose information is being saved
     * @param info The information to save
     */
    void addToPreLayout(ViewHolder holder, ItemHolderInfo info) {
        InfoRecord record = mLayoutHolderMap.get(holder);
        if (record == null) {
            record = InfoRecord.obtain();
            mLayoutHolderMap.put(holder, record);
        }
        record.preInfo = info;
        record.flags |= FLAG_PRE;
    }
```

addToPreLayout方法中会根据holder来查询InfoRecord信息，如果没有，则生成，然后将info信息赋值给InfoRecord的preInfo变量。最后标记FLAG_PRE信息，如此，完成函数。所以纵观整个layout的第一步，就是在记录当前的View信息，因为进入第二步后，View的信息就将被改变了。

## `dispatchLayoutStep2`

```java
    /**
     * The second layout step where we do the actual layout of the views for the final state.
     * This step might be run multiple times if necessary (e.g. measure).
     */
    private void dispatchLayoutStep2() {
        eatRequestLayout();
        onEnterLayoutOrScroll();
        mState.assertLayoutStep(State.STEP_LAYOUT | State.STEP_ANIMATIONS);
        mAdapterHelper.consumeUpdatesInOnePass();
        mState.mItemCount = mAdapter.getItemCount();
        mState.mDeletedInvisibleItemCountSincePreviousLayout = 0;

        // Step 2: Run layout
        mState.mInPreLayout = false;
        mLayout.onLayoutChildren(mRecycler, mState);

        mState.mStructureChanged = false;
        mPendingSavedState = null;

        // onLayoutChildren may have caused client code to disable item animations; re-check
        mState.mRunSimpleAnimations = mState.mRunSimpleAnimations && mItemAnimator != null;
        mState.mLayoutStep = State.STEP_ANIMATIONS;
        onExitLayoutOrScroll();
        resumeRequestLayout(false);
    }
```

layout的第二步主要就是真正的去布局View了，前面也说过，RecyclerView的布局是由LayoutManager负责的，所以第二步的主要工作也都在LayoutManager中，由于每种布局的方式不一样，这里我们以常见的LinearLayoutManager为例。我们看其onLayoutChildren方法：

```java
    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        // layout algorithm:
        // 1) by checking children and other variables, find an anchor coordinate and an anchor
        //  item position.
        // 2) fill towards start, stacking from bottom
        // 3) fill towards end, stacking from top
        // 4) scroll to fulfill requirements like stack from bottom.
        // create layout state
        if (DEBUG) {
            Log.d(TAG, "is pre layout:" + state.isPreLayout());
        }
        if (mPendingSavedState != null || mPendingScrollPosition != NO_POSITION) {
            if (state.getItemCount() == 0) {
                removeAndRecycleAllViews(recycler);
                return;
            }
        }
        if (mPendingSavedState != null && mPendingSavedState.hasValidAnchor()) {
            mPendingScrollPosition = mPendingSavedState.mAnchorPosition;
        }

        ensureLayoutState();
        mLayoutState.mRecycle = false;
        // resolve layout direction
        resolveShouldLayoutReverse();

        final View focused = getFocusedChild();
        if (!mAnchorInfo.mValid || mPendingScrollPosition != NO_POSITION
                || mPendingSavedState != null) {
            mAnchorInfo.reset();
            mAnchorInfo.mLayoutFromEnd = mShouldReverseLayout ^ mStackFromEnd;
            // calculate anchor position and coordinate
            updateAnchorInfoForLayout(recycler, state, mAnchorInfo);
            mAnchorInfo.mValid = true;
        } else if (focused != null && (mOrientationHelper.getDecoratedStart(focused)
                        >= mOrientationHelper.getEndAfterPadding()
                || mOrientationHelper.getDecoratedEnd(focused)
                <= mOrientationHelper.getStartAfterPadding())) {
            // This case relates to when the anchor child is the focused view and due to layout
            // shrinking the focused view fell outside the viewport, e.g. when soft keyboard shows
            // up after tapping an EditText which shrinks RV causing the focused view (The tapped
            // EditText which is the anchor child) to get kicked out of the screen. Will update the
            // anchor coordinate in order to make sure that the focused view is laid out. Otherwise,
            // the available space in layoutState will be calculated as negative preventing the
            // focused view from being laid out in fill.
            // Note that we won't update the anchor position between layout passes (refer to
            // TestResizingRelayoutWithAutoMeasure), which happens if we were to call
            // updateAnchorInfoForLayout for an anchor that's not the focused view (e.g. a reference
            // child which can change between layout passes).
            mAnchorInfo.assignFromViewAndKeepVisibleRect(focused);
        }
        if (DEBUG) {
            Log.d(TAG, "Anchor info:" + mAnchorInfo);
        }

        // LLM may decide to layout items for "extra" pixels to account for scrolling target,
        // caching or predictive animations.
        int extraForStart;
        int extraForEnd;
        final int extra = getExtraLayoutSpace(state);
        // If the previous scroll delta was less than zero, the extra space should be laid out
        // at the start. Otherwise, it should be at the end.
        if (mLayoutState.mLastScrollDelta >= 0) {
            extraForEnd = extra;
            extraForStart = 0;
        } else {
            extraForStart = extra;
            extraForEnd = 0;
        }
        extraForStart += mOrientationHelper.getStartAfterPadding();
        extraForEnd += mOrientationHelper.getEndPadding();
        if (state.isPreLayout() && mPendingScrollPosition != NO_POSITION
                && mPendingScrollPositionOffset != INVALID_OFFSET) {
            // if the child is visible and we are going to move it around, we should layout
            // extra items in the opposite direction to make sure new items animate nicely
            // instead of just fading in
            final View existing = findViewByPosition(mPendingScrollPosition);
            if (existing != null) {
                final int current;
                final int upcomingOffset;
                if (mShouldReverseLayout) {
                    current = mOrientationHelper.getEndAfterPadding()
                            - mOrientationHelper.getDecoratedEnd(existing);
                    upcomingOffset = current - mPendingScrollPositionOffset;
                } else {
                    current = mOrientationHelper.getDecoratedStart(existing)
                            - mOrientationHelper.getStartAfterPadding();
                    upcomingOffset = mPendingScrollPositionOffset - current;
                }
                if (upcomingOffset > 0) {
                    extraForStart += upcomingOffset;
                } else {
                    extraForEnd -= upcomingOffset;
                }
            }
        }
        int startOffset;
        int endOffset;
        final int firstLayoutDirection;
        if (mAnchorInfo.mLayoutFromEnd) {
            firstLayoutDirection = mShouldReverseLayout ? LayoutState.ITEM_DIRECTION_TAIL
                    : LayoutState.ITEM_DIRECTION_HEAD;
        } else {
            firstLayoutDirection = mShouldReverseLayout ? LayoutState.ITEM_DIRECTION_HEAD
                    : LayoutState.ITEM_DIRECTION_TAIL;
        }

        onAnchorReady(recycler, state, mAnchorInfo, firstLayoutDirection);
        detachAndScrapAttachedViews(recycler);
        mLayoutState.mInfinite = resolveIsInfinite();
        mLayoutState.mIsPreLayout = state.isPreLayout();
        if (mAnchorInfo.mLayoutFromEnd) {
            // fill towards start
            updateLayoutStateToFillStart(mAnchorInfo);
            mLayoutState.mExtra = extraForStart;
            fill(recycler, mLayoutState, state, false);
            startOffset = mLayoutState.mOffset;
            final int firstElement = mLayoutState.mCurrentPosition;
            if (mLayoutState.mAvailable > 0) {
                extraForEnd += mLayoutState.mAvailable;
            }
            // fill towards end
            updateLayoutStateToFillEnd(mAnchorInfo);
            mLayoutState.mExtra = extraForEnd;
            mLayoutState.mCurrentPosition += mLayoutState.mItemDirection;
            fill(recycler, mLayoutState, state, false);
            endOffset = mLayoutState.mOffset;

            if (mLayoutState.mAvailable > 0) {
                // end could not consume all. add more items towards start
                extraForStart = mLayoutState.mAvailable;
                updateLayoutStateToFillStart(firstElement, startOffset);
                mLayoutState.mExtra = extraForStart;
                fill(recycler, mLayoutState, state, false);
                startOffset = mLayoutState.mOffset;
            }
        } else {
            // fill towards end
            updateLayoutStateToFillEnd(mAnchorInfo);
            mLayoutState.mExtra = extraForEnd;
            fill(recycler, mLayoutState, state, false);
            endOffset = mLayoutState.mOffset;
            final int lastElement = mLayoutState.mCurrentPosition;
            if (mLayoutState.mAvailable > 0) {
                extraForStart += mLayoutState.mAvailable;
            }
            // fill towards start
            updateLayoutStateToFillStart(mAnchorInfo);
            mLayoutState.mExtra = extraForStart;
            mLayoutState.mCurrentPosition += mLayoutState.mItemDirection;
            fill(recycler, mLayoutState, state, false);
            startOffset = mLayoutState.mOffset;

            if (mLayoutState.mAvailable > 0) {
                extraForEnd = mLayoutState.mAvailable;
                // start could not consume all it should. add more items towards end
                updateLayoutStateToFillEnd(lastElement, endOffset);
                mLayoutState.mExtra = extraForEnd;
                fill(recycler, mLayoutState, state, false);
                endOffset = mLayoutState.mOffset;
            }
        }

        // changes may cause gaps on the UI, try to fix them.
        // TODO we can probably avoid this if neither stackFromEnd/reverseLayout/RTL values have
        // changed
        if (getChildCount() > 0) {
            // because layout from end may be changed by scroll to position
            // we re-calculate it.
            // find which side we should check for gaps.
            if (mShouldReverseLayout ^ mStackFromEnd) {
                int fixOffset = fixLayoutEndGap(endOffset, recycler, state, true);
                startOffset += fixOffset;
                endOffset += fixOffset;
                fixOffset = fixLayoutStartGap(startOffset, recycler, state, false);
                startOffset += fixOffset;
                endOffset += fixOffset;
            } else {
                int fixOffset = fixLayoutStartGap(startOffset, recycler, state, true);
                startOffset += fixOffset;
                endOffset += fixOffset;
                fixOffset = fixLayoutEndGap(endOffset, recycler, state, false);
                startOffset += fixOffset;
                endOffset += fixOffset;
            }
        }
        layoutForPredictiveAnimations(recycler, state, startOffset, endOffset);
        if (!state.isPreLayout()) {
            mOrientationHelper.onLayoutComplete();
        } else {
            mAnchorInfo.reset();
        }
        mLastStackFromEnd = mStackFromEnd;
        if (DEBUG) {
            validateChildOrder();
        }
    }
```

整个onLayoutChildren过程还是很复杂的，这里我尽量省略了一些与流程关系不大的细节处理代码。整个onLayoutChildren过程可以大致整理如下：

* 找到anchor点
* 根据anchor一直向前布局，直至填充满anchor点前面的所有区域
* 根据anchor一直向后布局，直至填充满anchor点后面的所有区域这里我以垂直布局来说明，mAnchorInfo为布局锚点信息，包含了子控件在Y轴上起始绘制偏移量（coordinate），ItemView在Adapter中的索引位置（position）和布局方向（mLayoutFromEnd）——这里是指start、end方向。这部分代码的功能就是：确定布局锚点，以此为起点向开始和结束方向填充ItemView

anchor点的寻找是由updateAnchorInfoForLayout函数负责的：

```java
    private void updateAnchorInfoForLayout(RecyclerView.Recycler recycler, RecyclerView.State state,
            AnchorInfo anchorInfo) {
        if (updateAnchorFromPendingData(state, anchorInfo)) {
            if (DEBUG) {
                Log.d(TAG, "updated anchor info from pending information");
            }
            return;
        }

        if (updateAnchorFromChildren(recycler, state, anchorInfo)) {
            if (DEBUG) {
                Log.d(TAG, "updated anchor info from existing children");
            }
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "deciding anchor info for fresh state");
        }
        anchorInfo.assignCoordinateFromPadding();
        anchorInfo.mPosition = mStackFromEnd ? state.getItemCount() - 1 : 0;
    }
```

## `dispatchLayoutStep3`
```java
private void dispatchLayoutStep3() {
    mState.mLayoutStep = State.STEP_START;
    if (mState.mRunSimpleAnimations) {
        for (int i = mChildHelper.getChildCount() - 1; i >= 0; i--) {
            ...
            final ItemHolderInfo animationInfo = mItemAnimator
                    .recordPostLayoutInformation(mState, holder);
                mViewInfoStore.addToPostLayout(holder, animationInfo);
        }
        mViewInfoStore.process(mViewInfoProcessCallback);
    }
    ...
}
```

这一步是与第一步呼应的的，此时由于子View都已完成布局，所以子View的信息都发生了变化。我们会看到第一步出现的mViewInfoStore和mItemAnimator再次登场，这次mItemAnimator调用的是recordPostLayoutInformation方法，而mViewInfoStore调用的是addToPostLayout方法，还记得刚刚我强调的吗，之前是pre，也就是真正布局之前的状态，而现在要记录布局之后的状态，我们追进addToPostLayout：

```java
    /**
     * Adds the item information to the post layout list
     * @param holder The ViewHolder whose information is being saved
     * @param info The information to save
     */
    void addToPostLayout(ViewHolder holder, ItemHolderInfo info) {
        InfoRecord record = mLayoutHolderMap.get(holder);
        if (record == null) {
            record = InfoRecord.obtain();
            mLayoutHolderMap.put(holder, record);
        }
        record.postInfo = info;
        record.flags |= FLAG_POST;
    }
```



和第一步的addToPreLayout类似，不过这次info信息被赋值给了record的postInfo变量，这样，一个record中就包含了布局前后view的状态。

最后，mViewInfoStore调用了process方法，这个方法就是根据mViewInfoStore中的View信息，来执行动画逻辑，这又是一个可以展看很多的点，这里不做探讨，感兴趣的可以深入的看一下，会对动画流程有更直观的体会。接下来就是onDraw,RecyclerView的draw过程可以分为２部分来看：RecyclerView负责绘制所有decoration；ItemView的绘制由ViewGroup处理，这里的绘制是android常规绘制逻辑，就不再阐述了。下面来看看RecyclerView的draw()和onDraw()方法：

```java
@Override
public void draw(Canvas c) {
    super.draw(c);
    final int count = mItemDecorations.size();
    for (int i = 0; i < count; i++) {
        mItemDecorations.get(i).onDrawOver(c, this, mState);
    }
    ...
}

@Override
public void onDraw(Canvas c) {
    super.onDraw(c);
    final int count = mItemDecorations.size();
    for (int i = 0; i < count; i++) {
        mItemDecorations.get(i).onDraw(c, this, mState);
    }
}
```

好了,测量,布局,绘制都大体讲了一下,回到我们开头,setLayoutManager已经完成,接下来是setAdapter(适配器模式),我们一起结合动画的实现(观察者模式)来解读,先看一下adapter类

```java
public static abstract class Adapter {
    private final AdapterDataObservable mObservable = new AdapterDataObservable();

    public void registerAdapterDataObserver(AdapterDataObserver observer) {
        mObservable.registerObserver(observer);
    }

    public void unregisterAdapterDataObserver(AdapterDataObserver observer) {
        mObservable.unregisterObserver(observer);
    }

    public final void notifyItemInserted(int position) {
        mObservable.notifyItemRangeInserted(position, 1);
    }
}
```

RecyclerView的Adapter，这个控件需要的是View(dst),而我们有的一般是datas(src),所以适配器Adapter就是完成了数据源datas 转化成 ItemView的工作。带入src->Adapter->dst中，即datas->Adapter->View. 通过public abstract void onBindViewHolder(VH holder, int position);将datas绑定到view然后返回ViewHolder我们可以看到Adapter中包含一个AdapterDataObservable的对象mObservable，这个是一个可观察者，在可观察者中可以注册一系列的观察者AdapterDataObserver。在我们调用的notify函数的时候，就是可观察者发出通知，这时已经注册的观察者都可以收到这个通知，然后依次进行处理.

注册观察者的地方就是在RecyclerView的这个函数中。这个是setAdapter方法最终调用的地方。它主要做了：

* 如果之前存在Adapter，先移除原来的，注销观察者，和从RecyclerView Detached。
* 然后根据参数，决定是否清除原来的ViewHolder
* 然后重置AdapterHelper，并更新Adapter，注册观察者。

```java
    /**
     * Set a new adapter to provide child views on demand.
     * <p>
     * When adapter is changed, all existing views are recycled back to the pool. If the pool has
     * only one adapter, it will be cleared.
     *
     * @param adapter The new adapter to set, or null to set no adapter.
     * @see #swapAdapter(Adapter, boolean)
     */
    public void setAdapter(Adapter adapter) {
        // bail out if layout is frozen
        setLayoutFrozen(false);
        setAdapterInternal(adapter, false, true);
        requestLayout();
    }
```


# 滑动

RecyclerView的滑动过程可以分为2个阶段：手指在屏幕上移动，使RecyclerView滑动的过程，可以称为scroll；手指离开屏幕，RecyclerView继续滑动一段距离的过程，可以称为fling。现在先看看RecyclerView的触屏事件处理onTouchEvent()方法：

# 缓存逻辑
与ListView不同，RecyclerView的缓存是分为多级的，但其实整个的缓存逻辑还是很容易理解的，Recycler的作用就是重用ItemView。在填充ItemView的时候，ItemView是从它获取的；滑出屏幕的ItemView是由它回收的。对于不同状态的ItemView存储在了不同的集合中，比如有scrapped、cached、exCached、recycled，当然这些集合并不是都定义在同一个类里。

回到之前的layoutChunk方法中，有行代码layoutState.next(recycler)，它的作用自然就是获取ItemView，我们进入这个方法查看，最终它会调用到RecyclerView.Recycler.getViewForPosition()方法：

```java
View getViewForPosition(int position, boolean dryRun) {
    boolean fromScrap = false;
    ViewHolder holder = null;
    if (mState.isPreLayout()) {
        holder = getChangedScrapViewForPosition(position);
        fromScrap = holder != null;
    }
    if (holder == null) {
        holder = getScrapViewForPosition(position, INVALID_TYPE, dryRun);
       ...
    }
    if (holder == null) {
        final int offsetPosition = mAdapterHelper.findPositionOffset(position);
        final int type = mAdapter.getItemViewType(offsetPosition);
        if (mAdapter.hasStableIds()) {
            holder = getScrapViewForId(mAdapter.getItemId(offsetPosition), type, dryRun);
        }
        if (holder == null && mViewCacheExtension != null) {
            final View view = mViewCacheExtension
                    .getViewForPositionAndType(this, position, type);
           ...
        }
        if (holder == null) { // fallback to recycler
            holder = getRecycledViewPool().getRecycledView(type);
            if (holder != null) {
                holder.resetInternal();
                if (FORCE_INVALIDATE_DISPLAY_LIST) {
                    invalidateDisplayListInt(holder);
                }
            }
        }
        if (holder == null) {
            holder = mAdapter.createViewHolder(RecyclerView.this, type);
        }
    }
   //生成LayoutParams的代码 ...
    return holder.itemView;
}
```



获取View的逻辑可以整理成如下：根据列表位置获取ItemView，先后从scrapped、cached、exCached、recycled集合中查找相应的ItemView，如果没有找到，就创建（Adapter.createViewHolder()），最后与数据集绑定。其中scrapped、cached和exCached集合定义在RecyclerView.Recycler中，分别表示将要在RecyclerView中删除的ItemView、一级缓存ItemView和二级缓存ItemView，cached集合的大小默认为２，exCached是需要我们通过RecyclerView.ViewCacheExtension自己实现的，默认没有；recycled集合其实是一个Map,private SparseArray<ArrayList<ViewHolder>> mScrap = new SparseArray<ArrayList<ViewHolder>>();，定义在RecyclerView.RecycledViewPool中，将ItemView以ItemType分类保存了下来，这里算是RecyclerView设计上的亮点，通过RecyclerView.RecycledViewPool可以实现在不同的RecyclerView之间共享ItemView，只要为这些不同RecyclerView设置同一个RecyclerView.RecycledViewPool就可以了。

上面解释了ItemView从不同集合中获取的方式，那么RecyclerView又是在什么时候向这些集合中添加ItemView的呢？下面我逐个介绍下。scrapped集合中存储的其实是正在执行REMOVE操作的ItemView，这部分会在后文进一步描述。在fill()方法的循环体中有行代码recycleByLayoutState(recycler, layoutState);，最终这个方法会执行到RecyclerView.Recycler.recycleViewHolderInternal()方法：


# [Recycler](http://blog.csdn.net/fyfcauc/article/details/54342303)
Recycler虽然命名上看，像是只承担了View回收的职责，其真正的定位是RecyclerView的View提供者(甚至是管理者), 包括生成新View, 复用旧View，回收View，重新绑定View等逻辑都被封装在Recycler中。外部调用者只需要调用Recycler的接口获取合适的View即可，不需要关心View获取和配置等具体细节，Recycler对外提供了View的回收和获取服务

先列一些概念:

1. View的detach和remove:
* detach: 在ViewGroup中的实现很简单，只是将ChildView**从ParentView的ChildView数组中移除，ChildView的mParent设置为null, 可以理解为轻量级的临时remove, 因为View此时和View树还是藕断丝连, 这个函数被经常用来改变ChildView在ChildView数组中的次序。** View被detach一般是临时的，在后面会被重新attach。
* remove: 真正的移除，不光被从ChildView数组中除名，其他和View树各项联系也会被彻底斩断(不考虑Animation/LayoutTransition这种特殊情况)， 比如焦点被清除，从TouchTarget中被移除等。
2. RecyclerView的Scrap View:
        Scrap View指的是在RecyclerView中，处于根据数据刷新界面等行为, ChildView被detach(注意这个detach指的是1中介绍的detach行为，而不是RecyclerView一部分注释中的”detach”，RecyclerView一部分注释中的”detach”其实指得是上面的remove)，并且被存储到了Recycler中，这部分ChildView就是Scrap View。
3. ViewHolder有一个Flag: FLAG_TMP_DETACHED代表的就是1中介绍的detach， 这也印证了2的推测，RecyclerView将remove视为”detach”, detach视为”tmp_detach”

Recycler**一般不会直接作用于View，其操作的对象一般是ViewHolder**。Recycler**分几个地方(代表不同层级)存放可复用的ViewHolder**:

    [一级缓存]: Scrap View: mAttachedScrap和mChangedScrap
    [一级缓存]: Removeed View: mCachedViews
    [二级缓存]: ViewCacheExtension(可选可配置): 供使用者自行扩展，让使用者可以控制缓存
    [三级缓存]: RecycledViewPool(可配置): RecyclerView之间共享ViewHolder的缓存池

Scrap VS Recycle:

    LayoutManager在布局时调用自己的detachAndScrapAttachedViews将当前所有的RecyclerView的ChildView进行回收,根据View的不同情况，会选择不同的回收方式， Scrap或Recycle:
        Recycle操作对应的是removeView, View被remove后调用Recycler的recycleViewHolderInternal回收其ViewHolder
        Scrap操作对应的是detachView，View被detach后调用Reccyler的scrapView暂存其ViewHolder
    在复用性上, 只被detach的View要比被remove的View高，detach的View**一般来说代表可以直接复用(其ViewHolder对应于Data的Position还是有效的，只需要重新绑定数据(如果数据也没变化的话，甚至都不用重新绑定数据)， View还是有效的，View绑定的数据可能有效的, 比如一个列表有N项，现在删除了其中一项，那么在没有其他变化的前提下，剩余的N-1个项对应的ViewHolder是可以直接复用的), 这一点非常关键, 避免了不必要的绑定(和ListView等相比)，项处理的粒度从整体细化到了单个项，即包含了对View的复用，也包含了对View当前绑定内容的复用**。
    被remove的View复用性上则要差一些，其对应的Position已经无效，这种复用层级和Scrap相比只有View层级的复用(稍带可以复用ViewHolder,只不过里面的信息要重新设置，但起码不用new一个)
    View在被回收时应该被Scrap还是Recycle可以这么判断:
        同时满足下面几条的ViewHodler会被Recycle:
            View**本身已经完全无效了，不光是数据，连ViewHolder对应的在Data中的Position都已经失效了(ViewHolder.isInvalid)
            View对应的项没有被remove(viewHolder.isRemoved, 这个判断是考虑到预加载的原因，先不具体说)**
            Adapter没有指定hasStableIds(因为如果指定了StableId， 那么就不存在View绑定内容无效的可能了)
        其他情况的ViewHolder均被Scrap

Scraped View: Scraped View在Recycler中又按照一定的规则被划分为两类

    mAttachedScrap: 被Remove/invalid或者数据仍然有效或者通过了canReuseUpdatedViewHolder检测的会被加入到mAttachedScrap中。
    mChangedScrap: 剩下的会被加入到m**Changed**Scrap中。

Recycled View: Recycled View**存放在mCachedViews中**.

ViewHolder是否可以被Recycle取决于ViewHolder的isRecyclable或者是否满足强制Recycle的条件
    mCachedViews **本身有容量限制，不可能无限制缓存View**
    如果缓存ViewHolder时发现超过了mCachedView的限制，会将最老的ViewHolder(也就是mCachedView缓存队列的第一个ViewHolder)移到RecycledViewPool中
    如果新的ViewHolder加入到mCachedView失败，也会被移动到RecycledViewPool中

RecycledViewPool:

    RecycledViewPool作为第三级ViewHolder缓存，立足于RecyclerView之间的ViewHolder共享。
    RecycledViewPool是有容量限制的, 以ViewType作为key来分类存放ViewHolder，每类ViewType都有单独容量限制，可以通过setMaxRecycledViews来为每种ViewType指定不同的容量限制。
    被加入到RecycledViewPool的ViewHolder会被reset，只保留itemView使得View可以被复用, 基本是一个半裸的ViewHolder。
    RecycledViewPool在没有被显式指定的情况下，如果被调用，RecyclerView会自动创建一个。
    RecycledViewPool还有一套attach/detach机制来在Adapter 变化时选择性的释放旧缓存。

ViewCacheExtension:

    ViewCacheExtension的定位是第二级View(注意，是View而非ViewHolder)缓存(可选的)，不过要注意，这一级缓存的定位比较特殊, RecyclerView在回收View时， 并不会将View放到ViewCacheExtension中，但是在提取View时，如果一级缓存不能满足需求，会尝试从ViewCacheExtension中提取，有点”只出不进”的意思， 潜在含义是，这一级View的回收时机需要ViewCacheExtension自己把握。
    ViewCacheExtension的实现者来自己决定内部View的缓存控制，只对外提供一个getViewForPositionAndType方法供Recycler使用。注释建议这个方法不要返回一个新创建的View，这一点比较好理解，如果每次都创建新的View，就失去了缓存的意义。
    目前看ViewCacheExtension的常规实现方式应该是动态的维护一个View池向外提供View，在合适的回调时机自己将View回收，不过就像这个类本身存在意义一样，它给了外部使用者自定义缓存控制策略的机会，有很大的实现自由度。

向外提供可用的View: getViewForPosition 和上面回收View的过程相反, 该函数封装了从各级缓存中提取View并判断以及在某些场景下生成和重新绑定View的复杂流程.

    下面的流程分析暂时先不考虑preLayout/StableId/Animation相关的逻辑.
    getScrapViewForPosition先从mAttachedScrapView/mChangedScrapView/hiddenView(之前介绍过的ChidlHelper机制: http://blog.csdn.net/fyfcauc/article/details/54175072) 和mCachedView中尝试提取可用缓存.
        先从mAttachedScrap中尝试获取。
        如果上一步没有得到合适的缓存，从HiddenView中尝试获取。
        如果上一步没有得到合适的缓存，从mCachedView中尝试获取。
    如果上一步没有得到可用的缓存，且设置了可用的mViewCacheExtension，那么尝试从mViewCacheExtension中获取
    如果上一步没有得到可用的缓存，尝试从RecycledViewPool中获取
    三级缓存全部搜索完，仍然没有找到合适的缓存，只能新建了： 调用Adapter的createViewHolder创建一个ViewHolder，其itemView会保存一个新创建的View
    检查历经上面步骤得到的ViewHolder**是否需要重新绑定数据(!holder.isBound() || holder.needsUpdate() || holder.isInvalid()), 如果需要，那么会调用Adapter的**bindViewHolder来绑定。
    之前说过，ViewHolder的存放位置被设定在了View的LayoutParam中，下面需要把View和其对应的ViewHolder关联起来,第一步是为View生成LayoutParam(如果没有或者不是指定类型的LayoutParam，有现成可用的就用现成的)，然后将ViewHolder保存在LayoutParam的mViewHolder中，而在这之前ViewHolder的itemView属性已经指向了得到的View，这样就实现了View和ViewHolder的双向关联, 代表着列表/表格/数据概念上的 “一项” 和 视图中的 “一项” 对应了起来。
        LayoutParam中除了维护一个mViewHolder外，还会维护mPendingInvalidate表示该View需要重绘,在被加入到RecyclerView后，会发起重绘请求(invalidate), 需要重绘的条件是: 该View是通过getScrapViewForPosition获取并且进行了数据绑定


http://www.jianshu.com/p/9ddfdffee5d3

# [ItemDecoration](http://www.jianshu.com/p/5f6151c1b6f8)
```java
    public void addItemDecoration(ItemDecoration decor, int index) {
        if (mLayout != null) {
            mLayout.assertNotInLayoutOrScroll("Cannot add item decoration during a scroll  or"
                    + " layout");
        }
        if (mItemDecorations.isEmpty()) {
            setWillNotDraw(false);
        }
        if (index < 0) {
            mItemDecorations.add(decor);
        } else {
            // 指定添加分割线在集合中的索引
            mItemDecorations.add(index, decor);
        }
        markItemDecorInsetsDirty();
        // 重新请求 View 的测量、布局、绘制
        requestLayout();
    }
```

mItemDecorations 是一个 ArrayList，我们将 ItemDecoration 也就是分割线对象，添加到其中。接着我们看下 markItemDecorInsetsDirty 这个方法做了些什么。
```java
    void markItemDecorInsetsDirty() {
        final int childCount = mChildHelper.getUnfilteredChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = mChildHelper.getUnfilteredChildAt(i);
            ((LayoutParams) child.getLayoutParams()).mInsetsDirty = true;
        }
        mRecycler.markItemDecorInsetsDirty();
    }
```
这个方法首先遍历了 RecyclerView 和 LayoutManager 的所有子 View，将其子 View 的 LayoutParams 中的 mInsetsDirty 属性置为 true。接着调用了 mRecycler.markItemDecorInsetsDirty()，Recycler 是 RecyclerView 的一个内部类，就是它管理着 RecyclerView 的复用逻辑。
```java
    void markItemDecorInsetsDirty() {
        final int cachedCount = mCachedViews.size();
        for (int i = 0; i < cachedCount; i++) {
            final ViewHolder holder = mCachedViews.get(i);
            LayoutParams layoutParams = (LayoutParams) holder.itemView.getLayoutParams();
            if (layoutParams != null) {
                layoutParams.mInsetsDirty = true;
            }
        }
    }
```
mCachedViews 见名知意，也就是 RecyclerView 缓存的集合，相信你也看到了，RecyclerView 的缓存单位是 ViewHolder。我们在 ViewHolder 中取出 itemView，然后获得 LayoutParams，将其 mInsetsDirty 字段一样置为 true。

mInsetsDirty 字段的作用其实是一种优化性能的缓存策略，添加分割线对象时，无论是 RecyclerView 的子 view，还是缓存的 view，都将其置为 true，接着就调用了 requestLayout 方法。

这里简单说一下 requestLayout 方法用一种责任链的方式，层层向上传递，最后传递到 ViewRootImpl，然后重新调用 view 的 measure、layout、draw 方法来展示布局。

我们在 RecyclerView 中搜索 mItemDecorations 集合，看看他是在什么时刻操作 ItemDecoration 这个分割线对象的。
```java
    @Override
    public void draw(Canvas c) {
        super.draw(c);

        final int count = mItemDecorations.size();
        for (int i = 0; i < count; i++) {
            mItemDecorations.get(i).onDrawOver(c, this, mState);
        }
        // TODO If padding is not 0 and clipChildrenToPadding is false, to draw glows properly, we
        // need find children closest to edges. Not sure if it is worth the effort.
        boolean needsInvalidate = false;
        if (mLeftGlow != null && !mLeftGlow.isFinished()) {
            final int restore = c.save();
            final int padding = mClipToPadding ? getPaddingBottom() : 0;
            c.rotate(270);
            c.translate(-getHeight() + padding, 0);
            needsInvalidate = mLeftGlow != null && mLeftGlow.draw(c);
            c.restoreToCount(restore);
        }
        if (mTopGlow != null && !mTopGlow.isFinished()) {
            final int restore = c.save();
            if (mClipToPadding) {
                c.translate(getPaddingLeft(), getPaddingTop());
            }
            needsInvalidate |= mTopGlow != null && mTopGlow.draw(c);
            c.restoreToCount(restore);
        }
        if (mRightGlow != null && !mRightGlow.isFinished()) {
            final int restore = c.save();
            final int width = getWidth();
            final int padding = mClipToPadding ? getPaddingTop() : 0;
            c.rotate(90);
            c.translate(-padding, -width);
            needsInvalidate |= mRightGlow != null && mRightGlow.draw(c);
            c.restoreToCount(restore);
        }
        if (mBottomGlow != null && !mBottomGlow.isFinished()) {
            final int restore = c.save();
            c.rotate(180);
            if (mClipToPadding) {
                c.translate(-getWidth() + getPaddingRight(), -getHeight() + getPaddingBottom());
            } else {
                c.translate(-getWidth(), -getHeight());
            }
            needsInvalidate |= mBottomGlow != null && mBottomGlow.draw(c);
            c.restoreToCount(restore);
        }

        // If some views are animating, ItemDecorators are likely to move/change with them.
        // Invalidate RecyclerView to re-draw decorators. This is still efficient because children's
        // display lists are not invalidated.
        if (!needsInvalidate && mItemAnimator != null && mItemDecorations.size() > 0 &&
                mItemAnimator.isRunning()) {
            needsInvalidate = true;
        }

        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }


    @Override
    public void onDraw(Canvas c) {
        super.onDraw(c);

        final int count = mItemDecorations.size();
        for (int i = 0; i < count; i++) {
            mItemDecorations.get(i).onDraw(c, this, mState);
        }
    }
```
可以看到在 View 的以上两个方法中，分别调用了 ItemDecoration 对象的 onDraw、onDrawOver 方法。

这两个抽象方法，由我们继承 ItemDecoration 来自己实现，他们区别就是 onDraw 在 item view 绘制之前调用，onDrawOver 在 item view 绘制之后调用。

所以绘制顺序就是 Decoration 的 onDraw，ItemView的 onDraw，Decoration 的 onDrawOver。

# LayoutManager
```java
    public void setLayoutManager(LayoutManager layout) {
        if (layout == mLayout) {
            return;
        }
        // 停止滑动
        stopScroll();
        // TODO We should do this switch a dispatchLayout pass and animate children. There is a good
        // chance that LayoutManagers will re-use views.
        if (mLayout != null) {
            // end all running animations
            if (mItemAnimator != null) {
                mItemAnimator.endAnimations();
            }
            // 移除并回收视图
            mLayout.removeAndRecycleAllViews(mRecycler);
            // 回收废弃视图
            mLayout.removeAndRecycleScrapInt(mRecycler);
            mRecycler.clear();

            if (mIsAttached) {
                mLayout.dispatchDetachedFromWindow(this, mRecycler);
            }
            mLayout.setRecyclerView(null);
            mLayout = null;
        } else {
            mRecycler.clear();
        }
        // this is just a defensive measure for faulty item animators.
        mChildHelper.removeAllViewsUnfiltered();
        mLayout = layout;
        if (layout != null) {
            if (layout.mRecyclerView != null) {
                throw new IllegalArgumentException("LayoutManager " + layout +
                        " is already attached to a RecyclerView: " + layout.mRecyclerView);
            }
            mLayout.setRecyclerView(this);
            if (mIsAttached) {
                mLayout.dispatchAttachedToWindow(this);
            }
        }
        mRecycler.updateViewCacheSize();
        requestLayout();
    }
```
这段代码主要做了一下几件事：
当之前设置过 LayoutManager 时，移除之前的视图，并缓存视图在 Recycler 中，将新的 mLayout 对象与 RecyclerView 绑定，更新缓存 View 的数量。最后去调用 requestLayout ，重新请求 measure、layout、draw。

# Recycler