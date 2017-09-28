学习难度：★★☆☆☆，使用频率：★★★★★

**定义**

是定义一个创建产品对象的工厂接口，让其子类决定实例化哪一个类，将实际创建工作推迟到子类当中。 
```java
public enum WeaponType {

  SHORT_SWORD("short sword"), SPEAR("spear"), AXE("axe"), UNDEFINED("");

  private String title;

  WeaponType(String title) {
    this.title = title;
  }

  @Override
  public String toString() {
    return title;
  }
}

public interface Weapon {

  WeaponType getWeaponType();

}

public class OrcWeapon implements Weapon {

  private WeaponType weaponType;

  public OrcWeapon(WeaponType weaponType) {
    this.weaponType = weaponType;
  }

  @Override
  public String toString() {
    return "Orcish " + weaponType;
  }

  @Override
  public WeaponType getWeaponType() {
    return weaponType;
  }
}

public class ElfWeapon implements Weapon {

  private WeaponType weaponType;

  public ElfWeapon(WeaponType weaponType) {
    this.weaponType = weaponType;
  }

  @Override
  public String toString() {
    return "Elven " + weaponType;
  }

  @Override
  public WeaponType getWeaponType() {
    return weaponType;
  }
}

public interface Blacksmith {

  Weapon manufactureWeapon(WeaponType weaponType);

}

public class OrcBlacksmith implements Blacksmith {

  public Weapon manufactureWeapon(WeaponType weaponType) {
    return new OrcWeapon(weaponType);
  }
}

public class ElfBlacksmith implements Blacksmith {

  public Weapon manufactureWeapon(WeaponType weaponType) {
    return new ElfWeapon(weaponType);
  }

}

  private final Blacksmith blacksmith;
  
  /**
   * Creates an instance of <code>App</code> which will use <code>blacksmith</code> to manufacture 
   * the weapons for war.
   * <code>App</code> is unaware which concrete implementation of {@link Blacksmith} it is using.
   * The decision of which blacksmith implementation to use may depend on configuration, or
   * the type of rival in war.
   * @param blacksmith a non-null implementation of blacksmith
   */
  public App(Blacksmith blacksmith) {
    this.blacksmith = blacksmith;
  }
  
  /**
   * Program entry point
   * 
   * @param args command line args
   */
  public static void main(String[] args) {
    // Lets go to war with Orc weapons
    App app = new App(new OrcBlacksmith());
    app.manufactureWeapons();
    
    // Lets go to war with Elf weapons
    app = new App(new ElfBlacksmith());
    app.manufactureWeapons();
  }
  
  private void manufactureWeapons() {
    Weapon weapon;
    weapon = blacksmith.manufactureWeapon(WeaponType.SPEAR);
    LOGGER.info(weapon.toString());
    weapon = blacksmith.manufactureWeapon(WeaponType.AXE);
    LOGGER.info(weapon.toString());
  }
```

# Android中的应用
我们在开发中会用到很多数据结构，比如ArrayList，HashMap等。

我们知道Iterator是迭代器，用来遍历一个集合中的元素。而不同的数据结构遍历的方式是不一样的，所以迭代器的实现也是不同的。使用工厂方法模式将迭代器的具体类型延迟到具体容器类中，比较灵活，容易扩展。

List和Set继承自Collection接口，Collection接口继承于Iterable接口。所以List和Set接口也需要继承并实现Iterable中的iterator()方法。然后我们常用的两个间接实现类ArrayList和HashSet中的iterator方法就给我们具体构造并返回了一个迭代器对象。
我们找到ArrayList类，查看iterator方法的实现。