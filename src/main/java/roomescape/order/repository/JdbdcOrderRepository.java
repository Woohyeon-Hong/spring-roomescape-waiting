package roomescape.order.repository;

import java.sql.PreparedStatement;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import roomescape.order.domain.Order;
import roomescape.order.exception.OrderNotFoundException;

@Repository
public class JdbdcOrderRepository implements OrderRepository{

    private static final RowMapper<Order> ORDER_ROW_MAPPER = (resultSet, rowNum) ->
            new Order(
                    resultSet.getLong("id"),
                    resultSet.getString("order_id"),
                    resultSet.getLong("amount"),
                    resultSet.getBoolean("is_confirmed")
            );

    private final JdbcTemplate jdbcTemplate;

    public JdbdcOrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Order save(Order order) {
        String sql = """
               INSERT INTO orders (order_id, amount, is_confirmed)
               VALUES (?, ?, ?)
               """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, order.getOrderId());
            ps.setLong(2, order.getAmount());
            ps.setBoolean(3, order.isConfirmed());
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
    public void confirmByOrderId(String orderId, String paymentKey) {
        String sql = """
               UPDATE orders
               SET is_confirmed = true, payment_key = ?
               WHERE order_id = ?
               """;

        int affectedRow = jdbcTemplate.update(sql, paymentKey, orderId);

        if (affectedRow == 0) {
            throw new OrderNotFoundException();
        }
    }
}
