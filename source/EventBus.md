https://juejin.im/entry/580f100a2f301e005c4ded79

http://www.jianshu.com/p/b3486441d7df

http://www.jianshu.com/p/b622334096fe

# 构造器
```java
    public EventBus() {
        this(DEFAULT_BUILDER);
    }

    EventBus(EventBusBuilder builder) {
        // 以事件类的class对象为键值，记录注册方法信息，值为一个Subscription的列表
        subscriptionsByEventType = new HashMap<>();
        // 以注册的类为键值，记录该类所注册的所有事件类型，值为一个Event的class对象的列表
        typesBySubscriber = new HashMap<>();
        // 记录sticky事件
        stickyEvents = new ConcurrentHashMap<>();
        // 三个Poster, 负责在不同的线程中调用订阅者的方法
        mainThreadPoster = new HandlerPoster(this, Looper.getMainLooper(), 10);
        backgroundPoster = new BackgroundPoster(this);
        asyncPoster = new AsyncPoster(this);
        indexCount = builder.subscriberInfoIndexes != null ? builder.subscriberInfoIndexes.size() : 0;
        // 方法的查找类，用于查找某个类中有哪些注册的方法
        subscriberMethodFinder = new SubscriberMethodFinder(builder.subscriberInfoIndexes,
                builder.strictMethodVerification, builder.ignoreGeneratedIndex);
        logSubscriberExceptions = builder.logSubscriberExceptions;
        logNoSubscriberMessages = builder.logNoSubscriberMessages;
        sendSubscriberExceptionEvent = builder.sendSubscriberExceptionEvent;
        sendNoSubscriberEvent = builder.sendNoSubscriberEvent;
        throwSubscriberException = builder.throwSubscriberException;
        eventInheritance = builder.eventInheritance;
        executorService = builder.executorService;
    }
```

首先来看管理注册信息的两个Map, 由于一个类中可能有多个方法监听多个事件，所以Subscription这个类封装一个注册信息，这个类很简单，只有三个属性，如下：
```java
final class Subscription {
    final Object subscriber;
    final SubscriberMethod subscriberMethod;
    /**
     * Becomes false as soon as {@link EventBus#unregister(Object)} is called, which is checked by queued event delivery
     * {@link EventBus#invokeSubscriber(PendingPost)} to prevent race conditions.
     */
    volatile boolean active;
}

/** Used internally by EventBus and generated subscriber indexes. */
public class SubscriberMethod {
    final Method method;
    final ThreadMode threadMode;
    final Class<?> eventType;
    final int priority;
    final boolean sticky;
    /** Used for efficient comparison */
    String methodString;

        private synchronized void checkMethodString() {
        if (methodString == null) {
            // Method.toString has more overhead, just take relevant parts of the method
            StringBuilder builder = new StringBuilder(64);
            builder.append(method.getDeclaringClass().getName());
            builder.append('#').append(method.getName());
            builder.append('(').append(eventType.getName());
            methodString = builder.toString();
        }
    }
}
```

所以类和方法唯一确定一条注册信息，active表示该注册信息是否有效(如注释所说它的作用)。所以在EventBus中，使用subscriptionsByEventType，以Event的class对象为键值，管理注册信息，值为一个处理事件类型为该键值的Subscription的列表。typesBySubscriber则相对简单，就是记录一个类注册了哪些事件类型。虽然二者有所冗余，它们在后面的注册和调度过程中都是为了便于查询，这样更为高效。

然后是stickyEvents，是一个线程安全的Map,用来记录sticky事件，sticky事件的含义是指即使被观察者发送sticky事件是在订阅者订阅该事件之前，订阅者在订阅之后，EventBus将该事件发送到该订阅者，即调用相应的订阅方法。（如果在之后，那就和普通事件一样）

接下来的三个Poster极为重要，但是这里理解很容易，就是负责在不同线程中调用方法，他们分别对应着threadMode中除去POSTING之外三种类型。

