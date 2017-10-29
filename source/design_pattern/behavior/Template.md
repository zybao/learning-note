模板方法: 学习难度：★★☆☆☆，使用频率：★★★☆☆

定义父类方法，子类继承并调用父类方法

```java
public abstract class StealingMethod {
  protected abstract String pickTarget();
  protected abstract void confuseTarget(String target);
  protected abstract void stealTheItem(String target);

  /**
   * Steal
   */
  public void steal() {
    String target = pickTarget();
    confuseTarget(target);
    stealTheItem(target);
  }
}

public class SubtleMethod extends StealingMethod {
  @Override
  protected String pickTarget() {
    return "shop keeper";
  }

  @Override
  protected void confuseTarget(String target) {
    LOGGER.info("Approach the {} with tears running and hug him!", target);
  }

  @Override
  protected void stealTheItem(String target) {
    LOGGER.info("While in close contact grab the {}'s wallet.", target);
  }
}

public class HitAndRunMethod extends StealingMethod {
  private static final Logger LOGGER = LoggerFactory.getLogger(HitAndRunMethod.class);

  @Override
  protected String pickTarget() {
    return "old goblin woman";
  }

  @Override
  protected void confuseTarget(String target) {
    LOGGER.info("Approach the {} from behind.", target);
  }

  @Override
  protected void stealTheItem(String target) {
    LOGGER.info("Grab the handbag and run away fast!");
  }
}

public class HalflingThief {

  private StealingMethod method;

  public HalflingThief(StealingMethod method) {
    this.method = method;
  }

  public void steal() {
    method.steal();
  }

  public void changeMethod(StealingMethod method) {
    this.method = method;
  }
}

  public static void main(String[] args) {
    HalflingThief thief = new HalflingThief(new HitAndRunMethod());
    thief.steal();
    thief.changeMethod(new SubtleMethod());
    thief.steal();
  }
```