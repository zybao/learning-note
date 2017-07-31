SPI 全名就是 Service Provider Interface, 服务提供方接口，服务通常是指一个接口或者一个抽象类，服务提供方是对这个接口或者抽象类的具体实现，由第三方来实现接口提供具体的服务。SPI 提供了一种动态的对应用程序进行扩展的机制，通常用作框架服务的拓展或者可替换的服务组件，

SPI运用场景主要有：

* Java Database Connectivity
* Java Cryptography Extension
* Java Naming and Directory Interface
* Java API for XML Processing
* Java Business Integration
* Java Sound
* Java Image I/O
* Java File Systems

# SPI机制

* 在resources/META-INF/services/目录中创建以服务全限定名命名的文件，该文件内容为服务的具体实现类的全限定名，文件中可以写多个服务的具体实现类
* 使用ServiceLoader类动态加载服务的具体实现类
* 服务具体的实现类必须有一个不带参数的构造方法
* 如果服务的具体实现类在Jar包中，则需要放在主程序的classPath中

# SPI的使用
1. 定义接口 IService, 实现接口;
2. 在工程的main目录下新建目录resources/META-INF/services，以服务接口名为文件名新建spi描述文件，内容为具体的服务实现类权限定名，可以有多个;
3. 使用ServiceLoader去加载具体服务类，然后遍历具体的实现类，ServiceLoader其实就是去META-INFO/services目录下读取文件内容，然后实例化;
```java
    ServiceLoader<IService> loader = ServiceLoader.load(IService.class);
    Iterator<IService> iterator = loader.iterator();
    boolean isKeepLoc = false;

    while (iterator.hasNext()) {
        if(iterator.next().keep()){
            isKeepLoc = true;
            break;
        }
    }
```
4. SPI的优点
只提供服务接口，具体服务由其他组件实现，接口和具体实现分离，同时能够通过系统的ServiceLoader拿到这些实现类的集合，统一处理。

# SPI的缺点
* Java中SPI是随jar发布的，每个不同的jar都可以包含一系列的SPI配置，而Android平台上，应用在构建的时候最终会将所有的jar合并，这样很容易造成相同的SPI冲突，常见的问题是DuplicatedZipEntryException异常
* 读取SPI配置信息是在运行时从jar包中读取，由于apk是签过名的，在从jar中读取的时候，签名校验的耗时问题会造成性能损失

# 优化思路
Java中使用ServiceLoader去读取SPI配置信息是在程序运行时，我们可以将这个读取配置信息提前，在编译时候就搞定，通过gradle插件，去扫描class文件，找到具体的服务类（可以通过标注来确定），然后生成新的java文件，这个文件中包含了具体的实现类。

这样程序在运行时，就已经知道了所有的具体服务类，缺点就是编译时间会加长，自己需要重新写一套读取SPI信息、生成java文件等逻辑。

经过优化后，SPI已经偏离了原本的初衷，但是可以做更多的事，可以将业务服务分离，通过SPI找到业务服务入口，业务组件化，抽成单独的aar，独立成工程。


# [AutoService](https://github.com/google/auto/tree/master/service)
谷歌官方也出品了一个开源库Auto-Service，通过注解`@AutoService(Processor.class)`可以省略上面配置的步骤.
AutoService注解处理器是用来生成 `META-INF/services/javax.annotation.processing.Processor` 文件的，你只需要在你定义的注解处理器上添加 `@AutoService(Processor.class)` 就可以了.

如果编译的时候出现DuplicateFileException，解决方法是在主项目build.gradle加上一段:
```xml
apply plugin: 'com.android.application'

android {
    // ...
    packagingOptions {
        exclude 'META-INF/services/javax.annotation.processing.Processor'
    }
}
```

另一种方法是使用android-apt，它有两个好处：
* 能在编译时期去依赖注解处理器并进行工作，但在生成 APK 时不会包含任何遗留的东西
* 能够辅助 Android Studio 在项目的对应目录中存放注解处理器在编译期间生成的文件

首先在整个工程的 build.gradle 中添加如下两段语句：

```xml
buildscript {
    repositories {
        jcenter()
        mavenCentral()  // add
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:2.1.2'
        classpath 'com.neenbedankt.gradle.plugins:android-apt:1.8'  // add
    }
}
```

在主项目(app)的 build.gradle 中也添加两段语句：
```xml
apply plugin: 'com.android.application'
apply plugin: 'com.neenbedankt.android-apt' // add
// ...
dependencies {
    compile fileTree(include: ['*.jar'], dir: 'libs')
    testCompile 'junit:junit:4.12'
    compile 'com.android.support:appcompat-v7:23.4.0'
    compile project(':annotations')
//    compile project(':processors')  替换为下面
    apt project(':processors')
}
```