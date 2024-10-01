package hello.itemservice.service;

import java.util.List;
import java.util.Optional;
import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import hello.itemservice.repository.v2.ItemQueryRepositoryV2;
import hello.itemservice.repository.v2.ItemRepositoryV2;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ItemServiceV2 implements ItemService {

    private final ItemRepositoryV2 itemRepositoryV2;
    private final ItemQueryRepositoryV2 itemQueryRepositoryV2;


    @Override
    public Item save(Item item) {
        return itemRepositoryV2.save(item);
    }


    @Override
    public void update(Long itemId, ItemUpdateDto updateParam) {
        itemRepositoryV2.findById(itemId).ifPresent(item -> {
            item.setItemName(updateParam.getItemName());
            item.setPrice(updateParam.getPrice());
            item.setQuantity(updateParam.getQuantity());
        });
    }


    @Override
    public Optional<Item> findById(Long id) {
        return itemRepositoryV2.findById(id);
    }


    @Override
    public List<Item> findItems(ItemSearchCond cond) {
        return itemQueryRepositoryV2.findAll(cond);
    }
}
