package hello.itemservice.config;

import javax.sql.DataSource;
import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.jdbctemplate.JdbTemplateItemRepositoryV1;
import hello.itemservice.service.ItemService;
import hello.itemservice.service.ItemServiceV1;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class JdbcTemplateV1Config {

    private final DataSource dataSource;


    @Bean
    public ItemService itemService() {
        return new ItemServiceV1(itemRepository());
    }


    @Bean
    public ItemRepository itemRepository() {
        return new JdbTemplateItemRepositoryV1(dataSource);
    }

}