最后是一个Finder，由于在EventBus的使用简便，都是以对象为单位调用registe()方法，但是一个对象中可能有多个注册方法，所以注册过程中需要subscriberMethodFinder查找一个类中有哪些注册方法，最后生成一个Subscrip，并存在subscriptionsByEventType的某一个列表中。

EventBus的构造器很简单，这里重点讲述了一下其中的几个比较重要的成员变量，尤其是管理注册信息的数据结构，下一步就是注册源码的分析。

# 注册
```java
    public void register(Object subscriber) {
        Class<?> subscriberClass = subscriber.getClass();
        List<SubscriberMethod> subscriberMethods = subscriberMethodFinder.findSubscriberMethods(subscriberClass);
        synchronized (this) {
            for (SubscriberMethod subscriberMethod : subscriberMethods) {
                subscribe(subscriber, subscriberMethod);
            }
        }
    }
```
思路很明确，两个步骤，一是查找注册方法的列表，这个利用到了SubscriberMethodFinder的对应方法，查找一个类中有哪些注册方法，而是调用订阅方法，参数为类和方法两个，即subscriber和subscriberMethod。查找我们留到下一部分，先看订阅方法：
```java
    // Must be called in synchronized block
    private void subscribe(Object subscriber, SubscriberMethod subscriberMethod) {
        Class<?> eventType = subscriberMethod.eventType;
        Subscription newSubscription = new Subscription(subscriber, subscriberMethod);
        CopyOnWriteArrayList<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
        if (subscriptions == null) {
            subscriptions = new CopyOnWriteArrayList<>();
            subscriptionsByEventType.put(eventType, subscriptions);
        } else {
            if (subscriptions.contains(newSubscription)) {
                throw new EventBusException("Subscriber " + subscriber.getClass() + " already registered to event "
                        + eventType);
            }
        }

        int size = subscriptions.size();
        for (int i = 0; i <= size; i++) {
            if (i == size || subscriberMethod.priority > subscriptions.get(i).subscriberMethod.priority) {
                subscriptions.add(i, newSubscription);
                break;
            }
        }

        List<Class<?>> subscribedEvents = typesBySubscriber.get(subscriber);
        if (subscribedEvents == null) {
            subscribedEvents = new ArrayList<>();
            typesBySubscriber.put(subscriber, subscribedEvents);
        }
        subscribedEvents.add(eventType);

        if (subscriberMethod.sticky) {
            if (eventInheritance) {
                // Existing sticky events of all subclasses of eventType have to be considered.
                // Note: Iterating over all events may be inefficient with lots of sticky events,
                // thus data structure should be changed to allow a more efficient lookup
                // (e.g. an additional map storing sub classes of super classes: Class -> List<Class>).
                Set<Map.Entry<Class<?>, Object>> entries = stickyEvents.entrySet();
                for (Map.Entry<Class<?>, Object> entry : entries) {
                    Class<?> candidateEventType = entry.getKey();
                    if (eventType.isAssignableFrom(candidateEventType)) {
                        Object stickyEvent = entry.getValue();
                        checkPostStickyEventToSubscription(newSubscription, stickyEvent);
                    }
                }
            } else {
                Object stickyEvent = stickyEvents.get(eventType);
                checkPostStickyEventToSubscription(newSubscription, stickyEvent);
            }
        }
    }
```
代码虽然有点长，但是逻辑很简单清晰，就是构建Subscription, 然后将该注册信息保存到两个数据结构中。对于subscriptionsByEventType首先获取EventType对应的列表，没有则创建，重复注册则抛异常，正常情况下，则根据priority插入到列表中适合的位置。对于typesBySubscriber，则是更新该subscriber对应的列表即可。最后是处理sticky事件，即在注册时，如果是监听sticky事件，则需要从stickyEvents中取出对应sticky事件，并发送到订阅者。这里需要注意eventInheritance是一个开关，表示是否处理EventType的继承关系，默认为true，如代码中，EventBus会向订阅者发送该类型的事件，以及该类型所有子类类型的事件。由于订阅者监听一个sticky事件，那么该sticky事件的子类型也可以认为是该类型的事件，所以订阅者也同样会接收到该事件。最后checkPostStickyEventToSubscription方法如下：
```java
    private void checkPostStickyEventToSubscription(Subscription newSubscription, Object stickyEvent) {
        if (stickyEvent != null) {
            // If the subscriber is trying to abort the event, it will fail (event is not tracked in posting state)
            // --> Strange corner case, which we don't take care of here.
            postToSubscription(newSubscription, stickyEvent, Looper.getMainLooper() == Looper.myLooper());
        }
    }
```

