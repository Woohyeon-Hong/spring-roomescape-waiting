package roomescape.reservation.repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import roomescape.order.domain.Order;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.ReservationStatus;
import roomescape.reservation.exception.ReservationNotFoundException;
import roomescape.reservation.service.dto.PopularThemeResult;
import roomescape.reservation.service.dto.ReservationWithStatusResult;
import roomescape.theme.domain.Theme;
import roomescape.time.domain.ReservationTime;

@RequiredArgsConstructor
@Repository
public class JdbcReservationRepository implements ReservationRepository {

    private static final RowMapper<Reservation> RESERVATION_ROW_MAPPER = (resultSet, rowNum) -> {
        ReservationTime time = new ReservationTime(
                resultSet.getLong("time_id"),
                resultSet.getTime("time_start_at").toLocalTime()
        );

        Theme theme = new Theme(
                resultSet.getLong("theme_id"),
                resultSet.getString("theme_name"),
                resultSet.getString("theme_description"),
                resultSet.getString("theme_thumbnail_url"),
                resultSet.getLong("theme_amount")
        );


        Order order = new Order(
                resultSet.getLong("orders_id"),
                resultSet.getString("orders_order_id"),
                resultSet.getLong("orders_amount"),
                resultSet.getString("orders_payment_key")
                );

        return new Reservation(
                resultSet.getLong("reservation_id"),
                resultSet.getString("reservation_name"),
                resultSet.getDate("reservation_date").toLocalDate(),
                ReservationStatus.valueOf(resultSet.getString("reservation_status")),
                time,
                theme,
                order
        );
    };

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Reservation save(Reservation reservation) {
        String sql = """
                INSERT INTO reservation (name, reservation_date, status, time_id, theme_id, order_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, reservation.getName());
            ps.setDate(2, Date.valueOf(reservation.getDate()));
            ps.setString(3, reservation.getStatus().name());
            ps.setLong(4, reservation.getReservationTime().getId());
            ps.setLong(5, reservation.getTheme().getId());
            ps.setLong(6, reservation.getOrder().getId());

            return ps;
        }, keyHolder);

        long id = keyHolder.getKey().longValue();
        return reservation.updateId(id);
    }

    @Override
    public List<ReservationWithStatusResult> findAllByName(String name) {
        String sql = """
        SELECT r.id AS id,
               r.name AS name,
               r.reservation_date AS reservation_date,
               t.id AS time_id,
               t.start_at AS time_start_at,
               h.id AS theme_id,
               h.name AS theme_name,
               h.description AS theme_description,
               h.thumbnail_url AS theme_url,
               h.amount AS theme_amount,
               r.status AS status,
               o.order_id AS order_id,
               o.amount AS order_amount,
               o.payment_key AS payment_key,
               0 AS waiting_order
        FROM reservation r
        INNER JOIN reservation_time t ON r.time_id = t.id
        INNER JOIN theme h ON r.theme_id = h.id
        INNER JOIN orders o ON r.order_id = o.id
        WHERE r.name = ?

        UNION ALL

        SELECT ranked.id AS id,
               ranked.name AS name,
               ranked.reservation_date AS reservation_date,
               t.id AS time_id,
               t.start_at AS time_start_at,
               h.id AS theme_id,
               h.name AS theme_name,
               h.description AS theme_description,
               h.thumbnail_url AS theme_url,
               h.amount AS theme_amount,
               'waiting' AS status,
               CAST(NULL AS VARCHAR(255)) AS order_id,
               CAST(NULL AS BIGINT) AS order_amount,
               CAST(NULL AS VARCHAR(255)) AS payment_key,
               ranked.waiting_order AS waiting_order
        FROM (
            SELECT rw.id,
                   rw.name,
                   rw.reservation_date,
                   rw.time_id,
                   rw.theme_id,
                   ROW_NUMBER() OVER (
                        PARTITION BY rw.reservation_date, rw.time_id, rw.theme_id
                        ORDER BY rw.id
                   ) AS waiting_order
            FROM reservation_waiting rw
        ) ranked
        INNER JOIN reservation_time t ON ranked.time_id = t.id
        INNER JOIN theme h ON ranked.theme_id = h.id
        WHERE ranked.name = ?

        ORDER BY reservation_date ASC, time_start_at ASC, waiting_order ASC
        """;

        RowMapper<ReservationWithStatusResult> withStatusResultRowMapper = (resultSet, rowNum) -> {
            ReservationTime time = new ReservationTime(
                    resultSet.getLong("time_id"),
                    resultSet.getTime("time_start_at").toLocalTime()
            );

            Theme theme = new Theme(
                    resultSet.getLong("theme_id"),
                    resultSet.getString("theme_name"),
                    resultSet.getString("theme_description"),
                    resultSet.getString("theme_url"),
                    resultSet.getLong("theme_amount")
            );

            return new ReservationWithStatusResult(
                    resultSet.getLong("id"),
                    resultSet.getString("name"),
                    resultSet.getDate("reservation_date").toLocalDate(),
                    time,
                    theme,
                    resultSet.getString("status"),
                    resultSet.getString("order_id"),
                    resultSet.getObject("order_amount", Long.class),
                    resultSet.getString("payment_key"),
                    resultSet.getLong("waiting_order")
            );
        };

        return jdbcTemplate.query(sql, withStatusResultRowMapper, name, name);
    }

    @Override
    public Optional<Reservation> findByIdForUpdate(Long id) {
        String sql = """
        SELECT r.id AS reservation_id,
               r.name AS reservation_name,
               r.reservation_date,
               r.status AS reservation_status,
               r.time_id,
               t.start_at AS time_start_at,
               h.id AS theme_id,
               h.name AS theme_name,
               h.description AS theme_description,
               h.thumbnail_url AS theme_thumbnail_url,
               h.amount AS theme_amount,
               o.id AS orders_id,
               o.order_id AS orders_order_id,
               o.amount AS orders_amount,
               o.payment_key AS orders_payment_key
        FROM reservation r
        INNER JOIN reservation_time t
          ON r.time_id = t.id
        INNER JOIN theme h
          ON r.theme_id = h.id
        LEFT JOIN orders o
          ON r.order_id = o.id
        WHERE r.id = ?
        FOR UPDATE
        """;

        return jdbcTemplate.query(sql, RESERVATION_ROW_MAPPER, id)
                .stream().findFirst();
    }

    @Override
    public Optional<Reservation> findByDateAndTimeIdAndThemeIdForUpdate(LocalDate date, Long timeId, Long themeId) {
        String sql = """
        SELECT r.id AS reservation_id,
               r.name AS reservation_name,
               r.reservation_date,
               r.status AS reservation_status,
               r.time_id,
               t.start_at AS time_start_at,
               h.id AS theme_id,
               h.name AS theme_name,
               h.description AS theme_description,
               h.thumbnail_url AS theme_thumbnail_url,
               h.amount AS theme_amount,
               o.id AS orders_id,
               o.order_id AS orders_order_id,
               o.amount AS orders_amount,
               o.payment_key AS orders_payment_key
        FROM reservation r
        INNER JOIN reservation_time t
          ON r.time_id = t.id
        INNER JOIN theme h
          ON r.theme_id = h.id
        LEFT JOIN orders o
          ON r.order_id = o.id
        WHERE  r.reservation_date = ? AND r.time_id = ? AND r.theme_id = ?
        FOR UPDATE
        """;

        return jdbcTemplate.query(sql, RESERVATION_ROW_MAPPER, date, timeId, themeId)
                .stream().findFirst();
    }

    @Override
    public boolean existByDateAndTimeIdAndThemeId(LocalDate date, Long timeId, Long themeId) {
        String sql = """
        SELECT EXISTS (
            SELECT 1
            FROM reservation
            WHERE reservation_date = ? AND time_id = ? AND theme_id = ?
        )
        """;

        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, date, timeId, themeId);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public List<Reservation> findAll() {
        String sql = """
        SELECT r.id AS reservation_id,
               r.name AS reservation_name,
               r.reservation_date,
               r.status AS reservation_status,
               r.time_id,
               t.start_at AS time_start_at,
               h.id AS theme_id,
               h.name AS theme_name,
               h.description AS theme_description,
               h.thumbnail_url AS theme_thumbnail_url,
               h.amount AS theme_amount,
               o.id AS orders_id,
               o.order_id AS orders_order_id,
               o.amount AS orders_amount,
               o.payment_key AS orders_payment_key
        FROM reservation r
        INNER JOIN reservation_time t
          ON r.time_id = t.id
        INNER JOIN theme h
          ON r.theme_id = h.id
        LEFT JOIN orders o
          ON r.order_id = o.id
        ORDER BY reservation_date ASC, time_start_at ASC, reservation_id ASC
        """;

        return jdbcTemplate.query(sql, RESERVATION_ROW_MAPPER);
    }

    @Override
    public List<PopularThemeResult> findPopularThemes(LocalDate from, LocalDate to, int limit) {
        String sql = """
        SELECT t.id,
               t.name,
               t.description,
               t.thumbnail_url,
               t.amount
        FROM reservation r
        INNER JOIN theme t
          ON r.theme_id = t.id
        WHERE r.reservation_date >= ?
          AND r.reservation_date <= ?
          AND r.status = 'CONFIRMED'
        GROUP BY t.id,
                 t.name,
                 t.description,
                 t.thumbnail_url,
                 t.amount
        ORDER BY COUNT(r.id) DESC,
                 t.id ASC
        LIMIT ?
        """;

        return jdbcTemplate.query(
                sql,
                (resultSet, rowNum) -> new PopularThemeResult(
                        resultSet.getLong("id"),
                        resultSet.getString("name"),
                        resultSet.getString("description"),
                        resultSet.getString("thumbnail_url"),
                        resultSet.getLong("amount")
                ),
                Date.valueOf(from),
                Date.valueOf(to),
                limit
        );
    }

    @Override
    public boolean existByDateAndTimeIdAndThemeIdExceptId(LocalDate date, Long timeId, Long themeId, Long id) {
        String sql = """
        SELECT EXISTS (
            SELECT 1
            FROM reservation
            WHERE reservation_date = ?  AND time_id = ? AND theme_id = ? AND id != ?
        )
        """;

        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, date, timeId, themeId, id);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public Optional<Reservation> findByOrderId(String orderId) {
        String sql = """
        SELECT r.id AS reservation_id,
               r.name AS reservation_name,
               r.reservation_date,
               r.status AS reservation_status,
               r.time_id,
               t.start_at AS time_start_at,
               h.id AS theme_id,
               h.name AS theme_name,
               h.description AS theme_description,
               h.thumbnail_url AS theme_thumbnail_url,
               h.amount AS theme_amount,
               o.id AS orders_id,
               o.order_id AS orders_order_id,
               o.amount AS orders_amount,
               o.payment_key AS orders_payment_key
        FROM reservation r
        INNER JOIN reservation_time t
          ON r.time_id = t.id
        INNER JOIN theme h
          ON r.theme_id = h.id
        INNER JOIN orders o
          ON r.order_id = o.id
        WHERE o.order_id = ?
        """;

        return jdbcTemplate.query(sql, RESERVATION_ROW_MAPPER, orderId)
                .stream().findFirst();
    }

    @Override
    public void update(Reservation reservation) {
        String sql = """
                UPDATE reservation
                SET name = ?, reservation_date = ?, status = ?, time_id = ?, theme_id = ?
                WHERE id = ?
                """;

        int affectedRow = jdbcTemplate.update(
                sql,
                reservation.getName(),
                reservation.getDate(),
                reservation.getStatus().name(),
                reservation.getReservationTime().getId(),
                reservation.getTheme().getId(),
                reservation.getId()
        );

        if (affectedRow == 0) {
            throw new ReservationNotFoundException();
        }
    }

    @Override
    public int deleteById(Long id) {
        String sql = """
               DELETE FROM reservation
               WHERE id = ?
               """;

        return jdbcTemplate.update(sql, id);
    }

    @Override
    public int deleteByOrderId(String orderId) {
        String sql = """
        DELETE FROM reservation
        WHERE order_id IN (
          SELECT id FROM orders WHERE order_id = ?
        )
        """;

        return jdbcTemplate.update(sql, orderId);
    }
}
