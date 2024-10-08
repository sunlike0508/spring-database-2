# 데이터 접근 기술 시작

## 데이터 접근 기술

### SQL Mapper

jdbcTemplate, MyBatis

* 개발자가 SQL만 작성하면 결과를 객체로 편리하게 매핑해줌
* JDBC를 직접 사용할 때 발생하는 여러가지 중복을 제거해주고, 기타 개발자에게 여러가지 편리한 기능을 제공함

### ORM

JPA, Hibernate, 스피링 데이터 JPA, Querydsl

* SQL 매퍼 기술은 개발자가 직접 SQL을 작성해야 한다. JPA를 사용하면 기본적인 SQL은 JPA가 대신 작성하고 처리해줌.
* 개발자는 저장하고 싶은 객체를 마치 자바 컬렉션에 저장하고 조회하듯이 사용하면 ORM이 데이터베이스 해당 객체를 저장하고 조회해준다.
* JPA는 자바 진영의 ORM 표준이고, Hibernate는 JPA에서 가장 많이 사용하는 구현체이다.
* 자바에서 ORM을 사용할 때는 JPA 인터페이스를 사용하고 구현체로 Hibernate를 사용한다.
* 스프링 Data JPA와 Querydsl은 JPA를 더 편리하게 사용할 수 있게 도와주는 프로젝트이다.

**DTO 위치**

***최종적으로 DTO를 사용하는 쪽에서 DTO를 가지고 있어야 한다.***

예를들어 검색을 위해 검색DTO를 통해 검색한다고 하면, 서비스에에서 repository를 가져다 쓰는건데 검색dto는 레포지토리 패키지에 있어야 한다.

# JdbcTemplate

SQL을 직접 사용하는 경우에 스프링이 제공하는 JdbcTemplate은 아주 좋은 선택지다. JdbcTemplate은 JDBC를
매우 편리하게 사용할 수 있게 도와준다.

**장점**

* 설정의 편리함
    * JdbcTemplate은 `spring-jdbc` 라이브러리에 포함되어 있는데, 이 라이브러리는 스프링으로 JDBC를 사용할 때 기본으로 사용되는 라이브러리이다.
    * 그리고 별도의 복잡한 설정 없이 바로 사용할 수 있다.
* 반복 문제 해결
    * JdbcTemplate은 템플릿 콜백 패턴을 사용해서, JDBC를 직접 사용할 때 발생하는 대부분의 반복 작업을 대신 처리해준다.
    * 개발자는 SQL을 작성하고, 전달할 파리미터를 정의하고, 응답 값을 매핑하기만 하면 된다.
    * 우리가 생각할 수 있는 대부분의 반복 작업을 대신 처리해준다.
        * 커넥션 획득 `statement` 를 준비하고 실행
        * 결과를 반복하도록 루프를 실행
        * 커넥션 종료, `statement` , `resultset` 종료 트랜잭션 다루기 위한 커넥션 동기화
        * 예외 발생시 스프링 예외 변환기 실행

**단점**

동적 SQL을 해결하기 어렵다.

## JdbcTemplate 문제

### 동적 쿼리

결과를 검색하는 `findAll()` 에서 어려운 부분은 사용자가 검색하는 값에 따라서 실행하는 SQL이 동적으로 달려져 야 한다는 점이다.

예를 들어서 다음과 같다.

* 검색 조건이 없음

```sql
select id, item_name, price, quantity
from item 
```

* 상품명( `itemName` )으로 검색

```sql
select id, item_name, price, quantity
from item
where item_name like concat('%', ?, '%')
```

* 최대 가격( `maxPrice` )으로 검색

```sql
select id, item_name, price, quantity
from item
where price <= ?
```

* 상품명( `itemName` ), 최대 가격( `maxPrice` ) 둘다 검색

```sql
 select id, item_name, price, quantity
 from item
 where item_name like concat('%', ?, '%')
   and price <= ?
```

결과적으로 4가지 상황에 따른 SQL을 동적으로 생성해야 한다.

동적 쿼리가 언듯 보면 쉬워 보이지만, 막상 개발해보면 생각보다 다양한 상황을 고민해야한다.

예를 들어서 어떤 경우에는 `where` 를 앞에 넣고 어떤 경우에는 `and` 를 넣어야 하는지 등을 모두 계산해야 한다.

그리고 각 상황에 맞추어 파라미터도 생성해야 한다.

물론 실무에서는 이보다 훨씬 더 복잡한 동적 쿼리들이 사용된다.

이를 해결하기 위해 MyBatis가 나왔다.

MyBatis의 가장 큰 장점은 SQL을 직접 사용할 때 동적 쿼리를 쉽게 작성할 수 있다는 점이다.

### 파라미터 바인딩

JdbcTemplate을 기본으로 사용하면 파라미터를 순서대로 바인딩 한다. 예를 들어서 다음 코드를 보자.

```
 String sql = "update item set item_name=?, price=?, quantity=? where id=?";
 template.update(sql, itemName, price, quantity, itemId);
```

여기서는 `itemName` , `price` , `quantity` 가 `SQL에 있는 ?` 에 순서대로 바인딩 된다.

따라서 순서만 잘 지키면 문제가 될 것은 없다. 그런데 문제는 변경시점에 발생한다.

누군가 다음과 같이 SQL 코드의 순서를 변경했다고 가정해보자. ( `price` 와 `quantity` 의 순서를 변경했다.)

