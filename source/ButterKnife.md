
```java
  private static Unbinder createBinding(@NonNull Object target, @NonNull View source) {
    Class<?> targetClass = target.getClass();
    if (debug) Log.d(TAG, "Looking up binding for " + targetClass.getName());
    Constructor<? extends Unbinder> constructor = findBindingConstructorForClass(targetClass);

    if (constructor == null) {
      return Unbinder.EMPTY;
    }

    //noinspection TryWithIdenticalCatches Resolves to API 19+ only type.
    try {
      return constructor.newInstance(target, source);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Unable to invoke " + constructor, e);
    } catch (InstantiationException e) {
      throw new RuntimeException("Unable to invoke " + constructor, e);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      if (cause instanceof Error) {
        throw (Error) cause;
      }
      throw new RuntimeException("Unable to create binding instance.", cause);
    }
  }
```

```java
  @Nullable @CheckResult @UiThread
  private static Constructor<? extends Unbinder> findBindingConstructorForClass(Class<?> cls) {
    Constructor<? extends Unbinder> bindingCtor = BINDINGS.get(cls);
    if (bindingCtor != null) {
      if (debug) Log.d(TAG, "HIT: Cached in binding map.");
      return bindingCtor;
    }
    String clsName = cls.getName();
    if (clsName.startsWith("android.") || clsName.startsWith("java.")) {
      if (debug) Log.d(TAG, "MISS: Reached framework class. Abandoning search.");
      return null;
    }
    try {
      Class<?> bindingClass = cls.getClassLoader().loadClass(clsName + "_ViewBinding");
      //noinspection unchecked
      bindingCtor = (Constructor<? extends Unbinder>) bindingClass.getConstructor(cls, View.class);
      if (debug) Log.d(TAG, "HIT: Loaded binding class and constructor.");
    } catch (ClassNotFoundException e) {
      if (debug) Log.d(TAG, "Not found. Trying superclass " + cls.getSuperclass().getName());
      bindingCtor = findBindingConstructorForClass(cls.getSuperclass());
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("Unable to find binding constructor for " + clsName, e);
    }
    BINDINGS.put(cls, bindingCtor);
    return bindingCtor;
  }
```

# AbstractProcessor介绍
AbstractProcessor有四个比较重要的方法：
```java
@Override
public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
}

@Override
public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    // ...
    return false;
}

@Override
public Set<String> getSupportedAnnotationTypes() {
    return super.getSupportedAnnotationTypes();
}

@Override
public SourceVersion getSupportedSourceVersion() {
    return super.getSupportedSourceVersion();
}
```

## init
初始化处理器，提供以下:

* Elements：一个用来处理Element的工具类，例如获取元素的包和注释等。
* Types：一个用来处理TypeMirror的工具类明，例如获取类型的超类型等。
* Filer：顾名思义，使用Filer你可以创建文件。

## process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv);
* annotations: 需要被处理的注解集合
* roundEnv: 通过这个可以查询出包含特定注解的被注解元素

## getSupportedAnnotationTypes
返回你需要处理的注解集合，可以@SupportedAnnotationTypes代替。

## getSupportedSourceVersion
指定Java版本，可以用@SupportedSourceVersion代替。