package roomescape.e2e;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import roomescape.e2e.E2ETest.WebConfig;
import roomescape.payment.PaymentGateway;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.exception.ReservationNotFoundException;
import roomescape.reservation.repository.ReservationRepository;
import roomescape.support.DatabaseHelper;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(WebConfig.class)
public abstract class E2ETest {

    @Autowired
    DatabaseHelper databaseHelper;

    @Autowired
    MutableClock clock;

    @Autowired
    ReservationRepository reservationRepository;

    @MockitoBean
    PaymentGateway paymentGateway;

    @LocalServerPort
    int port;

    @BeforeEach
    void setup() {
        databaseHelper.clear();
        clock.setInstant(Instant.parse("2026-05-01T09:00:00+09:00"));
        RestAssured.port = port;
    }

    protected void createReservationTime(String startAt) {
        Map<String, Object> requestBody = Map.of("startAt", startAt);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when().post("/admin/times")
                .then().statusCode(201);
    }

    protected void createTheme(String name, String description, String thumbnailUrl, Long amount) {
        Map<String, Object> requestBody = Map.of(
                "name", name,
                "description", description,
                "thumbnailUrl", thumbnailUrl,
                "amount", amount
        );

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when().post("/admin/themes")
                .then().statusCode(201);
    }

    protected String createReservation(String name, LocalDate date, Long timeId, Long themeId) {
        Map<String, Object> reservation = Map.of(
                "name", name,
                "date", date.toString(),
                "timeId", timeId,
                "themeId", themeId
        );

        long id = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(reservation)
                .when().post("/reservations")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        return reservationRepository.findByIdForUpdate(id)
                .orElseThrow(ReservationNotFoundException::new)
                .getOrder().getOrderId();
    }

    protected void confirm(String orderId) {
        Reservation reservation = reservationRepository
                .findByOrderId(orderId)
                .orElseThrow();

        Map<String, Object> confirmRequest = Map.of(
                "paymentKey", "test-payment-key",
                "amount", reservation.getOrder().getAmount()
        );

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(confirmRequest)
                .when().post("/orders/{orderId}/confirm", orderId)
                .then().statusCode(204);
    }

    protected void createConfirmedReservation(String name, LocalDate date, Long timeId, Long themeId) {
        confirm(createReservation(name, date, timeId, themeId));
    }

    protected void createReservationWaiting(String name, LocalDate date, long timeId, long themeId) {
        Map<String, Object> requestBody = Map.of(
                "name", name,
                "date", date.toString(),
                "timeId", timeId,
                "themeId", themeId
        );

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when().post("/reservation-waitings")
                .then().statusCode(201);
    }

    @TestConfiguration
    static class WebConfig {

        @Bean
        public MutableClock clock() {
            return new MutableClock(
                    Instant.parse("2026-05-01T09:00:00+09:00"),
                    ZoneId.of("Asia/Seoul")
            );
        }

        @Bean
        public DatabaseHelper databaseHelper(JdbcTemplate jdbcTemplate) {
            return new DatabaseHelper(jdbcTemplate);
        }
    }

    static class MutableClock extends Clock {

        private final ZoneId zone;
        private Instant instant;

        public MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        public void setInstant(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
