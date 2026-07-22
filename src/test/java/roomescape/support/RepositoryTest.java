package roomescape.support;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import roomescape.order.domain.Order;
import roomescape.reservationWaiting.domain.ReservationWaiting;
import roomescape.theme.domain.Theme;
import roomescape.time.domain.ReservationTime;

@JdbcTest
public abstract class RepositoryTest {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected ReservationTime createTime(LocalTime time) {
        jdbcTemplate.update(
                "INSERT INTO reservation_time (start_at) VALUES (?)",
                Time.valueOf(time)
        );

        long timeId = jdbcTemplate.queryForObject(
                "SELECT id FROM reservation_time WHERE start_at = ?",
                Long.class,
                Time.valueOf(time)
        );

        return new ReservationTime(timeId, time);
    }

    protected Theme createTheme(String name, String description, String thumbnailUrl, Long amount) {
        jdbcTemplate.update("""
            insert into theme(name, description, thumbnail_url, amount)
            values (?, ?, ?, ?)
        """, name, description, thumbnailUrl, amount
        );

        Long themeId = jdbcTemplate.queryForObject(
                "SELECT id FROM theme WHERE name = ?",
                Long.class,
                name
        );

        return new Theme(themeId, name, description, thumbnailUrl, amount);
    }

    protected Order createOrder(Long amount) {
        String orderId = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO orders (order_id, amount) VALUES (?, ?)",
                orderId, amount
        );

        Long id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM orders", Long.class);

        return new Order(id, orderId, amount, null);
    }

    protected void createReservation(String name, ReservationTime time, LocalDate date, Theme theme, Order order) {
        jdbcTemplate.update("""
            insert into reservation(name, reservation_date, time_id, theme_id, order_id)
            values (?, ?, ?, ?, ?)
        """, name, date, time.getId(), theme.getId(), order.getId()
        );
    }

    protected ReservationWaiting createReservationWaiting(String name, ReservationTime time, LocalDate date, Theme theme) {
        jdbcTemplate.update("""
            insert into reservation_waiting(name, reservation_date, time_id, theme_id)
            values (?, ?, ?, ?)
        """, name, date, time.getId(), theme.getId()
        );

        Long id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM reservation_waiting", Long.class);

        return new ReservationWaiting(id, name, date, time, theme);
    }
}