与注册相反的unrigister由于比较简单，这里一并说了，下面是其代码：
```java
    /** Unregisters the given subscriber from all event classes. */
    public synchronized void unregister(Object subscriber) {
        List<Class<?>> subscribedTypes = typesBySubscriber.get(subscriber);
        if (subscribedTypes != null) {
            for (Class<?> eventType : subscribedTypes) {
                unsubscribeByEventType(subscriber, eventType);
            }
            typesBySubscriber.remove(subscriber);
        } else {
            Log.w(TAG, "Subscriber to unregister was not registered before: " + subscriber.getClass());
        }
    }
```
逻辑很明确，调用unsubscribeByEventType方法，并更新typesBySubscriber数据结构，那么你可能猜到了，unsubscribeByEventType方法中就是更新另一个数据结构，代码如下：
```java
    /** Only updates subscriptionsByEventType, not typesBySubscriber! Caller must update typesBySubscriber. */
    private void unsubscribeByEventType(Object subscriber, Class<?> eventType) {
        List<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
        if (subscriptions != null) {
            int size = subscriptions.size();
            for (int i = 0; i < size; i++) {
                Subscription subscription = subscriptions.get(i);
                if (subscription.subscriber == subscriber) {
                    subscription.active = false;
                    subscriptions.remove(i);
                    i--;
                    size--;
                }
            }
        }
    }
```

这里逻辑很简单，但是有一点值得注意的是就遍历删除列表时，注意序号i和size的改变，容易出现数组越界的错误，遍历删除通常使用倒序的方式，不容易出现错误，这里动态改变size变量也是一样。注销过程需要考虑sticky事件，也不需要查找过程，所以过程很简单。
register和unregister的过程除了查找订阅方法以外，逻辑很简单，就是更新一下管理注册信息的数据结构，可以看出数据结构的设计还是很重要的，设计的好，逻辑就会很清晰，程序代码也就相对简洁易懂。下一步就是介绍事件的分发。

# 事件分发
在分析post()方法之前，先看EventBus的一个内部类，PostingThreadState：
```java
    /** For ThreadLocal, much faster to set (and get multiple values). */
    final static class PostingThreadState {
        final List<Object> eventQueue = new ArrayList<Object>();
        boolean isPosting;
        boolean isMainThread;
        Subscription subscription;
        Object event;
        boolean canceled;
    }
```
这个内部类只有几个属性变量，其中我们现在只需要注意第一个，事件消息队列即可。如注释所言，这个类的实例对象在EventBus中是一个ThreadLocal变量，即线程本地变量，不同线程之间不会相互影响，而eventQueue则是用来保存当前线程需要发送的事件（为什么会有队列，是因为POST线程也就是调用post()方法的线程与调用订阅者方法的线程不同，在POST线程中连续的调用post()方法发送事件，会造成事件的累积）。后面的四个变量都是与cancelEventDelivery()方法有关，在后面对其进行分析。

