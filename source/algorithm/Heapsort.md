* 最坏时间复杂度 O(nlog n)
* 最优时间复杂度 O(nlog n)
* 平均时间复杂度 O(nlog n)
* 空间复杂度 O(n)

# maxHeapify
```C++
void max_heapify(int arr[], int start, int end) {
    int dad = start;
    int son = dad * 2 + 1;
    while(son <= end) {
        if (son + 1 <= end && arr[son] < arr[song + 1]) {
            son++;
        }

        if (arr[dad] > arr[son]) return;
        else {
            swap(arr[dad], arr[son]);
            dad = son;
            son = dad * 2 + 1;
        }
    }
}
```
# buildMaxHeap
# HeapSort
```C++
void heap_sort(int arr[], int len) {
    for (i = len / 2 - 1; i >= 0; i--) {
        max_heapify(arr, i, len - 1);
        for (int i = len - 1; i > 0; i--) {
            swap(arr[0], arr[i]);
            max_heapify(arr, 0, i - 1);
        }
    }
}
```