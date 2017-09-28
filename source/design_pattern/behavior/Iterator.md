学习难度：★★★☆☆，使用频率：★★★★★

```java
public interface ItemIterator {

  boolean hasNext();

  Item next();
}

public enum ItemType {

  ANY, WEAPON, RING, POTION

}

public class Item {

  private ItemType type;
  private String name;

  public Item(ItemType type, String name) {
    this.setType(type);
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }

  public ItemType getType() {
    return type;
  }

  public final void setType(ItemType type) {
    this.type = type;
  }
}

public class TreasureChest {

  private List<Item> items;

  /**
   * Constructor
   */
  public TreasureChest() {
    items = new ArrayList<>();
    items.add(new Item(ItemType.POTION, "Potion of courage"));
    items.add(new Item(ItemType.RING, "Ring of shadows"));
    items.add(new Item(ItemType.POTION, "Potion of wisdom"));
    items.add(new Item(ItemType.POTION, "Potion of blood"));
    items.add(new Item(ItemType.WEAPON, "Sword of silver +1"));
    items.add(new Item(ItemType.POTION, "Potion of rust"));
    items.add(new Item(ItemType.POTION, "Potion of healing"));
    items.add(new Item(ItemType.RING, "Ring of armor"));
    items.add(new Item(ItemType.WEAPON, "Steel halberd"));
    items.add(new Item(ItemType.WEAPON, "Dagger of poison"));
  }

  ItemIterator iterator(ItemType itemType) {
    return new TreasureChestItemIterator(this, itemType);
  }

  /**
   * Get all items
   */
  public List<Item> getItems() {
    List<Item> list = new ArrayList<>();
    list.addAll(items);
    return list;
  }

}

public class TreasureChestItemIterator implements ItemIterator {

  private TreasureChest chest;
  private int idx;
  private ItemType type;

  /**
   * Constructor
   */
  public TreasureChestItemIterator(TreasureChest chest, ItemType type) {
    this.chest = chest;
    this.type = type;
    this.idx = -1;
  }

  @Override
  public boolean hasNext() {
    return findNextIdx() != -1;
  }

  @Override
  public Item next() {
    idx = findNextIdx();
    if (idx != -1) {
      return chest.getItems().get(idx);
    }
    return null;
  }

  private int findNextIdx() {

    List<Item> items = chest.getItems();
    boolean found = false;
    int tempIdx = idx;
    while (!found) {
      tempIdx++;
      if (tempIdx >= items.size()) {
        tempIdx = -1;
        break;
      }
      if (type.equals(ItemType.ANY) || items.get(tempIdx).getType().equals(type)) {
        break;
      }
    }
    return tempIdx;
  }
}

  public static void main(String[] args) {
    TreasureChest chest = new TreasureChest();

    ItemIterator ringIterator = chest.iterator(ItemType.RING);
    while (ringIterator.hasNext()) {
      LOGGER.info(ringIterator.next().toString());
    }

    LOGGER.info("----------");

    ItemIterator potionIterator = chest.iterator(ItemType.POTION);
    while (potionIterator.hasNext()) {
      LOGGER.info(potionIterator.next().toString());
    }

    LOGGER.info("----------");

    ItemIterator weaponIterator = chest.iterator(ItemType.WEAPON);
    while (weaponIterator.hasNext()) {
      LOGGER.info(weaponIterator.next().toString());
    }

    LOGGER.info("----------");

    ItemIterator it = chest.iterator(ItemType.ANY);
    while (it.hasNext()) {
      LOGGER.info(it.next().toString());
    }
  }
```

# Android中的应用
Android源码中，最典型的就是Cursor用到了迭代器模式，当我们使用SQLiteDatabase的query方法时，返回的就是Cursor对象，之后再通过Cursor去遍历数据