下面为post()方法的代码：
```java
/** Posts the given event to the event bus. */
public void post(Object event) {
    PostingThreadState postingState = currentPostingThreadState.get();
    List eventQueue = postingState.eventQueue;
    eventQueue.add(event);

    if (!postingState.isPosting) {
        postingState.isMainThread = Looper.getMainLooper() == Looper.myLooper();
        postingState.isPosting = true;
        if (postingState.canceled) {
            throw new EventBusException("Internal error. Abort state was not reset");
        }
        try {
            while (!eventQueue.isEmpty()) {
                postSingleEvent(eventQueue.remove(0), postingState);
            }
        } finally {
            postingState.isPosting = false;
            postingState.isMainThread = false;
        }
    }
}
```
首先需要明确的是，如注释所言，post()的方法是将事件发送到EventBus，至于何时调用订阅者的方法则有EventBus调度。从代码中看postingState是一个ThreadLocal,用于保存当前post事件的状态。post()方法就是设置一些postState的属性，然后遍历事件消息队列，调用postSingleEvent()方法，其代码如下：
```java
private void postSingleEvent(Object event, PostingThreadState postingState) throws Error {
    Class eventClass = event.getClass();
    boolean subscriptionFound = false;
    if (eventInheritance) {
        List> eventTypes = lookupAllEventTypes(eventClass);
        int countTypes = eventTypes.size();
        for (int h = 0; h < countTypes; h++) {
            Class clazz = eventTypes.get(h);
            subscriptionFound |= postSingleEventForEventType(event, postingState, clazz);
        }
    } else {
        subscriptionFound = postSingleEventForEventType(event, postingState, eventClass);
    }
    if (!subscriptionFound) {
        if (logNoSubscriberMessages) {
            Log.d(TAG, "No subscribers registered for event " + eventClass);
        }
        if (sendNoSubscriberEvent && eventClass != NoSubscriberEvent.class &&
                eventClass != SubscriberExceptionEvent.class) {
            post(new NoSubscriberEvent(this, event));
        }
    }
}
```

这个方法中同样也用到了eventInheritance这个开关，即是否考虑Event事件类型的继承关系，默认为true，这里的lookupAllEventTypes()方法是EventBus的静态方法，查找eventClass所有包括自己在内的父类以及它们所实现的接口，然后对于没有eventClass，调用postSingleForEventType()方法，返回的结果为是否找到了对应的订阅方法，在没有找到的情况下，会做出打印Log信息和发送事件处理，这里的logNoSubscriberMessages和sendNoSubscriberEvent是EventBus的开关属性，与eventInheritance类似，也可以在Builder中设置，默认为true。如果当我们调用post()方法发出某个事件时想知道我们的事件有没有被订阅者接收，就可以在发送消息的类中接收NoSubscriberEvent事件，如果收到该事件说明应用中没有订阅者接收我们发出的事件。
这里在看源码时有一点点小疑问，就是在注册时处理sticky事件时是找到Event的所有子类并发送给该订阅者，而这里是Event的所有父类，并将其发送出去。前者是站在订阅者的角度上，订阅者在注册时要求接收某个sticky事件，那么该事件的所有子类也是该sticky事件的一种，所以应该发送给该订阅者。比如一个订阅者订阅天气预报的sticky事件，那么如果在stickyEvent中有一个今天下雨的事件（假设该事件继承自天气预报，随便想的例子，可能不太恰当）也应该发送给该订阅者。而此处postSingleEvent方法中是发送单个Event，是站在发送者发送到EventBus的角度上，我需要发送某个事件通知订阅者，比如我发送一个天气预报的事件到EventBus, 但是EventBus由于不知道天气预报是否表示下雨，就不应该通知哪些是否下雨事件的订阅者，相反那些监听新闻的订阅者（假设天气预报继承自新闻，而天气预报是一条新闻），EventBus需要负责通知他们这条天气预报的新闻。被观察者没有将新闻发送到EventBus上，但是EventBus则因为一条天气预报需要找出新闻并发送到相应的订阅者上，所以发送是两个过程。接下来是postSingleEventForEventType方法，其代码如下:
```java
private boolean postSingleEventForEventType(Object event, PostingThreadState postingState, Class eventClass) {
    CopyOnWriteArrayList subscriptions;
    synchronized (this) {
        subscriptions = subscriptionsByEventType.get(eventClass);
    }
    if (subscriptions != null && !subscriptions.isEmpty()) {
        for (Subscription subscription : subscriptions) {
            postingState.event = event;
            postingState.subscription = subscription;
            boolean aborted = false;
            try {
                postToSubscription(subscription, event, postingState.isMainThread);
                aborted = postingState.canceled;
            } finally {
                postingState.event = null;
                postingState.subscription = null;
                postingState.canceled = false;
            }
            if (aborted) {
                break;
            }
        }
        return true;
    }
    return false;
}
```

