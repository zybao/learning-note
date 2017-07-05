# 获取Class对象
* forName
* class文件
* getClass()

# 创建实例
(1)
```java
Class<?> clazz = String.class;
Object obj = clazz.newInstance();
```

(2)
```java
Class<?> clazz = String.class;
Constructor c = clazz.getConstructor(String.class);
Object obj = c.newInstance("12345");
```

# 获取方法
* getDeclaredMethods()方法返回类或接口声明的所有方法，包括公共、保护、默认（包）访问和私有方法，但不包括继承的方法。
`public Method[] getDeclaredMethods() throws SecurityException`
* getMethods()方法返回某个类的所有公用（public）方法，包括其继承类的公用方法。
`public Method[] getMethods() throws SecurityException`
* getMethod方法返回一个特定的方法，其中第一个参数为方法名称，后面的参数为方法的参数对应Class的对象
`public Method getMethod(String name, Class<?>... parameterTypes)`

# 获取类的成员变量（字段）信息
getFiled: 访问公有的成员变量

getDeclaredField：所有已声明的成员变量。但不能得到其父类的成员变量

getFileds和getDeclaredFields用法同上（参照Method）

