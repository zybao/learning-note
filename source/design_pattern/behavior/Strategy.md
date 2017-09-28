学习难度：★☆☆☆☆，使用频率：★★★★☆

```java
@FunctionalInterface
public interface DragonSlayingStrategy {

  void execute();

}

public class MeleeStrategy implements DragonSlayingStrategy {

  private static final Logger LOGGER = LoggerFactory.getLogger(MeleeStrategy.class);

  @Override
  public void execute() {
    LOGGER.info("With your Excalibur you sever the dragon's head!");
  }
}

public class ProjectileStrategy implements DragonSlayingStrategy {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProjectileStrategy.class);

  @Override
  public void execute() {
    LOGGER.info("You shoot the dragon with the magical crossbow and it falls dead on the ground!");
  }
}

public class SpellStrategy implements DragonSlayingStrategy {

  private static final Logger LOGGER = LoggerFactory.getLogger(SpellStrategy.class);

  @Override
  public void execute() {
    LOGGER.info("You cast the spell of disintegration and the dragon vaporizes in a pile of dust!");
  }

}

public class DragonSlayer {

  private DragonSlayingStrategy strategy;

  public DragonSlayer(DragonSlayingStrategy strategy) {
    this.strategy = strategy;
  }

  public void changeStrategy(DragonSlayingStrategy strategy) {
    this.strategy = strategy;
  }

  public void goToBattle() {
    strategy.execute();
  }
}

  public static void main(String[] args) {
    // GoF Strategy pattern
    LOGGER.info("Green dragon spotted ahead!");
    DragonSlayer dragonSlayer = new DragonSlayer(new MeleeStrategy());
    dragonSlayer.goToBattle();
    LOGGER.info("Red dragon emerges.");
    dragonSlayer.changeStrategy(new ProjectileStrategy());
    dragonSlayer.goToBattle();
    LOGGER.info("Black dragon lands before you.");
    dragonSlayer.changeStrategy(new SpellStrategy());
    dragonSlayer.goToBattle();

    // Java 8 Strategy pattern
    LOGGER.info("Green dragon spotted ahead!");
    dragonSlayer = new DragonSlayer(
        () -> LOGGER.info("With your Excalibur you severe the dragon's head!"));
    dragonSlayer.goToBattle();
    LOGGER.info("Red dragon emerges.");
    dragonSlayer.changeStrategy(() -> LOGGER.info(
        "You shoot the dragon with the magical crossbow and it falls dead on the ground!"));
    dragonSlayer.goToBattle();
    LOGGER.info("Black dragon lands before you.");
    dragonSlayer.changeStrategy(() -> LOGGER.info(
        "You cast the spell of disintegration and the dragon vaporizes in a pile of dust!"));
    dragonSlayer.goToBattle();
  }
```

# Android中的应用
Android在属性动画中使用时间插值器的时候就用到了策略模式。在使用动画时，你可以选择线性插值器LinearInterpolator、加速减速插值器AccelerateDecelerateInterpolator、减速插值器DecelerateInterpolator以及自定义的插值器。这些插值器都是实现根据时间流逝的百分比来计算出当前属性值改变的百分比。通过根据需要选择不同的插值器，实现不同的动画效果。