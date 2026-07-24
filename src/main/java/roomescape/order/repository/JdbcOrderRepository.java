package roomescape.order.repository;

import java.sql.PreparedStatement;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import roomescape.order.domain.Order;
import roomescape.order.exception.OrderNotFoundException;

@RequiredArgsConstructor
@Repository
public class JdbcOrderRepository implements OrderRepository{

    private static final RowMapper<Order> ORDER_ROW_MAPPER = (resultSet, rowNum) ->
            new Order(
                    resultSet.getLong("id"),
                    resultSet.getString("order_id"),
                    resultSet.getLong("amount"),
                    resultSet.getString("payment_key")
            );

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Order save(Order order) {
        String sql = """
               INSERT INTO orders (order_id, amount, payment_key)
               VALUES (?, ?, ?)
               """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, order.getOrderId());
            ps.setLong(2, order.getAmount());
            ps.setString(3, order.getPaymentKey());
            return ps;
        }, keyHolder);

        Long id = keyHolder.getKey().longValue();
        return order.updateId(id);
    }

    @Override
    public Optional<Order> findById(Long id) {
        String sql = """
               SELECT *
               FROM orders
               WHERE id = ?
               """;

        return jdbcTemplate.query(sql, ORDER_ROW_MAPPER, id)
                .stream().findFirst();
    }

    @Override
    public Optional<Order> findByOrderId(String orderId) {
        String sql = """
               SELECT *
               FROM orders
               WHERE order_id = ?
               """;

        return jdbcTemplate.query(sql, ORDER_ROW_MAPPER, orderId)
                .stream().findFirst();
    }

    @Override
    public Optional<Order> findByOrderIdForUpdate(String orderId) {
        String sql = """
               SELECT *
               FROM orders
               WHERE order_id = ?
               FOR UPDATE
               """;

        return jdbcTemplate.query(sql, ORDER_ROW_MAPPER, orderId)
                .stream().findFirst();
    }

    @Override
    public void update(Order order) {
        String sql = """
               UPDATE orders
               SET order_id = ?, amount = ?, payment_key = ?
               WHERE id = ?
               """;

        int affectedRow = jdbcTemplate.update(sql, order.getOrderId(),
                order.getAmount(), order.getPaymentKey(), order.getId());

        if (affectedRow == 0) {
            throw new OrderNotFoundException();
        }
    }

    @Override
    public int deleteByOrderId(String orderId) {
        String sql = """
               DELETE FROM orders
               WHERE order_id = ?
               """;

        return jdbcTemplate.update(sql, orderId);
    }
}
