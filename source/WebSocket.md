http://blog.csdn.net/yinqingwang/article/details/52565133

WebSocket用于在Web浏览器和服务器之间进行任意的双向数据传输的一种技术。WebSocket协议基于TCP协议实现，包含初始的握手过程，以及后续的多次数据帧双向传输过程。其目的是在WebSocket应用和WebSocket服务器进行频繁双向通信时，可以使服务器避免打开多个HTTP连接进行工作来节约资源，提高了工作效率和资源利用率。

WebSocket技术的优点有：
1. 通过第一次HTTP Request建立了连接之后，后续的数据交换都不用再重新发送HTTP Request，节省了带宽资源； 
2.  WebSocket的连接是双向通信的连接，在同一个TCP连接上，既可以发送，也可以接收; 
3. 具有多路复用的功能(multiplexing)，也即几个不同的URI可以复用同一个WebSocket连接。这些特点非常类似TCP连接，但是因为它借用了HTTP协议的一些概念，所以被称为了WebSocket。

WebSocket的结论如下：

    基于TCP/IP协议实现
    是一种全双向的通信, 具有底层socket的特点
    节约带宽，节省服务器资源
    是HTML5的技术之一，具有巨大的应用前景
