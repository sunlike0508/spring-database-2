package hello.itemservice.repository.v2;


import java.util.List;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import hello.itemservice.domain.Item;
import static hello.itemservice.domain.QItem.item;
import hello.itemservice.repository.ItemSearchCond;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class ItemQueryRepositoryV2 {

    private final JPAQueryFactory queryFactory;


    public ItemQueryRepositoryV2(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }


    public List<Item> findAll(ItemSearchCond cond) {
        return queryFactory.select(item).from(item)
                .where(likeItemName(cond.getItemName()), maxPrice(cond.getMaxPrice())).fetch();
    }


    private BooleanExpression likeItemName(String itemName) {

        if(StringUtils.hasText(itemName)) {
            return item.itemName.like('%' + itemName + '%');
        } else {
            return null;
        }
    }


    private BooleanExpression maxPrice(Integer maxPrice) {

        if(maxPrice != null) {
            return item.price.loe(maxPrice);
        } else {
            return null;
        }
    }
}
