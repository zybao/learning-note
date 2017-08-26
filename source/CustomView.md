# Canvas与Canvas
Paint就是相当于笔，而Canvas就是纸，这里叫画布。

Paint的基本设置函数：
```java
    paint.setAntiAlias(true);//抗锯齿功能
    paint.setColor(Color.RED);  //设置画笔颜色    
    paint.setStyle(Style.FILL);//设置填充样式
    paint.setStrokeWidth(30);//设置画笔宽度
    paint.setShadowLayer(10, 15, 15, Color.GREEN);//设置阴影
```

1. setStyle(Paint.Style style) 设置填充样式
* Paint.Style.FILL    :填充内部
* Paint.Style.FILL_AND_STROKE  ：填充内部和描边
* Paint.Style.STROKE  ：仅描边

2. setShadowLayer (float radius, float dx, float dy, int color) 添加阴影
* radius:阴影的倾斜度
* dx:水平位移
* dy:垂直位移

## 基本几何图形绘制
* void drawLine (float startX, float startY, float stopX, float stopY, Paint paint)
* void drawLines (float[] pts, Paint paint)
* void drawLines (float[] pts, int offset, int count, Paint paint)
* void drawPoint (float x, float y, Paint paint)
* void drawPoints (float[] pts, Paint paint)
* void drawPoints (float[] pts, int offset, int count, Paint paint)

参数：

float[] pts: 点的合集，与上面直线一直，样式为｛x1,y1,x2,y2,x3,y3,……｝

int offset: 集合中跳过的数值个数，注意不是点的个数！一个点是两个数值；

count: 参与绘制的数值的个数，指pts[]里人数值个数，而不是点的个数，因为一个点是两个数值

### 矩形工具类RectF与Rect
* RectF：

构造函数有下面四个，但最常用的还是第二个，根据四个点构造出一个矩形；

RectF()

RectF(float left, float top, float right, float bottom)

RectF(RectF r)

RectF(Rect r)

* Rect

构造函数如下，最常用的也是根据四个点来构造矩形

Rect()

Rect(int left, int top, int right, int bottom)

Rect(Rect r)

