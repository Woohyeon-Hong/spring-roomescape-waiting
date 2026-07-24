DELETE FROM reservation;
DELETE FROM reservation_waiting;
DELETE FROM reservation_time;
DELETE FROM theme;
DELETE FROM orders;

ALTER TABLE reservation ALTER COLUMN id RESTART WITH 1;
ALTER TABLE reservation_waiting ALTER COLUMN id RESTART WITH 1;
ALTER TABLE reservation_time ALTER COLUMN id RESTART WITH 1;
ALTER TABLE theme ALTER COLUMN id RESTART WITH 1;
ALTER TABLE orders ALTER COLUMN id RESTART WITH 1;

INSERT INTO reservation_time (start_at) VALUES
('10:00:00'),
('11:00:00'),
('12:00:00'),
('13:00:00'),
('14:00:00'),
('15:00:00'),
('16:00:00'),
('17:00:00'),
('18:00:00');

INSERT INTO theme (name, description, thumbnail_url, amount) VALUES
('우주선 탈출', '고장 난 우주선에서 제한 시간 안에 탈출하세요.', 'https://picsum.photos/seed/space-escape/480/320', 20000),
('좀비 아포칼립스', '봉쇄된 도시에서 생존 키트를 찾아 탈출해야 합니다.', 'https://picsum.photos/seed/zombie-apocalypse/480/320', 20000),
('고대 피라미드', '피라미드 깊숙한 곳의 비밀 방을 열어 보물을 찾으세요.', 'https://picsum.photos/seed/pyramid/480/320', 20000),
('마법학교의 비밀', '사라진 마법서를 찾아 학교의 저주를 풀어야 합니다.', 'https://picsum.photos/seed/magic-school/480/320', 20000),
( '해적선의 보물', '해적선 선장의 단서를 모아 숨겨진 보물창고를 여세요.', 'https://picsum.photos/seed/pirate-treasure/480/320', 20000),
('미스터리 연구소', '폐쇄된 연구소에서 실험 기록을 복구하고 탈출하세요.', 'https://picsum.photos/seed/lab-mystery/480/320', 20000),
( '시간여행자', '뒤틀린 시간 장치를 복구해 현재로 돌아오세요.', 'https://picsum.photos/seed/time-traveler/480/320', 20000),
( '유령의 저택', '밤이 끝나기 전 저택의 원혼을 달래는 의식을 완성하세요.', 'https://picsum.photos/seed/haunted-mansion/480/320', 20000),
( '사라진 화가의 작품', '실종된 화가가 남긴 암호를 풀어 진짜 작품을 찾으세요.', 'https://picsum.photos/seed/missing-painting/480/320', 20000),
( '심해 탐험', '산소가 떨어지기 전에 심해 기지의 전원을 복구해야 합니다.', 'https://picsum.photos/seed/deep-sea/480/320', 20000),
( '왕실 음모', '왕궁에서 벌어진 음모의 증거를 찾아 누명을 벗기세요.', 'https://picsum.photos/seed/royal-conspiracy/480/320', 20000),
( '폐병원 탈출', '버려진 병원에서 수상한 흔적을 추적해 출구를 찾으세요.', 'https://picsum.photos/seed/abandoned-hospital/480/320', 20000),
('한밤중의 서커스', '멈춰버린 서커스 공연의 비밀을 밝히고 무대를 탈출하세요.', 'https://picsum.photos/seed/midnight-circus/480/320', 20000),
('비밀 요원 작전', '이중 잠금 장치를 해제하고 기밀 문서를 회수하세요.', 'https://picsum.photos/seed/secret-agent/480/320', 20000),
('드래곤의 동굴', '드래곤이 잠든 사이 고대 룬을 해독해 동굴을 빠져나오세요.', 'https://picsum.photos/seed/dragon-cave/480/320', 20000);

