```java
//深搜框架一：递归实现  
    public  void dfs(int v) {  
      visited[v] = true;  
      System.out.print(v+"  ");  
      for (int i = 0; i < k; i++) {  
        //递归调用搜索没有被访问过的当前节点的下一个节点（邻接点）  
        if (G[v][i] == 1 && !visited[i])//G[v][i]是图的邻接矩阵  
          dfs(i);//递归调用  
      }  
    }  
  
  
  
  
//深搜框架二：栈实现     
   public void dfs(){  
     // 从顶点 v0开始搜索     
      visited[v0]= true;  //标记已访问过     
      display(v0); //显示顶点信息                  
      theStack.push(v0); // 进栈     
    
      while( !theStack.isEmpty() ) {          
        // 查看栈顶元素，看其是否有未访问过的邻接点     
         int v = getAdjUnvisitedVertex( theStack.peek() );     
         if(v == -1)  // 没有邻接点    
            theStack.pop();             //出栈     
         else{       //有     
           visited[v]= true;  // 标记已访问v     
            display(v);                
            theStack.push(v);                 // 进栈     
         }     
     }  
   }
```