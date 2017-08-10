
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

# ButterKnifeProcessor
process:

```java
  private void parseBindView(Element element, Map<TypeElement, BindingSet.Builder> builderMap,
      Set<TypeElement> erasedTargetNames) {
    TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

    // 判断是否被注解在属性上，如果该属性是被 private 或者 static 修饰的，则出错
    // 判断是否被注解在错误的包中，若包名以“android”或者“java”开头，则出错
    // Start by verifying common generated code restrictions.
    boolean hasError = isInaccessibleViaGeneratedCode(BindView.class, "fields", element)
        || isBindingInWrongPackage(BindView.class, element);

    // Verify that the target type extends from View.
    TypeMirror elementType = element.asType();
    if (elementType.getKind() == TypeKind.TYPEVAR) {
      TypeVariable typeVariable = (TypeVariable) elementType;
      elementType = typeVariable.getUpperBound();
    }
    Name qualifiedName = enclosingElement.getQualifiedName();
    Name simpleName = element.getSimpleName();
    // 判断元素是不是View及其子类或者Interface
    if (!isSubtypeOfType(elementType, VIEW_TYPE) && !isInterface(elementType)) {
      if (elementType.getKind() == TypeKind.ERROR) {
        note(element, "@%s field with unresolved type (%s) "
                + "must elsewhere be generated as a View or interface. (%s.%s)",
            BindView.class.getSimpleName(), elementType, qualifiedName, simpleName);
      } else {
        error(element, "@%s fields must extend from View or be an interface. (%s.%s)",
            BindView.class.getSimpleName(), qualifiedName, simpleName);
        hasError = true;
      }
    }
    // 如果有错误，直接返回
    if (hasError) {
      return;
    }

    // Assemble information on the field.
    int id = element.getAnnotation(BindView.class).value();
    // 根据所在的类元素去查找 builder
    BindingSet.Builder builder = builderMap.get(enclosingElement);
    QualifiedId qualifiedId = elementToQualifiedId(element, id);
    // 如果相应的 builder 已经存在
    if (builder != null) {
      // 验证 ID 是否已经被绑定
      String existingBindingName = builder.findExistingBindingName(getId(qualifiedId));
      // 被绑定了，出错，返回
      if (existingBindingName != null) {
        error(element, "Attempt to use @%s for an already bound ID %d on '%s'. (%s.%s)",
            BindView.class.getSimpleName(), id, existingBindingName,
            enclosingElement.getQualifiedName(), element.getSimpleName());
        return;
      }
    } else {
      // 如果没有相应的 builder，就需要重新生成，并别存放到  builderMap 中
      builder = getOrCreateBindingBuilder(builderMap, enclosingElement);
    }

    String name = simpleName.toString();
    TypeName type = TypeName.get(elementType);
    boolean required = isFieldRequired(element);

    builder.addField(getId(qualifiedId), new FieldViewBinding(name, type, required));

    // Add the type-erased version to the valid binding targets set.
    erasedTargetNames.add(enclosingElement);
  }
```

```java
  private Map<TypeElement, BindingSet> findAndParseTargets(RoundEnvironment env) {
    Map<TypeElement, BindingSet.Builder> builderMap = new LinkedHashMap<>();
    Set<TypeElement> erasedTargetNames = new LinkedHashSet<>();

    scanForRClasses(env);

    ···

    // Process each @BindView element.
    for (Element element : env.getElementsAnnotatedWith(BindView.class)) {
      // we don't SuperficialValidation.validateElement(element)
      // so that an unresolved View type can be generated by later processing rounds
      try {
        parseBindView(element, builderMap, erasedTargetNames);
      } catch (Exception e) {
        logParsingError(element, BindView.class, e);
      }
    }

    ···

    // Associate superclass binders with their subclass binders. This is a queue-based tree walk
    // which starts at the roots (superclasses) and walks to the leafs (subclasses).
    Deque<Map.Entry<TypeElement, BindingSet.Builder>> entries =
        new ArrayDeque<>(builderMap.entrySet());
    Map<TypeElement, BindingSet> bindingMap = new LinkedHashMap<>();
    while (!entries.isEmpty()) {
      Map.Entry<TypeElement, BindingSet.Builder> entry = entries.removeFirst();

      TypeElement type = entry.getKey();
      BindingSet.Builder builder = entry.getValue();
      //获取 type 的父类的 TypeElement
      TypeElement parentType = findParentType(type, erasedTargetNames);
      if (parentType == null) {
        // 为空，存进 map
        bindingMap.put(type, builder.build());
      } else {
        // 获取 parentType 的 BindingSet
        BindingSet parentBinding = bindingMap.get(parentType);
        if (parentBinding != null) {
          builder.setParent(parentBinding);
          bindingMap.put(type, builder.build());
        } else {
          // Has a superclass binding but we haven't built it yet. Re-enqueue for later.
          // 为空，加到队列的尾部，等待下一次处理
          entries.addLast(entry);
        }
      }
    }

    return bindingMap;
  }
```

```java
  @Override public boolean process(Set<? extends TypeElement> elements, RoundEnvironment env) {
    //  拿到所有的注解信息，TypeElement 作为 key，BindingSet 作为 value
    Map<TypeElement, BindingSet> bindingMap = findAndParseTargets(env);
    // 遍历 map 里面的所有信息，并生成 java 代码
    for (Map.Entry<TypeElement, BindingSet> entry : bindingMap.entrySet()) {
      TypeElement typeElement = entry.getKey();
      BindingSet binding = entry.getValue();
      // 生成 javaFile 对象
      JavaFile javaFile = binding.brewJava(sdk);
      try {
        //  生成 java 模板代码 
        javaFile.writeTo(filer);
      } catch (IOException e) {
        error(typeElement, "Unable to write binding for type %s: %s", typeElement, e.getMessage());
      }
    }

    return false;
  }
```