这里首先说明CopyOnWriteArrayList是为了线程安全，每次对List的修改都会重新一份，由于是线程安全的所以不需要同步处理，但是对HashMap的读取操作则不是线程安全的，所以需要线程同步。这个方法的逻辑也很简单，就是从subscriptionsByEventType中找出事件类型对应的注册信息列表，然后遍历调用postToSubscription()方法，这个方法有些熟悉了，就是在注册中处理sticky事件时调用的方法。不过这里需要注意的是abort每次都会读取postingState的cancel状态判断发送事件是否被终止，而另外两个遍历 event和subscription的赋值和清空也是为了用于cancelEventDelivery()方法，后面会统一说。最后终于到了postToSubscription()方法，其代码为：
```java
private void postToSubscription(Subscription subscription, Object event, boolean isMainThread) {
    switch (subscription.subscriberMethod.threadMode) {
        case POSTING:
            invokeSubscriber(subscription, event);
            break;
        case MAIN:
            if (isMainThread) {
                invokeSubscriber(subscription, event);
            } else {
                mainThreadPoster.enqueue(subscription, event);
            }
            break;
        case BACKGROUND:
            if (isMainThread) {
                backgroundPoster.enqueue(subscription, event);
            } else {
                invokeSubscriber(subscription, event);
            }
            break;
        case ASYNC:
            asyncPoster.enqueue(subscription, event);
            break;
        default:
            throw new IllegalStateException("Unknown thread mode: " + subscription.subscriberMethod.threadMode);
    }
}
```
这里就到了真正的事件分发了，分为我们所熟知的四种threadModed的情形，其代码逻辑很清晰，根据threadMode以及isMainThread选择调用invokeSubscriber方法还是加入相应的队列，异步执行。关于入队异步执行放在第三篇中讲述，重点分析三个Poster的实现，这里先贴出invokeSubscriber的代码，其实就是利用反射调用订阅者的方法，方法存储在subsription的subsribeMethod变量中：
```java
void invokeSubscriber(Subscription subscription, Object event) {
    try {
        subscription.subscriberMethod.method.invoke(subscription.subscriber, event);
    } catch (InvocationTargetException e) {
        handleSubscriberException(subscription, event, e.getCause());
    } catch (IllegalAccessException e) {
        throw new IllegalStateException("Unexpected exception", e);
    }
}
```
到这里事件的分发就结束了，梳理一下就是一下流程：1. post(Object event) -> 2. postSingleEvent(Object event, PostingThreadState postingState) -> 3. postSingleEventForEventType(Object event, PostingThreadState postingState, Class eventClass) -> 4. postToSubscription(Subscription subscription, Object event, boolean isMainThread).
其中第一步是遍历一个线程本地变量中保存的事件消息队列的所有消息，第二步是遍历一个事件类型的所有父类，第三步是遍历一个事件类型的所有注册信息，第四步则是事件分发，根据threadMode选择合适的处理方式。
读到这里就感觉优秀代码的确是逻辑结构十分清晰，看起来一目了然，在一个任务中合理的划分步骤，拆分成多个方法，让人更容易理解。

