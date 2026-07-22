package roomescape.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.time.domain.ReservationTime;

public class ReservationTimeE2ETest extends E2ETest {

    @DisplayName("예약 시간을 생성, 조회, 삭제한다.")
    @Test
    void manageReservationTime() {
        Map<String, String> requestBody = Map.of("startAt", "10:00");

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when().post("/admin/times")
                .then().log().all()
                .statusCode(201);

        RestAssured.given().log().all()
                .when().get("/times")
                .then().log().all()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].startAt", is("10:00:00"));

        RestAssured.given().log().all()
                .when().delete("/admin/times/1")
                .then().log().all()
                .statusCode(204);

        RestAssured.given().log().all()
                .when().get("/times")
                .then().log().all()
                .statusCode(200)
                .body("size()", is(0));
    }

    @DisplayName("예약 가능한 시간 목록을 조회한다.")
    @Test
    void retrieveAvailableTimes() {
        createReservationTime("10:00");
        createReservationTime("11:00");
        createReservationTime("12:00");
        createReservationTime("13:00");

        createTheme("우아한 테마", "우아한테크코스 전용 테마입니다.", "https://example.com/image.png", 1000L);
        createTheme("페어 테마", "페어 전용 테마입니다.", "https://example.com/pair.png", 1000L);

        List<ReservationTime> beforeReservationResults = getAvailableTimes(LocalDate.of(2026, 5, 5), 1L);

        assertAll(
                () -> assertThat(beforeReservationResults).hasSize(4),
                () -> assertThat(beforeReservationResults.stream().map(ReservationTime::getId).toList())
                        .containsExactly(1L, 2L, 3L, 4L),
                () -> assertThat(beforeReservationResults.stream().map(ReservationTime::getStartAt).toList())
                        .containsExactly(
                                LocalTime.of(10, 0),
                                LocalTime.of(11, 0),
                                LocalTime.of(12, 0),
                                LocalTime.of(13, 0)
                        )
        );

        createConfirmedReservation("brown", LocalDate.of(2026, 5, 5), 1L, 1L);
        createConfirmedReservation("pobi", LocalDate.of(2026, 5, 6), 2L, 2L);

        assertAll(
                () -> assertThat(getAvailableTimes(LocalDate.of(2026, 5, 5), 1L)).hasSize(3),
                () -> assertThat(getAvailableTimes(LocalDate.of(2026, 5, 6), 1L)).hasSize(4),
                () -> assertThat(getAvailableTimes(LocalDate.of(2026, 5, 5), 2L)).hasSize(4),
                () -> assertThat(getAvailableTimes(LocalDate.of(2026, 5, 6), 2L)).hasSize(3)
        );
    }

    private List<ReservationTime> getAvailableTimes(LocalDate date, Long themeId) {
        return RestAssured.given()
                .queryParam("date", date.toString())
                .queryParam("themeId", themeId)
                .when().get("/times/available-times")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList(".", ReservationTime.class);
    }
}
