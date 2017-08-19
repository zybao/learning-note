# gradle setup
```xml
buildscript {
  repositories {
    mavenCentral()
    jcenter()
  }
  dependencies {
    classpath 'com.android.tools.build:gradle:1.0.0'
    classpath 'com.neenbedankt.gradle.plugins:android-apt:1.4'
  }
}

apply plugin: 'com.android.application'
apply plugin: 'com.neenbedankt.android-apt'

repositories {
  mavenCentral()
  jcenter()
  maven {url "https://clojars.org/repo/"}
}

dependencies {
  apt 'frankiesardo:auto-parcel:1.0.3'
}
```

# AutoParcel
```java
@AutoValue
abstract class Person implements Parcelable {
  abstract String name();
  abstract List<Address> addresses();
  abstract Map<Person, Integer> likes();

  static Person create(String name, List<Address> addresses, Map<Person, Integer> likes) {
    return new AutoValue_Person(name, addresses, likes);
  }

  @AutoValue.Builder
  public abstract class Builder {
    public abstract Builder name(String name);
    public abstract Builder address();
  }
}
```