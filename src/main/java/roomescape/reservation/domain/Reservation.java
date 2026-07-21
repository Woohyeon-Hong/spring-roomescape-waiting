package roomescape.reservation.domain;

import java.time.LocalDate;
import java.util.Objects;
import lombok.Getter;
import roomescape.order.domain.Order;
import roomescape.theme.domain.Theme;
import roomescape.time.domain.ReservationTime;

@Getter
public class Reservation {

    private final Long id;
    private final String name;
    private final LocalDate date;
    private final ReservationStatus status;
    private final ReservationTime reservationTime;
    private final Theme theme;
    private final Order order;

    public Reservation(Long id, String name, LocalDate date, ReservationStatus status,
                       ReservationTime reservationTime, Theme theme, Order order) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.status = status;
        this.reservationTime = reservationTime;
        this.theme = theme;
        this.order = order;
    }

    public static Reservation of(String name, LocalDate date, ReservationTime time, Theme theme, Order order) {
        return new Reservation(null, name, date, ReservationStatus.PENDING, time, theme, order);
    }

    public Reservation confirm() {
        return new Reservation(
                this.id,
                this.name,
                this.date,
                ReservationStatus.CONFIRMED,
                this.reservationTime,
                this.theme,
                order
        );
    }

    public Reservation updateId(long id) {
        return new Reservation(
                id,
                this.name,
                this.date,
                this.status,
                this.reservationTime,
                this.theme,
                order
        );
    }

    public Reservation updateDate(LocalDate date) {
        return new Reservation(
                this.id,
                this.name,
                date,
                this.status,
                this.reservationTime,
                this.theme,
                order
        );
    }

    public Reservation updateTime(ReservationTime time) {
        return new Reservation(
                this.id,
                this.name,
                this.date,
                this.status,
                time,
                this.theme,
                order
        );
    }

    public boolean hasSameName(String name) {
        return this.name.equals(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Reservation that = (Reservation) o;
        return id != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
