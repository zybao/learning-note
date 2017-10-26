# [Room](http://blog.csdn.net/hubinqiang/article/details/73012353)
Room提供了一个SQLite之上的抽象层，使得在充分利用SQLite功能的前提下顺畅的访问数据库。

对于需要处理大量结构化数据的App来说，把这些数据做本地持久化会带来很大的好处。常见的用例是缓存重要数据块。这样当设备无法连网的时候，用户仍然可以浏览内容。而用户对内容做出的任何改动都在网络恢复的时候同步到服务端。

核心framework内置了对SQL的支持。虽然这些API很强大，但是都很低级，使用起来很花时间和精力：

* 没有编译时的SQL查询检查机制。当数据表发生改变的时候，需要手动更新受影响的SQL查询。这个过程既耗时又容易出错。

* 需要写很多公式化的代码在SQL查询与Java对象之间转换。

Room处理了这些相关的事情，同时提供了SQLite之上的抽象层。

Room中有三个主要的组件：

* `Database`:你可以用这个组件来创建一个database holder。注解定义实体的列表，类的内容定义从数据库中获取数据的对象（DAO）。它也是底层连接的主要入口。

这个被注解的类是一个继承RoomDatabase的抽象类。在运行时，可以通过调用Room.databaseBuilder() 或者 Room.inMemoryDatabaseBuilder()来得到它的实例。

* `Entity`:这个组件代表一个持有数据库的一个表的类。对每一个entity，都会创建一个表来持有这些item。你必须在Database类中的entities数组中引用这些entity类。entity中的每一个field都将被持久化到数据库，除非使用了@Ignore注解。

    注：实体可以有一个空构造函数（如果DAO类可以访问每个持久化字段），或者一个构造函数的参数包含与实体中的字段匹配的类型和名称。Romm还可以使用全部或部分构造函数，例如只接收一些字段的构造函数。

* `DAO`:这个组件代表一个作为Data Access Objec的类或者接口。DAO是Room的主要组件，负责定义查询（添加或者删除等）数据库的方法。使用@Database注解的类必须包含一个0参数的，返回类型为@Dao注解过的类的抽象方法。Room会在编译时生成这个类的实现。

    注：通过DAO而不是query builders或者直接的query语句来处理数据库，可以把数据库的各个部分分离开来。而且DAO还可以让你轻松的使用假的database来测试app。

```java
@Database(entities = {UserEntity.class}, version = 1)
@TypeConverters(DateConverter.class)
public abstract class AppDatabase extends RoomDatabase {

    static final String DATABASE_NAME = "basic-sample-db";

    public abstract UserDao userDao();
}

public class DateConverter {
    @TypeConverter
    public static Date toDate(Long timestamp) {
        return timestamp == null ? null : new Date(timestamp);
    }

    @TypeConverter
    public static Long toTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
}

public interface UserDao {
    @Query("SELECT * from user")
    List<UserEntity> getAllUser();

    @Query("SELECT * FROM user WHERE uid IN (:userIds)")
    List<UserEntity> findUserByIds(int[] userIds);

    @Query("SELECT * FROM user WHERE first_name LIKE :first AND "
            + "last_name LIKE :last LIMIT 1")
    UserEntity findByName(String first, String last);

    @Insert
    void insertAll(UserEntity... users);

    @Delete
    void delete(UserEntity user);
}

@Entity(tableName = "user")
public class UserEntity {
    @PrimaryKey
    public int uid;

    @ColumnInfo(name = "first_name")
    public String firstName;

    @ColumnInfo(name = "second_name")
    public String secondName;
}
```