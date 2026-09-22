-- #####################################################################
-- Migration 3: Перенос спортивных товаров (Flyway + MySQL)
-- Тип наследования: JOINED
-- СУБД: MySQL / MariaDB
-- #####################################################################


-- 3. Копируем ID объявлений в дочернюю таблицу sports_posts
INSERT IGNORE INTO sports_posts (id, sports_kind, sports_type, gender_sports, condition_sports)
SELECT
    pt.id,
    NULL as sports_kind,
    NULL as sports_type,
    NULL as gender_sports,
    NULL as condition_sports
FROM post_text pt
WHERE pt.category = 'sports';
