package roomescape.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.repository.ReservationRepository;
import roomescape.reservationWaiting.repository.ReservationWaitingRepository;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(ServiceIntegrationTest.ServiceIntegrationTestConfig.class)
public abstract class ServiceIntegrationTest {

    @Autowired
    DatabaseHelper databaseHelper;

    @Autowired
    ReservationRepository reservationRepository;

    @Autowired
    ReservationWaitingRepository reservationWaitingRepository;

    @BeforeEach
    void setup() {
        databaseHelper.clear();
    }

    protected void assertReservationName(LocalDate date, Long timeId, Long themeId, String expectedName) {
        Optional<Reservation> found = reservationRepository.findByDateAndTimeIdAndThemeIdForUpdate(
                date, timeId, themeId);

        assertThat(found).isPresent();
        Reservation reservation = found.get();

        assertThat(reservation.getName()).isEqualTo(expectedName);
    }

    protected void assertWaitingExists(Long id) {
        assertThat(reservationWaitingRepository.findById(id)).isPresent();
    }

    @TestConfiguration
    static class ServiceIntegrationTestConfig {

        @Bean
        public Clock clock() {
            return Clock.fixed(
                    Instant.parse("2026-05-01T09:00:00+09:00"),
                    ZoneId.of("Asia/Seoul")
            );
        }

        @Bean
        public DatabaseHelper databaseHelper(JdbcTemplate jdbcTemplate) {
            return new DatabaseHelper(jdbcTemplate);
        }
    }
}
