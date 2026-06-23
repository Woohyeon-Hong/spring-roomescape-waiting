package roomescape.gatling.seed;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class GatlingSeedPlan {

    private final int reservationCount;
    private final int waitingCount;
    private final int averageWaitingPerSlot;
    private final int averageReservationPerUser;
    private final int averageWaitingPerUser;
    private final LocalDate startDate;

    public GatlingSeedPlan(
            int reservationCount,
            int waitingCount,
            int averageWaitingPerSlot,
            int averageReservationPerUser,
            int averageWaitingPerUser,
            LocalDate startDate
    ) {
        this.reservationCount = reservationCount;
        this.waitingCount = waitingCount;
        this.averageWaitingPerSlot = averageWaitingPerSlot;
        this.averageReservationPerUser = averageReservationPerUser;
        this.averageWaitingPerUser = averageWaitingPerUser;
        this.startDate = startDate;
    }

    public List<ReservationSeed> createReservations(int timeCount, int themeCount) {
        List<ReservationSeed> reservations = new ArrayList<>();
        int slotsPerDate = timeCount * themeCount;
        int userCount = (reservationCount + averageReservationPerUser - 1) / averageReservationPerUser;

        for (int index = 0; index < reservationCount; index++) {
            int slotIndex = index;
            reservations.add(new ReservationSeed(
                    "reservation-user-" + ((index % userCount) + 1),
                    startDate.plusDays(slotIndex / slotsPerDate),
                    (slotIndex % timeCount) + 1L,
                    ((slotIndex / timeCount) % themeCount) + 1L
            ));
        }

        return reservations;
    }

    public List<WaitingSeed> createWaitings(int timeCount, int themeCount) {
        if (waitingCount % averageWaitingPerSlot != 0) {
            throw new IllegalArgumentException("waitingCount must be divisible by averageWaitingPerSlot");
        }
        if (waitingCount % averageWaitingPerUser != 0) {
            throw new IllegalArgumentException("waitingCount must be divisible by averageWaitingPerUser");
        }

        List<WaitingSeed> waitings = new ArrayList<>(waitingCount);
        int slotsPerDate = timeCount * themeCount;
        int userCount = waitingCount / averageWaitingPerUser;

        for (int index = 0; index < waitingCount; index++) {
            int slotIndex = index / averageWaitingPerSlot;
            waitings.add(new WaitingSeed(
                    "waiting-user-" + ((index % userCount) + 1),
                    startDate.plusDays(slotIndex / slotsPerDate),
                    (slotIndex % timeCount) + 1L,
                    ((slotIndex / timeCount) % themeCount) + 1L
            ));
        }

        return waitings;
    }
}
