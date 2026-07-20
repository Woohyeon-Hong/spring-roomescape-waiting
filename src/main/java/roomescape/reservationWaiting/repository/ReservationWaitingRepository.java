package roomescape.reservationWaiting.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import roomescape.reservationWaiting.domain.ReservationWaiting;

public interface ReservationWaitingRepository {

    ReservationWaiting save(ReservationWaiting reservationWaiting);

    Optional<ReservationWaiting> findById(Long id);

    List<ReservationWaiting> findPromotableByName(String name);

    Optional<ReservationWaiting> findFirstByReservationDateAndTimeIdAndThemeIdForUpdate(LocalDate date, Long timeId, Long themeId);

    boolean existByDateAndTimeIdAndThemeIdAndName(LocalDate date, Long timeId, Long themeId, String name);

    boolean existsByDateAndTimeIdAndThemeId(LocalDate date, Long timeId, Long themeId);

    int deleteById(Long id);
}
