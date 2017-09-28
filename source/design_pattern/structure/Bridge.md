学习难度：★★★☆☆，使用频率：★★★☆☆

```java
public interface Enchantment {

  void onActivate();

  void apply();

  void onDeactivate();
}

public interface Weapon {

  void wield();

  void swing();

  void unwield();

  Enchantment getEnchantment();
}

public class Sword implements Weapon {

  private static final Logger LOGGER = LoggerFactory.getLogger(Sword.class);

  private final Enchantment enchantment;

  public Sword(Enchantment enchantment) {
    this.enchantment = enchantment;
  }

  @Override
  public void wield() {
    LOGGER.info("The sword is wielded.");
    enchantment.onActivate();
  }

  @Override
  public void swing() {
    LOGGER.info("The sword is swinged.");
    enchantment.apply();
  }

  @Override
  public void unwield() {
    LOGGER.info("The sword is unwielded.");
    enchantment.onDeactivate();
  }

  @Override
  public Enchantment getEnchantment() {
    return enchantment;
  }
}

public class Hammer implements Weapon {

  private static final Logger LOGGER = LoggerFactory.getLogger(Hammer.class);

  private final Enchantment enchantment;

  public Hammer(Enchantment enchantment) {
    this.enchantment = enchantment;
  }

  @Override
  public void wield() {
    LOGGER.info("The hammer is wielded.");
    enchantment.onActivate();
  }

  @Override
  public void swing() {
    LOGGER.info("The hammer is swinged.");
    enchantment.apply();
  }

  @Override
  public void unwield() {
    LOGGER.info("The hammer is unwielded.");
    enchantment.onDeactivate();
  }

  @Override
  public Enchantment getEnchantment() {
    return enchantment;
  }
}

public class FlyingEnchantment implements Enchantment {

  private static final Logger LOGGER = LoggerFactory.getLogger(FlyingEnchantment.class);

  @Override
  public void onActivate() {
    LOGGER.info("The item begins to glow faintly.");
  }

  @Override
  public void apply() {
    LOGGER.info("The item flies and strikes the enemies finally returning to owner's hand.");
  }

  @Override
  public void onDeactivate() {
    LOGGER.info("The item's glow fades.");
  }
}

public class SoulEatingEnchantment implements Enchantment {

  private static final Logger LOGGER = LoggerFactory.getLogger(SoulEatingEnchantment.class);

  @Override
  public void onActivate() {
    LOGGER.info("The item spreads bloodlust.");
  }

  @Override
  public void apply() {
    LOGGER.info("The item eats the soul of enemies.");
  }

  @Override
  public void onDeactivate() {
    LOGGER.info("Bloodlust slowly disappears.");
  }
}

  public static void main(String[] args) {
    LOGGER.info("The knight receives an enchanted sword.");
    Sword enchantedSword = new Sword(new SoulEatingEnchantment());
    enchantedSword.wield();
    enchantedSword.swing();
    enchantedSword.unwield();

    LOGGER.info("The valkyrie receives an enchanted hammer.");
    Hammer hammer = new Hammer(new FlyingEnchantment());
    hammer.wield();
    hammer.swing();
    hammer.unwield();
  }
```