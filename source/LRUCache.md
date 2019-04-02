http://hao.jobbole.com/disklrucache/


copied from glide
```java
public class LruCache<T, Y> {
    private final LinkedHashMap<T, Y> cache = new LinkedHashMap<T, Y>(100, 0.75f, true);
    private int maxSize;
    private final int initialMaxSize;
    private int currentSize = 0;

    public LruCache(int size) {
        this.initialMaxSize = size;
        this.maxSize = size;
    }

    public void setSizeMultiplier(float multiplier) {
        if (multiplier < 0) {
            throw new IllegalArgumentException("Multiplier must be >= 0");
        }
        maxSize = Math.round(initialMaxSize * multiplier);
        evict();
    }

    protected int getSize(Y item) {
        return 1;
    }

    protected void onItemEvicted(T key, Y item) {
        // optional override
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getCurrentSize() {
        return currentSize;
    }

    public boolean contains(T key) {
        return cache.containsKey(key);
    }

    public Y get(T key) {
        return cache.get(key);
    }

    public Y put(T key, Y item) {
        final int itemSize = getSize(item);
        if (itemSize >= maxSize) {
            onItemEvicted(key, item);
            return null;
        }

        final Y result = cache.put(key, item);
        if (item != null) {
            currentSize += getSize(item);
        }
        if (result != null) {
            // TODO: should we call onItemEvicted here?
            currentSize -= getSize(result);
        }
        evict();

        return result;
    }

    public Y remove(T key) {
        final Y value = cache.remove(key);
        if (value != null) {
            currentSize -= getSize(value);
        }
        return value;
    }

    public void clearMemory() {
        trimToSize(0);
    }

    protected void trimToSize(int size) {
        Map.Entry<T, Y> last;
        while (currentSize > size) {
            last = cache.entrySet().iterator().next();
            final Y toRemove = last.getValue();
            currentSize -= getSize(toRemove);
            final T key = last.getKey();
            cache.remove(key);
            onItemEvicted(key, toRemove);
        }
    }

    private void evict() {
        trimToSize(maxSize);
    }
}
```