```
String sql = "update item set item_name=?, quantity=?, price=? where id=?";
template.update(sql, itemName, price, quantity, itemId);
```

이렇게 되면 다음과 같은 순서로 데이터가 바인딩 된다.

`item_name=itemName, quantity=price, price=quantity`

결과적으로 `price` 와 `quantity` 가 바뀌는 매우 심각한 문제가 발생한다.

실무에서는 파라 미터가 10~20개가 넘어가는 일도 아주 많다.

그래서 미래에 필드를 추가하거나, 수정하면서 이런 문제가 충분히 발생 할 수 있다.

이러한 문제를 해결하기 위해 JdbcTemplate은 자동 파라미터 바인딩을 제공한다.

**1. Map**

단순히 `Map` 을 사용한다.

```java
Map<String, Object> param = Map.of("id", id);
Item item = template.queryForObject(sql, param, itemRowMapper());
```

**2. MapSqlParameterSource**

`Map` 과 유사한데, SQL 타입을 지정할 수 있는 등 SQL에 좀 더 특화된 기능을 제공한다.

`SqlParameterSource` 인터페이스의 구현체이다.

`MapSqlParameterSource` 는 메서드 체인을 통해 편리한 사용법도 제공한다.

`update()` 코드에서 확인할 수 있다.

```java
public void update(Long itemId, ItemUpdateDto updateParam) {
    String sql = "UPDATE item SET item_name = :itemName, price = :price , quantity = :quantity WHERE id = :id";

    SqlParameterSource param = new MapSqlParameterSource().addValue("itemName", updateParam.getItemName()).addValue("price", updateParam.getPrice())
            .addValue("quantity", updateParam.getQuantity()).addValue("id", itemId);

    jdbcTemplate.update(sql, param);
}
```

**3. BeanPropertySqlParameterSource**

자바빈 프로퍼티 규약을 통해서 자동으로 파라미터 객체를 생성한다.

예)(`getXxx() -> xxx, getItemName() -> itemName` )

예를 들어서 `getItemName()` , `getPrice()` 가 있으면 다음과 같은 데이터를 자동으로 만들어낸다.

`key=itemName, value=상품명 값` `key=price, value=가격 값`

`SqlParameterSource` 인터페이스의 구현체이다. `save()` , `findAll()` 코드에서 확인할 수 있다.

```java
public Item save(Item item) {
    String sql = "INSERT INTO item (item_name,  price, quantity) VALUES (:itemName, :price, :quantity)";

    SqlParameterSource param = new BeanPropertySqlParameterSource(item);

    KeyHolder keyHolder = new GeneratedKeyHolder();

    jdbcTemplate.update(sql, param, keyHolder);

    long key = keyHolder.getKey().longValue();
    item.setId(key);
    return item;
}
```

여기서 보면 `BeanPropertySqlParameterSource` 가 많은 것을 자동화 해주기 때문에 가장 좋아보이지만, `BeanPropertySqlParameterSource` 를 항상 사용할 수 있는 것은 아니다.

예를 들어서 `update()` 에서는 SQL에 `:id` 를 바인딩 해야 하는데, `update()` 에서 사용하는 `ItemUpdateDto` 에는 `itemId` 가 없다.

따라서 `BeanPropertySqlParameterSource` 를 사용할 수 없고, 대신에 `MapSqlParameterSource` 를 사용했다.

```java
private RowMapper<Item> itemRowMapper() {
    return BeanPropertyRowMapper.newInstance(Item.class); //camel 변환 지원
}
```

`BeanPropertyRowMapper` 는 `ResultSet` 의 결과를 받아서 자바빈 규약에 맞추어 데이터를 변환한다.

예를 들어서 데이터베이스에서 조회한 결과가 `select id, price` 라고 하면 다음과 같은 코드를 작성해준다.
(실제로는 리플렉션 같은 기능을 사용한다.)

**별칭**

그런데 `select item_name` 의 경우 `setItem_name()` 이라는 메서드가 없기 때문에 골치가 아프다.

이런 경우 개발자가 조회 SQL을 다음과 같이 고치면 된다.

`select item_name as itemName`

별칭 `as` 를 사용해서 SQL 조회 결과의 이름을 변경하는 것이다.

실제로 이 방법은 자주 사용된다. 특히 데이터베이스 컬럼 이름과 객체 이름이 완전히 다를 때 문제를 해결할 수 있다.

예를 들어서 데이터베이스에는 `member_name` 이라고 되어 있는데 객체에 `username` 이라고 되어 있다면 다음과 같이 해결할 수 있다.

`select member_name as username`

이렇게 데이터베이스 컬럼 이름과 객체의 이름이 다를 때 별칭( `as` )을 사용해서 문제를 많이 해결한다.

`JdbcTemplate` 은 물론이고, `MyBatis` 같은 기술에서도 자주 사용된다.

**관례의 불일치**

자바 객체는 카멜( `camelCase` ) 표기법을 사용한다. `itemName` 처럼 중간에 낙타 봉이 올라와 있는 표기법이다.

반면에 관계형 데이터베이스에서는 주로 언더스코어를 사용하는 `snake_case` 표기법을 사용한다.

`item_name` 처 럼 중간에 언더스코어를 사용하는 표기법이다.

이 부분을 관례로 많이 사용하다 보니 `BeanPropertyRowMapper` 는 언더스코어 표기법을 카멜로 자동 변환해준다.

