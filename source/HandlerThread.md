HanlderThread继承自Thread，它的run()中通过Looper.prepare()创建了消息队列，并通过Looper.loop()开启了消息循环，这样我们就可以在HandlerThread中创建Handler, 从而外界通过Hanlder通知HandlerThread来执行一个具体的任务。