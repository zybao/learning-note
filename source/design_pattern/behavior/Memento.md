学习难度：★★☆☆☆，使用频率：★★☆☆☆

**定义**

在不破坏封闭的前提下，捕获一个对象的内部状态，并在对象之外保存这个状态，这样，以后就可将对象恢复到原先保存的状态中。

```java
public enum StarType {

  SUN("sun"), RED_GIANT("red giant"), WHITE_DWARF("white dwarf"), SUPERNOVA("supernova"), DEAD(
      "dead star"), UNDEFINED("");

  private String title;

  StarType(String title) {
    this.title = title;
  }

  @Override
  public String toString() {
    return title;
  }
}

public interface StarMemento {

}

public class Star {

  private StarType type;
  private int ageYears;
  private int massTons;

  /**
   * Constructor
   */
  public Star(StarType startType, int startAge, int startMass) {
    this.type = startType;
    this.ageYears = startAge;
    this.massTons = startMass;
  }

  /**
   * Makes time pass for the star
   */
  public void timePasses() {
    ageYears *= 2;
    massTons *= 8;
    switch (type) {
      case RED_GIANT:
        type = StarType.WHITE_DWARF;
        break;
      case SUN:
        type = StarType.RED_GIANT;
        break;
      case SUPERNOVA:
        type = StarType.DEAD;
        break;
      case WHITE_DWARF:
        type = StarType.SUPERNOVA;
        break;
      case DEAD:
        ageYears *= 2;
        massTons = 0;
        break;
      default:
        break;
    }
  }

  StarMemento getMemento() {

    StarMementoInternal state = new StarMementoInternal();
    state.setAgeYears(ageYears);
    state.setMassTons(massTons);
    state.setType(type);
    return state;

  }

  void setMemento(StarMemento memento) {

    StarMementoInternal state = (StarMementoInternal) memento;
    this.type = state.getType();
    this.ageYears = state.getAgeYears();
    this.massTons = state.getMassTons();

  }

  @Override
  public String toString() {
    return String.format("%s age: %d years mass: %d tons", type.toString(), ageYears, massTons);
  }

  /**
   * 
   * StarMemento implementation
   * 
   */
  private static class StarMementoInternal implements StarMemento {

    private StarType type;
    private int ageYears;
    private int massTons;

    public StarType getType() {
      return type;
    }

    public void setType(StarType type) {
      this.type = type;
    }

    public int getAgeYears() {
      return ageYears;
    }

    public void setAgeYears(int ageYears) {
      this.ageYears = ageYears;
    }

    public int getMassTons() {
      return massTons;
    }

    public void setMassTons(int massTons) {
      this.massTons = massTons;
    }
  }
}
```

# Android中的应用
Activity的onSaveInstanceState和onRestoreInstanceState就是用到了备忘录模式，分别用于保存和恢复。