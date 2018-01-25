#SjtSimpleDao
一个基于 apache commons-dbutils 的简单易用的Dao实现。   
支持jdbc数据库和mongodb。简单的修改dao类的父类即可切换关系型数据库和mongodb数据库。   

##配置：DataSource，queryRunner 和 dao类。
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
          class="org.shenjitang.simpledao.test.dao.TestDaoImpl">
        <property name="colName" value="test"/>
    </bean>

```

##编写EntityBean:

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

##编写Dao:
```
public class TestDao<Test> extends JdbcDao {
}

```

##使用Dao：

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