package roomescape.gatling.seed;

import java.time.LocalDate;

public record ReservationSeed(String name, LocalDate date, long timeId, long themeId) {
}