따라서 `select item_name` 으로 조회해도 `setItemName()` 에 문제 없이 값이 들어간다.

정리하면 `snake_case` 는 자동으로 해결되니 그냥 두면 되고, 컬럼 이름과 객체 이름이 완전히 다른 경우에는 조회 SQL에서 별칭을 사용하면 된다.

## JdbcTemplate - SimpleJdbcInsert

JdbcTemplate은 INSERT SQL를 직접 작성하지 않아도 되도록 `SimpleJdbcInsert` 라는 편리한 기능을 제공한다.

```java
public class JdbTemplateItemRepositoryV3 implements ItemRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;


    public JdbTemplateItemRepositoryV3(DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        this.jdbcInsert = new SimpleJdbcInsert(dataSource).withTableName("item").usingGeneratedKeyColumns("id");
        //.usingColumns("item_name", "price", "quantity"); 생략가능
    }


    public Item save(Item item) {
        SqlParameterSource param = new BeanPropertySqlParameterSource(item);
        Number key = jdbcInsert.executeAndReturnKey(param);
        item.setId(key.longValue());
        return item;
    }
}
```

`SimpleJdbcInsert` 는 생성 시점에 데이터베이스 테이블의 메타 데이터를 조회한다.

따라서 어떤 컬럼이 있는지 확 인 할 수 있으므로 `usingColumns` 을 생략할 수 있다.

만약 특정 컬럼만 지정해서 저장하고 싶다면 `usingColumns` 를 사용하면 된다.

# 테스트 - 데이터베이스 연동

테스트 실행 - 로컬DB `ItemRepositoryTest` 테스트 코드를 확인해보자.

**@SpringBootTest**

```java

@SpringBootTest
class ItemRepositoryTest {}
```

`ItemRepositoryTest` 는 `@SpringBootTest` 를 사용한다.

`@SpringBootTest` 는 `@SpringBootApplication` 를 찾아서 설정으로 사용한다.

## 테스트 - 데이터베이스 분리

로컬에서 사용하는 애플리케이션 서버와 테스트에서 같은 데이터베이스를 사용하고 있으니 테스트에서 문제가 발생한다.

이런 문제를 해결하려면 테스트를 다른 환경과 철저하게 분리해야 한다.

가장 간단한 방법은 테스트 전용 데이터베이스를 별도로 운영하는 것이다.

H2 데이터베이스를 용도에 따라 2가지로 구분하면 된다.

* `jdbc:h2:tcp://localhost/~/test` local에서 접근하는 서버 전용 데이터베이스
* `jdbc:h2:tcp://localhost/~/testcase` test 케이스에서 사용하는 전용 데이터베이스

**데이터베이스 파일 생성 방법**

데이터베이스 서버를 종료하고 다시 실행한다.

사용자명은 `sa` 입력 JDBC URL에 다음 입력,

`jdbc:h2:~/testcase` (최초 한번) `~/testcase.mv.db` 파일 생성 확인

이후부터는 `jdbc:h2:tcp://localhost/~/testcase` 이렇게 접속

**접속 정보 변경**

`test` 에 있는 `application.properties` 변경해야 한다.

**test - application.properties** `

src/test/resources/application.properties`

```properties
 spring.profiles.active=test
spring.datasource.url=jdbc:h2:tcp://localhost/~/testcase
spring.datasource.username=sa 
```

접속 정보가 `jdbc:h2:tcp://localhost/~/test`

`jdbc:h2:tcp://localhost/~/testcase` 로 변경 된 것을 확인할 수 있다.

### 테스트에서 매우 중요한 원칙은 다음과 같다.

**테스트는 다른 테스트와 격리해야 한다.**

**테스트는 반복해서 실행할 수 있어야 한다.**

테스트가 끝날 때 마다 추가한 데이터에 `DELETE SQL` 을 사용해도 되겠지만, 이 방법도 궁극적인 해결책은 아니다.

만약 테스트 과정에서 데이터를 이미 추가했는데, 테스트가 실행되는 도중에 예외가 발생하거나 애플리케이션이 종료되어 버려서

테스트 종료 시점에 `DELETE SQL` 을 호출하지 못할 수 도 있다! 그러면 결국 데이터가 남아있게 된다.

## 테스트 - 데이터 롤백

**트랜잭션과 롤백 전략**

이때 도움이 되는 것이 바로 트랜잭션이다.

테스트가 끝나고 나서 트랜잭션을 강제로 롤백해버리면 데이터가 깔끔하게 제거된다.

테스트를 하면서 데이터를 이미 저장했는데, 중간에 테스트가 실패해서 롤백을 호출하지 못해도 괜찮다.

트랜잭션을 커밋하지 않았기 때문에 데이터베이스에 해당 데이터가 반영되지 않는다.

이렇게 트랜잭션을 활용하면 테스트가 끝나고 나서 데이터를 깔끔하게 원래 상태로 되돌릴 수 있다.

예를 들어서 다음 순서와 같이 각각의 테스트 실행 직전에 트랜잭션을 시작하고, 각각의 테스트 실행 직후에 트랜잭션을 롤백해야 한다.

그래야 다음 테스트에 데이터로 인한 영향을 주지 않는다.

```
1. 트랜잭션 시작
2. 테스트 A 실행 3. 트랜잭션 롤백
4. 트랜잭션 시작
5. 테스트 B 실행
6. 트랜잭션 롤백 
```

