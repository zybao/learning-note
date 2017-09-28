学习难度：★★★☆☆，使用频率：★★★☆☆

**定义**

用原型实例指定创建对象的种类，并通过拷贝这些原型创建新的对象。
可以在类的属性特别多，但是又要经常对类进行拷贝的时候可以用原型模式，这样代码比较简洁，而且比较方便。
拷贝时要注意浅拷贝与深拷贝

在 Java 中，除了基本数据类型（元类型）之外，还存在 类的实例对象 这个引用数据类型。而一般使用 『 = 』号做赋值操作的时候。对于基本数据类型，实际上是拷贝的它的值，但是对于对象而言，其实赋值的只是这个对象的引用，将原对象的引用传递过去，他们实际上还是指向的同一个对象。

而浅拷贝和深拷贝就是在这个基础之上做的区分，如果在拷贝这个对象的时候，只对基本数据类型进行了拷贝，而对引用数据类型只是进行了引用的传递，而没有真实的创建一个新的对象，则认为是浅拷贝。反之，在对引用数据类型进行拷贝的时候，创建了一个新的对象，并且复制其内的成员变量，则认为是深拷贝

```java
public abstract class Prototype implements Cloneable {

  @Override
  public abstract Object clone() throws CloneNotSupportedException;

}

public abstract class Warlord extends Prototype {

  @Override
  public abstract Warlord clone() throws CloneNotSupportedException;

}

public class OrcWarlord extends Warlord {

  public OrcWarlord() {}

  @Override
  public Warlord clone() throws CloneNotSupportedException {
    return new OrcWarlord();
  }

  @Override
  public String toString() {
    return "Orcish warlord";
  }

}

public class ElfWarlord extends Warlord {

  public ElfWarlord() {}

  @Override
  public Warlord clone() throws CloneNotSupportedException {
    return new ElfWarlord();
  }

  @Override
  public String toString() {
    return "Elven warlord";
  }

}

public abstract class Mage extends Prototype {

  @Override
  public abstract Mage clone() throws CloneNotSupportedException;

}

public class OrcMage extends Mage {

  public OrcMage() {}

  @Override
  public Mage clone() throws CloneNotSupportedException {
    return new OrcMage();
  }

  @Override
  public String toString() {
    return "Orcish mage";
  }

}

public class ElfMage extends Mage {

  public ElfMage() {}

  @Override
  public Mage clone() throws CloneNotSupportedException {
    return new ElfMage();
  }

  @Override
  public String toString() {
    return "Elven mage";
  }

}

public abstract class Beast extends Prototype {

  @Override
  public abstract Beast clone() throws CloneNotSupportedException;

}

public class ElfBeast extends Beast {

  public ElfBeast() {}

  @Override
  public Beast clone() throws CloneNotSupportedException {
    return new ElfBeast();
  }

  @Override
  public String toString() {
    return "Elven eagle";
  }

}

public class OrcBeast extends Beast {

  public OrcBeast() {}

  @Override
  public Beast clone() throws CloneNotSupportedException {
    return new OrcBeast();
  }

  @Override
  public String toString() {
    return "Orcish wolf";
  }

}

public interface HeroFactory {

  Mage createMage();

  Warlord createWarlord();

  Beast createBeast();

}

public class HeroFactoryImpl implements HeroFactory {

  private Mage mage;
  private Warlord warlord;
  private Beast beast;

  /**
   * Constructor
   */
  public HeroFactoryImpl(Mage mage, Warlord warlord, Beast beast) {
    this.mage = mage;
    this.warlord = warlord;
    this.beast = beast;
  }

  /**
   * Create mage
   */
  public Mage createMage() {
    try {
      return mage.clone();
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }

  /**
   * Create warlord
   */
  public Warlord createWarlord() {
    try {
      return warlord.clone();
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }

  /**
   * Create beast
   */
  public Beast createBeast() {
    try {
      return beast.clone();
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }

}

  public static void main(String[] args) {
    HeroFactory factory;
    Mage mage;
    Warlord warlord;
    Beast beast;

    factory = new HeroFactoryImpl(new ElfMage(), new ElfWarlord(), new ElfBeast());
    mage = factory.createMage();
    warlord = factory.createWarlord();
    beast = factory.createBeast();
    LOGGER.info(mage.toString());
    LOGGER.info(warlord.toString());
    LOGGER.info(beast.toString());

    factory = new HeroFactoryImpl(new OrcMage(), new OrcWarlord(), new OrcBeast());
    mage = factory.createMage();
    warlord = factory.createWarlord();
    beast = factory.createBeast();
    LOGGER.info(mage.toString());
    LOGGER.info(warlord.toString());
    LOGGER.info(beast.toString());
  }
```