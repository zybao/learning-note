学习难度：★★★☆☆，使用频率：★★★☆☆

```java
public interface State {

  void onEnterState();

  void observe();

}  

public class Mammoth {

  private State state;

  public Mammoth() {
    state = new PeacefulState(this);
  }

  /**
   * Makes time pass for the mammoth
   */
  public void timePasses() {
    if (state.getClass().equals(PeacefulState.class)) {
      changeStateTo(new AngryState(this));
    } else {
      changeStateTo(new PeacefulState(this));
    }
  }

  private void changeStateTo(State newState) {
    this.state = newState;
    this.state.onEnterState();
  }

  @Override
  public String toString() {
    return "The mammoth";
  }

  public void observe() {
    this.state.observe();
  }
}

public class PeacefulState implements State {

  private static final Logger LOGGER = LoggerFactory.getLogger(PeacefulState.class);

  private Mammoth mammoth;

  public PeacefulState(Mammoth mammoth) {
    this.mammoth = mammoth;
  }

  @Override
  public void observe() {
    LOGGER.info("{} is calm and peaceful.", mammoth);
  }

  @Override
  public void onEnterState() {
    LOGGER.info("{} calms down.", mammoth);
  }

}

public class AngryState implements State {

  private static final Logger LOGGER = LoggerFactory.getLogger(AngryState.class);

  private Mammoth mammoth;

  public AngryState(Mammoth mammoth) {
    this.mammoth = mammoth;
  }

  @Override
  public void observe() {
    LOGGER.info("{} is furious!", mammoth);
  }

  @Override
  public void onEnterState() {
    LOGGER.info("{} gets angry!", mammoth);
  }

}

  public static void main(String[] args) {
    Mammoth mammoth = new Mammoth();
    mammoth.observe();
    mammoth.timePasses();
    mammoth.observe();
    mammoth.timePasses();
    mammoth.observe();

  }
```

# Android中的应用
Android源码中很多地方都有用到状态模式，举一个例子，就是Android的WIFI管理模块。当WIFI开启时，自动扫描周围的接入点，然后以列表的形式展示；当wifi关闭时则清空。这里wifi管理模块就是根据不同的状态执行不同的行为。