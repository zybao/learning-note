学习难度：★★★☆☆，使用频率：★★★★☆

```java
public abstract class LetterComposite {

  private List<LetterComposite> children = new ArrayList<>();

  public void add(LetterComposite letter) {
    children.add(letter);
  }

  public int count() {
    return children.size();
  }

  protected void printThisBefore() {}

  protected void printThisAfter() {}

  /**
   * Print
   */
  public void print() {
    printThisBefore();
    for (LetterComposite letter : children) {
      letter.print();
    }
    printThisAfter();
  }
}

public class Letter extends LetterComposite {

  private char c;

  public Letter(char c) {
    this.c = c;
  }

  @Override
  protected void printThisBefore() {
    System.out.print(c);
  }
}

public class Word extends LetterComposite {

  /**
   * Constructor
   */
  public Word(List<Letter> letters) {
    for (Letter l : letters) {
      this.add(l);
    }
  }

  @Override
  protected void printThisBefore() {
    System.out.print(" ");
  }
}

public class Sentence extends LetterComposite {

  /**
   * Constructor
   */
  public Sentence(List<Word> words) {
    for (Word w : words) {
      this.add(w);
    }
  }

  @Override
  protected void printThisAfter() {
    System.out.print(".");
  }
}


public class Messenger {

  LetterComposite messageFromOrcs() {

    List<Word> words = new ArrayList<>();

    words.add(new Word(Arrays.asList(new Letter('W'), new Letter('h'), new Letter('e'), new Letter(
        'r'), new Letter('e'))));
    words.add(new Word(Arrays.asList(new Letter('t'), new Letter('h'), new Letter('e'), new Letter(
        'r'), new Letter('e'))));
    words.add(new Word(Arrays.asList(new Letter('i'), new Letter('s'))));
    words.add(new Word(Arrays.asList(new Letter('a'))));
    words.add(new Word(Arrays.asList(new Letter('w'), new Letter('h'), new Letter('i'), new Letter(
        'p'))));
    words.add(new Word(Arrays.asList(new Letter('t'), new Letter('h'), new Letter('e'), new Letter(
        'r'), new Letter('e'))));
    words.add(new Word(Arrays.asList(new Letter('i'), new Letter('s'))));
    words.add(new Word(Arrays.asList(new Letter('a'))));
    words.add(new Word(Arrays.asList(new Letter('w'), new Letter('a'), new Letter('y'))));

    return new Sentence(words);

  }

  LetterComposite messageFromElves() {

    List<Word> words = new ArrayList<>();

    words.add(new Word(Arrays.asList(new Letter('M'), new Letter('u'), new Letter('c'), new Letter(
        'h'))));
    words.add(new Word(Arrays.asList(new Letter('w'), new Letter('i'), new Letter('n'), new Letter(
        'd'))));
    words.add(new Word(Arrays.asList(new Letter('p'), new Letter('o'), new Letter('u'), new Letter(
        'r'), new Letter('s'))));
    words.add(new Word(Arrays.asList(new Letter('f'), new Letter('r'), new Letter('o'), new Letter(
        'm'))));
    words.add(new Word(Arrays.asList(new Letter('y'), new Letter('o'), new Letter('u'), new Letter(
        'r'))));
    words.add(new Word(Arrays.asList(new Letter('m'), new Letter('o'), new Letter('u'), new Letter(
        't'), new Letter('h'))));

    return new Sentence(words);

  }

}
```

# Android 应用

定义：将对象组成成树形结构，以表示“部分-整体”的层次结构，使得用户对单个对象和组合对象的使用具有一致性。

上面的定义不太好理解，我们直接从Android中用到的组合模式说起。我们知道，Android中View的结构是树形结构，每个ViewGroup包含一系列的View，而ViewGroup本身又是View。这是Android中非常典型的组合模式。