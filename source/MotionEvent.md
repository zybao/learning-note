# ACTION_POINTER_DOWN

已经有一个触摸点的情况下，有新出现了一个触摸点

# ACTION_POINTER_UP

在多个触摸点存在的情况下，其中一个触摸点消失了

# getAction 和 getActionMasked 的区别

getAction获得的int值是由pointer的index值和事件类型值组合而成的，而getActionWithMasked则只返回事件的类型值

一般来说，getAction() & ACTION_POINTER_INDEX_MASK就获得了pointer的id,等同于getActionIndex函数;getAction()& ACTION_MASK就获得了pointer的事件类型，等同于getActionMasked函数。