테스트는 각각의 테스트 실행 전 후로 동작하는 `@BeforeEach` , `@AfterEach` 라는 편리한 기능을 제공한다.

테스트에 트랜잭션과 롤백을 적용하기 위해 다음 코드를 추가하자.

```java

@Autowired
PlatformTransactionManager transactionManager;
TransactionStatus status;


@BeforeEach
void beforeEach() {
    //트랜잭션 시작
    status = transactionManager.getTransaction(new DefaultTransactionDefinition());
}


@AfterEach
void afterEach() {
    //MemoryItemRepository 의 경우 제한적으로 사용
    if(itemRepository instanceof MemoryItemRepository) {
        ((MemoryItemRepository) itemRepository).clearStore();
    }
    //트랜잭션 롤백
    transactionManager.rollback(status);
}
```

트랜잭션 관리자는 `PlatformTransactionManager` 를 주입 받아서 사용하면 된다.

참고로 스프링 부트는 자동으로 적절한 트랜잭션 매니저를 스프링 빈으로 등록해준다.

`@BeforeEach` : 각각의 테스트 케이스를 실행하기 직전에 호출된다. 따라서 여기서 트랜잭션을 시작하면 된다.

그러면 각각의 테스트를 트랜잭션 범위 안에서 실행할 수 있다.

`transactionManager.getTransaction(new DefaultTransactionDefinition())` 로 트랜잭션을 시작한다.

`@AfterEach` : 각각의 테스트 케이스가 완료된 직후에 호출된다. 따라서 여기서 트랜잭션을 롤백하면 된다.

그러면 데이터를 트랜잭션 실행 전 상태로 복구할 수 있다.

`transactionManager.rollback(status)` 로 트랜잭션을 롤백한다.

### @Transactional

위에 각종 설정하는 것을 이 어노테이션 하나로 끝난다.

**@Transactional 원리**

스프링이 제공하는 `@Transactional` 애노테이션은 로직이 성공적으로 수행되면 커밋하도록 동작한다.

그런데 `@Transactional` 애노테이션을 테스트에서 사용하면 아주 특별하게 동작한다.

`@Transactional` 이 테스트에 있으면 스프링은 테스트를 트랜잭션 안에서 실행하고, 테스트가 끝나면 트랜잭션을 자동으로 롤백시켜 버린다! `findItems()` 를 예시로 알아보자.

**@Transactional이 적용된 테스트 동작 방식**

<img width="920" alt="Screenshot 2024-09-29 at 18 23 24" src="https://github.com/user-attachments/assets/6a007e3d-8413-479c-b808-9aecc61f53cb">

1. 테스트에 `@Transactional` 애노테이션이 테스트 메서드나 클래스에 있으면 먼저 트랜잭션을 시작한다.
2. 테스트 로직을 실행한다. 테스트가 끝날 때 까지 모든 로직은 트랜잭션 안에서 수행된다.
    * 트랜잭션은 기본적으로 전파되기 때문에, 리포지토리에서 사용하는 JdbcTemplate도 같은 트랜잭션을 사용한다.
3. 테스트 실행 중에 INSERT SQL을 사용해서 `item1` , `item2` , `item3` 를 데이터베이스에 저장한다.
    * 물론 테스트가 리포지토리를 호출하고, 리포지토리는 JdbcTemplate을 사용해서 데이터를 저장한다.
4. 검증을 위해서 SELECT SQL로 데이터를 조회한다. 여기서는 앞서 저장한 `item1` , `item2` , `item3` 이 조회되었다.
    * SELECT SQL도 같은 트랜잭션을 사용하기 때문에 저장한 데이터를 조회할 수 있다.
    * 다른 트랜잭션에서는 해당 데이터를 확인할 수 없다.
    * 여기서 `assertThat()` 으로 검증이 모두 끝난다.
5. `@Transactional` 이 테스트에 있으면 테스트가 끝날때 트랜잭션을 강제로 롤백한다.
6. 롤백에 의해 앞서 데이터베이스에 저장한 `item1` , `item2` , `item3` 의 데이터가 제거된다.

**참고**
테스트 케이스의 메서드나 클래스에 `@Transactional` 을 직접 붙여서 사용할 때 만 이렇게 동작한다.

그리고 트랜잭션을 테스트에서 시작하기 때문에 서비스, 리포지토리에 있는 `@Transactional` 도 테스트에서 시작한 트랜잭션에 참여한다.

(이 부분은 뒤에 트랜잭션 전파에서 더 자세히 설명하겠다.

지금은 테스트에서 트랜잭션을 실행하면 테스트 실행이 종료될 때 까지 테스트가 실행하는 모든 코드가 같은 트랜잭션 범위에 들어간다고 이해하면 된다.

같은 범위라는 뜻은 쉽게 이야기해서 같은 트랜잭션을 사용한다는 뜻이다.

그리고 같은 트랜잭션을 사용한다는 것은 같은 커넥션을 사용한다는 뜻이기도 하다.)

**정리**
테스트가 끝난 후 개발자가 직접 데이터를 삭제하지 않아도 되는 편리함을 제공한다.

테스트 실행 중에 데이터를 등록하고 중간에 테스트가 강제로 종료되어도 걱정이 없다.

이 경우 트랜잭션을 커밋 하지 않기 때문에, 데이터는 자동으로 롤백된다. (보통 데이터베이스 커넥션이 끊어지면 자동으로 롤백되어 버린다.)