-- 예약 27건과 1:1로 대응하는 주문 (order_id 1~27은 아래 reservation insert의 행 순서와 일치), 이미 결제 확정된 것으로 시딩
INSERT INTO orders (order_id, amount, payment_key) VALUES
('seed-order-01', 20000, 'seed-payment-key-01'), ('seed-order-02', 20000, 'seed-payment-key-02'), ('seed-order-03', 20000, 'seed-payment-key-03'), ('seed-order-04', 20000, 'seed-payment-key-04'), ('seed-order-05', 20000, 'seed-payment-key-05'),
('seed-order-06', 20000, 'seed-payment-key-06'), ('seed-order-07', 20000, 'seed-payment-key-07'), ('seed-order-08', 20000, 'seed-payment-key-08'), ('seed-order-09', 20000, 'seed-payment-key-09'), ('seed-order-10', 20000, 'seed-payment-key-10'),
('seed-order-11', 20000, 'seed-payment-key-11'), ('seed-order-12', 20000, 'seed-payment-key-12'), ('seed-order-13', 20000, 'seed-payment-key-13'), ('seed-order-14', 20000, 'seed-payment-key-14'), ('seed-order-15', 20000, 'seed-payment-key-15'),
('seed-order-16', 20000, 'seed-payment-key-16'), ('seed-order-17', 20000, 'seed-payment-key-17'), ('seed-order-18', 20000, 'seed-payment-key-18'), ('seed-order-19', 20000, 'seed-payment-key-19'), ('seed-order-20', 20000, 'seed-payment-key-20'),
('seed-order-21', 20000, 'seed-payment-key-21'), ('seed-order-22', 20000, 'seed-payment-key-22'), ('seed-order-23', 20000, 'seed-payment-key-23'), ('seed-order-24', 20000, 'seed-payment-key-24'), ('seed-order-25', 20000, 'seed-payment-key-25'),
('seed-order-26', 20000, 'seed-payment-key-26'), ('seed-order-27', 20000, 'seed-payment-key-27');

INSERT INTO reservation (name, reservation_date, status, time_id, theme_id, order_id) VALUES
-- 최근 7일 이내 20개 (findPopularThemes 조회 범위: 오늘-7 ~ 오늘-1, CURRENT_DATE 기준으로 매번 계산)
('Minsu Kim', CURRENT_DATE - 1, 'CONFIRMED', 1, 1, 1),
('Soyeon Lee', CURRENT_DATE - 1, 'CONFIRMED', 2, 2, 2),
('Jihoon Park', CURRENT_DATE - 1, 'CONFIRMED', 3, 3, 3),
('Yujin Choi', CURRENT_DATE - 1, 'CONFIRMED', 4, 4, 4),
('Haneul Jung', CURRENT_DATE - 1, 'CONFIRMED', 5, 5, 5),

('Jimin Han', CURRENT_DATE - 2, 'CONFIRMED', 1, 6, 6),
('Sehun Oh', CURRENT_DATE - 2, 'CONFIRMED', 2, 7, 7),
('Areum Yoon', CURRENT_DATE - 2, 'CONFIRMED', 3, 8, 8),
('Doyoon Kang', CURRENT_DATE - 2, 'CONFIRMED', 4, 9, 9),
('Yerin Shin', CURRENT_DATE - 2, 'CONFIRMED', 5, 10, 10),

('Jaehyun Lim', CURRENT_DATE - 3, 'CONFIRMED', 1, 11, 11),
('Nayeon Song', CURRENT_DATE - 3, 'CONFIRMED', 2, 12, 12),
('Hyunwoo Jo', CURRENT_DATE - 3, 'CONFIRMED', 3, 13, 13),
('Sujin Baek', CURRENT_DATE - 3, 'CONFIRMED', 4, 14, 14),
('Jiho Moon', CURRENT_DATE - 3, 'CONFIRMED', 5, 15, 15),

('Daeun Seo', CURRENT_DATE - 4, 'CONFIRMED', 1, 2, 16),
('Minjae Kwon', CURRENT_DATE - 5, 'CONFIRMED', 2, 4, 17),
('Jisu Nam', CURRENT_DATE - 6, 'CONFIRMED', 3, 6, 18),
('Yejun Hong', CURRENT_DATE - 7, 'CONFIRMED', 4, 8, 19),
('Dain Yoo', CURRENT_DATE - 7, 'CONFIRMED', 5, 10, 20),

-- 7일 이전 7개 (인기 테마 집계 범위 밖 - 제외되는지 확인용)
('Taeyoon Jang', CURRENT_DATE - 8, 'CONFIRMED', 1, 3, 21),
('Seojin Noh', CURRENT_DATE - 10, 'CONFIRMED', 2, 5, 22),
('Siwoo Ryu', CURRENT_DATE - 12, 'CONFIRMED', 3, 7, 23),
('Gaeun Bae', CURRENT_DATE - 15, 'CONFIRMED', 4, 9, 24),
('Hyunseo Ahn', CURRENT_DATE - 20, 'CONFIRMED', 5, 11, 25),
('Mina Koo', CURRENT_DATE - 30, 'CONFIRMED', 1, 13, 26),
('Dohyun Cha', CURRENT_DATE - 45, 'CONFIRMED', 2, 15, 27);
