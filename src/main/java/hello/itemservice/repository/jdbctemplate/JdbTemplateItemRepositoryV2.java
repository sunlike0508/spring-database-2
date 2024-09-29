package hello.itemservice.repository.jdbctemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.util.StringUtils;

/**
 * NamedParameterJdbcTemplate
 * SqlParameterSource
 * - BeanPropertySqlParameterSource
 * - MapSqlParameterSource
 * Map
 * BeanPropertyRowMapper
 */
@Slf4j
public class JdbTemplateItemRepositoryV2 implements ItemRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;


    public JdbTemplateItemRepositoryV2(DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }


    @Override
    public Item save(Item item) {
        String sql = "INSERT INTO item (item_name,  price, quantity) VALUES (:itemName, :price, :quantity)";

        SqlParameterSource param = new BeanPropertySqlParameterSource(item);

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(sql, param, keyHolder);

        long key = keyHolder.getKey().longValue();
        item.setId(key);
        return item;
    }


    @Override
    public void update(Long itemId, ItemUpdateDto updateParam) {
        String sql = "UPDATE item SET item_name = :itemName, price = :price , quantity = :quantity WHERE id = :id";

        SqlParameterSource param = new MapSqlParameterSource().addValue("itemName", updateParam.getItemName())
                .addValue("price", updateParam.getPrice()).addValue("quantity", updateParam.getQuantity())
                .addValue("id", itemId);

        jdbcTemplate.update(sql, param);
    }


    @Override
    public Optional<Item> findById(Long id) {

        String sql = "SELECT id, item_name, price, quantity FROM item WHERE id = :id";
        try {

            Map<String, Object> param = Map.of("id", id);


            Item item = jdbcTemplate.queryForObject(sql, param, itemRowMapper());

            return Optional.of(item);
        } catch(EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }


    @Override
    public List<Item> findAll(ItemSearchCond cond) {

        String itemName = cond.getItemName();
        Integer maxPrice = cond.getMaxPrice();

        SqlParameterSource param = new BeanPropertySqlParameterSource(cond);

        String sql = "SELECT id, item_name, price, quantity FROM item";

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

        return jdbcTemplate.query(sql, param, itemRowMapper());
    }


    private RowMapper<Item> itemRowMapper() {
        return BeanPropertyRowMapper.newInstance(Item.class);
    }
}
