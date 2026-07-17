package roomescape.reservationWaiting.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import roomescape.reservationWaiting.domain.ReservationWaiting;
import roomescape.theme.domain.Theme;
import roomescape.time.domain.ReservationTime;

@JdbcTest
class JdbcReservationWaitingRepositoryTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    ReservationWaitingRepository reservationWaitingRepository;

    @Autowired
    public JdbcReservationWaitingRepositoryTest(JdbcTemplate jdbcTemplate) {
        this.reservationWaitingRepository = new JdbcReservationWaitingRepository(jdbcTemplate);
    }

    @Test
    @DisplayName("예약 대기를 저장하고 반환된 객체의 ID를 확인한다.")
    void saveTest() {
        // given
        ReservationTime time = createTime(LocalTime.of(10, 0));
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com");

        // when
        ReservationWaiting saved1 = reservationWaitingRepository.save(
                ReservationWaiting.of(
                        "brown",
                        LocalDate.of(2024, 5, 1),
                        time,
                        theme
                )
        );

        ReservationWaiting saved2 = reservationWaitingRepository.save(
                ReservationWaiting.of(
                        "pobi",
                        LocalDate.of(2024, 5, 1),
                        time,
                        theme
                )
        );

        // then
        assertAll(
                () -> assertThat(saved1.getId()).isNotNull(),
                () -> assertThat(saved2.getId()).isNotNull()
        );
    }

    private ReservationTime createTime(LocalTime time) {
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

    private Theme createTheme(String name, String description, String thumbnailUrl) {
        jdbcTemplate.update(
                "INSERT INTO theme (name, description, thumbnail_url) VALUES (?, ?, ?)",
                name, description, thumbnailUrl
        );

        Long themeId = jdbcTemplate.queryForObject(
                "SELECT id FROM theme WHERE name = ?",
                Long.class,
                name
        );

        return new Theme(themeId, name, description, thumbnailUrl);
    }

    @Test
    @DisplayName("기존에 이미 동일한 예약 대기가 있으면 예외가 발생한다.")
    void saveTest_duplicate() {
        // given
        ReservationTime time = createTime(LocalTime.of(10, 0));
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com");

        reservationWaitingRepository.save(
                ReservationWaiting.of(
                        "brown",
                        LocalDate.of(2024, 5, 1),
                        time,
                        theme
                )
        );

        // when & then
        assertThatThrownBy(() -> reservationWaitingRepository.save(
                ReservationWaiting.of(
                        "brown",
                        LocalDate.of(2024, 5, 1),
                        time,
                        theme
                )
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("아이디를 기반으로 예약 대기를 조회한다.")
    void findByIdTest() {
        // given
        ReservationTime time = createTime(LocalTime.of(10, 0));
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com");
        ReservationWaiting saved = saveReservationWaiting("brown", LocalDate.of(2024, 5, 1), time, theme);

        // when & then
        assertThat(reservationWaitingRepository.findById(saved.getId())
        ).isPresent();
    }

    private ReservationWaiting saveReservationWaiting(String name, LocalDate date, ReservationTime time, Theme theme) {
        return reservationWaitingRepository.save(
                ReservationWaiting.of(name, date, time, theme)
        );
    }

    @Test
    @DisplayName("날짜, 시간, 테마를 기반으로 첫번 째 예약 대기를 조회한다.")
    void findFirstByReservationDateAndTimeIdAndThemeIdTest() {
        // given
        ReservationTime time = createTime(LocalTime.of(10, 0));
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com");
        saveReservationWaiting("brown", LocalDate.of(2024, 5, 1), time, theme);
        saveReservationWaiting("pobi", LocalDate.of(2024, 5, 1), time, theme);

        // when
        Optional<ReservationWaiting> result = reservationWaitingRepository.findFirstByReservationDateAndTimeIdAndThemeId(
                LocalDate.of(2024, 5, 1), time.getId(), theme.getId()
        );

        // then
        assertAll(
                () -> assertThat(result).isPresent(),
                () -> assertThat(result.get().getName()).isEqualTo("brown")
        );
    }

    @DisplayName("날짜, 시간, 테마, 예약자 이름에 해당하는 예약 대기가 존재하는지 조회한다.")
    @Test
    void existByDateAndTimeIdAndThemeIdAndNameTest() {
        //given
        ReservationTime time = createTime(LocalTime.of(10, 0));
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com");
        ReservationWaiting saved = saveReservationWaiting("brown", LocalDate.of(2024, 5, 1), time, theme);

        //when & then
        assertAll(
                () -> assertThat(reservationWaitingRepository.existByDateAndTimeIdAndThemeIdAndName(
                        saved.getDate(),
                        saved.getTime().getId(),
                        saved.getTheme().getId(),
                        "brown"
                )).isTrue(),
                () -> assertThat(reservationWaitingRepository.existByDateAndTimeIdAndThemeIdAndName(
                        saved.getDate(),
                        saved.getTime().getId(),
                        saved.getTheme().getId(),
                        "pobi"
                )).isFalse()
        );
    }

    @DisplayName("날짜, 시간, 테마에 해당하는 예약 대기가 존재하는지 조회한다.")
    @Test
    void existsByDateAndTimeIdAndThemeIdTest() {
        //given
        ReservationTime time = createTime(LocalTime.of(10, 0));
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com");
        ReservationWaiting saved = saveReservationWaiting("brown", LocalDate.of(2024, 5, 1), time, theme);

        //when & then
        assertAll(
                () -> assertThat(reservationWaitingRepository.existsByDateAndTimeIdAndThemeId(
                        saved.getDate(),
                        saved.getTime().getId(),
                        saved.getTheme().getId()
                )).isTrue(),
                () -> assertThat(reservationWaitingRepository.existsByDateAndTimeIdAndThemeId(
                        LocalDate.of(2026, 5, 20),
                        saved.getTime().getId(),
                        saved.getTheme().getId()
                )).isFalse()
        );
    }

    @DisplayName("승격 가능한 예약 대기를 조회하면, 슬롯이 비어있고 대기열 1순위인 대기만 반환된다.")
    @Test
    void findPromotableByNameTest_returns_first_when_slot_free() {
        // given
        ReservationTime time = createTime(LocalTime.of(10, 0));
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com");
        ReservationWaiting saved = saveReservationWaiting("brown", LocalDate.of(2024, 5, 1), time, theme);

        // when & then
        assertThat(reservationWaitingRepository.findPromotableByName("brown"))
                .extracting(ReservationWaiting::getId)
                .containsExactly(saved.getId());
    }

    @DisplayName("슬롯에 예약이 남아있으면, 대기열 1순위여도 승격 가능한 예약 대기 목록에서 제외된다.")
    @Test
    void findPromotableByNameTest_excludes_when_slot_occupied() {
        // given
        ReservationTime time = createTime(LocalTime.of(10, 0));
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com");
        LocalDate date = LocalDate.of(2024, 5, 1);
        saveReservation("someone", date, time, theme);
        saveReservationWaiting("pobi", date, time, theme);

        // when & then
        assertThat(reservationWaitingRepository.findPromotableByName("pobi")).isEmpty();
    }

    @DisplayName("대기열 1순위가 아니면, 슬롯이 비어있어도 승격 가능한 예약 대기 목록에서 제외된다.")
    @Test
    void findPromotableByNameTest_excludes_when_not_first_in_queue() {
        // given
        ReservationTime time = createTime(LocalTime.of(10, 0));
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com");
        LocalDate date = LocalDate.of(2024, 5, 1);
        ReservationWaiting first = saveReservationWaiting("brown", date, time, theme);
        saveReservationWaiting("pobi", date, time, theme);

        // when & then
        assertAll(
                () -> assertThat(reservationWaitingRepository.findPromotableByName("pobi")).isEmpty(),
                () -> assertThat(reservationWaitingRepository.findPromotableByName("brown"))
                        .extracting(ReservationWaiting::getId)
                        .containsExactly(first.getId())
        );
    }

    private void saveReservation(String name, LocalDate date, ReservationTime time, Theme theme) {
        jdbcTemplate.update(
                "INSERT INTO reservation (name, reservation_date, time_id, theme_id) VALUES (?, ?, ?, ?)",
                name, Date.valueOf(date), time.getId(), theme.getId()
        );
    }

    @Test
    @DisplayName("아이디를 기반으로 예약 대기를 삭제한다.")
    void deleteByIdTest() {
        // given
        ReservationTime time = createTime(LocalTime.of(10, 0));
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com");
        ReservationWaiting saved = saveReservationWaiting("brown", LocalDate.of(2024, 5, 1), time, theme);

        // when
        int deletedCount = reservationWaitingRepository.deleteById(saved.getId());

        // then
        assertThat(deletedCount).isEqualTo(1);
    }
}
