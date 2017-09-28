学习难度：★★★☆☆，使用频率：★★☆☆☆

```java
public enum RequestType {
  DEFEND_CASTLE, TORTURE_PRISONER, COLLECT_TAX
}

public class Request {

  /**
   * The type of this request, used by each item in the chain to see if they should or can handle
   * this particular request
   */
  private final RequestType requestType;

  /**
   * A description of the request
   */
  private final String requestDescription;

  /**
   * Indicates if the request is handled or not. A request can only switch state from unhandled to
   * handled, there's no way to 'unhandle' a request
   */
  private boolean handled;

  /**
   * Create a new request of the given type and accompanied description.
   *
   * @param requestType        The type of request
   * @param requestDescription The description of the request
   */
  public Request(final RequestType requestType, final String requestDescription) {
    this.requestType = Objects.requireNonNull(requestType);
    this.requestDescription = Objects.requireNonNull(requestDescription);
  }

  /**
   * Get a description of the request
   *
   * @return A human readable description of the request
   */
  public String getRequestDescription() {
    return requestDescription;
  }

  /**
   * Get the type of this request, used by each person in the chain of command to see if they should
   * or can handle this particular request
   *
   * @return The request type
   */
  public RequestType getRequestType() {
    return requestType;
  }

  /**
   * Mark the request as handled
   */
  public void markHandled() {
    this.handled = true;
  }

  /**
   * Indicates if this request is handled or not
   *
   * @return <tt>true</tt> when the request is handled, <tt>false</tt> if not
   */
  public boolean isHandled() {
    return this.handled;
  }

  @Override
  public String toString() {
    return getRequestDescription();
  }
}

public abstract class RequestHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandler.class);

  private RequestHandler next;

  public RequestHandler(RequestHandler next) {
    this.next = next;
  }

  /**
   * Request handler
   */
  public void handleRequest(Request req) {
    if (next != null) {
      next.handleRequest(req);
    }
  }

  protected void printHandling(Request req) {
    LOGGER.info("{} handling request \"{}\"", this, req);
  }

  @Override
  public abstract String toString();
}

public abstract class RequestHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandler.class);

  private RequestHandler next;

  public RequestHandler(RequestHandler next) {
    this.next = next;
  }

  /**
   * Request handler
   */
  public void handleRequest(Request req) {
    if (next != null) {
      next.handleRequest(req);
    }
  }

  protected void printHandling(Request req) {
    LOGGER.info("{} handling request \"{}\"", this, req);
  }

  @Override
  public abstract String toString();
}

public class OrcSoldier extends RequestHandler {

  public OrcSoldier(RequestHandler handler) {
    super(handler);
  }

  @Override
  public void handleRequest(Request req) {
    if (req.getRequestType().equals(RequestType.COLLECT_TAX)) {
      printHandling(req);
      req.markHandled();
    } else {
      super.handleRequest(req);
    }
  }

  @Override
  public String toString() {
    return "Orc soldier";
  }
}

public class OrcOfficer extends RequestHandler {

  public OrcOfficer(RequestHandler handler) {
    super(handler);
  }

  @Override
  public void handleRequest(Request req) {
    if (req.getRequestType().equals(RequestType.TORTURE_PRISONER)) {
      printHandling(req);
      req.markHandled();
    } else {
      super.handleRequest(req);
    }
  }

  @Override
  public String toString() {
    return "Orc officer";
  }

}

public class OrcKing {

  RequestHandler chain;

  public OrcKing() {
    buildChain();
  }

  private void buildChain() {
    chain = new OrcCommander(new OrcOfficer(new OrcSoldier(null)));
  }

  public void makeRequest(Request req) {
    chain.handleRequest(req);
  }

}

public class OrcCommander extends RequestHandler {

  public OrcCommander(RequestHandler handler) {
    super(handler);
  }

  @Override
  public void handleRequest(Request req) {
    if (req.getRequestType().equals(RequestType.DEFEND_CASTLE)) {
      printHandling(req);
      req.markHandled();
    } else {
      super.handleRequest(req);
    }
  }

  @Override
  public String toString() {
    return "Orc commander";
  }
}

  public static void main(String[] args) {

    OrcKing king = new OrcKing();
    king.makeRequest(new Request(RequestType.DEFEND_CASTLE, "defend castle"));
    king.makeRequest(new Request(RequestType.TORTURE_PRISONER, "torture prisoner"));
    king.makeRequest(new Request(RequestType.COLLECT_TAX, "collect tax"));

  }
```

# Android中的应用
在Android处理点击事件时，父View先接收到点击事件，如果父View不处理则交给子View，把责任依次往下传递；还有Java的异常捕获机制也是责任链模式的一种体现