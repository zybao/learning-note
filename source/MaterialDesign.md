# AppBarLayout

app:layout_scrollFlags="scroll|enterAlways"

scroll: 所有想滚动出屏幕的view都需要设置这个flag， 没有设置这个flag的view将被固定在屏幕顶部。

enterAlways: 设置这个flag时，向下的滚动都会导致该view变为可见。

enterAlwaysCollapsed: 当你的视图已经设置minHeight属性又使用此标志时，你的视图只能已最小高度进入，只有当滚动视图到达顶部时才扩大到完整高度。

exitUntilCollapsed: 滚动退出屏幕，最后折叠在顶端。

snap: 视图在滚动时会有一种“就近原则”，怎么说呢，就是当视图展开时，如果滑动中展 开的内容超过视图的75%,那么视图依旧会保持展开；当视图处于关闭时，如果滑动中展开的部分小于视图的25%，那么视图会保持关闭。总的来说，就是会让动画有一种弹性的视觉效果。

# CollapsingToolbarLayout

app:layout_collapseMode="parallax"

“pin”：固定模式，在折叠的时候最后固定在顶端；“parallax”：视差模式，在折叠的时候会有个视差折叠的效果

# NavigationView
# FloatingActionButton
# TextInputLayout
# Snackbar
# TabLayout
# AppBarLayout
# CoordinatorLayout
# CollapsingToolbarLayout
# Coordinator.Behavior
# CardView
# Palette
# Toolbar
# DrawerLayout
# SwipeRefreshLayout