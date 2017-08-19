APT，就是Annotation Processing Tool 的简称，就是可以在代码编译期间对注解进行处理，并且生成Java文件，减少手动的代码输入。

# AbstractProcessor
自定义的处理器需要继承AbstractProcessor，需要自己实现process方法,一般我们会实现其中的4个方法：
```java
public class AnnotationProcessor extends AbstractProcessor { 
    @Override 
    public synchronized void init(ProcessingEnvironment processingEnvironment) { } 
    
    @Override 
    public SourceVersion getSupportedSourceVersion() { } 
    
    @Override 
    public Set<String> getSupportedAnnotationTypes() { } 
    
    @Override 
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) { } }
```
init()方法可以初始化拿到一些使用的工具，比如文件相关的辅助类 Filer;元素相关的辅助类Elements;日志相关的辅助类Messager;
getSupportedSourceVersion()方法返回 Java 版本;
getSupportedAnnotationTypes()方法返回要处理的注解的结合;
上面几个方法写法基本都是固定的，重头戏是process()方法。
