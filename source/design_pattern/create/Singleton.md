学习难度：★☆☆☆☆，使用频率：★★★★☆
```java
public enum EnumIvoryTower {

  INSTANCE;

  @Override
  public String toString() {
    return getDeclaringClass().getCanonicalName() + "@" + hashCode();
  }
}

public final class InitializingOnDemandHolderIdiom {

  /**
   * Private constructor.
   */
  private InitializingOnDemandHolderIdiom() {}

  /**
   * @return Singleton instance
   */
  public static InitializingOnDemandHolderIdiom getInstance() {
    return HelperHolder.INSTANCE;
  }

  /**
   * Provides the lazy-loaded Singleton instance.
   */
  private static class HelperHolder {
    private static final InitializingOnDemandHolderIdiom INSTANCE =
        new InitializingOnDemandHolderIdiom();
  }
}

public final class IvoryTower {

  /**
   * Private constructor so nobody can instantiate the class.
   */
  private IvoryTower() {}

  /**
   * Static to class instance of the class.
   */
  private static final IvoryTower INSTANCE = new IvoryTower();

  /**
   * To be called by user to obtain instance of the class.
   *
   * @return instance of the singleton.
   */
  public static IvoryTower getInstance() {
    return INSTANCE;
  }
}

/**
 * Thread-safe Singleton class. The instance is lazily initialized and thus needs synchronization
 * mechanism.
 *
 * Note: if created by reflection then a singleton will not be created but multiple options in the
 * same classloader
 */
public final class ThreadSafeLazyLoadedIvoryTower {

  private static ThreadSafeLazyLoadedIvoryTower instance;

  private ThreadSafeLazyLoadedIvoryTower() {
  // to prevent instantiating by Reflection call
    if (instance != null) {
      throw new IllegalStateException("Already initialized.");
    }
  }

  /**
   * The instance gets created only when it is called for first time. Lazy-loading
   */
  public static synchronized ThreadSafeLazyLoadedIvoryTower getInstance() {

    if (instance == null) {
      instance = new ThreadSafeLazyLoadedIvoryTower();
    }

    return instance;
  }
}

public final class ThreadSafeDoubleCheckLocking {

  private static volatile ThreadSafeDoubleCheckLocking instance;

  /**
   * private constructor to prevent client from instantiating.
   */
  private ThreadSafeDoubleCheckLocking() {
    // to prevent instantiating by Reflection call
    if (instance != null) {
      throw new IllegalStateException("Already initialized.");
    }
  }

  /**
   * Public accessor.
   *
   * @return an instance of the class.
   */
  public static ThreadSafeDoubleCheckLocking getInstance() {
    // local variable increases performance by 25 percent
    // Joshua Bloch "Effective Java, Second Edition", p. 283-284
    
    ThreadSafeDoubleCheckLocking result = instance;
    // Check if singleton instance is initialized. If it is initialized then we can return the instance.
    if (result == null) {
      // It is not initialized but we cannot be sure because some other thread might have initialized it
      // in the meanwhile. So to make sure we need to lock on an object to get mutual exclusion.
      synchronized (ThreadSafeDoubleCheckLocking.class) {
        // Again assign the instance to local variable to check if it was initialized by some other thread
        // while current thread was blocked to enter the locked zone. If it was initialized then we can 
        // return the previously created instance just like the previous null check.
        result = instance;
        if (result == null) {
          // The instance is still not initialized so we can safely (no other thread can enter this zone)
          // create an instance and make it our singleton instance.
          instance = result = new ThreadSafeDoubleCheckLocking();
        }
      }
    }
    return result;
  }
}
```