学习难度：★★★☆☆，使用频率：★★☆☆☆

中介者模式包装了一系列对象相互作用的方式，使得这些对象不必相互明显调用，从而使他们可以轻松耦合。当某些对象之间的作用发生改变时，不会立即影响其他的一些对象之间的作用保证这些作用可以彼此独立的变化，中介者模式将多对多的相互作用转为一对多的相互作用。
其实，中介者对象是将系统从网状结构转为以调停者为中心的星型结构。
举个简单的例子，一台电脑包括：CPU、内存、显卡、IO设备。其实，要启动一台计算机，有了CPU和内存就够了。当然，如果你需要连接显示器显示画面，那就得加显卡，如果你需要存储数据，那就要IO设备，但是这并不是最重要的，它们只是分割开来的普通零件而已，我们需要一样东西把这些零件整合起来，变成一个完整体，这个东西就是主板。主板就是起到中介者的作用，任何两个模块之间的通信都会经过主板协调。

```java
public enum Action {

  HUNT("hunted a rabbit", "arrives for dinner"), TALE("tells a tale", "comes to listen"), GOLD(
      "found gold", "takes his share of the gold"), ENEMY("spotted enemies", "runs for cover"), NONE(
      "", "");

  private String title;
  private String description;

  Action(String title, String description) {
    this.title = title;
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  public String toString() {
    return title;
  }
}

public interface Party {

  void addMember(PartyMember member);

  void act(PartyMember actor, Action action);

}

public class PartyImpl implements Party {

  private final List<PartyMember> members;

  public PartyImpl() {
    members = new ArrayList<>();
  }

  @Override
  public void act(PartyMember actor, Action action) {
    for (PartyMember member : members) {
      if (!member.equals(actor)) {
        member.partyAction(action);
      }
    }
  }

  @Override
  public void addMember(PartyMember member) {
    members.add(member);
    member.joinedParty(this);
  }
}

public interface PartyMember {

  void joinedParty(Party party);

  void partyAction(Action action);

  void act(Action action);
}

public abstract class PartyMemberBase implements PartyMember {

  private static final Logger LOGGER = LoggerFactory.getLogger(PartyMemberBase.class);

  protected Party party;

  @Override
  public void joinedParty(Party party) {
    LOGGER.info("{} joins the party", this);
    this.party = party;
  }

  @Override
  public void partyAction(Action action) {
    LOGGER.info("{} {}", this, action.getDescription());
  }

  @Override
  public void act(Action action) {
    if (party != null) {
      LOGGER.info("{} {}", this, action);
      party.act(this, action);
    }
  }

  @Override
  public abstract String toString();

}

public class Hobbit extends PartyMemberBase {

  @Override
  public String toString() {
    return "Hobbit";
  }

}

public class Hunter extends PartyMemberBase {

  @Override
  public String toString() {
    return "Hunter";
  }
}

public class Rogue extends PartyMemberBase {

  @Override
  public String toString() {
    return "Rogue";
  }

}

  public static void main(String[] args) {

    // create party and members
    Party party = new PartyImpl();
    Hobbit hobbit = new Hobbit();
    Wizard wizard = new Wizard();
    Rogue rogue = new Rogue();
    Hunter hunter = new Hunter();

    // add party members
    party.addMember(hobbit);
    party.addMember(wizard);
    party.addMember(rogue);
    party.addMember(hunter);

    // perform actions -> the other party members
    // are notified by the party
    hobbit.act(Action.ENEMY);
    wizard.act(Action.TALE);
    rogue.act(Action.GOLD);
    hunter.act(Action.HUNT);
  }
```

# Android中的应用

在Binder机制中，就用到了中介者模式。我们知道系统启动时，各种系统服务会向ServiceManager提交注册，即ServiceManager持有各种系统服务的引用 ，当我们需要获取系统的Service时，比如ActivityManager、WindowManager等（它们都是Binder），首先是向ServiceManager查询指定标示符对应的Binder，再由ServiceManager返回Binder的引用。并且客户端和服务端之间的通信是通过Binder驱动来实现，这里的ServiceManager和Binder驱动就是中介者。