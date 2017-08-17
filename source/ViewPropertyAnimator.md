ViewPropertyAnimator 提供了一种可以使多个属性同时做动画的简单方法，而且它在内部只使用一个 Animator。

虽然属性动画给我们提供了ValueAnimator类和ObjectAnimator类，在正常情况下，基本都能满足我们对动画操作的需求，但ValueAnimator类和ObjectAnimator类本身并不是针对View对象的而设计的，而我们在大多数情况下主要都还是对View进行动画操作的，因此Google官方在android 3.1系统中补充了ViewPropertyAnimator类，这个类便是专门为View动画而设计的。当然这个类不仅仅是为提供View而简单设计的，它存在以下优点： 


* 专门针对View对象动画而操作的类;
* 提供了更简洁的链式调用设置多个属性动画，这些动画可以同时进行的;
* 拥有更好的性能，多个属性动画是一次同时变化，只执行一次UI刷新（也就是只调用一次invalidate,而n个ObjectAnimator就会进行n次属性变化，就有n次invalidate）;
* 每个属性提供两种类型方法设置;
* 该类只能通过View的animate()获取其实例对象的引用.

# 使用

使用属性动画设置旋转360是这样的:
```java
    ObjectAnimator.ofFloat(btn,"rotation",360).setDuration(200).start();
```
如果是使用ViewPropertyAnimator:
```java
    btn.animate().rotation(360).setDuration(200);
```
# 原理解析

1. 通过imageView.animate()获取ViewPropertyAnimator对象。
2. 调用alpha、translationX等方法，返回当前ViewPropertyAnimator对象，可以继续链式调用
3. alpha、translationX等方法内部最终调用animatePropertyBy(int constantName, float startValue, float byValue)方法
4. 在animatePropertyBy方法中则会将alpha、translationX等方法的操作封装成NameVauleHolder，并将每个NameValueHolder对象添加到准备列表mPendingAnimations中。
5. animatePropertyBy方法启动mAnimationStarter，调用startAnimation，开始动画。
6. startAnimation方法中会创建一个ValueAnimator对象设置内部监听器AnimatorEventListener，并将mPendingAnimations和要进行动画的属性名称封装成一个PropertyBundle对象，最后mAnimatorMap保存当前Animator和对应的PropertyBundle对象。该Map将会在animatePropertyBy方法和Animator监听器mAnimatorEventListener中使用，启动动画。
7. 在动画的监听器的onAnimationUpdate方法中设置所有属性的变化值，并通过RenderNode类优化绘制性能，最后刷新界面。

```java
    /**
     * Utility function, called by animateProperty() and animatePropertyBy(), which handles the
     * details of adding a pending animation and posting the request to start the animation.
     *
     * @param constantName The specifier for the property being animated
     * @param startValue The starting value of the property
     * @param byValue The amount by which the property will change
     */
    private void animatePropertyBy(int constantName, float startValue, float byValue) {
        // First, cancel any existing animations on this property
        //判断该属性上是否存在运行的动画，存在则结束。
        if (mAnimatorMap.size() > 0) {
            Animator animatorToCancel = null;
            Set<Animator> animatorSet = mAnimatorMap.keySet();
            for (Animator runningAnim : animatorSet) {
                PropertyBundle bundle = mAnimatorMap.get(runningAnim);
                if (bundle.cancel(constantName)) {// 结束对应属性动画
                    // property was canceled - cancel the animation if it's now empty
                    // Note that it's safe to break out here because every new animation
                    // on a property will cancel a previous animation on that property, so
                    // there can only ever be one such animation running.
                    if (bundle.mPropertyMask == NONE) {//判断是否还有其他属性
                        // the animation is no longer changing anything - cancel it
                        animatorToCancel = runningAnim;
                        break;
                    }
                }
            }
            if (animatorToCancel != null) {
                animatorToCancel.cancel();
            }
        }
//将要执行的属性的名称，开始值，变化值封装成NameValuesHolder对象
        NameValuesHolder nameValuePair = new NameValuesHolder(constantName, startValue, byValue);
        //添加到准备列表中
        mPendingAnimations.add(nameValuePair);
        mView.removeCallbacks(mAnimationStarter);
        mView.postOnAnimation(mAnimationStarter);
    }
```

```java
static class NameValuesHolder {
    int mNameConstant;//要进行动画的属性名称
    float mFromValue;//开始值
    float mDeltaValue;//变化值
    NameValuesHolder(int nameConstant, float fromValue, float deltaValue) {
        mNameConstant = nameConstant;
        mFromValue = fromValue;
        mDeltaValue = deltaValue;
    }
}
```

mPendingAnimations：装载的是准备进行动画的属性值（NameValueHolder）所有列表，也就是每次要同时进行动画的全部属性的集合