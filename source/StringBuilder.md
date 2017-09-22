关于StringBuilder，字符串拼接要用StringBuilder，不要用"＋"

1. 初始长度很重要
StringBuilder的内部有一个char[]， 不断的append()就是不断的往char[]里填东西的过程。

new StringBuilder() 时char[]的默认长度是16，然后，如果要append第17个字符，怎么办？

用System.arraycopy成倍复制扩容！！！！

这样一来有数组拷贝的成本，二来原来的char[]也白白浪费了要被GC掉。可以想见，一个129字符长度的字符串，经过了16，32，64, 128四次的复制和丢弃，合共申请了496字符的数组，在高性能场景下，这几乎不能忍。

所以，合理设置一个初始值多重要。

但如果我实在估算不好呢？多估一点点好了，只要字符串最后大于16，就算浪费一点点，也比成倍的扩容好。

2. Liferay的StringBundler类

Liferay的StringBundler类提供了另一个长度设置的思路，它在append()的时候，不急着往char[]里塞东西，而是先拿一个String[]把它们都存起来，到了最后才把所有String的length加起来，构造一个合理长度的StringBuilder。

4. 重用StringBuilder
这个做法来源于JDK里的BigDecimal类（没事看看JDK代码多重要），后来发现Netty也同样使用。SpringSide里将代码提取成StringBuilderHolder，里面只有一个函数

public StringBuilder getStringBuilder() {
sb.setLength(0);
return sb;
}

StringBuilder.setLength()函数只重置它的count指针，而char[]则会继续重用，而toString()时会把当前的count指针也作为参数传给String的构造函数，所以不用担心把超过新内容大小的旧内容也传进去了。可见，StringBuilder是完全可以被重用的。

为了避免并发冲突，这个Holder一般设为ThreadLocal，标准写法见BigDecimal或StringBuilderHolder的注释。

5. ＋ 与 StringBuilder

String s ＝ “hello ” + user.getName();

这一句经过javac编译后的效果，的确等价于使用StringBuilder，但没有设定长度。

String s ＝ new StringBuilder().append(“hello”).append(user.getName());

但是，如果像下面这样：

String s ＝ “hello ”;
// 隔了其他一些语句
s = s ＋ user.getName();

每一条语句，都会生成一个新的StringBuilder，这里就有了两个StringBuilder，性能就完全不一样了。如果是在循环体里s+=i; 就更加多得没谱。
