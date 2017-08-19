```java
@AutoService(Processor.class)
public class FakeProcessor extends AbstractProcessor {

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
        annotations.add(Fake.class.getCanonicalName());
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
```



* init(ProcessingEnvironment env)
    
每一个注解处理器类都必须有一个空的构造函数。然而，这里有一个特殊的init()方法，它会被注解处理工具调用，并输入ProcessingEnviroment参数。ProcessingEnviroment提供很多有用的工具类Elements,Types和Filer。

* public boolean process(Set<? extends TypeElement> annoations, RoundEnvironment env)

这相当于每个处理器的主函数main()。
在这里写扫描、评估和处理注解的代码，以及生成Java文件。输入参数RoundEnviroment，可以让查询出包含特定注解的被注解元素。

* getSupportedAnnotationTypes():

这里必须指定，这个注解处理器是注册给哪个注解的。注意，它的返回值是一个字符串的集合，包含本处理器想要处理的注解类型的合法全称。换句话说，在这里定义你的注解处理器注册到哪些注解上。

* getSupportedSourceVersion():

用来指定你使用的Java版本。通常这里返回SourceVersion.latestSupported()。然而，如果有足够的理由只支持Java6的话，也可以返回SourceVersion.RELEASE_6。推荐使用前者。


# Support Annotations注解
* 资源引用限制类：用于限制参数必须为对应的资源类型

AnimRes AnyRes ArrayRes AttrRes BoolRes ColorRes DimenRes DrawableRes FractionRes IdRes IntegerRes InterpolatorRes LayoutRes MenuRes PluralsRes Px RawRes StringRes StyleableRes StyleRes TransitionRes XmlRes

* 线程执行限制类：用于限制方法或者类必须在指定的线程执行

AnyThread BinderThread MainThread UiThread WorkerThread

* 参数为空性限制类：用于限制参数是否可以为空

NonNull Nullable

* 类型范围限制类：用于限制标注值的值范围

FloatRang IntRange

* 类型定义类：用于限制定义的注解的取值集合

IntDef StringDef

* 其他的功能性注解：

CallSuper CheckResult ColorInt Dimension Keep Px RequiresApi RequiresPermission RestrictTo Size VisibleForTesting