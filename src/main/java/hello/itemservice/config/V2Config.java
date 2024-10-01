package hello.itemservice.config;

import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.jpa.JpaItemRepositoryV3;
import hello.itemservice.repository.v2.ItemQueryRepositoryV2;
import hello.itemservice.repository.v2.ItemRepositoryV2;
import hello.itemservice.service.ItemServiceV2;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class V2Config {

    private final EntityManager entityManager;
    private final ItemRepositoryV2 itemRepositoryV2;


    @Bean
    public ItemServiceV2 itemService() {
        return new ItemServiceV2(itemRepositoryV2, itemQueryRepositoryV2());
    }


    @Bean
    public ItemQueryRepositoryV2 itemQueryRepositoryV2() {
        return new ItemQueryRepositoryV2(entityManager);
    }

    
    @Bean
    public ItemRepository itemRepository() {
        return new JpaItemRepositoryV3(entityManager);
    }
}
