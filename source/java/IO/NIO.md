Java NIO(New IO)是一个可以替代标准Java IO API的IO API（从Java 1.4开始)，Java NIO提供了与标准IO不同的IO工作方式。
**Java NIO: Channels and Buffers（通道和缓冲区）**

标准的IO基于字节流和字符流进行操作的，而NIO是基于通道（Channel）和缓冲区（Buffer）进行操作，数据总是从通道读取到缓冲区中，或者从缓冲区写入到通道中。

**Java NIO: Non-blocking IO（非阻塞IO）**

Java NIO可以让你非阻塞的使用IO，例如：当线程从通道读取数据到缓冲区时，线程还是可以进行其他事情。当数据被写入到缓冲区时，线程可以继续处理它。从缓冲区写入通道也类似。

**Java NIO: Selectors（选择器）**

Java NIO引入了选择器的概念，选择器用于监听多个通道的事件（比如：连接打开，数据到达）。因此，单个的线程可以监听多个数据通道。

Java NIO 由以下几个核心部分组成：
* Channels
* Buffers
* Selectors

基本上，所有的 IO 在NIO 中都从一个Channel 开始。Channel 有点象流。 数据可以从Channel读到Buffer中，也可以从Buffer 写到Channel中。
Channel和Buffer有好几种类型。下面是JAVA NIO中的一些主要Channel的实现：
* FileChannel
* DatagramChannel
* SocketChannel
* ServerSocketChannel

以下是Java NIO里关键的Buffer实现：

* ByteBuffer
* CharBuffer
* DoubleBuffer
* FloatBuffer
* IntBuffer
* LongBuffer
* ShortBuffer

这些Buffer覆盖了你能通过IO发送的基本数据类型：byte, short, int, long, float, double 和 char.

**Selector**
Selector允许单线程处理多个 Channel。如果你的应用打开了多个连接（通道），但每个连接的流量都很低，使用Selector就会很方便。例如，在一个聊天服务器中。

要使用Selector，得向Selector注册Channel，然后调用它的select()方法。这个方法会一直阻塞到某个注册的通道有事件就绪。一旦这个方法返回，线程就可以处理这些事件，事件的例子有如新连接进来，数据接收等。

# Channel
ava NIO的通道类似流，但又有些不同：
* 既可以从通道中读取数据，又可以写数据到通道。但流的读写通常是单向的;
* 通道可以异步地读写;
* 通道中的数据总是要先读到一个Buffer，或者总是要从一个Buffer中写入。

## Channel的实现
FileChannel 从文件中读写数据。

DatagramChannel 能通过UDP读写网络中的数据。

SocketChannel 能通过TCP读写网络中的数据。

ServerSocketChannel可以监听新进来的TCP连接，像Web服务器那样。对每一个新进来的连接都会创建一个SocketChannel。

```java
RandomAccessFile raf = new RandomAccessFile("data/nio-data.txt", "rw");
FileChannel inChannel = raf.getChannel();

ByteBuffer buf = ByteBuffer.allocate(48);

int bytesRead = inChannel.read(buf);
while (bytesRead != -1) {
    System.out.println("Read " + bytesRead);
    buf.flip();

    while(buf.hasRemaining()){
        System.out.print((char) buf.get());
    }
    
    buf.clear();
    bytesRead = inChannel.read(buf);
}
raf.close();
```