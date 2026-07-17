DROP TABLE IF EXISTS reservation;
DROP TABLE IF EXISTS reservation_waiting;
DROP TABLE IF EXISTS reservation_time;
DROP TABLE IF EXISTS theme;
DROP TABLE IF EXISTS orders;

CREATE TABLE reservation_time (
    id                 BIGINT           NOT NULL AUTO_INCREMENT,
    start_at           TIME             NOT NULL UNIQUE,
    PRIMARY KEY (id)
);

CREATE TABLE theme (
    id                 BIGINT           NOT NULL AUTO_INCREMENT,
    name               VARCHAR(255)     NOT NULL UNIQUE,
    description        VARCHAR(255)     NOT NULL,
    thumbnail_url      VARCHAR(255)     NOT NULL,
    amount             BIGINT           NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE orders (
    id                 BIGINT           NOT NULL AUTO_INCREMENT,
    order_id           VARCHAR(255)     NOT NULL UNIQUE,
    amount             VARCHAR(255)     NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE reservation (
    id                 BIGINT          NOT NULL AUTO_INCREMENT,
    name               VARCHAR(255)    NOT NULL,
    reservation_date   DATE            NOT NULL,
    time_id BIGINT                     NOT NULL,
    theme_id BIGINT                    NOT NULL,
    order_id BIGINT,
    PRIMARY KEY (id),
    FOREIGN KEY (time_id) REFERENCES reservation_time (id),
    FOREIGN KEY (theme_id) REFERENCES theme (id),
    FOREIGN KEY (order_id) REFERENCES orders (id),
    UNIQUE (reservation_date, time_id, theme_id)
);

CREATE INDEX idx_reservation_name
    ON reservation (name);

CREATE TABLE reservation_waiting (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    name               VARCHAR(255) NOT NULL,
    reservation_date   DATE         NOT NULL,
    time_id            BIGINT       NOT NULL,
    theme_id           BIGINT       NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (time_id) REFERENCES reservation_time (id),
    FOREIGN KEY (theme_id) REFERENCES theme (id),
    UNIQUE (reservation_date, time_id, theme_id, name)
);

CREATE INDEX idx_reservation_waiting_name
    ON reservation_waiting (name);
