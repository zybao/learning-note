# 取消AsyncTask
    在Android应用开发过程中，为了防止UI线程堵塞，耗时的工作都应该另起一个后台线程来完成，其中AsyncTask就是其中的一种方式。最近在案子中需要“停止/取消”某个AsyncTask，在网上查了些资料，这里做个笔记。

    查看AsyncTask.java文件，其中有个cancel()函数，可以通过调用cancel(true)来停止正在运行的AsyncTask。

 　　值得注意的是，调用cancel(true)函数需要注意以下几点：

　　1. 当AsyncTask已经完成，或则以及被取消，亦或其他原因不能被取消，调用cancel()会失败;

　　2. 如果AsyncTask还没有开始执行，调用cancel(true)函数后，AsyncTask不会再执行;

　　3. 如果AsyncTask已经开始执行，参数mayInterruptIfRunning决定是否立即stop该Task;

　　4. 调用cancel()后，doInBackground()完成后，不再调用onPostExecute()，而是执行onCancelled();