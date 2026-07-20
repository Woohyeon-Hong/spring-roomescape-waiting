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
('우주선 탈출', '고장 난 우주선에서 제한 시간 안에 탈출하세요.', 'https://example.com/themes/space-escape.jpg', 20000),
('좀비 아포칼립스', '봉쇄된 도시에서 생존 키트를 찾아 탈출해야 합니다.', 'https://example.com/themes/zombie-apocalypse.jpg', 20000),
('고대 피라미드', '피라미드 깊숙한 곳의 비밀 방을 열어 보물을 찾으세요.', 'https://example.com/themes/pyramid.jpg', 20000),
('마법학교의 비밀', '사라진 마법서를 찾아 학교의 저주를 풀어야 합니다.', 'https://example.com/themes/magic-school.jpg', 20000),
( '해적선의 보물', '해적선 선장의 단서를 모아 숨겨진 보물창고를 여세요.', 'https://example.com/themes/pirate-treasure.jpg', 20000),
('미스터리 연구소', '폐쇄된 연구소에서 실험 기록을 복구하고 탈출하세요.', 'https://example.com/themes/lab-mystery.jpg', 20000),
( '시간여행자', '뒤틀린 시간 장치를 복구해 현재로 돌아오세요.', 'https://example.com/themes/time-traveler.jpg', 20000),
( '유령의 저택', '밤이 끝나기 전 저택의 원혼을 달래는 의식을 완성하세요.', 'https://example.com/themes/haunted-mansion.jpg', 20000),
( '사라진 화가의 작품', '실종된 화가가 남긴 암호를 풀어 진짜 작품을 찾으세요.', 'https://example.com/themes/missing-painting.jpg', 20000),
( '심해 탐험', '산소가 떨어지기 전에 심해 기지의 전원을 복구해야 합니다.', 'https://example.com/themes/deep-sea.jpg', 20000),
( '왕실 음모', '왕궁에서 벌어진 음모의 증거를 찾아 누명을 벗기세요.', 'https://example.com/themes/royal-conspiracy.jpg', 20000),
( '폐병원 탈출', '버려진 병원에서 수상한 흔적을 추적해 출구를 찾으세요.', 'https://example.com/themes/abandoned-hospital.jpg', 20000),
('한밤중의 서커스', '멈춰버린 서커스 공연의 비밀을 밝히고 무대를 탈출하세요.', 'https://example.com/themes/midnight-circus.jpg', 20000),
('비밀 요원 작전', '이중 잠금 장치를 해제하고 기밀 문서를 회수하세요.', 'https://example.com/themes/secret-agent.jpg', 20000),
('드래곤의 동굴', '드래곤이 잠든 사이 고대 룬을 해독해 동굴을 빠져나오세요.', 'https://example.com/themes/dragon-cave.jpg', 20000);

-- 예약 27건과 1:1로 대응하는 주문 (order_id 1~27은 아래 reservation insert의 행 순서와 일치), 이미 결제 확정된 것으로 시딩
INSERT INTO orders (order_id, amount, is_confirmed) VALUES
('seed-order-01', 20000, true), ('seed-order-02', 20000, true), ('seed-order-03', 20000, true), ('seed-order-04', 20000, true), ('seed-order-05', 20000, true),
('seed-order-06', 20000, true), ('seed-order-07', 20000, true), ('seed-order-08', 20000, true), ('seed-order-09', 20000, true), ('seed-order-10', 20000, true),
('seed-order-11', 20000, true), ('seed-order-12', 20000, true), ('seed-order-13', 20000, true), ('seed-order-14', 20000, true), ('seed-order-15', 20000, true),
('seed-order-16', 20000, true), ('seed-order-17', 20000, true), ('seed-order-18', 20000, true), ('seed-order-19', 20000, true), ('seed-order-20', 20000, true),
('seed-order-21', 20000, true), ('seed-order-22', 20000, true), ('seed-order-23', 20000, true), ('seed-order-24', 20000, true), ('seed-order-25', 20000, true),
('seed-order-26', 20000, true), ('seed-order-27', 20000, true);

INSERT INTO reservation (name, reservation_date, status, time_id, theme_id, order_id) VALUES
-- 최근 7일 이내 20개 (기준: 2026-05-06)
('Minsu Kim', '2026-05-05', 'CONFIRMED', 1, 1, 1),
('Soyeon Lee', '2026-05-05', 'CONFIRMED', 2, 2, 2),
('Jihoon Park', '2026-05-05', 'CONFIRMED', 3, 3, 3),
('Yujin Choi', '2026-05-05', 'CONFIRMED', 4, 4, 4),
('Haneul Jung', '2026-05-05', 'CONFIRMED', 5, 5, 5),

('Jimin Han', '2026-05-04', 'CONFIRMED', 1, 6, 6),
('Sehun Oh', '2026-05-04', 'CONFIRMED', 2, 7, 7),
('Areum Yoon', '2026-05-04', 'CONFIRMED', 3, 8, 8),
('Doyoon Kang', '2026-05-04', 'CONFIRMED', 4, 9, 9),
('Yerin Shin', '2026-05-04', 'CONFIRMED', 5, 10, 10),

('Jaehyun Lim', '2026-05-03', 'CONFIRMED', 1, 11, 11),
('Nayeon Song', '2026-05-03', 'CONFIRMED', 2, 12, 12),
('Hyunwoo Jo', '2026-05-03', 'CONFIRMED', 3, 13, 13),
('Sujin Baek', '2026-05-03', 'CONFIRMED', 4, 14, 14),
('Jiho Moon', '2026-05-03', 'CONFIRMED', 5, 15, 15),

('Daeun Seo', '2026-05-02', 'CONFIRMED', 1, 2, 16),
('Minjae Kwon', '2026-05-01', 'CONFIRMED', 2, 4, 17),
('Jisu Nam', '2026-04-30', 'CONFIRMED', 3, 6, 18),
('Yejun Hong', '2026-04-29', 'CONFIRMED', 4, 8, 19),
('Dain Yoo', '2026-04-29', 'CONFIRMED', 5, 10, 20),

-- 7일 이전 7개
('Taeyoon Jang', '2026-04-28', 'CONFIRMED', 1, 3, 21),
('Seojin Noh', '2026-04-26', 'CONFIRMED', 2, 5, 22),
('Siwoo Ryu', '2026-04-24', 'CONFIRMED', 3, 7, 23),
('Gaeun Bae', '2026-04-21', 'CONFIRMED', 4, 9, 24),
('Hyunseo Ahn', '2026-04-16', 'CONFIRMED', 5, 11, 25),
('Mina Koo', '2026-04-06', 'CONFIRMED', 1, 13, 26),
('Dohyun Cha', '2026-03-22', 'CONFIRMED', 2, 15, 27);