接下来就是前面一直提到的cancelEventDelivery方法：
```java
/**
 * Called from a subscriber's event handling method, further event delivery will be canceled. Subsequent
 * subscribers
 * won't receive the event. Events are usually canceled by higher priority subscribers (see
 * {@link Subscribe#priority()}). Canceling is restricted to event handling methods running in posting thread
 * {@link ThreadMode#POSTING}.
 */
public void cancelEventDelivery(Object event) {
    PostingThreadState postingState = currentPostingThreadState.get();
    if (!postingState.isPosting) {
        throw new EventBusException(
                "This method may only be called from inside event handling methods on the posting thread");
    } else if (event == null) {
        throw new EventBusException("Event may not be null");
    } else if (postingState.event != event) {
        throw new EventBusException("Only the currently handled event may be aborted");
    } else if (postingState.subscription.subscriberMethod.threadMode != ThreadMode.POSTING) {
        throw new EventBusException(" event handlers may only abort the incoming event");
    }

    postingState.canceled = true;
}
```
这里重点先看一下注释，这个方法的作用就是在事件处理方法中调用，终止事件的进一步传递，这个和android系统中的顺序广播是相同的道理（即通过提高自己的的优先权可以率先收到事件，然后截取该事件）。但是这里有一个条件就是事件处理方法的threadMode必须是POSTING，即执行线程与事件的发送线程为同一个线程。
从代码中可以看出终止事件条件十分苛刻，PostState中的三个变量都是用来判断是否可以终止事件的，第一个是isPosting，该变量在post方法中被设置true，表示一个事件类型及其父类正在被发送的状态中，发送完毕以后才被设置为false。所以在这个过程以外都不可以调用该方法。最后两个条件postSingleEventForEventType中被设置的，对于一个事件类型（不包括其父类）遍历它的所有注册信息，针对每一个注册信息调用postToSubscription方法之前和之后这两个变量都会被设置，就是为了在这里判断，是否可以在某一个注册信息的方法被调用时终止这个事件的继续发送。（这里有一点不明白，一个事件的所有注册信息遍历时，事件是同一个，为什么在遍历过程中都要赋值和清空，这里是否可以改成设置一次和清空一次即可？）。最后在设置了canceled变量以后（如果cancelEventDelivery在事件处理方法中被调用了），事件处理方法返回之后，postToSubscription接着返回，canceled被赋值到了abort变量，这时候abort如果为true, 则break跳出循环，从而终止了该事件类型的继续发送到其他的注册信息。（注意这里不会影响其他事件类型，如其父类事件等）

看到这里也就明白了为什么限制在threadMode为POSTING的事件处理方法中调用cancelEventDelivery方法了，这是因为post()之后的一系列方法是在事件发送的线程中执行，而这些状态字的赋值与判断必须处在同一线程中才能有效，所以事件处理方法必须与post()方法处在同一线程，所以也就只能是POSTING模式下才能保证。

最后invokeSubscriber方法中在调用订阅者方法失败时有一个异常处理方法，其代码如下：
```java
private void handleSubscriberException(Subscription subscription, Object event, Throwable cause) {
    if (event instanceof SubscriberExceptionEvent) {
        if (logSubscriberExceptions) {
            // Don't send another SubscriberExceptionEvent to avoid infinite event recursion, just log
            Log.e(TAG, "SubscriberExceptionEvent subscriber " + subscription.subscriber.getClass()
                    + " threw an exception", cause);
            SubscriberExceptionEvent exEvent = (SubscriberExceptionEvent) event;
            Log.e(TAG, "Initial event " + exEvent.causingEvent + " caused exception in "
                    + exEvent.causingSubscriber, exEvent.throwable);
        }
    } else {
        if (throwSubscriberException) {
            throw new EventBusException("Invoking subscriber failed", cause);
        }
        if (logSubscriberExceptions) {
            Log.e(TAG, "Could not dispatch event: " + event.getClass() + " to subscribing class "
                    + subscription.subscriber.getClass(), cause);
        }
        if (sendSubscriberExceptionEvent) {
            SubscriberExceptionEvent exEvent = new SubscriberExceptionEvent(this, cause, event,
                    subscription.subscriber);
            post(exEvent);
        }
    }
}
```
代码逻辑很简单，通常由于是发送的是我们自定义的Event, 所以会走else, 接着就会根据开关抛异常打log以及发送事件等，读一下这段代码也有利于我们以后调试有关EventBus的相关问题。

