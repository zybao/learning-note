为了提高性能，该容器提供了一个优化：当删除key键时，不是立马删除这一项，而是留下需要删除的选项给一个删除的标记。该条目可以被重新用于相同的key,或者被单个垃圾收集器逐步删除完全部的条目后压缩。在任何时候，当数组需要增长（注：这里我理解为 put、append之类的操作）或者Map 的长度、entry的value需要被检索的时候，该垃圾收集器就会执行（注：这里可以从代码中发现，google在相应的方法中调用 gc() 方法）。
```java
    /**
     * Removes the mapping from the specified key, if there was any.
     */
    public void delete(int key) {
        int i = ContainerHelpers.binarySearch(mKeys, mSize, key);

        if (i >= 0) {
            if (mValues[i] != DELETED) {
                mValues[i] = DELETED;
                mGarbage = true;
            }
        }
    }

    /**
     * Adds a mapping from the specified key to the specified value,
     * replacing the previous mapping from the specified key if there
     * was one.
     */
    public void put(int key, E value) {
        int i = ContainerHelpers.binarySearch(mKeys, mSize, key);

        if (i >= 0) {
            mValues[i] = value;
        } else {
            i = ~i;

            if (i < mSize && mValues[i] == DELETED) {
                mKeys[i] = key;
                mValues[i] = value;
                return;
            }

            if (mGarbage && mSize >= mKeys.length) {
                gc();

                // Search again because indices may have changed.
                i = ~ContainerHelpers.binarySearch(mKeys, mSize, key);
            }

            mKeys = GrowingArrayUtils.insert(mKeys, mSize, i, key);
            mValues = GrowingArrayUtils.insert(mValues, mSize, i, value);
            mSize++;
        }
    }

    private void gc() {
        // Log.e("SparseArray", "gc start with " + mSize);

        int n = mSize;
        int o = 0;
        int[] keys = mKeys;
        Object[] values = mValues;

        for (int i = 0; i < n; i++) {
            Object val = values[i];

            if (val != DELETED) {
                if (i != o) {
                    keys[o] = keys[i];
                    values[o] = val;
                    values[i] = null;
                }

                o++;
            }
        }

        mGarbage = false;
        mSize = o;

        // Log.e("SparseArray", "gc end with " + mSize);
    }
```
|类名|key|value|
|---|---|-----|
|HashMap|任意类型|任意类型|
|SparseArray|Integer|任意类型| 
|SparseBooleanArray|Integer|Boolean类型|    
|SparseIntArray|Integer|Integer类型|    
|LongSparseArray|Long|任意类型|