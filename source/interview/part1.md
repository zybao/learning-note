# [LruCache 底层原理](http://www.cnblogs.com/sjm19910902/p/6438598.html)
LruCache是一个泛型类，它内部采用LinkedHashMap,并以强引用的方式存储外界的缓存对象，提供get和put方法来完成缓存的获取和添加操作。当缓存满时，LruCache会移除较早的缓存对象，然后再添加新的缓存对象。

介绍源码前 先介绍LinkedHashMap一些特性

LinkedHashMap实现与HashMap的不同之处在于，后者维护着一个运行于所有条目的双重链接列表。此链接列表定义了迭代顺序，该迭代顺序可以是插入顺序或者是访问顺序。

对于LinkedHashMap而言，它继承与HashMap、底层使用哈希表与双向链表来保存所有元素。其基本操作与父类HashMap相似，它通过重写父类相关的方法，来实现自己的链接列表特性
 
1) Entry元素：

    LinkedHashMap采用的hash算法和HashMap相同，但是它重新定义了数组中保存的元素Entry，该Entry除了保存当前对象的引用外，还保存了其上一个元素before和下一个元素after的引用，从而在哈希表的基础上又构成了双向链接列表。
```java
    /**
    * 双向链表的表头元素。
    */
    private transient Entry<K,V> header;
 
    /**
    * LinkedHashMap的Entry元素。
    * 继承HashMap的Entry元素，又保存了其上一个元素before和下一个元素after的引用。
    */
    private static class Entry<K,V> extends HashMap.Entry<K,V> {
        Entry<K,V> before, after;
        ……
    }
```
 
2) 读取：

    LinkedHashMap重写了父类HashMap的get方法，实际在调用父类getEntry()方法取得查找的元素后，再判断当排序模式accessOrder为true时，记录访问顺序，将最新访问的元素添加到双向链表的表头（这个特性保证了LRU最近最少使用），并从原来的位置删除。由于的链表的增加、删除操作是常量级的，故并不会带来性能的损失。

