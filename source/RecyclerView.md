# [Recycler](http://blog.csdn.net/fyfcauc/article/details/54342303)
Recycler虽然命名上看，像是只承担了View回收的职责，其真正的定位是RecyclerView的View提供者(甚至是管理者), 包括生成新View, 复用旧View，回收View，重新绑定View等逻辑都被封装在Recycler中。外部调用者只需要调用Recycler的接口获取合适的View即可，不需要关心View获取和配置等具体细节，Recycler对外提供了View的回收和获取服务

先列一些概念:

1. View的detach和remove:
* detach: 在ViewGroup中的实现很简单，只是将ChildView**从ParentView的ChildView数组中移除，ChildView的mParent设置为null, 可以理解为轻量级的临时remove, 因为View此时和View树还是藕断丝连, 这个函数被经常用来改变ChildView在ChildView数组中的次序。**View被detach一般是临时的，在后面会被重新attach。
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