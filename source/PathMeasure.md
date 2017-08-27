# Public constructors

* PathMeasure 创建一个空的 pathmeasure 对象
* PathMeasure(Path path,boolean forceClosed)创建一个带 path 参数的 PathMeasure,forceClosed控制 path 是否自动闭合

# Public methods
* getLength() 返回当前 Path 的总长度。
* getMatrix(float distance, Matrix matrix, int flags)
* getPosTan(float distance, float[] pos, float[] tan)获取distance长度的 point 值给 pos，point 点的正切值给 tan。
* getSegment(float startD, float stopD, Path dst, boolean startWithMoveTo) 获取 path 的一个片段，即startD到 stopD 的线段，辅助给 dst。
* isClosed() 是否自动闭合
* nextContour() 移动到下一条曲线。如果 path 中含有不连续的线条，getLength、getPosTan等方法之会在第一条线上运行，需要使用这个方法跳到第二条线
* setPath(Path path, boolean forceClosed)

