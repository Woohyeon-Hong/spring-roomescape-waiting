package roomescape.gatling.seed;

import java.time.LocalDate;

public record WaitingSeed(String name, LocalDate date, long timeId, long themeId) {
}
