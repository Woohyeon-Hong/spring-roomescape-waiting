package roomescape.reservationWaiting.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.exception.AuthorizationException;
import roomescape.order.domain.Order;
import roomescape.order.repository.OrderRepository;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.exception.DuplicateReservationException;
import roomescape.reservation.repository.ReservationRepository;
import roomescape.reservation.service.ExpiryValidator;
import roomescape.reservationWaiting.domain.ReservationWaiting;
import roomescape.reservationWaiting.exception.AlreadyReservedSameSlotException;
import roomescape.reservationWaiting.exception.DuplicateReservationWaitingException;
import roomescape.reservationWaiting.exception.ReservationWaitingNotFoundException;
import roomescape.reservationWaiting.exception.ReservationWaitingNotPromotableException;
import roomescape.reservationWaiting.exception.ReservationWaitingTargetNotFoundException;
import roomescape.reservationWaiting.repository.ReservationWaitingRepository;
import roomescape.reservationWaiting.service.dto.ReservationWaitingCommand;
import roomescape.theme.domain.Theme;
import roomescape.theme.exception.ThemeNotFoundException;
import roomescape.theme.repository.ThemeRepository;
import roomescape.time.domain.ReservationTime;
import roomescape.time.exception.TimeNotFoundException;
import roomescape.time.repository.ReservationTimeRepository;

@RequiredArgsConstructor
@Service
public class ReservationWaitingService {

    private final ReservationWaitingRepository reservationWaitingRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationTimeRepository reservationTimeRepository;
    private final ThemeRepository themeRepository;
    private final OrderRepository orderRepository;
    private final ExpiryValidator expiryValidator;

    @Transactional
    public ReservationWaiting makeReservationWaiting(ReservationWaitingCommand command) {
        if (reservationWaitingRepository.existByDateAndTimeIdAndThemeIdAndName(
                command.date(), command.timeId(), command.themeId(), command.name()
        )) {
            throw new DuplicateReservationWaitingException();
        }

        ReservationTime time = reservationTimeRepository.findById(command.timeId())
                .orElseThrow(TimeNotFoundException::new);

        expiryValidator.validate(command.date(), time.getStartAt());

        Theme theme = themeRepository.findById(command.themeId())
                .orElseThrow(ThemeNotFoundException::new);

        Reservation reservation = reservationRepository.findByDateAndTimeIdAndThemeIdForUpdate(
                command.date(), command.timeId(), command.themeId()
        ).orElseThrow(ReservationWaitingTargetNotFoundException::new);

        if (reservation.hasSameName(command.name())) {
            throw new AlreadyReservedSameSlotException();
        }

        try {
            return reservationWaitingRepository.save(
                    ReservationWaiting.of(
                            command.name(),
                            command.date(),
                            time,
                            theme
                    )
            );
        } catch (DuplicateKeyException e) {
            throw new DuplicateReservationWaitingException();
        }
    }


    public void deleteReservationWaitingById(Long id, String name) {
        ReservationWaiting reservationWaiting = reservationWaitingRepository.findById(id)
                .orElseThrow(ReservationWaitingNotFoundException::new);

        if (!reservationWaiting.hasSameName(name)) {
            throw new AuthorizationException();
        }

        expiryValidator.validate(
                reservationWaiting.getDate(),
                reservationWaiting.getTime().getStartAt()
        );

        int affectedRow = reservationWaitingRepository.deleteById(id);
        int nonAffected = 0;

        if (affectedRow == nonAffected) {
            throw new ReservationWaitingNotFoundException();
        }
    }

    public List<ReservationWaiting> findPromotableWaitingsByName(String name) {
        return reservationWaitingRepository.findPromotableByName(name);
    }

    @Transactional
    public void promoteWaiting(Long waitingId, String name) {
        ReservationWaiting waiting = reservationWaitingRepository.findById(waitingId)
                .orElseThrow(ReservationWaitingNotFoundException::new);

        if (!waiting.hasSameName(name)) {
            throw new AuthorizationException();
        }

        ReservationWaiting first = reservationWaitingRepository.findFirstByReservationDateAndTimeIdAndThemeIdForUpdate(
                waiting.getDate(), waiting.getTime().getId(), waiting.getTheme().getId()
        ).orElseThrow(ReservationWaitingNotFoundException::new);

        if (!first.getId().equals(waiting.getId())) {
            throw new ReservationWaitingNotPromotableException();
        }

        Order order = orderRepository.save(Order.of(waiting.getTheme().getAmount()));

        int affectedRow = reservationWaitingRepository.deleteById(waiting.getId());
        int nonAffected = 0;

        if (affectedRow == nonAffected) {
            throw new ReservationWaitingNotFoundException();
        }

        try {
            reservationRepository.save(
                    Reservation.of(waiting.getName(), waiting.getDate(), waiting.getTime(), waiting.getTheme(), order)
            );
        } catch (DuplicateKeyException e) {
            throw new DuplicateReservationException();
        }
    }
}
