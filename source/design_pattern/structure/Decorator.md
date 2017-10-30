装饰器模式: 学习难度：★★★☆☆，使用频率：★★★☆☆

装饰器模式的应用场景：

1. 需要扩展一个类的功能。

2. 动态的为一个对象增加功能，而且还能动态撤销。（继承不能做到这一点，继承的功能是静态的，不能动态增删。）

缺点：产生过多相似的对象，不易排错！

```java
public interface Troll {

  void attack();

  int getAttackPower();

  void fleeBattle();

}

public class SimpleTroll implements Troll {

  private static final Logger LOGGER = LoggerFactory.getLogger(SimpleTroll.class);

  @Override
  public void attack() {
    LOGGER.info("The troll tries to grab you!");
  }

  @Override
  public int getAttackPower() {
    return 10;
  }

  @Override
  public void fleeBattle() {
    LOGGER.info("The troll shrieks in horror and runs away!");
  }
}

public class ClubbedTroll implements Troll {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClubbedTroll.class);

  private Troll decorated;

  public ClubbedTroll(Troll decorated) {
    this.decorated = decorated;
  }

  @Override
  public void attack() {
    decorated.attack();
    LOGGER.info("The troll swings at you with a club!");
  }

  @Override
  public int getAttackPower() {
    return decorated.getAttackPower() + 10;
  }

  @Override
  public void fleeBattle() {
    decorated.fleeBattle();
  }
}

  public static void main(String[] args) {

    // simple troll
    LOGGER.info("A simple looking troll approaches.");
    Troll troll = new SimpleTroll();
    troll.attack();
    troll.fleeBattle();
    LOGGER.info("Simple troll power {}.\n", troll.getAttackPower());

    // change the behavior of the simple troll by adding a decorator
    LOGGER.info("A troll with huge club surprises you.");
    troll = new ClubbedTroll(troll);
    troll.attack();
    troll.fleeBattle();
    LOGGER.info("Clubbed troll power {}.\n", troll.getAttackPower());
  }
```