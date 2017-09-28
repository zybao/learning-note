学习难度：★★★★☆，使用频率：★★★★★

**定义**

为创建一组相关或者是相互依赖的对象提供一个接口，而不需要制定他们的具体类
抽象工厂模式是指当有多个抽象角色时，使用的一种工厂模式。抽象工厂模式可以向客户端提供一个接口，使客户端在不必指定产品的具体情况下，创建多个产品族中的产品对象。

```java
public interface King {

  String getDescription();
}

public class ElfKing implements King {

  static final String DESCRIPTION = "This is the Elven king!";

  @Override
  public String getDescription() {
    return DESCRIPTION;
  }
}

public class OrcKing implements King {

  static final String DESCRIPTION = "This is the Orc king!";

  @Override
  public String getDescription() {
    return DESCRIPTION;
  }
}

public interface Army {

  String getDescription();
}

public class ElfArmy implements Army {

  static final String DESCRIPTION = "This is the Elven Army!";

  @Override
  public String getDescription() {
    return DESCRIPTION;
  }
}

public class OrcArmy implements Army {

  static final String DESCRIPTION = "This is the Orc Army!";

  @Override
  public String getDescription() {
    return DESCRIPTION;
  }
}

public interface Castle {

  String getDescription();
}

public class ElfCastle implements Castle {

  static final String DESCRIPTION = "This is the Elven castle!";

  @Override
  public String getDescription() {
    return DESCRIPTION;
  }
}

public class OrcCastle implements Castle {

  static final String DESCRIPTION = "This is the Orc castle!";

  @Override
  public String getDescription() {
    return DESCRIPTION;
  }
}

public interface KingdomFactory {

  Castle createCastle();

  King createKing();

  Army createArmy();

}

public class OrcKingdomFactory implements KingdomFactory {

  public Castle createCastle() {
    return new OrcCastle();
  }

  public King createKing() {
    return new OrcKing();
  }

  public Army createArmy() {
    return new OrcArmy();
  }
}

public class ElfKingdomFactory implements KingdomFactory {

  public Castle createCastle() {
    return new ElfCastle();
  }

  public King createKing() {
    return new ElfKing();
  }

  public Army createArmy() {
    return new ElfArmy();
  }

}

  private King king;
  private Castle castle;
  private Army army;

  /**
   * Creates kingdom
   */
  public void createKingdom(final KingdomFactory factory) {
    setKing(factory.createKing());
    setCastle(factory.createCastle());
    setArmy(factory.createArmy());
  }

  King getKing(final KingdomFactory factory) {
    return factory.createKing();
  }

  public King getKing() {
    return king;
  }

  private void setKing(final King king) {
    this.king = king;
  }
  
  Castle getCastle(final KingdomFactory factory) {
    return factory.createCastle();
  }

  public Castle getCastle() {
    return castle;
  }

  private void setCastle(final Castle castle) {
    this.castle = castle;
  }
  
  Army getArmy(final KingdomFactory factory) {
    return factory.createArmy();
  }

  public Army getArmy() {
    return army;
  }

  private void setArmy(final Army army) {
    this.army = army;
  }
  
  /**
   * Program entry point
   * 
   * @param args
   *          command line args
   */
  public static void main(String[] args) {

    App app = new App();

    LOGGER.info("Elf Kingdom");
    app.createKingdom(new ElfKingdomFactory());
    LOGGER.info(app.getArmy().getDescription());
    LOGGER.info(app.getCastle().getDescription());
    LOGGER.info(app.getKing().getDescription());

    LOGGER.info("Orc Kingdom");
    app.createKingdom(new OrcKingdomFactory());
    LOGGER.info(app.getArmy().getDescription());
    LOGGER.info(app.getCastle().getDescription());
    LOGGER.info(app.getKing().getDescription());

  }
```

# Android中的应用

由于该模式存在的局限性，Android中很少有用到这个模式的地方，com.android.internal.policy包下的IPolicy有使用到这个模式，它是关于Android窗口，窗口管理，布局加载，以及事件回退Handler这一系列窗口相关产品的抽象工厂，但是其在源码中其实也只有一个具体的工厂实现。因为这部分结构较为复杂，代码量大，有兴趣的同学可以自己去查看相关资料或者阅读源码。