学习难度：★☆☆☆☆，使用频率：★★★★★

A facade is an object that provides a simplified interface to a larger body of code, such as a class library. A facade can

* make a software library easier to use, understand, and test, since the facade has convenient methods for common tasks,
* make the library more readable, for the same reason,
* reduce dependencies of outside code on the inner workings of a library, since most code uses the facade, thus allowing more flexibility in developing the system,
* wrap a poorly designed collection of APIs with a single well-designed API.


```java
public abstract class DwarvenMineWorker {

  private static final Logger LOGGER = LoggerFactory.getLogger(DwarvenMineWorker.class);

  public void goToSleep() {
    LOGGER.info("{} goes to sleep.", name());
  }

  public void wakeUp() {
    LOGGER.info("{} wakes up.", name());
  }

  public void goHome() {
    LOGGER.info("{} goes home.", name());
  }

  public void goToMine() {
    LOGGER.info("{} goes to the mine.", name());
  }

  private void action(Action action) {
    switch (action) {
      case GO_TO_SLEEP:
        goToSleep();
        break;
      case WAKE_UP:
        wakeUp();
        break;
      case GO_HOME:
        goHome();
        break;
      case GO_TO_MINE:
        goToMine();
        break;
      case WORK:
        work();
        break;
      default:
        LOGGER.info("Undefined action");
        break;
    }
  }

  /**
   * Perform actions
   */
  public void action(Action... actions) {
    for (Action action : actions) {
      action(action);
    }
  }

  public abstract void work();

  public abstract String name();

  static enum Action {
    GO_TO_SLEEP, WAKE_UP, GO_HOME, GO_TO_MINE, WORK
  }
}

public class DwarvenGoldDigger extends DwarvenMineWorker {

  private static final Logger LOGGER = LoggerFactory.getLogger(DwarvenGoldDigger.class);

  @Override
  public void work() {
    LOGGER.info("{} digs for gold.", name());
  }

  @Override
  public String name() {
    return "Dwarf gold digger";
  }
}

public class DwarvenTunnelDigger extends DwarvenMineWorker {

  private static final Logger LOGGER = LoggerFactory.getLogger(DwarvenTunnelDigger.class);

  @Override
  public void work() {
    LOGGER.info("{} creates another promising tunnel.", name());
  }

  @Override
  public String name() {
    return "Dwarven tunnel digger";
  }
}

public class DwarvenCartOperator extends DwarvenMineWorker {

  private static final Logger LOGGER = LoggerFactory.getLogger(DwarvenCartOperator.class);

  @Override
  public void work() {
    LOGGER.info("{} moves gold chunks out of the mine.", name());
  }

  @Override
  public String name() {
    return "Dwarf cart operator";
  }
}

public class DwarvenGoldmineFacade {

  private final List<DwarvenMineWorker> workers;

  /**
   * Constructor
   */
  public DwarvenGoldmineFacade() {
    workers = new ArrayList<>();
    workers.add(new DwarvenGoldDigger());
    workers.add(new DwarvenCartOperator());
    workers.add(new DwarvenTunnelDigger());
  }

  public void startNewDay() {
    makeActions(workers, DwarvenMineWorker.Action.WAKE_UP, DwarvenMineWorker.Action.GO_TO_MINE);
  }

  public void digOutGold() {
    makeActions(workers, DwarvenMineWorker.Action.WORK);
  }

  public void endDay() {
    makeActions(workers, DwarvenMineWorker.Action.GO_HOME, DwarvenMineWorker.Action.GO_TO_SLEEP);
  }

  private static void makeActions(Collection<DwarvenMineWorker> workers,
      DwarvenMineWorker.Action... actions) {
    for (DwarvenMineWorker worker : workers) {
      worker.action(actions);
    }
  }
}

  public static void main(String[] args) {
    DwarvenGoldmineFacade facade = new DwarvenGoldmineFacade();
    facade.startNewDay();
    facade.digOutGold();
    facade.endDay();
  }
}
```