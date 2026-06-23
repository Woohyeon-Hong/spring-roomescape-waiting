package roomescape.gatling;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Time;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import roomescape.gatling.seed.ReservationSeed;
import roomescape.gatling.seed.WaitingSeed;

@Repository
public class GatlingSeedRepository {

    private static final LocalTime MIDNIGHT = LocalTime.of(0, 0);

    private final JdbcTemplate jdbcTemplate;

    public GatlingSeedRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void reset() {
        jdbcTemplate.update("DELETE FROM reservation_waiting");
        jdbcTemplate.update("DELETE FROM reservation");
        jdbcTemplate.update("DELETE FROM reservation_time");
        jdbcTemplate.update("DELETE FROM theme");
        jdbcTemplate.update("ALTER TABLE reservation_waiting ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.update("ALTER TABLE reservation ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.update("ALTER TABLE reservation_time ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.update("ALTER TABLE theme ALTER COLUMN id RESTART WITH 1");
    }

    public void insertTimes(int timeCount) {
        List<LocalTime> times = IntStream.range(0, timeCount)
                .mapToObj(MIDNIGHT::plusHours)
                .toList();

        jdbcTemplate.batchUpdate("INSERT INTO reservation_time (start_at) VALUES (?)", new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int index) throws java.sql.SQLException {
                preparedStatement.setTime(1, Time.valueOf(times.get(index)));
            }

            @Override
            public int getBatchSize() {
                return times.size();
            }
        });
    }

    public void insertThemes(int themeCount) {
        List<ThemeSeed> themes = IntStream.rangeClosed(1, themeCount)
                .mapToObj(index -> new ThemeSeed(
                        "gatling-theme-%02d".formatted(index),
                        "Gatling test theme %02d".formatted(index),
                        "https://example.com/gatling/theme-%02d.jpg".formatted(index)
                ))
                .toList();

        jdbcTemplate.batchUpdate("INSERT INTO theme (name, description, thumbnail_url) VALUES (?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement preparedStatement, int index) throws java.sql.SQLException {
                        ThemeSeed theme = themes.get(index);
                        preparedStatement.setString(1, theme.name());
                        preparedStatement.setString(2, theme.description());
                        preparedStatement.setString(3, theme.thumbnailUrl());
                    }

                    @Override
                    public int getBatchSize() {
                        return themes.size();
                    }
                });
    }

    public void insertReservations(List<ReservationSeed> reservations) {
        jdbcTemplate.batchUpdate("INSERT INTO reservation (name, reservation_date, time_id, theme_id) VALUES (?, ?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement preparedStatement, int index) throws java.sql.SQLException {
                        ReservationSeed reservation = reservations.get(index);
                        preparedStatement.setString(1, reservation.name());
                        preparedStatement.setDate(2, Date.valueOf(reservation.date()));
                        preparedStatement.setLong(3, reservation.timeId());
                        preparedStatement.setLong(4, reservation.themeId());
                    }

                    @Override
                    public int getBatchSize() {
                        return reservations.size();
                    }
                });
    }

    public void insertWaitings(List<WaitingSeed> waitings) {
        jdbcTemplate.batchUpdate("INSERT INTO reservation_waiting (name, reservation_date, time_id, theme_id) VALUES (?, ?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement preparedStatement, int index) throws java.sql.SQLException {
                        WaitingSeed waiting = waitings.get(index);
                        preparedStatement.setString(1, waiting.name());
                        preparedStatement.setDate(2, Date.valueOf(waiting.date()));
                        preparedStatement.setLong(3, waiting.timeId());
                        preparedStatement.setLong(4, waiting.themeId());
                    }

                    @Override
                    public int getBatchSize() {
                        return waitings.size();
                    }
                });
    }

    private record ThemeSeed(String name, String description, String thumbnailUrl) {
    }
}