트랜잭션 범위 안에서 테스트를 진행하기 때문에 동시에 다른 테스트가 진행되어도 서로 영향을 주지 않는 장점이 있다.

`@Transactional` 덕분에 아주 편리하게 다음 원칙을 지킬수 있게 되었다. 테스트는 다른 테스트와 격리해야 한다.

테스트는 반복해서 실행할 수 있어야 한다.

**강제로 커밋하기 - @Commit**

`@Transactional` 을 테스트에서 사용하면 테스트가 끝나면 바로 롤백되기 때문에 테스트 과정에서 저장한 모든 데이터가 사라진다.

당연히 이렇게 되어야 하지만, 정말 가끔은 데이터베이스에 데이터가 잘 보관되었는지 최종 결과를 눈으로 확인하고 싶을 때도 있다.

이럴 때는 다음과 같이 `@Commit` 을 클래스 또는 메서드에 붙이면 테스트 종료후 롤백 대신 커밋이 호출된다. 참고로 `@Rollback(value = false)` 를 사용해도 된다.

```java
 import org.springframework.test.annotation.Commit;

@Commit
@Transactional
@SpringBootTest
class ItemRepositoryTest {} 
```

## 테스트 - 임베디드 모드 DB

테스트 케이스를 실행하기 위해서 별도의 데이터베이스를 설치하고, 운영하는 것은 상당히 번잡한 작업이다.

단순히 테스트를 검증할 용도로만 사용하기 때문에 테스트가 끝나면 데이터베이스의 데이터를 모두 삭제해도 된다.

더 나아가서 테스트가 끝나면 데이터베이스 자체를 제거해도 된다.

**임베디드 모드**

H2 데이터베이스는 자바로 개발되어 있고, JVM안에서 메모리 모드로 동작하는 특별한 기능을 제공한다.

그래서 애플리케이션을 실행할 때 H2 데이터베이스도 해당 JVM 메모리에 포함해서 함께 실행할 수 있다.

DB를 애플리케이션에 내장해서 함께 실행한다고 해서 임베디드 모드(Embedded mode)라 한다.

물론 애플리케이션이 종료되면 임베디드 모드로 동작하는 H2 데이터베이스도 함께 종료되고, 데이터도 모두 사라진다.

쉽게 이야기해서 애플리케이션에서 자바 메모리를 함께 사용하는 라이브러리처럼 동작하는 것이다.

이제 H2 데이터베이스를 임베디드 모드로 사용해보자.

```java

@Bean
@Profile("test")
public DataSource dataSource() {
    log.info("메모리 데이터베이스 초기화");
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName("org.h2.Driver");
    dataSource.setUrl("jdbc:h2:mem:db;DB_CLOSE_DELAY=-1");
    dataSource.setUsername("sa");
    dataSource.setPassword("");
    return dataSource;
}

```

`@Profile("test")` :프로필이 `test` 인 경우에만 데이터소스를 스프링 빈으로 등록한다.

테스트 케이스에서만 이 데이터소스를 스프링 빈으로 등록해서 사용하겠다는 뜻이다.

`jdbc:h2:mem:db` : 이 부분이 중요하다. 데이터소스를 만들때 이렇게만 적으면 임베디드 모드(메모리 모 드)로 동작하는 H2 데이터베이스를 사용할 수 있다.

`DB_CLOSE_DELAY=-1` : 임베디드 모드에서는 데이터베이스 커넥션 연결이 모두 끊어지면 데이터베이스 도 종료되는데, 그것을 방지하는 설정이다.

이 데이터소스를 사용하면 메모리 DB를 사용할 수 있다.

## 테스트 - 스프링 부트와 임베디드 모드

스프링 부트는 개발자에게 정말 많은 편리함을 제공하는데, 임베디드 데이터베이스에 대한 설정도 기본으로 제공한다.

스프링 부트는 데이터베이스에 대한 별다른 설정이 없으면 임베디드 데이터베이스를 사용한다.

앞서 직접 설정했던 메모리 DB용 데이터소스를 주석처리하자.

**dataSource bean 등록 즈삭**
**test - application.properties 에 datasource 설정**

로그를 보면 다음 부분을 확인할 수 있는데 `jdbc:h2:mem` 뒤에 임의의 데이터베이스 이름이 들어가 있다.

이것은 혹시라도 여러 데이터소스가 사용될 때 같은 데이터베이스를 사용하면서 발생하는 충돌을 방지하기 위해 스프링 부트가 임의의 이름을 부여한 것이다.

``` shell
HikariPool-1 - Added connection conn0:url=jdbc:h2:mem:37f9de4d-b071-451e-bc4b-dc775bb871a2 user=SA
```

임베디드 데이터베이스 이름을 스프링 부트가 기본으로 제공하는 `jdbc:h2:mem:testdb` 로 고정하고 싶으면

`application.properties` 에 다음 설정을 추가하면 된다.

```properties
spring.datasource.generate-unique-name=false
```

# MyBatis

MyBatis는 앞서 설명한 JdbcTemplate보다 더 많은 기능을 제공하는 SQL Mapper 이다.

기본적으로 JdbcTemplate이 제공하는 대부분의 기능을 제공한다.

JdbcTemplate과 비교해서 MyBatis의 가장 매력적인 점은 SQL을 XML에 편리하게 작성할 수 있고 또 동적 쿼리를 매우 편리하게 작성할 수 있다는 점이다.

