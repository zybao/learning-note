学习难度：★★☆☆☆，使用频率：★★★★☆

```java
public class FishingBoat {

  private static final Logger LOGGER = LoggerFactory.getLogger(FishingBoat.class);

  public void sail() {
    LOGGER.info("The fishing boat is sailing");
  }

}

public interface RowingBoat {
  void row();
}

public class FishingBoatAdapter implements RowingBoat {
  private FishingBoat boat;

  public FishingBoatAdapter() {
    boat = new FishingBoat();
  }

  @Override
  public void row() {
    boat.sail();
  }
}

public class Captain implements RowingBoat {
  private RowingBoat rowingBoat;

  public Captain() {}

  public Captain(RowingBoat rowingBoat) {
    this.rowingBoat = rowingBoat;
  }

  public void setRowingBoat(RowingBoat rowingBoat) {
    this.rowingBoat = rowingBoat;
  }

  @Override
  public void row() {
    rowingBoat.row();
  }

}

  public static void main(String[] args) {
    // The captain can only operate rowing boats but with adapter he is able to use fishing boats as well
    Captain captain = new Captain(new FishingBoatAdapter());
    captain.row();
  }
```