package roomescape.gatling;

import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import roomescape.gatling.seed.GatlingSeedPlan;

@Component
@Profile("gatling")
public class GatlingDataInitializer implements ApplicationRunner {

    private final GatlingSeedRepository gatlingSeedRepository;
    private final int reservationCount;
    private final int waitingCount;
    private final int averageWaitingPerSlot;
    private final int averageReservationPerUser;
    private final int averageWaitingPerUser;
    private final LocalDate startDate;
    private final int timeCount;
    private final int themeCount;

    public GatlingDataInitializer(
            GatlingSeedRepository gatlingSeedRepository,
            @Value("${roomescape.gatling.seed.reservation-count}") int reservationCount,
            @Value("${roomescape.gatling.seed.waiting-count}") int waitingCount,
            @Value("${roomescape.gatling.seed.average-waiting-per-slot}") int averageWaitingPerSlot,
            @Value("${roomescape.gatling.seed.average-reservation-per-user}") int averageReservationPerUser,
            @Value("${roomescape.gatling.seed.average-waiting-per-user}") int averageWaitingPerUser,
            @Value("${roomescape.gatling.seed.start-date}") LocalDate startDate,
            @Value("${roomescape.gatling.seed.time-count}") int timeCount,
            @Value("${roomescape.gatling.seed.theme-count}") int themeCount
    ) {
        validateSeedSettings(
                reservationCount,
                waitingCount,
                averageWaitingPerSlot,
                averageReservationPerUser,
                averageWaitingPerUser,
                timeCount,
                themeCount
        );

        this.gatlingSeedRepository = gatlingSeedRepository;
        this.reservationCount = reservationCount;
        this.waitingCount = waitingCount;
        this.averageWaitingPerSlot = averageWaitingPerSlot;
        this.averageReservationPerUser = averageReservationPerUser;
        this.averageWaitingPerUser = averageWaitingPerUser;
        this.startDate = startDate;
        this.timeCount = timeCount;
        this.themeCount = themeCount;
    }

    private void validateSeedSettings(
            int reservationCount,
            int waitingCount,
            int averageWaitingPerSlot,
            int averageReservationPerUser,
            int averageWaitingPerUser,
            int timeCount,
            int themeCount
    ) {
        if (reservationCount <= 0 || waitingCount <= 0) {
            throw new IllegalArgumentException("reservationCount and waitingCount must be positive");
        }
        if (averageWaitingPerSlot <= 0 || averageReservationPerUser <= 0 || averageWaitingPerUser <= 0) {
            throw new IllegalArgumentException("average seed values must be positive");
        }
        if (timeCount <= 0 || themeCount <= 0) {
            throw new IllegalArgumentException("timeCount and themeCount must be positive");
        }
        if (timeCount > 24) {
            throw new IllegalArgumentException("timeCount must be less than or equal to 24");
        }
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        GatlingSeedPlan plan = new GatlingSeedPlan(
                reservationCount,
                waitingCount,
                averageWaitingPerSlot,
                averageReservationPerUser,
                averageWaitingPerUser,
                startDate
        );

        gatlingSeedRepository.reset();

        gatlingSeedRepository.insertTimes(timeCount);
        gatlingSeedRepository.insertThemes(themeCount);

        gatlingSeedRepository.insertReservations(plan.createReservations(timeCount, themeCount));
        gatlingSeedRepository.insertWaitings(plan.createWaitings(timeCount, themeCount));
    }
}
