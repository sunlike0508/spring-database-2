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
















