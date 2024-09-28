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
