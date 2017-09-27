封装一些作用于某种数据结构中各元素的操作，它可以在不改变这个数据结构的前提下定义作用于这些元素的新的操作。
假如一个对象中存在着一些与本对象不相干（或者关系较弱）的操作，为了避免这些操作污染这个对象，则可以使用访问者模式来把这些操作封装到访问者中去。
假如一组对象中，存在着相似的操作，为了避免出现大量重复的代码，也可以将这些重复的操作封装到访问者中去。
访问者模式的目的是封装一些施加于某种数据结构元素之上的操作，一旦这些操作需要修改的话，接受这个操作的数据结构则可以保持不变。
访问者模式是23种设计模式中最复杂最难理解的一个，但他的使用率并不高，大部分情况下，我们不需要使用访问者模式，少数特定的场景才需要。

```java
public interface UnitVisitor {

  void visitSoldier(Soldier soldier);

  void visitSergeant(Sergeant sergeant);

  void visitCommander(Commander commander);

}

public class SoldierVisitor implements UnitVisitor {

  private static final Logger LOGGER = LoggerFactory.getLogger(SoldierVisitor.class);

  @Override
  public void visitSoldier(Soldier soldier) {
    LOGGER.info("Greetings {}", soldier);
  }

  @Override
  public void visitSergeant(Sergeant sergeant) {
    // Do nothing
  }

  @Override
  public void visitCommander(Commander commander) {
    // Do nothing
  }
}

public class SergeantVisitor implements UnitVisitor {

  private static final Logger LOGGER = LoggerFactory.getLogger(SergeantVisitor.class);

  @Override
  public void visitSoldier(Soldier soldier) {
    // Do nothing
  }

  @Override
  public void visitSergeant(Sergeant sergeant) {
    LOGGER.info("Hello {}", sergeant);
  }

  @Override
  public void visitCommander(Commander commander) {
    // Do nothing
  }
}

public class CommanderVisitor implements UnitVisitor {

  private static final Logger LOGGER = LoggerFactory.getLogger(CommanderVisitor.class);

  @Override
  public void visitSoldier(Soldier soldier) {
    // Do nothing
  }

  @Override
  public void visitSergeant(Sergeant sergeant) {
    // Do nothing
  }

  @Override
  public void visitCommander(Commander commander) {
    LOGGER.info("Good to see you {}", commander);
  }
}

public abstract class Unit {

  private Unit[] children;

  public Unit(Unit... children) {
    this.children = children;
  }

  /**
   * Accept visitor
   */
  public void accept(UnitVisitor visitor) {
    for (Unit child : children) {
      child.accept(visitor);
    }
  }
}

public class Soldier extends Unit {

  public Soldier(Unit... children) {
    super(children);
  }

  @Override
  public void accept(UnitVisitor visitor) {
    visitor.visitSoldier(this);
    super.accept(visitor);
  }

  @Override
  public String toString() {
    return "soldier";
  }
}

public class Sergeant extends Unit {

  public Sergeant(Unit... children) {
    super(children);
  }

  @Override
  public void accept(UnitVisitor visitor) {
    visitor.visitSergeant(this);
    super.accept(visitor);
  }

  @Override
  public String toString() {
    return "sergeant";
  }
}

public class Commander extends Unit {

  public Commander(Unit... children) {
    super(children);
  }

  @Override
  public void accept(UnitVisitor visitor) {
    visitor.visitCommander(this);
    super.accept(visitor);
  }

  @Override
  public String toString() {
    return "commander";
  }
}

  public static void main(String[] args) {

    Commander commander =
        new Commander(new Sergeant(new Soldier(), new Soldier(), new Soldier()), new Sergeant(
            new Soldier(), new Soldier(), new Soldier()));
    commander.accept(new SoldierVisitor());
    commander.accept(new SergeantVisitor());
    commander.accept(new CommanderVisitor());

  }
```

# Android中的应用
Android中运用访问者模式，其实主要是在编译期注解中，编译期注解核心原理依赖APT(Annotation Processing Tools)，著名的开源库比如ButterKnife、Dagger、Retrofit都是基于APT。