package hello.itemservice.repository.jdbctemplate;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.util.StringUtils;

@Slf4j
public class JdbTemplateItemRepositoryV1 implements ItemRepository {

    private final JdbcTemplate jdbcTemplate;


    public JdbTemplateItemRepositoryV1(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }


    @Override
    public Item save(Item item) {
        String sql = "INSERT INTO item (item_name,  price, quantity) VALUES (?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, new String[] {"id"});
            ps.setString(1, item.getItemName());
            ps.setDouble(2, item.getPrice());
            ps.setInt(3, item.getQuantity());
            return ps;
        }, keyHolder);

        long key = keyHolder.getKey().longValue();

        item.setId(key);

        return item;
    }


    @Override
    public void update(Long itemId, ItemUpdateDto updateParam) {
        String sql = "UPDATE item SET item_name = ?, price = ? , quantity = ? WHERE id = ?";

        jdbcTemplate.update(sql, updateParam.getItemName(), updateParam.getPrice(), updateParam.getQuantity(), itemId);
    }


    @Override
    public Optional<Item> findById(Long id) {

        String sql = "SELECT id, item_name, price, quantity FROM item WHERE id = ?";
        try {
            Item item = jdbcTemplate.queryForObject(sql, itemRowMapper(), id);

            return Optional.of(item);
        } catch(EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }


    @Override
    public List<Item> findAll(ItemSearchCond cond) {

        String itemName = cond.getItemName();
        Integer maxPrice = cond.getMaxPrice();

        String sql = "SELECT id, item_name, price, quantity FROM item";

        if(StringUtils.hasText(itemName) || maxPrice != null) {
            sql += " where";
        }

        boolean andFlag = false;

        List<Object> param = new ArrayList<>();

        if(StringUtils.hasText(itemName)) {
            sql += " item_name like concat('%',?,'%')";
            param.add(itemName);
            andFlag = true;
        }

        if(maxPrice != null) {
            if(andFlag) {
                sql += " and";
            }

            sql += " price <= ?";

            param.add(maxPrice);
        }

        log.info("sql={}", sql);

        return jdbcTemplate.query(sql, itemRowMapper(), param.toArray());
    }


    private RowMapper<Item> itemRowMapper() {
        return ((rs, rowNum) -> {
            Item item = new Item();
            item.setId(rs.getLong("id"));
            item.setItemName(rs.getString("item_name"));
            item.setPrice(rs.getInt("price"));
            item.setQuantity(rs.getInt("quantity"));
            return item;
        });
    }
}