```java
    @Override 
    public V get(Object key) {
       /*
        * This method is overridden to eliminate the need for a polymorphic
        * invocation in superclass at the expense of code duplication.
        */
       if (key == null) {
           HashMapEntry<K, V> e = entryForNullKey;
           if (e == null)
               return null;
           if (accessOrder)
               makeTail((LinkedEntry<K, V>) e);
           return e.value;
       }
 
       int hash = Collections.secondaryHash(key);
       HashMapEntry<K, V>[] tab = table;
       for (HashMapEntry<K, V> e = tab[hash & (tab.length - 1)];
               e != null; e = e.next) {
           K eKey = e.key;
           if (eKey == key || (e.hash == hash && key.equals(eKey))) {
               if (accessOrder)
                   makeTail((LinkedEntry<K, V>) e);
               return e.value;
           }
       }
       return null;
   }
 
   /**
    * Relinks the given entry to the tail of the list. Under access ordering,
    * this method is invoked whenever the value of a  pre-existing entry is
    * read by Map.get or modified by Map.put.
    */
   private void makeTail(LinkedEntry<K, V> e) {
       // Unlink e
       e.prv.nxt = e.nxt;
       e.nxt.prv = e.prv;
 
       // Relink e as tail
       LinkedEntry<K, V> header = this.header;
       LinkedEntry<K, V> oldTail = header.prv;
       e.nxt = header;
       e.prv = oldTail;
       oldTail.nxt = header.prv = e;
       modCount++;
   }
```
## LruCache源码
```java
public class LruCache<K, V> {
    private final LinkedHashMap<K, V> map;
 
    /** Size of this cache in units. Not necessarily the number of elements. */
    private int size;//当前缓存大小
    private int maxSize;//缓存最大
 
    private int putCount;//put次数
    private int createCount;
    private int evictionCount;//回收次数
    private int hitCount;//命中次数
    private int missCount;//没有命中次数
 
    /**
     * @param maxSize for caches that do not override {@link #sizeOf}, this is
     *     the maximum number of entries in the cache. For all other caches,
     *     this is the maximum sum of the sizes of the entries in this cache.
     */
    public LruCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        this.maxSize = maxSize;
        this.map = new LinkedHashMap<K, V>(0, 0.75f, true);
    }
 
    /**
     * Sets the size of the cache.
     *
     * @param maxSize The new maximum size.
     */
    public void resize(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
 
        synchronized (this) {
            this.maxSize = maxSize;
        }
        trimToSize(maxSize);
    }
 
    /**
     *  返回缓存中key对应的value,如果不存在则创建一个并返回。
     *  如果value被返回，它就会被移动到队列的头部，如果value为null或者不能被创建，方法返回nul
     */
    public final V get(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }
 
        V mapValue;
        synchronized (this) {
            mapValue = map.get(key);
            if (mapValue != null) {
                hitCount++;
                return mapValue;
            }
            missCount++;
        }
 
        /*
         * 如果未被命中，则试图创建一个value.这将会消耗较长时间，创建过程中，
     * 如果要添加的value值和map中已有的值冲突，则释放已经创建value.
         */
 
        V createdValue = create(key);
        if (createdValue == null) {
            return null;
        }
 
        synchronized (this) {
            createCount++;
            mapValue = map.put(key, createdValue);
 
            if (mapValue != null) {
                // There was a conflict so undo that last put
                map.put(key, mapValue);
            } else {
                size += safeSizeOf(key, createdValue);
            }
        }
 
        if (mapValue != null) {
            entryRemoved(false, key, createdValue, mapValue);
            return mapValue;
        } else {
      //判断缓存是否越界
            trimToSize(maxSize);
            return createdValue;
        }
    }
 
    /**
     * 缓存key对应的value.value 会被移动至队列头部。
     * the queue.
     *
     * @return the previous value mapped by {@code key}.
     */
    public final V put(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException("key == null || value == null");
        }
 
        V previous;
        synchronized (this) {
            putCount++;
            size += safeSizeOf(key, value);
            previous = map.put(key, value);
            if (previous != null) {
                size -= safeSizeOf(key, previous);
            }
        }
 
        if (previous != null) {
            entryRemoved(false, key, previous, value);
        }
 
        trimToSize(maxSize);
        return previous;
    }
 
    /**
     * Remove the eldest entries until the total of remaining entries is at or
     * below the requested size.
     *
     * @param maxSize the maximum size of the cache before returning. May be -1
     *            to evict even 0-sized elements.
     */
    public void trimToSize(int maxSize) {
        while (true) {
            K key;
            V value;
            synchronized (this) {
                if (size < 0 || (map.isEmpty() && size != 0)) {
                    throw new IllegalStateException(getClass().getName()
                            + ".sizeOf() is reporting inconsistent results!");
                }
 
                if (size <= maxSize) {
                    break;
                }
 
                Map.Entry<K, V> toEvict = map.eldest();
                if (toEvict == null) {
                    break;
                }
 
                key = toEvict.getKey();
                value = toEvict.getValue();
                map.remove(key);
                size -= safeSizeOf(key, value);
                evictionCount++;
            }
 
            entryRemoved(true, key, value, null);
        }
    }
 
    /**
     * Removes the entry for {@code key} if it exists.
     *
     * @return the previous value mapped by {@code key}.
     */
    public final V remove(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }
 
        V previous;
        synchronized (this) {
            previous = map.remove(key);
            if (previous != null) {
                size -= safeSizeOf(key, previous);
            }
        }
 
        if (previous != null) {
            entryRemoved(false, key, previous, null);
        }
 
        return previous;
    }
 
    /**
     * Called for entries that have been evicted or removed. This method is
     * invoked when a value is evicted to make space, removed by a call to
     * {@link #remove}, or replaced by a call to {@link #put}. The default
     * implementation does nothing.
     *
     * <p>The method is called without synchronization: other threads may
     * access the cache while this method is executing.
     *
     * @param evicted true if the entry is being removed to make space, false
     *     if the removal was caused by a {@link #put} or {@link #remove}.
     * @param newValue the new value for {@code key}, if it exists. If non-null,
     *     this removal was caused by a {@link #put}. Otherwise it was caused by
     *     an eviction or a {@link #remove}.
     */
    protected void entryRemoved(boolean evicted, K key, V oldValue, V newValue) {}
 
    /**
     * Called after a cache miss to compute a value for the corresponding key.
     * Returns the computed value or null if no value can be computed. The
     * default implementation returns null.
     *
     * <p>The method is called without synchronization: other threads may
     * access the cache while this method is executing.
     *
     * <p>If a value for {@code key} exists in the cache when this method
     * returns, the created value will be released with {@link #entryRemoved}
     * and discarded. This can occur when multiple threads request the same key
     * at the same time (causing multiple values to be created), or when one
     * thread calls {@link #put} while another is creating a value for the same
     * key.
     */
    protected V create(K key) {
        return null;
    }
 
    private int safeSizeOf(K key, V value) {
        int result = sizeOf(key, value);
        if (result < 0) {
            throw new IllegalStateException("Negative size: " + key + "=" + value);
        }
        return result;
    }
 
    /**
     * Returns the size of the entry for {@code key} and {@code value} in
     * user-defined units.  The default implementation returns 1 so that size
     * is the number of entries and max size is the maximum number of entries.
     *
     * <p>An entry's size must not change while it is in the cache.
     */
    protected int sizeOf(K key, V value) {
        return 1;
    }
 
    /**
     * Clear the cache, calling {@link #entryRemoved} on each removed entry.
     */
    public final void evictAll() {
        trimToSize(-1); // -1 will evict 0-sized elements
    }
 
    /**
     * For caches that do not override {@link #sizeOf}, this returns the number
     * of entries in the cache. For all other caches, this returns the sum of
     * the sizes of the entries in this cache.
     */
    public synchronized final int size() {
        return size;
    }
 
    /**
     * For caches that do not override {@link #sizeOf}, this returns the maximum
     * number of entries in the cache. For all other caches, this returns the
     * maximum sum of the sizes of the entries in this cache.
     */
    public synchronized final int maxSize() {
        return maxSize;
    }
 
    /**
     * Returns the number of times {@link #get} returned a value that was
     * already present in the cache.
     */
    public synchronized final int hitCount() {
        return hitCount;
    }
 
    /**
     * Returns the number of times {@link #get} returned null or required a new
     * value to be created.
     */
    public synchronized final int missCount() {
        return missCount;
    }
 
    /**
     * Returns the number of times {@link #create(Object)} returned a value.
     */
    public synchronized final int createCount() {
        return createCount;
    }
 
    /**
     * Returns the number of times {@link #put} was called.
     */
    public synchronized final int putCount() {
        return putCount;
    }
 
    /**
     * Returns the number of values that have been evicted.
     */
    public synchronized final int evictionCount() {
        return evictionCount;
    }
 
    /**
     * Returns a copy of the current contents of the cache, ordered from least
     * recently accessed to most recently accessed.
     */
    public synchronized final Map<K, V> snapshot() {
        return new LinkedHashMap<K, V>(map);
    }
 
    @Override 
    public synchronized final String toString() {
        int accesses = hitCount + missCount;
        int hitPercent = accesses != 0 ? (100 * hitCount / accesses) : 0;
        return String.format("LruCache[maxSize=%d,hits=%d,misses=%d,hitRate=%d%%]",
                maxSize, hitCount, missCount, hitPercent);
    }
}
```

总结
      
1. LruCache 是基于 Lru 算法实现的一种缓存机制;
2. Lru算法的原理是把近期最少使用的数据给移除掉，当然前提是当前数据的量大于设定的最大值;
3. LruCache 没有真正的释放内存，只是从 Map中移除掉数据，真正释放内存还是要用户手动释放。

# [图片缓存](http://www.jianshu.com/p/97455f080065)
图片的三级缓存机制一般是指应用加载图片的时候，分别去访问内存，文件和网络而获取图片数据的一种行为。

## Glide

## picasso