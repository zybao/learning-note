学习难度：★★☆☆☆，使用频率：★★★☆☆

```java
public enum WeaponType {
  SWORD, AXE, BOW, SPEAR
}

public interface Weapon {
}

public class Sword implements Weapon {
  @Override
  public String toString() {
    return "Sword";
  }
}

public class Spear implements Weapon {
  @Override
  public String toString() {
    return "Spear";
  }
}

public class Bow implements Weapon {
  @Override
  public String toString() {
    return "Bow";
  }
}

public class Axe implements Weapon {
  @Override
  public String toString() {
    return "Axe";
  }
}

public interface Builder {
  void add(WeaponType name, Supplier<Weapon> supplier);
}

public interface WeaponFactory {

  /**
   * Creates an instance of the given type.
   * @param name representing enum of an object type to be created.
   * @return new instance of a requested class implementing {@link Weapon} interface.
   */
  Weapon create(WeaponType name);

  /**
   * Creates factory - placeholder for specified {@link Builder}s.
   * @param consumer for the new builder to the factory.
   * @return factory with specified {@link Builder}s
   */
  static WeaponFactory factory(Consumer<Builder> consumer) {
    Map<WeaponType, Supplier<Weapon>> map = new HashMap<>();
    consumer.accept(map::put);
    return name -> map.get(name).get();
  }
}

  public static void main(String[] args) {
    WeaponFactory factory = WeaponFactory.factory(builder -> {
      builder.add(WeaponType.SWORD, Sword::new);
      builder.add(WeaponType.AXE, Axe::new);
      builder.add(WeaponType.SPEAR, Spear::new);
      builder.add(WeaponType.BOW, Bow::new);
    });
    Weapon axe = factory.create(WeaponType.AXE);
    LOGGER.info(axe.toString());
  }
```

# Android中的应用
```java
public Object getSystemService(String name) {
    if (getBaseContext() == null) {
        throw new IllegalStateException("System services not available to Activities before onCreate()");
    }
    //........
    if (WINDOW_SERVICE.equals(name)) {
         return mWindowManager;
    } else if (SEARCH_SERVICE.equals(name)) {
        ensureSearchManager();
        return mSearchManager;
    }
    //.......
    return super.getSystemService(name);
  }
```
在getSystemService方法中就是用到了简单工厂模式，根据传入的参数决定创建哪个对象，由于这些对象以单例模式提前创建好了，所以此处不用new了，直接把单例返回就好。