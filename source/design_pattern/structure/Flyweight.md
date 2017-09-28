学习难度：★★★★☆，使用频率：★☆☆☆☆

What problems can the Flyweight design pattern solve? [2]
* Large numbers of objects should be supported efficiently.
* Creating large numbers of objects should be avoided.

When representing large text documents, for example, creating an object for each character in the document would result in a huge amount of objects that couldn't be processed efficiently.

```java
public interface Potion {
  void drink();
}

public enum PotionType {
  HEALING, INVISIBILITY, STRENGTH, HOLY_WATER, POISON
}

public class StrengthPotion implements Potion {

  private static final Logger LOGGER = LoggerFactory.getLogger(StrengthPotion.class);

  @Override
  public void drink() {
    LOGGER.info("You feel strong. (Potion={})", System.identityHashCode(this));
  }
}

public class StrengthPotion implements Potion {

  private static final Logger LOGGER = LoggerFactory.getLogger(StrengthPotion.class);

  @Override
  public void drink() {
    LOGGER.info("You feel strong. (Potion={})", System.identityHashCode(this));
  }
}

public class InvisibilityPotion implements Potion {

  private static final Logger LOGGER = LoggerFactory.getLogger(InvisibilityPotion.class);

  @Override
  public void drink() {
    LOGGER.info("You become invisible. (Potion={})", System.identityHashCode(this));
  }
}

public class HolyWaterPotion implements Potion {

  private static final Logger LOGGER = LoggerFactory.getLogger(HolyWaterPotion.class);

  @Override
  public void drink() {
    LOGGER.info("You feel blessed. (Potion={})", System.identityHashCode(this));
  }
}

public class HealingPotion implements Potion {

  private static final Logger LOGGER = LoggerFactory.getLogger(HealingPotion.class);

  @Override
  public void drink() {
    LOGGER.info("You feel healed. (Potion={})", System.identityHashCode(this));
  }
}

public class PotionFactory {

  private final Map<PotionType, Potion> potions;

  public PotionFactory() {
    potions = new EnumMap<>(PotionType.class);
  }

  Potion createPotion(PotionType type) {
    Potion potion = potions.get(type);
    if (potion == null) {
      switch (type) {
        case HEALING:
          potion = new HealingPotion();
          potions.put(type, potion);
          break;
        case HOLY_WATER:
          potion = new HolyWaterPotion();
          potions.put(type, potion);
          break;
        case INVISIBILITY:
          potion = new InvisibilityPotion();
          potions.put(type, potion);
          break;
        case POISON:
          potion = new PoisonPotion();
          potions.put(type, potion);
          break;
        case STRENGTH:
          potion = new StrengthPotion();
          potions.put(type, potion);
          break;
        default:
          break;
      }
    }
    return potion;
  }
}

  public static void main(String[] args) {
    AlchemistShop alchemistShop = new AlchemistShop();
    alchemistShop.enumerate();
  }
```