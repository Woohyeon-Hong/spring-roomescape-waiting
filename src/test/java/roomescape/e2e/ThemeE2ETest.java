package roomescape.e2e;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ThemeE2ETest extends E2ETest {

    @DisplayName("테마를 생성, 조회, 삭제한다.")
    @Test
    void manageTheme() {
        Map<String, Object> requestBody = Map.of(
                "name", "테마",
                "description", "설명",
                "thumbnailUrl", "url",
                "amount", 1000L
        );

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when().post("/admin/themes")
                .then().log().all()
                .statusCode(201);

        RestAssured.given().log().all()
                .when().get("/themes")
                .then().log().all()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].name", is("테마"))
                .body("[0].description", is("설명"))
                .body("[0].thumbnailUrl", is("url"))
                .body("[0].amount", is(1000));

        RestAssured.given().log().all()
                .when().delete("/admin/themes/1")
                .then().log().all()
                .statusCode(204);

        RestAssured.given().log().all()
                .when().get("/themes")
                .then().log().all()
                .statusCode(200)
                .body("size()", is(0));
    }

    @DisplayName("5월 1일 기준, 직전 period 일 동안의 예약 수를 기준으로 상위 limit 개의 테마들을 조회한다.")
    @Test
    void readPopular() {
        // given
        clock.setInstant(Instant.parse("2026-04-22T09:00:00+09:00"));

        createReservationTime("10:00");

        createTheme("우아한 테마", "우아한테크코스 전용 테마입니다.", "https://example.com/woowa.png", 1000L);
        createTheme("페어 테마", "페어 전용 테마입니다.", "https://example.com/pair.png", 1000L);
        createTheme("당근 테마", "당근 전용 테마입니다.", "https://example.com/carrot.png", 1000L);

        createConfirmedReservation("brown", LocalDate.of(2026, 4, 29), 1L, 1L);
        createConfirmedReservation("pobi", LocalDate.of(2026, 4, 30), 1L, 1L);
        createConfirmedReservation("eden", LocalDate.of(2026, 4, 30), 1L, 2L);
        createConfirmedReservation("boundaryReservation", LocalDate.of(2026, 4, 24), 1L, 2L);
        createConfirmedReservation("todayReservation", LocalDate.of(2026, 5, 1), 1L, 3L);
        createConfirmedReservation("outOfRangeReservation", LocalDate.of(2026, 4, 23), 1L, 3L);

        clock.setInstant(Instant.parse("2026-05-01T09:00:00+09:00"));

        // when & then
        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .when().get("/themes?popular=true&period=7&limit=2")
                .then().log().all()
                .statusCode(200)
                .body("size()", is(2))
                .body("[0].name", is("우아한 테마"))
                .body("[1].name", is("페어 테마"))
                .body("name", not(hasItem("당근 테마")));

        assertThat(LocalDate.of(2026, 5, 1))
                .isEqualTo(LocalDate.now(clock));
    }
}
