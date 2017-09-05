# NIO学习笔记

核心组成：Channels、Buffers、Selectors

## Channel
* FileChannel
* DatagramChannel
* SocketChannel
* ServerSocketChannel

Channel Example:
```java
    RandomAccessFile aFile = new RandomAccessFile("data/nio-data.txt", "rw");
    FileChannel inChannel = aFile.getChannel();

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
    aFile.close();
```

### FileChannel
```java
RandomAccessFile raf = new RamdomAccessFile("a.txt", "rw");
FileChannel channel = raf.getChannel();
ByteBuffer buf = ByteBuffer.allocate(48);
int byteRead = channel.read(buf);

// write data
String newData = "This is a string" + System.currentTimeMillis();
buf.clear();
buf.put(newData.getBytes());
buf.flip();

while(buf.hasRemaining()) {
    channel.write(buf);
}
```

### SocketChannel
```java
SocketChannel socketChannel = SocketChannel.open();
socketChannel.connect(new InetSocketAddress("http://www.ucas.ac.cn", 80));
ByteBuffer buf = ByteBuffer.allocate(48);
// read data
int byteRead = socketChannel.read(buf);
// write to socketChannel
buf.clear();
buf.put("a test".getBytes());
buf.flip();
while(buf.hasRemaining()) {
    socketChannel.write(buf);
}
```
## Buffer
* ByteBuffer
* CharBuffer
* DoubleBuffer
* FloatBuffer
* IntBuffer
* LongBuffer
* ShortBuffer

Using a Buffer to read and write data typically follows this little 4-step process:

1. Write data into the Buffer
2. Call buffer.flip()
3. Read data out of the Buffer
4. Call buffer.clear() or buffer.compact()

http://www.jianshu.com/p/052035037297?hmsr=toutiao.io&utm_medium=toutiao.io&utm_source=toutiao.io