먼저 SQL이 여러줄에 걸쳐 있을 때 둘을 비교해보자.

**JdbcTemplate - SQL 여러줄**

```java
 String sql = "update item " + "set item_name=:itemName, price=:price, quantity=:quantity " + "where id=:id";
```

**MyBatis - SQL 여러줄**

```xml

<update id="update">
    update item
    set item_name=#{itemName},
    price=#{price},
    quantity=#{quantity}
    where id = #{id}
</update> 
```

MyBatis는 XML에 작성하기 때문에 라인이 길어져도 문자 더하기에 대한 불편함이 없다.

다음으로 상품을 검색하는 로직을 통해 동적 쿼리를 비교해보자.

**JdbcTemplate - 동적 쿼리**

```java
void query() {
    String sql = "select id, item_name, price, quantity from item"; //동적 쿼리
    if(StringUtils.hasText(itemName) || maxPrice != null) {
        sql += " where";
    }
    boolean andFlag = false;
    if(StringUtils.hasText(itemName)) {
        sql += " item_name like concat('%',:itemName,'%')";
        andFlag = true;
    }
    if(maxPrice != null) {
        if(andFlag) {
            sql += " and";
        }
        sql += " price <= :maxPrice";
    }
    log.info("sql={}", sql);
    return template.query(sql, param, itemRowMapper());
}
```

**MyBatis - 동적 쿼리**

```xml

<select id="findAll" resultType="Item">
    select id, item_name, price, quantity
    from item
    <where>
        <if test="itemName != null and itemName != ''">
            and item_name like concat('%',#{itemName},'%')
        </if>
        <if test="maxPrice != null">
            and price &lt;= #{maxPrice}
        </if>
    </where>
</select>
```

JdbcTemplate은 자바 코드로 직접 동적 쿼리를 작성해야 한다.

반면에 MyBatis는 동적 쿼리를 매우 편리하게 작성 할 수 있는 다양한 기능들을 제공해준다.

**설정의 장단점**

JdbcTemplate은 스프링에 내장된 기능이고, 별도의 설정없이 사용할 수 있다는 장점이 있다. 반면에 MyBatis는 약 간의 설정이 필요하다.

**정리**
프로젝트에서 동적 쿼리와 복잡한 쿼리가 많다면 MyBatis를 사용하고, 단순한 쿼리들이 많으면 JdbcTemplate을 선택해서 사용하면 된다.

물론 둘을 함께 사용해도 된다. 하지만 MyBatis를 선택했다면 그것으로 충분할 것이다.

## 설정

```groovy
implementation 'org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.3'
```

## 분석

ItemMapper는 구현체가 없는데 어떻게 작동되는걸까?

<img width="930" alt="Screenshot 2024-09-29 at 22 46 35" src="https://github.com/user-attachments/assets/f505a039-60e4-421f-8744-099f170f422c">


MyBatis 스프링 연동 모듈에서 자동으로 처리해주는데 다음과 같다.

1. 애플리케이션 로딩 시점에 MyBatis 스프링 연동 모듈은 `@Mapper` 가 붙어있는 인터페이스를 조사한다.
2. 해당 인터페이스가 발견되면 동적 프록시 기술을 사용해서 `ItemMapper` 인터페이스의 구현체를 만든다.
3. 생성된 구현체를 스프링 빈으로 등록한다.

로그를 찍어보면 다음가 같다.

```shell
itemMapper class=class jdk.proxy3.$Proxy71
```

**매퍼 구현체**
마이바티스 스프링 연동 모듈이 만들어주는 `ItemMapper` 의 구현체 덕분에 인터페이스 만으로 편리하게 XML 의 데이터를 찾아서 호출할 수 있다.

원래 마이바티스를 사용하려면 더 번잡한 코드를 거쳐야 하는데, 이런 부분을 인터페이스 하나로 매우 깔끔하고 편리하게 사용할 수 있다.

매퍼 구현체는 예외 변환까지 처리해준다. MyBatis에서 발생한 예외를 스프링 예외 추상화인 `DataAccessException` 에 맞게 변환해서 반환해준다.

JdbcTemplate이 제공하는 예외 변환 기능을 여기서 도 제공한다고 이해하면 된다.

**정리**

매퍼 구현체 덕분에 마이바티스를 스프링에 편리하게 통합해서 사용할 수 있다.

매퍼 구현체를 사용하면 스프링 예외 추상화도 함께 적용된다.

마이바티스 스프링 연동 모듈이 많은 부분을 자동으로 설정해주는데, 데이터베이스 커넥션, 트랜잭션과 관련된 기능도 마이바티스와 함께 연동하고, 동기화해준다.

**참고**

마이바티스 스프링 연동 모듈이 자동으로 등록해주는 부분은 `MybatisAutoConfiguration` 클래스를 참고하 자.

**복잡한 결과매핑**

MyBatis도 매우 복잡한 결과에 객체 연관관계를 고려해서 데이터를 조회하는 것이 가능하다.

이때는 `<association>` , `<collection>` 등을 사용한다.

이 부분은 성능과 실효성에서 측면에서 많은 고민이 필요하다.

JPA는 객체와 관계형 데이터베이스를 ORM 개념으로 매핑하기 때문에 이런 부분이 자연스럽지만, MyBatis에서는 들 어가는 공수도 많고, 성능을 최적화하기도 어렵다.

