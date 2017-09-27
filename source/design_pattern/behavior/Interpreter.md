```java
public abstract class Expression {

  public abstract int interpret();

  @Override
  public abstract String toString();
}

public class MinusExpression extends Expression {

  private Expression leftExpression;
  private Expression rightExpression;

  public MinusExpression(Expression leftExpression, Expression rightExpression) {
    this.leftExpression = leftExpression;
    this.rightExpression = rightExpression;
  }

  @Override
  public int interpret() {
    return leftExpression.interpret() - rightExpression.interpret();
  }

  @Override
  public String toString() {
    return "-";
  }

}

public class MultiplyExpression extends Expression {

  private Expression leftExpression;
  private Expression rightExpression;

  public MultiplyExpression(Expression leftExpression, Expression rightExpression) {
    this.leftExpression = leftExpression;
    this.rightExpression = rightExpression;
  }

  @Override
  public int interpret() {
    return leftExpression.interpret() * rightExpression.interpret();
  }

  @Override
  public String toString() {
    return "*";
  }

}

public class NumberExpression extends Expression {

  private int number;

  public NumberExpression(int number) {
    this.number = number;
  }

  public NumberExpression(String s) {
    this.number = Integer.parseInt(s);
  }

  @Override
  public int interpret() {
    return number;
  }

  @Override
  public String toString() {
    return "number";
  }
}

public class PlusExpression extends Expression {

  private Expression leftExpression;
  private Expression rightExpression;

  public PlusExpression(Expression leftExpression, Expression rightExpression) {
    this.leftExpression = leftExpression;
    this.rightExpression = rightExpression;
  }

  @Override
  public int interpret() {
    return leftExpression.interpret() + rightExpression.interpret();
  }

  @Override
  public String toString() {
    return "+";
  }
}

  public static void main(String[] args) {
    String tokenString = "4 3 2 - 1 + *";
    Stack<Expression> stack = new Stack<>();

    String[] tokenList = tokenString.split(" ");
    for (String s : tokenList) {
      if (isOperator(s)) {
        Expression rightExpression = stack.pop();
        Expression leftExpression = stack.pop();
        LOGGER.info("popped from stack left: {} right: {}",
            leftExpression.interpret(), rightExpression.interpret());
        Expression operator = getOperatorInstance(s, leftExpression, rightExpression);
        LOGGER.info("operator: {}", operator);
        int result = operator.interpret();
        NumberExpression resultExpression = new NumberExpression(result);
        stack.push(resultExpression);
        LOGGER.info("push result to stack: {}", resultExpression.interpret());
      } else {
        Expression i = new NumberExpression(s);
        stack.push(i);
        LOGGER.info("push to stack: {}", i.interpret());
      }
    }
    LOGGER.info("result: {}", stack.pop().interpret());
  }

  public static boolean isOperator(String s) {
    return s.equals("+") || s.equals("-") || s.equals("*");
  }

  /**
   * Get expression for string
   */
  public static Expression getOperatorInstance(String s, Expression left, Expression right) {
    switch (s) {
      case "+":
        return new PlusExpression(left, right);
      case "-":
        return new MinusExpression(left, right);
      case "*":
        return new MultiplyExpression(left, right);
      default:
        return new MultiplyExpression(left, right);
    }
  }
```

# Android中的应用
这个用到的地方也不少，其一就是Android的四大组件需要在AndroidManifest.xml中定义，其实AndroidManifest.xml就定义了，等标签（语句）的属性以及其子标签，规定了具体的使用（语法），通过PackageManagerService（解释器）进行解析。