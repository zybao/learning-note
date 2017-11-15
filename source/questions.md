

# 启动插件Activity
那么是时候启动插件Activity了。通过startActivity便可以启动。从上面的流程我们知道启动是从Instrumentation.execStartActivity();开始的，而系统的Instrumentation已经被VAInstrumentation替换，其中VAInstrumentation重写了几个关键方法：

* execStartActivity：入口;
* newActivity：创建;
* callActivityOnCreate：通知;
* handleMessage：处理.