따라서 해당기능을 사용할 때는 신중하게 사용해야 한다.

# JPA

스프링이 DI 컨테이너를 포함한 애플리케이션 전반의 다양한 기능을 제공한다면, JPA는 ORM 데이터 접근 기술을 제공한다.

JPA는 스프링 만큼이나 방대하고, 학습해야할 분량도 많다.

하지만 한번 배워두면 데이터 접근 기술에서 매우 큰 생산성 향상을 얻을 수 있다.

대표적으로 JdbcTemplate이나 MyBatis 같은 SQL 매퍼 기술은 SQL을 개발자가 직접 작성 해야 하지만, JPA를 사용하면 SQL도 JPA가 대신 작성하고 처리해준다.

실무에서는 JPA를 더욱 편리하게 사용하기 위해 스프링 데이터 JPA와 Querydsl이라는 기술을 함께 사용한다.

중요한 것은 JPA이다.

스프링 데이터 JPA, Querydsl은 JPA를 편리하게 사용하도록 도와주는 도구라 생각하면 된다.

이 강의에서는 모든 내용을 다루지 않고, JPA와 스프링 데이터 JPA, 그리고 Querydsl로 이어지는 전체 그림을 볼 것 이다.

그리고 이 기술들을 우리 애플리케이션에 적용하면서 자연스럽게 왜 사용해야 하는지, 그리고 어떤 장점이 있는지 이해할 수 있게 된다.

이렇게 전체 그림을 보고 나면 앞으로 어떻게 공부해야 할지 쉽게 접근할 수 있을 것이다.

## JPA 예외

`EntityManager` 는 순수한 JPA 기술이고, 스프링과는 관계가 없다.

따라서 엔티티 매니저는 예외가 발생하면 JPA 관련 예외를 발생시킨다.

JPA는 `PersistenceException` 과 그 하위 예외를 발생시킨다.

추가로 JPA는 `IllegalStateException` , `IllegalArgumentException` 을 발생시킬 수 있다.

그렇다면 JPA 예외를 스프링 예외 추상화( `DataAccessException` )로 어떻게 변환할 수 있을까?

비밀은 바로 `@Repository` 에 있다.

**@Repository의 기능**

<img width="926" alt="Screenshot 2024-10-01 at 11 20 29" src="https://github.com/user-attachments/assets/5dbe60f1-1666-4336-b7f9-ba5a27459545">

`@Repository` 가 붙은 클래스는 컴포넌트 스캔의 대상이 된다.

`@Repository` 가 붙은 클래스는 예외 변환 AOP의 적용 대상이 된다.

스프링과 JPA를 함께 사용하는 경우 스프링은 JPA 예외 변환기 ( `PersistenceExceptionTranslator` )를 등록한다.

예외 변환 AOP 프록시는 JPA 관련 예외가 발생하면 JPA 예외 변환기를 통해 발생한 예외를 스프링 데이터 접근 예외로 변환한다.

<img width="937" alt="Screenshot 2024-10-01 at 11 21 04" src="https://github.com/user-attachments/assets/53860b3b-5e4b-4afc-a63a-f094c2a13771">

결과적으로 리포지토리에 `@Repository` 애노테이션만 있으면 스프링이 예외 변환을 처리하는 AOP를 만들어준다.

**참고**

복잡한 과정을 거쳐서 실제 예외를 변환하는데, 실제 JPA 예외를 변환하는 코드는

`EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible()` 이다.

# Spring Data JPA

Repository 인터페이스를 상속 받은 CrudRepository 인터페이스

Repository 인터페이스를 상속 받은 PagingAndSortingRepository 인터페이스

이거 두 개를 상속 받은게 JpaRepository

# 스프링 데이터 JPA, trade off

중간에서 `JpaItemRepositoryV2` 가 어댑터 역할을 해준 덕분에 `ItemService` 가 사용하는

`ItemRepository` 인터페이스를 그대로 유지할 수 있고 클라이언트인 `ItemService` 의 코드를 변경하지 않아도 되는 장점이 있다.

**고민**

<img width="692" alt="Screenshot 2024-10-01 at 20 55 28" src="https://github.com/user-attachments/assets/1f20e6d2-24e3-42a4-adb7-c213ff6726d6">

구조를 맞추기 위해서, 중간에 어댑터가 들어가면서 전체 구조가 너무 복잡해지고 사용하는 클래스도 많아지는 단점이 생겼다.

실제 이 코드를 구현해야하는 개발자 입장에서 보면 중간에 어댑터도 만들고, 실제 코드까지 만들어야 하는 불편함이 생긴다.

유지보수 관점에서 `ItemService` 를 변경하지 않고, `ItemRepository` 의 구현체를 변경할 수 있는 장점이 있다.

그러니까 DI, OCP 원칙을 지킬 수 있다는 좋은 점이 분명히 있다.

하지만 반대로 구조가 복잡해지면서 어댑터 코드와 실제 코드까지 함께 유지보수 해야하는 어려움도 발생한다.

**다른 선택**

여기서 완전히 다른 선택을 할 수도 있다.

`ItemService` 코드를 일부 고쳐서 직접 스프링 데이터 JPA를 사용하는 방법이다.

DI, OCP 원칙을 포기하는 대신에, 복잡한 어댑터를 제거하고, 구조를 단순하게 가져갈 수 있는 장점이 있다.