# Sticky事件
在注册部分我们提到过sticky事件，即在订阅时即可以接收到之前post出去的sticky事件以及其子类事件，下面为postSticky()方法：
```java
/**
 * Posts the given event to the event bus and holds on to the event (because it is sticky). The most recent sticky
 * event of an event's type is kept in memory for future access by subscribers using {@link Subscribe#sticky()}.
 */
public void postSticky(Object event) {
    synchronized (stickyEvents) {
        stickyEvents.put(event.getClass(), event);
    }
    // Should be posted after it is putted, in case the subscriber wants to remove immediately
    post(event);
}
```
之前可能对于sticky的解释不太清楚，不过这个方法的注释则对sticky事件的解释很清晰，浅显易懂。接着需要注意方法中的那句注释，就是需要先将sticky事件保存到stickyEvents中在调用post()方法，是为了防止remove失败，如在事件处理方法中调用removeStickyEvent，remove在put之前则会造成remove失败，post方法返回以后event又被添加到stickyEvents，与我们期望的就有差别了。接下来是sticky事件的remove方法：
```java
 /**
 * Remove and gets the recent sticky event for the given event type.
 *
 * @see #postSticky(Object)
 */
public  T removeStickyEvent(Class eventType) {
    synchronized (stickyEvents) {
        return eventType.cast(stickyEvents.remove(eventType));
    }
}

/**
 * Removes the sticky event if it equals to the given event.
 *
 * @return true if the events matched and the sticky event was removed.
 */
public boolean removeStickyEvent(Object event) {
    synchronized (stickyEvents) {
        Class eventType = event.getClass();
        Object existingEvent = stickyEvents.get(eventType);
        if (event.equals(existingEvent)) {
            stickyEvents.remove(eventType);
            return true;
        } else {
            return false;
        }
    }
}
/**
 * Removes all sticky events.
 */
public void removeAllStickyEvents() {
    synchronized (stickyEvents) {
        stickyEvents.clear();
    }
}
```
这几个方法都比较容易理解，不再解释了，最后关于sticky还有一个get方法，如下：
```java
/**
 * Gets the most recent sticky event for the given type.
 *
 * @see #postSticky(Object)
 */
public  T getStickyEvent(Class eventType) {
    synchronized (stickyEvents) {
        return eventType.cast(stickyEvents.get(eventType));
    }
}
```
最后贴出来两个静态方法，就是前面提到的查找事件类型和它所有父类以及它们的实现的所有接口的方法，对于过程的分析无关紧要，有兴趣的可以看一下源码，如下：
```java
/** Looks up all Class objects including super classes and interfaces. Should also work for interfaces. */
private static List> lookupAllEventTypes(Class eventClass) {
    synchronized (eventTypesCache) {
        List> eventTypes = eventTypesCache.get(eventClass);
        if (eventTypes == null) {
            eventTypes = new ArrayList<>();
            Class clazz = eventClass;
            while (clazz != null) {
                eventTypes.add(clazz);
                addInterfaces(eventTypes, clazz.getInterfaces());
                clazz = clazz.getSuperclass();
            }
            eventTypesCache.put(eventClass, eventTypes);
        }
        return eventTypes;
    }
}

/** Recurses through super interfaces. */
static void addInterfaces(List> eventTypes, Class[] interfaces) {
    for (Class interfaceClass : interfaces) {
        if (!eventTypes.contains(interfaceClass)) {
            eventTypes.add(interfaceClass);
            addInterfaces(eventTypes, interfaceClass.getInterfaces());
        }
    }
}
```