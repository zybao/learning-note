Observable是观察者模式的典型应用。在Android下，Observable是一个泛型的抽象类，表示一个观察者对象，提供了观察者注册、反注册及清空三个方法，其源码如下：

```java
public abstract class DataSetObserver {
    /**
     * This method is called when the entire data set has changed,
     * most likely through a call to {@link Cursor#requery()} on a {@link Cursor}.
     */
    public void onChanged() {
        // Do nothing
    }

    /**
     * This method is called when the entire data becomes invalid,
     * most likely through a call to {@link Cursor#deactivate()} or {@link Cursor#close()} on a
     * {@link Cursor}.
     */
    public void onInvalidated() {
        // Do nothing
    }
}

public abstract class Observable<T> {
    /**
     * The list of observers.  An observer can be in the list at most
     * once and will never be null.
     */
    protected final ArrayList<T> mObservers = new ArrayList<T>();

    /**
     * Adds an observer to the list. The observer cannot be null and it must not already
     * be registered.
     * @param observer the observer to register
     * @throws IllegalArgumentException the observer is null
     * @throws IllegalStateException the observer is already registered
     */
    public void registerObserver(T observer) {
        if (observer == null) {
            throw new IllegalArgumentException("The observer is null.");
        }
        synchronized(mObservers) {
            if (mObservers.contains(observer)) {
                throw new IllegalStateException("Observer " + observer + " is already registered.");
            }
            mObservers.add(observer);
        }
    }

    /**
     * Removes a previously registered observer. The observer must not be null and it
     * must already have been registered.
     * @param observer the observer to unregister
     * @throws IllegalArgumentException the observer is null
     * @throws IllegalStateException the observer is not yet registered
     */
    public void unregisterObserver(T observer) {
        if (observer == null) {
            throw new IllegalArgumentException("The observer is null.");
        }
        synchronized(mObservers) {
            int index = mObservers.indexOf(observer);
            if (index == -1) {
                throw new IllegalStateException("Observer " + observer + " was not registered.");
            }
            mObservers.remove(index);
        }
    }

    /**
     * Remove all registered observers.
     */
    public void unregisterAll() {
        synchronized(mObservers) {
            mObservers.clear();
        }
    }
}
```

Observable的直接继承者有两个：DataSetObservable和ContentObservable。ContentObservable实现比较复杂，不过功能与DataSetObservable类似，这里只讲解DataSetObservable。
DataSetObservable在很多的Adapter中都用到，像BaseAdapter。DataSetObservable使用DataSetObserver实例化了Observable。DataSetObserver表示了一个数据集对象的观察者，主要提供了两个方法：
```java
public abstract class DataSetObserver {
    /**
     * This method is called when the entire data set has changed,
     * most likely through a call to {@link Cursor#requery()} on a {@link Cursor}.
     */
    public void onChanged() {
        // Do nothing
    }

    /**
     * This method is called when the entire data becomes invalid,
     * most likely through a call to {@link Cursor#deactivate()} or {@link Cursor#close()} on a
     * {@link Cursor}.
     */
    public void onInvalidated() {
        // Do nothing
    }
}

public class DataSetObservable extends Observable<DataSetObserver> {
    /**
     * Invokes {@link DataSetObserver#onChanged} on each observer.
     * Called when the contents of the data set have changed.  The recipient
     * will obtain the new contents the next time it queries the data set.
     */
    public void notifyChanged() {
        synchronized(mObservers) {
            // since onChanged() is implemented by the app, it could do anything, including
            // removing itself from {@link mObservers} - and that could cause problems if
            // an iterator is used on the ArrayList {@link mObservers}.
            // to avoid such problems, just march thru the list in the reverse order.
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onChanged();
            }
        }
    }

    /**
     * Invokes {@link DataSetObserver#onInvalidated} on each observer.
     * Called when the data set is no longer valid and cannot be queried again,
     * such as when the data set has been closed.
     */
    public void notifyInvalidated() {
        synchronized (mObservers) {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onInvalidated();
            }
        }
    }
}
```

当数据集有变化时，会调用DataSetObserver的onChanged()方法；当数据集失效时，会调用DataSetObserver的onINvalidated()方法。