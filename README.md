# SjtSimpleDao
一个基于 apache commons-dbutils 的非常简单实用的Dao实现。   
目前支持jdbc数据库和mongodb。简单的修改dao类的父类即可切换关系型数据库和mongodb数据库。   
## 一、快速掌握（Spring Ioc + Dao方式)
### Spring中配置：DataSource，queryRunner 和 dao类。
```
    <bean id="dataSource" class="org.apache.commons.dbcp.BasicDataSource"
          destroy-method="close">
        <property name="driverClassName" value="com.mysql.jdbc.Driver"/>
        <property name="url" value="jdbc:mysql://localhost:3306/flow"/>
        <property name="username" value="root"/>
        <property name="password" value="password"/>
    </bean>
    <bean id="queryRunner" class="org.apache.commons.dbutils.QueryRunner" >
        <constructor-arg index="0" ref="dataSource" type="javax.sql.DataSource" />
    </bean>    
    <bean id="TestDao"
          class="org.shenjitang.simpledao.test.dao.TestDao">
        <property name="colName" value="test"/>
    </bean>

```

### 编写EntityBean:

```
public class Test {
    public Test(String name) {
        this.name = name;
    }
    private Integer id:
    private String name;
    private Long value;
    // get set method
    ...... 
}

```

### 编写Dao:
```
public class TestDao<Test> extends JdbcDao {
}

```

### 使用Dao：

```
Test test = new Test("hello");
testDao.insert(test);
test.setName("world");
testDao.update(test);
Test test1 = testDao.findOne("select * from test where name='world'");
Test test2 = testDao.findOne(Maps.newHashMap("name", "world"));
assertEquals(test1.getId(), test2.getId());
List<Test> all = testDao.findAll();
testDao.remove("name", "world");
......

```

## 二、Mysql，CommonSqlDao的用法
先上代码，一个非常复杂的查询，用一个非常简单的方式实现。同时有一对多，多对一，多对多
```
List<UserTable> list = CommonSqlDao.create(UserTable.class, queryRunner)
.where().eq("gender", "male").find();

```
这就查完了，那么一对多，多对一，多对多在哪里呢？
我们只要看一下UserTable.java就明白了。
```
import java.util.List;
import org.shenjitang.simple.dao.annotation.*;

@DbTable(value = "user_table", alias = "u")
@DbJoin(type=DbJoinType.INNER, table = "role_table", alias = "r", on = "id", eq = "role_id")
@DbJoin(type=DbJoinType.INNER, table = "class_table", alias = "c", on = "id", eq = "class_id")
public class UserTable {
    @DbField("id")
    private int userId;
    private String name;
    @DbField(value = "name", table = "r")
    private String roleName;
    @DbField(value = "name", table = "c")
    private String className;
    @DbLink(value = "userId", thisField = "userId")
    private List<Contact> contact;
    @DbLink(value = "id", thisField = "userId", bridge = "user_id:user_group_table:group_id")
    private List<GroupTable> groups;
    @DbNoMap
    private String lalala;
    public UserTable() {
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
//get set
......

        
}
```
我们观察一下这个类，查询的主表是user_table。我们可以通过标注@DbTable指定。可以同时指定多个表
```
@DbTable（value="table1", alias="a")
@DbTable（value="table2")
public class MyBean {
    ...
}
```
相当于 from table1 a, table2

当然，也可以不使用这个标注，那么系统将根据约定规则得到表名。规则就是类名由驼峰转下划线。比如类名为UserInfo，表名就是user_info.

@DbJoin标签表示有表连接，需要5个参数：
#### type: 表示连接方式，连接方式有三种，分别为：
- DbJoinType.INNER 内连接，表现在SQL中为:inner join
- DbJoinType.LEFT 左连接，表现在SQL中为:left join
- DbJoinType.RIGHT 右连接，表现在SQL中为:right join
#### table: 连接的表名
#### alias: 连接的表的别名
#### on: 被连接表的连接字段
#### eq: 主表的连接字段。
例子中：
`@DbJoin(type=DbJoinType.INNER, table = "role_table", alias = "r", on = "id", eq = "role_id")`
翻译成sql相当于：`inner join role_table on u.id=r.role_id`。on参数和eq参数的值可以带上表的别名，比如：`on = "u.id", eq = "r.role_id"`。

@DbField指定字段对应的表字段名，也可以省略这个标签，省略时按照约定的规则匹配字段名，驼峰转下划线。比如字段名为userId，对应的数据库表字段名为：user_id。
```
    @DbField(value = "name", table = "r")
    private String roleName;
```
这段代码指出字段roleName对应表role_table的name字段。
