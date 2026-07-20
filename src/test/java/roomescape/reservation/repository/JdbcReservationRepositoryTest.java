package roomescape.reservation.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.sql.Time;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import roomescape.order.domain.Order;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.ReservationStatus;
import roomescape.reservation.exception.ReservationNotFoundException;
import roomescape.reservation.service.dto.PopularThemeResult;
import roomescape.reservation.service.dto.ReservationWithStatusResult;
import roomescape.reservationWaiting.domain.ReservationWaiting;
import roomescape.reservationWaiting.repository.JdbcReservationWaitingRepository;
import roomescape.reservationWaiting.repository.ReservationWaitingRepository;
import roomescape.theme.domain.Theme;
import roomescape.time.domain.ReservationTime;

@JdbcTest
class JdbcReservationRepositoryTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    ReservationRepository reservationRepository;

    ReservationWaitingRepository reservationWaitingRepository;

    @Autowired
    public JdbcReservationRepositoryTest(JdbcTemplate jdbcTemplate) {
        this.reservationRepository = new JdbcReservationRepository(jdbcTemplate);
        this.reservationWaitingRepository = new JdbcReservationWaitingRepository(jdbcTemplate);
    }

    @Test
    @DisplayName("예약을 저장하고 반환된 객체의 ID를 확인한다.")
    void saveTest() {
        // given
        ReservationTime time = createTime(LocalTime.of(10, 0));
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com", 1000L);
        Order order = createOrder(1000L);

        // when
        Reservation saved = reservationRepository.save(
                new Reservation(
                        null,
                        "brown",
                        LocalDate.of(2024, 5, 1),
                        ReservationStatus.CONFIRMED,
                        time,
                        theme, 
                        order
                )
        );

        // then
        assertAll(
                () -> assertThat(saved.getId()).isNotNull(),
                () -> assertThat(saved.getName()).isEqualTo("brown")
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

    private Theme createTheme(String name, String description, String thumbnailUrl, Long amount) {
        jdbcTemplate.update(
                "INSERT INTO theme (name, description, thumbnail_url, amount) VALUES (?, ?, ?, ?)",
                name, description, thumbnailUrl, amount
        );

        Long themeId = jdbcTemplate.queryForObject(
                "SELECT id FROM theme WHERE name = ?",
                Long.class,
                name
        );

        return new Theme(themeId, name, description, thumbnailUrl, amount);
    }

    private Order createOrder(Long amount) {
        String orderId = java.util.UUID.randomUUID().toString();

        jdbcTemplate.update(
                "INSERT INTO orders (order_id, amount) VALUES (?, ?)",
                orderId, amount
        );

        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM orders WHERE order_id = ?",
                Long.class,
                orderId
        );

        return new Order(id, orderId, amount);
    }

    @Test
    @DisplayName("기존에 이미 동일한 예약이 있으면 예외가 발생한다.")
    void saveTest_duplicate() {
        // given
        ReservationTime time = createTime(LocalTime.of(10, 0));
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com", 1000L);
        Order order1 = createOrder(1000L);

        reservationRepository.save(
                new Reservation(
                        null,
                        "brown",
                        LocalDate.of(2024, 5, 1),
                        ReservationStatus.CONFIRMED,
                        time,
                        theme,
                        order1
                )
        );

        Order order2 = createOrder(1000L);

        // when & then
        assertThatThrownBy(() -> reservationRepository.save(
                new Reservation(
                        null,
                        "brown",
                        LocalDate.of(2024, 5, 1),
                        ReservationStatus.CONFIRMED,
                        time,
                        theme,
                        order2
                )
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("ID를 통해 예약을 삭제한다.")
    void deleteByIdTest() {
        // given
        ReservationTime time = createTime(LocalTime.of(10, 0));
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com", 1000L);
        Order order = createOrder(1000L);

        Reservation saved = saveReservation("brown",  LocalDate.of(2024, 5, 1), time, theme, order);

        // when
        reservationRepository.deleteById(saved.getId());

        // then
        List<ReservationWithStatusResult> results = reservationRepository.findAllByName("브라");
        assertThat(results).isEmpty();
    }

    private Reservation saveReservation(String name, LocalDate date, ReservationTime time, Theme theme, Order order) {
        return reservationRepository.save(
                Reservation.of(name, date, time, theme, order).confirm()
        );
    }

    @Test
    @DisplayName("예약 데이터를 삭제해도 시간/테마 데이터는 함께 삭제되지 않는다.")
    void deleteByIdTest_reservation_delete_does_not_delete_time_and_theme() {
        // given
        ReservationTime time = createTime(LocalTime.of(10, 0));
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com", 1000L);
        Order order = createOrder(1000L);

        Reservation saved = saveReservation("brown", LocalDate.of(2024, 5, 1), time, theme, order);

        // when
        reservationRepository.deleteById(saved.getId());

        // then
        Integer reservationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservation WHERE id = ?",
                Integer.class,
                saved.getId()
        );
        Integer timeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservation_time WHERE id = ?",
                Integer.class,
                time.getId()
        );
        Integer themeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM theme WHERE id = ?",
                Integer.class,
                theme.getId()
        );
        Integer orderCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE id = ?",
                Integer.class,
                order.getId()
        );

        assertAll(
                () -> assertThat(reservationCount).isEqualTo(0),
                () -> assertThat(timeCount).isEqualTo(1),
                () -> assertThat(themeCount).isEqualTo(1),
                () -> assertThat(orderCount).isEqualTo(1)
        );
    }

    @Test
    @DisplayName("예약을 수정한다.")
    void updateTest() {
        // given
        ReservationTime time = createTime(LocalTime.of(10, 0));
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com", 1000L);
        Order order = createOrder(1000L);

        Reservation saved = reservationRepository.save(
                new Reservation(
                        null,
                        "brown",
                        LocalDate.of(2024, 5, 1),
                        ReservationStatus.CONFIRMED,
                        time,
                        theme,
                        order
                )
        );

        Reservation updated = saved.updateDate(LocalDate.of(2024, 5, 5));

        // when
        reservationRepository.update(updated);

        // then
        Reservation found = reservationRepository.findByIdForUpdate(saved.getId())
                .orElseThrow();

        assertAll(
                () -> assertThat(found.getId()).isEqualTo(saved.getId()),
                () -> assertThat(found.getDate()).isEqualTo(LocalDate.of(2024, 5, 5))
        );
    }

    @Test
    @DisplayName("저장돼 있지 않은 예약을 수정하면 예외가 발생한다.")
    void updateTest_do_not_exist() {
        // given
        ReservationTime time = createTime(LocalTime.of(10, 0));
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com", 1000L);
        Order order = createOrder(1000L);

        // when & then
        assertThatThrownBy(
                () -> reservationRepository.update(
                        new Reservation(
                                999L,
                                "brown",
                                LocalDate.of(2024, 5, 1),
                                ReservationStatus.CONFIRMED,
                                time,
                                theme,
                                order
                        )
                )
        ).isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    @DisplayName("수정하려는 예약이 이미 존재하면 예외가 발생한다.")
    void updateTest_duplicate() {
        // given
        ReservationTime time = createTime(LocalTime.of(10, 0));
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com", 1000L);
        Order order1 = createOrder(1000L);

        reservationRepository.save(
                new Reservation(
                        null,
                        "brown",
                        LocalDate.of(2024, 5, 5),
                        ReservationStatus.CONFIRMED,
                        time,
                        theme,
                        order1
                )
        );

        Order order2 = createOrder(1000L);

        Reservation saved = reservationRepository.save(
                new Reservation(
                        null,
                        "brown",
                        LocalDate.of(2024, 5, 1),
                        ReservationStatus.CONFIRMED,
                        time,
                        theme,
                        order2
                )
        );

        Reservation updated = saved.updateDate(LocalDate.of(2024, 5, 5));

        // when & then
        assertThatThrownBy(
                () -> reservationRepository.update(updated)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("id에 해당하는 예외를 조회한다.")
    void findByIdForUpdate() {
        // given
        ReservationTime time = createTime(LocalTime.of(10, 0));
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com", 1000L);
        Order order = createOrder(1000L);

        Reservation saved = saveReservation("brown", LocalDate.of(2024, 5, 1), time, theme, order);

        // when
        Optional<Reservation> found = reservationRepository.findByIdForUpdate(saved.getId());

        // then
        assertAll(
                () -> assertThat(found).isPresent(),
                () -> assertThat(found.get().getTheme().getAmount()).isEqualTo(1000L),
                () -> assertThat(found.get().getOrder().getOrderId()).isEqualTo(order.getOrderId()),
                () -> assertThat(found.get().getOrder().getAmount()).isEqualTo(1000L),
                () -> assertThat(reservationRepository.findByIdForUpdate(999L)).isEmpty()
        );
    }

    @Test
    @DisplayName("모든 예약 목록을 조회한다.")
    void findAll() {
        // given
        ReservationTime time = createTime(LocalTime.of(10, 0));
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com", 1000L);

        Reservation saved1 = saveReservation("brown", LocalDate.of(2024, 5, 1), time, theme, createOrder(1000L));
        Reservation saved2 = saveReservation("brown", LocalDate.of(2024, 5, 2), time, theme, createOrder(1000L));
        Reservation saved3 = saveReservation("poppy", LocalDate.of(2024, 5, 3), time, theme, createOrder(1000L));

        // when
        List<Reservation> reservations = reservationRepository.findAll();

        // then
        assertAll(
                () -> assertThat(reservations).hasSize(3),
                () -> assertThat(reservations).containsExactly(saved1, saved2, saved3)
        );
    }

    @Test
    @DisplayName("이름에 해당하는 모든 예약과 대기 목록을 (날짜, 시간, 순번) 순서대로 조회한다.")
    void findAllByName() {
        // given
        ReservationTime time = createTime(LocalTime.of(10, 0));
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com", 1000L);

        Reservation reservation1 = saveReservation("brown", LocalDate.of(2024, 5, 1), time, theme, createOrder(1000L));
        saveReservation("poppy", LocalDate.of(2024, 5, 2), time, theme, createOrder(1000L));
        Reservation reservation2 = saveReservation("brown", LocalDate.of(2024, 5, 3), time, theme, createOrder(1000L));

        ReservationWaiting waiting1 = reservationWaitingRepository.save(ReservationWaiting.of(
                "brown", LocalDate.of(2024, 5, 2), time, theme
        ));

        // when
        List<ReservationWithStatusResult> results = reservationRepository.findAllByName("brown");

        // then
        assertAll(
                () -> assertThat(results).hasSize(3),
                () -> assertThat(results).containsExactly(
                        new ReservationWithStatusResult(reservation1.getId(), reservation1.getName(), reservation1.getDate(), reservation1.getReservationTime(), reservation1.getTheme(), "reserved", 0L),
                        new ReservationWithStatusResult(waiting1.getId(), waiting1.getName(), waiting1.getDate(), waiting1.getTime(), waiting1.getTheme(), "waiting", 1L),
                        new ReservationWithStatusResult(reservation2.getId(), reservation2.getName(), reservation2.getDate(), reservation2.getReservationTime(), reservation2.getTheme(), "reserved", 0L)
                )
        );
    }

    @Test
    @DisplayName("같은 슬롯에 대기가 여러 명이면, 본인 이름으로 조회해도 실제 대기열 순번이 반환된다.")
    void findAllByName_waitingOrder_reflects_actual_queue_position() {
        // given
        ReservationTime time = createTime(LocalTime.of(10, 0));
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com", 1000L);
        LocalDate date = LocalDate.of(2024, 5, 1);

        ReservationWaiting first = reservationWaitingRepository.save(ReservationWaiting.of(
                "brown", date, time, theme
        ));
        ReservationWaiting second = reservationWaitingRepository.save(ReservationWaiting.of(
                "pobi", date, time, theme
        ));

        // when
        List<ReservationWithStatusResult> brownResults = reservationRepository.findAllByName("brown");
        List<ReservationWithStatusResult> pobiResults = reservationRepository.findAllByName("pobi");

        // then
        assertAll(
                () -> assertThat(brownResults)
                        .extracting(ReservationWithStatusResult::waitingOrder)
                        .containsExactly(1L),
                () -> assertThat(pobiResults)
                        .extracting(ReservationWithStatusResult::waitingOrder)
                        .containsExactly(2L)
        );
    }

    @Test
    @DisplayName("from과 to 사이 일정의 예약 중, 결제 승인된 건을 기준으로 예약이 많은 상위 limit 개의 테마를 순서대로 조회한다.")
    void findPopularThemesTest() {
        // given
        Theme carrotTheme = createTheme("당근", "당근 전용", "https://carrot.com", 1000L); // 삽입 1순위 -> 결과 3위 예상
        Theme pairTheme = createTheme("페어", "페어 전용", "https://pair.com", 1000L);   // 삽입 2순위 -> 결과 2위 예상
        Theme woowaTheme = createTheme("우테코", "우테코 전용", "https://woowa.com", 1000L); // 삽입 3순위 -> 결과 1위 예상

        ReservationTime time = createTime(LocalTime.of(10, 0));
        LocalDate today = LocalDate.now(Clock.fixed(Instant.parse("2026-05-06T00:00:00Z"), ZoneId.of("Asia/Seoul")));

        // 1. from 경계값 테스트 (-7일)
        saveReservation("eden", today.minusDays(7), time, woowaTheme, createOrder(1000L));
        // 2. to 경계값 테스트 (-1일)
        saveReservation("pobi", today.minusDays(1), time, woowaTheme, createOrder(1000L));
        saveReservation("jason", today.minusDays(3), time, woowaTheme, createOrder(1000L));
        // => 우테코 총 3건 (1위 보장)

        saveReservation("lea", today.minusDays(2), time, pairTheme, createOrder(1000L));
        saveReservation("brown", today.minusDays(4), time, pairTheme, createOrder(1000L));
        // => 페어 총 2건 (2위 보장)

        saveReservation("wedge", today.minusDays(5), time, carrotTheme, createOrder(1000L));
        // => 당근 총 1건 (3위 보장)

        // 만약 Pending이 카운트된다면 당근 테마가 4건이 되어 1위로 올라가므로 테스트가 즉시 실패함.
        saveReservationAsPending("pending1", today.minusDays(2), time, carrotTheme, createOrder(1000L));
        saveReservationAsPending("pending2", today.minusDays(3), time, carrotTheme, createOrder(1000L));
        saveReservationAsPending("pending3", today.minusDays(4), time, carrotTheme, createOrder(1000L));

        saveReservation("outOfRange", today.minusDays(8), time, pairTheme, createOrder(1000L));
        saveReservation("todayRes", today, time, pairTheme, createOrder(1000L));

        // when
        List<PopularThemeResult> popularThemes = reservationRepository.findPopularThemes(
                today.minusDays(7),
                today.minusDays(1),
                2
        );

        // then
        assertThat(popularThemes)
                .extracting(PopularThemeResult::name)
                .containsExactly("우테코", "페어");
    }

    private Reservation saveReservationAsPending(String name, LocalDate date, ReservationTime time, Theme theme, Order order) {
        return reservationRepository.save(
                Reservation.of(name, date, time, theme, order)
        );
    }

    @DisplayName("name, date, themeId, timeId가 같고 id가 다른 예약이 있는지 조회한다.")
    @Test
    void existByDateAndTimeIdAndThemeIdExceptId() {
        //given
        ReservationTime time = createTime(LocalTime.of(10, 0));
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com", 1000L);
        Order order = createOrder(1000L);

        Reservation saved = saveReservation("brown", LocalDate.of(2024, 5, 1), time, theme, order);

        //when & then
        assertAll(
                () -> assertThat(reservationRepository.existByDateAndTimeIdAndThemeIdExceptId(
                        saved.getDate(),
                        saved.getReservationTime().getId(),
                        saved.getTheme().getId(),
                        saved.getId() + 1
                )).isTrue(),
                () -> assertThat(reservationRepository.existByDateAndTimeIdAndThemeIdExceptId(
                        saved.getDate(),
                        saved.getReservationTime().getId(),
                        saved.getTheme().getId(),
                        saved.getId()
                )).isFalse()
        );
    }

    @DisplayName("date, themeId, timeId를 기준으로 예약을 조회한다.")
    @Test
    void findByDateAndTimeIdAndThemeIdForUpdateTest() {
        //given
        ReservationTime time = createTime(LocalTime.of(10, 0));
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com", 1000L);
        Order order = createOrder(1000L);

        Reservation saved = saveReservation("brown", LocalDate.of(2024, 5, 1), time, theme, order);

        //when
        Optional<Reservation> result = reservationRepository.findByDateAndTimeIdAndThemeIdForUpdate(
                saved.getDate(),
                saved.getReservationTime().getId(),
                saved.getTheme().getId()
        );

        //then
        assertAll(
                () -> assertThat(result).isPresent(),
                () -> assertThat(result.get().getTheme().getAmount()).isEqualTo(1000L),
                () -> assertThat(reservationRepository.findByDateAndTimeIdAndThemeIdForUpdate(
                        saved.getDate(), saved.getReservationTime().getId() + 1, saved.getTheme().getId()
                )).isEmpty()
        );
    }

    @DisplayName("date, timeId, themeId에 해당하는 예약이 존재하는지 조회한다.")
    @Test
    void existByDateAndTimeIdAndThemeIdTest() {
        //given
        ReservationTime time = createTime(LocalTime.of(10, 0));
        Theme theme = createTheme("우테코", "우테코 전용 테마", "https://example.com", 1000L);
        Order order = createOrder(1000L);

        Reservation saved = saveReservation("brown", LocalDate.of(2024, 5, 1), time, theme, order);

        //when & then
        assertAll(
                () -> assertThat(reservationRepository.existByDateAndTimeIdAndThemeId(
                        saved.getDate(),
                        saved.getReservationTime().getId(),
                        saved.getTheme().getId()
                )).isTrue(),
                () -> assertThat(reservationRepository.existByDateAndTimeIdAndThemeId(
                        LocalDate.of(2026, 5, 20),
                        saved.getReservationTime().getId(),
                        saved.getTheme().getId()
                )).isFalse()
        );
    }
}