<img width="708" alt="Screenshot 2024-10-01 at 20 57 30" src="https://github.com/user-attachments/assets/a237cfb1-56f9-415e-b186-ee6e8bc293ae">


`ItemService` 에서 스프링 데이터 JPA로 만든 리포지토리를 직접 참조한다. 물론 이 경우 `ItemService` 코드를 변경해야 한다.

<img width="693" alt="Screenshot 2024-10-01 at 20 57 48" src="https://github.com/user-attachments/assets/5e0a8f63-f94e-4bca-9598-b6183360032b">

**트레이드 오프**

이것이 바로 트레이드 오프다.

DI, OCP를 지키기 위해 어댑터를 도입하고, 더 많은 코드를 유지한다.

어댑터를 제거하고 구조를 단순하게 가져가지만, DI, OCP를 포기하고, `ItemService` 코드를 직접 변경한다.

결국 여기서 발생하는 트레이드 오프는 구조의 안정성 vs 단순한 구조와 개발의 편리성 사이의 선택이다.

이 둘 중에 하나의 정답만 있을까? 그렇지 않다. 어떤 상황에서는 구조의 안정성이 매우 중요하고, 어떤 상황에서는 단 순한 것이 더 나은 선택일 수 있다.

개발을 할 때는 항상 자원이 무한한 것이 아니다.

그리고 어설픈 추상화는 오히려 독이 되는 경우도 많다. 무엇보다 **추상 화도 비용이 든다.**

인터페이스도 비용이 든다. 여기서 말하는 비용은 유지보수 관점에서 비용을 뜻한다.

이 추상화 비용 을 넘어설 만큼 효과가 있을 때 추상화를 도입하는 것이 실용적이다.

이런 선택에서 하나의 정답이 있는 것은 아니지만, 프로젝트의 현재 상황에 맞는 더 적절한 선택지가 있다고 생각한다.

그리고 현재 상황에 맞는 선택을 하는 개발자가 좋은 개발자라 생각한다.

## 실용적인 구조

`ItemRepositoryV2` 는 스프링 데이터 JPA의 기능을 제공하는 리포지토리이다.

`ItemQueryRepositoryV2` 는 Querydsl을 사용해서 복잡한 쿼리 기능을 제공하는 리포지토리이다.

이렇게 둘을 분리하면 기본 CRUD와 단순 조회는 스프링 데이터 JPA가 담당하고, 복잡한 조회 쿼리는 Querydsl이 담당하게 된다.

물론 `ItemService` 는 기존 `ItemRepository` 를 사용할 수 없기 때문에 코드를 변경해야 한다.

## 다양한 데이터 접근 기술 조합

JPA, 스프링 데이터 JPA, Querydsl은 모두 JPA 기술을 사용하는 것이기 때문에 트랜잭션 매니저로`JpaTransactionManager` 를 선택하면 된다.

해당 기술을 사용하면 스프링 부트는 자동으로 `JpaTransactionManager` 를 스프링 빈에 등록한다.

그런데 `JdbcTemplate` , `MyBatis` 와 같은 기술들은 내부에서 JDBC를 직접 사용하기 때문에 `DataSourceTransactionManager` 를 사용한다.

따라서 JPA와 JdbcTemplate 두 기술을 함께 사용하면 트랜잭션 매니저가 달라진다.

결국 트랜잭션을 하나로 묶을 수 없는 문제가 발생할 수 있다. 그런데 이 부분은 걱정하지 않아도 된다.

**JpaTransactionManager의 다양한 지원**

`JpaTransactionManager` 는 놀랍게도 `DataSourceTransactionManager` 가 제공하는 기능도 대부분 제공한다.

JPA라는 기술도 결국 내부에서는 DataSource와 JDBC 커넥션을 사용하기 때문이다.

따라서 `JdbcTemplate` , `MyBatis` 와 함께 사용할 수 있다.

결과적으로 `JpaTransactionManager` 를 하나만 스프링 빈에 등록하면, JPA, JdbcTemplate, MyBatis 모두를 하나의 트랜잭션으로 묶어서 사용할 수 있다.

물론 함께 롤백도 할 수 있다.

**주의점**

이렇게 JPA와 JdbcTemplate을 함께 사용할 경우 JPA의 플러시 타이밍에 주의해야 한다.

JPA는 데이터를 변경하면 변경 사항을 즉시 데이터베이스에 반영하지 않는다.

기본적으로 트랜잭션이 커밋되는 시점에 변경 사항을 데이터베이스 에 반영한다.

그래서 하나의 트랜잭션 안에서 JPA를 통해 데이터를 변경한 다음에 JdbcTemplate을 호출하는 경우 JdbcTemplate에서는 JPA가 변경한 데이터를 읽기 못하는 문제가 발생한다.

이 문제를 해결하려면 JPA 호출이 끝난 시점에 JPA가 제공하는 플러시라는 기능을 사용해서 JPA의 변경 내역을 데이 터베이스에 반영해주어야 한다.

그래야 그 다음에 호출되는 JdbcTemplate에서 JPA가 반영한 데이터를 사용할 수 있 다.

참고로 방금 설명한 JPA 플러시에 대한 부분은 JPA를 학습해야 이해할 수 있다. 지금은 `JpaTransactionManager` 를 사용해서 여러 데이터 접근 기술들을 함께 사용할 수 있다는 점만 기억하자.



























