package roomescape.theme.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import roomescape.support.RepositoryTest;
import roomescape.theme.domain.Theme;
import roomescape.time.domain.ReservationTime;

class JdbcThemeRepositoryTest extends RepositoryTest {

    JdbcThemeRepository themeRepository;

    @Autowired
    public JdbcThemeRepositoryTest(JdbcTemplate jdbcTemplate) {
        this.themeRepository = new JdbcThemeRepository(jdbcTemplate);
    }

    @Test
    @DisplayName("새로운 테마를 저장하고 반환된 객체의 ID를 확인한다.")
    void saveTest() {
        // given
        Theme theme = Theme.of("테마", "설명", "thumbnailUrl", 1000L);

        // when
        Theme saved = themeRepository.save(theme);

        //then
        assertAll(
                () -> assertThat(saved.getId()).isNotNull(),
                () -> assertThat(saved.getName()).isEqualTo(theme.getName())
        );
    }

    @Test
    @DisplayName("기존에 이미 같은 테마가 저장됐으면 예외가 발생한다.")
    void saveTest_duplicate() {
        // given
        Theme theme = Theme.of("테마", "설명", "thumbnailUrl", 1000L);
        themeRepository.save(theme);

        // when & then
        assertThatThrownBy(() -> themeRepository.save(theme))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("기존에 이미 테마 이름이 겹치는 테마가 있으면 예외가 발생한다.")
    void saveTest_duplicate_name() {
        // given
        themeRepository.save(Theme.of("테마", "설명", "thumbnailUrl", 1000L));

        // when & then
        assertThatThrownBy(() -> themeRepository.save(Theme.of("테마", "other", "otherThumbnailUrl", 1000L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("ID를 통해 저장된 테마를 조회한다.")
    void findByIdTest() {
        // given
        Theme saved = themeRepository.save(Theme.of("테마", "설명", "thumbnailUrl", 1000L));

        // when
        Theme found = themeRepository.findById(saved.getId())
                .orElseThrow(() -> new AssertionError("조회된 결과가 없습니다. id: " + saved.getId()));

        // then
        assertAll(
                () -> assertThat(found.getName()).isEqualTo(saved.getName()),
                () -> assertThat(found.getDescription()).isEqualTo(saved.getDescription()),
                () -> assertThat(found.getThumbnailUrl()).isEqualTo(saved.getThumbnailUrl())
        );
    }

    @DisplayName("테마 이름을 기준으로 조회한다.")
    @Test
    void existByNameTest() {
        //given
        themeRepository.save(
                Theme.of("테마", "테마 설명", "썸네일_url", 1000L)
        );

        //when & then
        assertAll(
                () -> assertThat(themeRepository.existByName("테마"))
                        .isTrue(),
                () -> assertThat(themeRepository.existByName("없는_것"))
                        .isFalse()
        );
    }

    @Test
    @DisplayName("존재하는 모든 테마 목록을 리스트로 조회한다.")
    void findAllTest() {
        // given
        Theme saved1 = themeRepository.save(Theme.of("테마1", "설명", "thumbnailUrl", 1000L));
        Theme saved2 = themeRepository.save(Theme.of("테마2", "설명", "thumbnailUrl", 1000L));

        // when
        List<Theme> result = themeRepository.findAll();

        // then
        assertThat(result).containsExactly(saved1, saved2);
    }

    @Test
    @DisplayName("id로 테마를 삭제한다.")
    void deleteByIdTest() {
        //given
        ReservationTime time = createTime(LocalTime.of(10, 0));

        themeRepository.save(
                Theme.of("테마", "테마 설명", "썸네일_url", 1000L)
        );
        Long themeId = jdbcTemplate.queryForObject(
                "SELECT id FROM theme WHERE name = ?",
                Long.class,
                "테마"
        );

        //when & then
        assertThat(themeRepository.deleteById(themeId))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("ID가 사용되고 있으면 예외가 발생한다.")
    void deleteByIdTest_used() {
        //given
        ReservationTime time = createTime(LocalTime.of(10, 0));

        themeRepository.save(
                Theme.of("테마", "테마 설명", "썸네일_url", 1000L)
        );
        Long themeId = jdbcTemplate.queryForObject(
                "SELECT id FROM theme WHERE name = ?",
                Long.class,
                "테마"
        );

        Long orderId = createOrder(1000L).getId();

        jdbcTemplate.update("""
            insert into reservation(name, reservation_date, time_id, theme_id, order_id)
            values (?, ?, ?, ?, ?)
        """, "brown", LocalDate.of(2026, 5, 6), time.getId(), themeId, orderId
        );

        //when & then
        assertThatThrownBy(
                () -> themeRepository.deleteById(themeId)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}
