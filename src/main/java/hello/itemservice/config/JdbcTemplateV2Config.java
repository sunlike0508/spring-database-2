package hello.itemservice.config;

import javax.sql.DataSource;
import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.jdbctemplate.JdbTemplateItemRepositoryV2;
import hello.itemservice.service.ItemService;
import hello.itemservice.service.ItemServiceV1;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class JdbcTemplateV2Config {

    private final DataSource dataSource;


    @Bean
    public ItemService itemService() {
        return new ItemServiceV1(itemRepository());
    }


    @Bean
    public ItemRepository itemRepository() {
        return new JdbTemplateItemRepositoryV2(dataSource);
    }

}
