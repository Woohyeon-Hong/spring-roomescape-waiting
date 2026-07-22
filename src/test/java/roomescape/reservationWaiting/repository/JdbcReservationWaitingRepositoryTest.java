package roomescape.reservationWaiting.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import roomescape.order.domain.Order;
import roomescape.reservationWaiting.domain.ReservationWaiting;
import roomescape.support.RepositoryTest;
import roomescape.theme.domain.Theme;
import roomescape.time.domain.ReservationTime;

class JdbcReservationWaitingRepositoryTest extends RepositoryTest {

    ReservationWaitingRepository reservationWaitingRepository;

    @Autowired
    public JdbcReservationWaitingRepositoryTest(JdbcTemplate jdbcTemplate) {
        this.reservationWaitingRepository = new JdbcReservationWaitingRepository(jdbcTemplate);
    }

    @Test
    @DisplayName("예약 대기를 저장하고 반환된 객체의 id를 반환한다.")
    void saveTest() {
        // given
        ReservationTime time = createTime(LocalTime.of(10, 0));
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com", 1000L);

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

    @Test
    @DisplayName("기존에 이미 예약 대기가 됐으면 예외가 발생한다.")
    void saveTest_duplicate() {
        // given
        ReservationTime time = createTime(LocalTime.of(10, 0));
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com", 1000L);

        ReservationWaiting waiting = ReservationWaiting.of(
                "brown",
                LocalDate.of(2024, 5, 1),
                time,
                theme
        );

        reservationWaitingRepository.save(waiting);

        // when & then
        assertThatThrownBy(() -> reservationWaitingRepository.save(waiting))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("기존에 이미 같은 사람이 같은 예약으로 예약 대기를 했으면 예외가 발생한다.")
    void saveTest_duplicate_slot_and_name() {
        // given
        ReservationTime time = createTime(LocalTime.of(10, 0));
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com", 1000L);

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
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com", 1000L);
        ReservationWaiting saved = createReservationWaiting("brown", time, LocalDate.of(2024, 5, 1), theme);

        // when
        Optional<ReservationWaiting> found = reservationWaitingRepository.findById(saved.getId());

        // then
        assertAll(
                () -> assertThat(found).isPresent(),
                () -> assertThat(found.get().getTheme().getAmount()).isEqualTo(1000L)
        );
    }

    @DisplayName("날짜, 시간, 테마, 예약자 이름에 해당하는 예약 대기가 존재하는지 조회한다.")
    @Test
    void existByDateAndTimeIdAndThemeIdAndNameTest() {
        //given
        ReservationTime time = createTime(LocalTime.of(10, 0));
       Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com", 1000L);
        ReservationWaiting saved = createReservationWaiting("brown", time, LocalDate.of(2024, 5, 1), theme);

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
       Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com", 1000L);
        ReservationWaiting saved = createReservationWaiting("brown", time, LocalDate.of(2024, 5, 1), theme);

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
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com", 1000L);
        ReservationWaiting saved = createReservationWaiting("brown", time, LocalDate.of(2024, 5, 1), theme);

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
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com", 1000L);
        LocalDate date = LocalDate.of(2024, 5, 1);
        Order order = createOrder(1000L);

        createReservation("someone", time, date, theme, order);
        createReservationWaiting("pobi", time, date, theme);

        // when & then
        assertThat(reservationWaitingRepository.findPromotableByName("pobi")).isEmpty();
    }

    @DisplayName("대기열 1순위가 아니면, 슬롯이 비어있어도 승격 가능한 예약 대기 목록에서 제외된다.")
    @Test
    void findPromotableByNameTest_excludes_when_not_first_in_queue() {
        // given
        ReservationTime time = createTime(LocalTime.of(10, 0));
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com", 1000L);
        LocalDate date = LocalDate.of(2024, 5, 1);
        ReservationWaiting first = createReservationWaiting("brown", time, date, theme);
        createReservationWaiting("pobi", time,  date, theme);

        // when & then
        assertAll(
                () -> assertThat(reservationWaitingRepository.findPromotableByName("pobi")).isEmpty(),
                () -> assertThat(reservationWaitingRepository.findPromotableByName("brown"))
                        .extracting(ReservationWaiting::getId)
                        .containsExactly(first.getId())
        );
    }

    @Test
    @DisplayName("아이디를 기반으로 예약 대기를 삭제한다.")
    void deleteByIdTest() {
        // given
        ReservationTime time = createTime(LocalTime.of(10, 0));
       Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com", 1000L);
        ReservationWaiting saved = createReservationWaiting("brown", time, LocalDate.of(2024, 5, 1), theme);

        // when
        int deletedCount = reservationWaitingRepository.deleteById(saved.getId());

        // then
        assertThat(deletedCount).isEqualTo(1);
    }
}
