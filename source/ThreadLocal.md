# ThreadLocal
## set()
```java
    /**
     * Sets the current thread's copy of this thread-local variable
     * to the specified value.  Most subclasses will have no need to
     * override this method, relying solely on the {@link #initialValue}
     * method to set the values of thread-locals.
     *
     * @param value the value to be stored in the current thread's copy of
     *        this thread-local.
     */
    public void set(T value) {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
    }
```

```java
    /**
     * Get the map associated with a ThreadLocal. Overridden in
     * InheritableThreadLocal.
     *
     * @param  t the current thread
     * @return the map
     */
    ThreadLocalMap getMap(Thread t) {
        return t.threadLocals;
    }
```

threadLocals 是线程 Thread 类的一个属性，这个属性是 ThreadLocalMap类型的.
当我们调用 threadLocal.set() 的时候，首先会得到该线程的 threadLocals，
然后就会把该threadlocal 和 值 作为一个键值对，放入到该线程的 threadLocals 中。

## get()
```java
    /**
     * Returns the value in the current thread's copy of this
     * thread-local variable.  If the variable has no value for the
     * current thread, it is first initialized to the value returned
     * by an invocation of the {@link #initialValue} method.
     *
     * @return the current thread's value of this thread-local
     */
    public T get() {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null) {
            ThreadLocalMap.Entry e = map.getEntry(this);
            if (e != null) {
                @SuppressWarnings("unchecked")
                T result = (T)e.value;
                return result;
            }
        }
        return setInitialValue();
    }
```

```java
    private T setInitialValue() {
        T value = initialValue();
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
        return value;
    }
```

* 每个线程都有 一个 ThreadLocalMap 类型的 threadLocals 属性;
* ThreadLocalMap 类相当于一个Map，key 是 ThreadLocal 本身，value 就是我们的值;
* 当我们通过 threadLocal.set(new Integer(123)); ，我们就会在这个线程中的 threadLocals 属性中放入一个键值对，key 是 这个 threadLocal.set(new Integer(123)); 的 threadlocal，value 就是值;
* 当我们通过 threadlocal.get() 方法的时候，首先会根据这个线程得到这个线程的 threadLocals 属性，然后由于这个属性放的是键值对，我们就可以根据键 threadlocal 拿到值。 注意，这时候这个键 threadlocal 和 我们 set 方法的时候的那个键 threadlocal 是一样的，所以我们能够拿到相同的值。

# InheritableThreadLocal
```java
public class InheritableThreadLocal<T> extends ThreadLocal<T> {
    protected T childValue(T parentValue) {
        return parentValue;
    }

    ThreadLocalMap getMap(Thread t) {
       return t.inheritableThreadLocals;
    }

    void createMap(Thread t, T firstValue) {
        t.inheritableThreadLocals = new ThreadLocalMap(this, firstValue);
    }
}
```
与`ThreadLocal`不同的是，这里的`get()`得到的是`Thread`下的另一个属性`inheritableThreadLocals`。

当我们创建新的线程时，我们会先复制当前线程的值到新线程的`inheritableThreadLocal`中，这样就
避免了在新线程中得到的`threadlocals`中没有东西。这里的复制过程中是浅拷贝，key和value都是原来的引用地址。


构造方法里有一段：
```java
if (parent.inheritableThreadLocals != null)
    this.inheritableThreadLocals = ThreadLocal.createInheritedMap(parent.inheritableThreadLocals);
```
```java
static ThreadLocalMap createInheritedMap(ThreadLocalMap parentMap) {
    return new ThreadLocalMap(parentMap);
}

        /**
         * Construct a new map including all Inheritable ThreadLocals
         * from given parent map. Called only by createInheritedMap.
         *
         * @param parentMap the map associated with parent thread.
         */
        private ThreadLocalMap(ThreadLocalMap parentMap) {
            Entry[] parentTable = parentMap.table;
            int len = parentTable.length;
            setThreshold(len);
            table = new Entry[len];

            for (int j = 0; j < len; j++) {
                Entry e = parentTable[j];
                if (e != null) {
                    @SuppressWarnings("unchecked")
                    ThreadLocal<Object> key = (ThreadLocal<Object>) e.get();
                    if (key != null) {
                        Object value = key.childValue(e.value);
                        Entry c = new Entry(key, value);
                        int h = key.threadLocalHashCode & (len - 1);
                        while (table[h] != null)
                            h = nextIndex(h, len);
                        table[h] = c;
                        size++;
                    }
                }
            }
        }
```

* 首先要理解 为什么 在 新线程中得不到值，是因为*我们其实是根据 `Thread.currentThread()`，拿到该线程的 `threadlocals`，从而进一步得到我们之前预先 `set` 好的值。那么如果我们新开一个线程，这个时候，由于 `Thread.currentThread()` 已经变了，从而导致获得的 `threadlocals` 不一样，我们之前并没有在这个新的线程的 `threadlocals` 中放入值，那么我就再通过 `threadlocal.get()`方法 是不可能拿到值的。*
* 那么解决办法就是 我们在新线程中，要把父线程的 `threadlocals` 的值 给复制到 新线程中的 `threadlocals` 中来。这样，我们在新线程中得到的 `threadlocals` 才会有东西，再通过 `threadlocal.get()` 中的 `threadlocal`，就会得到值。

# 线程池的情况 [transmittable-thread-local](https://github.com/alibaba/transmittable-thread-local)
