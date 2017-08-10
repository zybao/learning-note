# 一些使用建议
1. 对Fragment传递数据，建议使用setArguments(Bundle args)，而后在onCreate中使用getArguments()取出，在 “内存重启”前，系统会帮你保存数据，不会造成数据的丢失。和Activity的Intent恢复机制类似。

2. 使用newInstance(参数) 创建Fragment对象，优点是调用者只需要关系传递的哪些数据，而无需关心传递数据的Key是什么。

3. 如果你需要在Fragment中用到宿主Activity对象，建议在你的基类Fragment定义一个Activity的全局变量，在onAttach中初始化，这不是最好的解决办法，但这可以有效避免一些意外Crash。

# add(), show(), hide(), replace()的那点事
1. 区别

show()，hide()最终是让Fragment的View setVisibility(true还是false)，不会调用生命周期；

replace()的话会销毁视图，即调用onDestoryView、onCreateView等一系列生命周期；

add()和 replace()不要在同一个阶级的FragmentManager里混搭使用。

2. onHiddenChanged的回调时机
当使用add()+show()，hide()跳转新的Fragment时，旧的Fragment回调onHiddenChanged()，不会回调onStop()等生命周期方法，而新的Fragment在创建时是不会回调onHiddenChanged()，这点要切记。

3. Fragment重叠问题
使用show()，hide()带来的一个问题就是，如果你不做任何额外处理，在“内存重启”后，Fragment会重叠；（该BUG在support-v4 24.0.0+以上 官方已修复）

* 如果你在用24.0.0+的版本，不需要特殊处理，官方已经修复该BUG；
* 如果你在使用小于24.0.0以下的v4包，可以参考：
```java
public class BaseFragment extends Fragment { 
    private static final String STATE_SAVE_IS_HIDDEN = "STATE_SAVE_IS_HIDDEN"; 
    @Override 
    public void onCreate(@Nullable Bundle savedInstanceState) {
        ... 
        if (savedInstanceState != null) { 
            boolean isSupportHidden = savedInstanceState.getBoolean(STATE_SAVE_IS_HIDDEN); 
            FragmentTransaction ft = getFragmentManager().beginTransaction(); 
            if (isSupportHidden) { 
                 ft.hide(this); 
            } else { 
                ft.show(this); 
            } 
        ft.commit(); 
        } 
        
        @Override 
        public void onSaveInstanceState(Bundle outState) { 
            ... 
            outState.putBoolean(STATE_SAVE_IS_HIDDEN, isHidden()); 
        } 
    }
```

# 关于FragmentManager你需要知道的
FragmentManager栈视图:

（1）每个Fragment以及宿主Activity(继承自FragmentActivity)都会在创建时，初始化一个FragmentManager对象，处理好Fragment嵌套问题的关键，就是理清这些不同阶级的栈视图。 

（2）对于宿主Activity，getSupportFragmentManager()获取的FragmentActivity的FragmentManager对象;

对于Fragment，getFragmentManager()是获取的是父Fragment(如果没有，则是FragmentActivity)的FragmentManager对象，而getChildFragmentManager()是获取自己的FragmentManager对象。

# 使用FragmentPagerAdapter+ViewPager的注意事项

使用FragmentPagerAdapter+ViewPager时，切换回上一个Fragment页面时（已经初始化完毕），不会回调任何生命周期方法以及onHiddenChanged()，只有setUserVisibleHint(boolean isVisibleToUser)会被回调，所以如果你想进行一些懒加载，需要在这里处理。

在给ViewPager绑定FragmentPagerAdapter时，
new FragmentPagerAdapter(fragmentManager)的FragmentManager，一定要保证正确，如果ViewPager是Activity内的控件，则传递getSupportFragmentManager()，如果是Fragment的控件中，则应该传递getChildFragmentManager()。只要记住ViewPager内的Fragments是当前组件的子Fragment这个原则即可。

你不需要考虑在“内存重启”的情况下，去恢复的Fragments的问题，因为FragmentPagerAdapter已经帮我们处理啦。
