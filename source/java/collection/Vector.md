Vector 是矢量队列，它是JDK1.0版本添加的类。继承于AbstractList，实现了List, RandomAccess, Cloneable这些接口。
Vector 继承了AbstractList，实现了List；所以，它是一个队列，支持相关的添加、删除、修改、遍历等功能。
Vector 实现了RandmoAccess接口，即提供了随机访问功能。RandmoAccess是java中用来被List实现，为List提供快速访问功能的。在Vector中，我们即可以通过元素的序号快速获取元素对象；这就是快速随机访问。
Vector 实现了Cloneable接口，即实现clone()函数。它能被克隆。


# Vector和ArrayList比较
1. 它们都是List
它们都继承于AbstractList，并且实现List接口。ArrayList和Vector的类定义如下：
```java
// ArrayList的定义
public class ArrayList<E> extends AbstractList<E>
        implements List<E>, RandomAccess, Cloneable, java.io.Serializable

// Vector的定义
public class Vector<E> extends AbstractList<E>
    implements List<E>, RandomAccess, Cloneable, java.io.Serializable {}
```

2. 它们都实现了RandomAccess和Cloneable接口

实现RandomAccess接口，意味着它们都支持快速随机访问；
实现Cloneable接口，意味着它们能克隆自己。

3. 它们都是通过数组实现的，本质上都是动态数组

ArrayList.java中定义数组elementData用于保存元素
```java
// 保存ArrayList中数据的数组
private transient Object[] elementData;
// Vector.java中也定义了数组elementData用于保存元素
// 保存Vector中数据的数组
protected Object[] elementData;
```

4. 它们的默认数组容量是10

若创建ArrayList或Vector时，没指定容量大小；则使用默认容量大小10。

ArrayList的默认构造函数如下：
```java
// ArrayList构造函数。默认容量是10。
public ArrayList() {
    this(10);
}
```
Vector的默认构造函数如下：
```java
// Vector构造函数。默认容量是10。
public Vector() {
    this(10);
} 
```
5. 它们都支持Iterator和listIterator遍历

它们都继承于AbstractList，而AbstractList中分别实现了 “iterator()接口返回Iterator迭代器” 和 “listIterator()返回ListIterator迭代器”。

## 不同点
1. 线程安全性不一样

ArrayList是非线程安全；
而Vector是线程安全的，它的函数都是synchronized的，即都是支持同步的。
ArrayList适用于单线程，Vector适用于多线程。

2. 对序列化支持不同

ArrayList支持序列化，而Vector不支持；即ArrayList有实现java.io.Serializable接口，而Vector没有实现该接口。

3. 构造函数个数不同 ArrayList有3个构造函数，而Vector有4个构造函数。Vector除了包括和ArrayList类似的3个构造函数之外，另外的一个构造函数可以指定容量增加系数。

4. 容量增加方式不同

逐个添加元素时，若ArrayList容量不足时，“新的容量”=“(原始容量x3)/2 + 1”。
而Vector的容量增长与“增长系数有关”，若指定了“增长系数”，且“增长系数有效(即，大于0)”；那么，每次容量不足时，“新的容量”=“原始容量+增长系数”。若增长系数无效(即，小于/等于0)，则“新的容量”=“原始容量 x 2”。

5. 对Enumeration的支持不同。Vector支持通过Enumeration去遍历，而List不支持
# Stack
