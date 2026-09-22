-- ============================================================
-- Миграция V2: Перенос товаров из post_text в категорийные таблицы
-- Дата: 2026-08-30
-- ============================================================

-- ============================================================
-- 1. Перенос существующих товаров по категориям
-- ============================================================

-- 1.1 Недвижимость (REALTY) - уже есть в realty_posts
-- Если данные уже есть, пропускаем, иначе переносим
INSERT IGNORE INTO realty_posts (id, property_type, rooms_count, floors_count,
    floors_count_house, has_elevator, build_year, floors_apartment,
    material_up, repair_up, house_area, land_area, material_house,
    repair_house, distance_to_city_house, distance_to_city,
    land_area_garage, land_area_commercial)
SELECT
    pt.id,
    NULL, -- property_type (будет заполнено позже)
    NULL, -- rooms_count
    NULL, -- floors_count
    NULL, -- floors_count_house
    NULL, -- has_elevator
    NULL, -- build_year
    NULL, -- floors_apartment
    NULL, -- material_up
    NULL, -- repair_up
    NULL, -- house_area
    NULL, -- land_area
    NULL, -- material_house
    NULL, -- repair_house
    NULL, -- distance_to_city_house
    NULL, -- distance_to_city
    NULL, -- land_area_garage
    NULL  -- land_area_commercial
FROM post_text pt
WHERE pt.post_type = 'REALTY'
AND NOT EXISTS (SELECT 1 FROM realty_posts rp WHERE rp.id = pt.id);

-- 1.2 Транспорт (VEHICLE) - уже есть в vehicle_posts
INSERT IGNORE INTO vehicle_posts (id, vehicle_type, vehicle_origin, vehicle_year,
    mileage, number_of_owners)
SELECT
    pt.id,
    NULL, -- vehicle_type
    NULL, -- vehicle_origin
    NULL, -- vehicle_year
    NULL, -- mileage
    NULL  -- number_of_owners
FROM post_text pt
WHERE pt.post_type = 'VEHICLE'
AND NOT EXISTS (SELECT 1 FROM vehicle_posts vp WHERE vp.id = pt.id);

-- 1.3 Животные (ANIMALS)
INSERT IGNORE INTO animals_posts (id, animals_kind)
SELECT
    pt.id,
    NULL -- animals_kind
FROM post_text pt
WHERE pt.post_type = 'ANIMALS'
AND NOT EXISTS (SELECT 1 FROM animals_posts ap WHERE ap.id = pt.id);

-- 1.4 Бытовая техника (APPLIANCES)
INSERT IGNORE INTO appliances_posts (id, appliances_type, condition_appliances)
SELECT
    pt.id,
    NULL, -- appliances_type
    NULL  -- condition_appliances
FROM post_text pt
WHERE pt.post_type = 'APPLIANCES'
AND NOT EXISTS (SELECT 1 FROM appliances_posts ap WHERE ap.id = pt.id);

-- 1.5 Авто (AUTO)
INSERT IGNORE INTO auto_posts (id, auto_type, condition_auto)
SELECT
    pt.id,
    NULL, -- auto_type
    NULL  -- condition_auto
FROM post_text pt
WHERE pt.post_type = 'AUTO'
AND NOT EXISTS (SELECT 1 FROM auto_posts ap WHERE ap.id = pt.id);

-- 1.6 Детские товары (CHILDRENPR)
INSERT IGNORE INTO childrenpr_posts (id, gender_childrenpr, condition_childrenpr)
SELECT
    pt.id,
    NULL, -- gender_childrenpr
    NULL  -- condition_childrenpr
FROM post_text pt
WHERE pt.post_type = 'CHILDRENPR'
AND NOT EXISTS (SELECT 1 FROM childrenpr_posts cp WHERE cp.id = pt.id);

-- 1.7 Одежда (CLOTHING)
INSERT IGNORE INTO clothing_posts (id, clothing_type, gender_clothing, condition_clothing)
SELECT
    pt.id,
    NULL, -- clothing_type
    NULL, -- gender_clothing
    NULL  -- condition_clothing
FROM post_text pt
WHERE pt.post_type = 'CLOTHING'
AND NOT EXISTS (SELECT 1 FROM clothing_posts cp WHERE cp.id = pt.id);

-- 1.8 Стройматериалы (CONSTRUCTIONS)
INSERT IGNORE INTO constructions_posts (id, constructions_type)
SELECT
    pt.id,
    NULL -- constructions_type
FROM post_text pt
WHERE pt.post_type = 'CONSTRUCTIONS'
AND NOT EXISTS (SELECT 1 FROM constructions_posts cp WHERE cp.id = pt.id);

-- 1.9 Электроника (ELECTRONICS)
INSERT IGNORE INTO electronics_posts (id, electronics_type, condition_electronics)
SELECT
    pt.id,
    NULL, -- electronics_type
    NULL  -- condition_electronics
FROM post_text pt
WHERE pt.post_type = 'ELECTRONICS'
AND NOT EXISTS (SELECT 1 FROM electronics_posts ep WHERE ep.id = pt.id);

-- 1.10 Продукты (FOOD)
INSERT IGNORE INTO food_posts (id, food_kind)
SELECT
    pt.id,
    NULL -- food_kind
FROM post_text pt
WHERE pt.post_type = 'FOOD'
AND NOT EXISTS (SELECT 1 FROM food_posts fp WHERE fp.id = pt.id);

-- 1.11 Здоровье (HEALTH)
INSERT IGNORE INTO health_posts (id, health_kind, health_type, gender_health)
SELECT
    pt.id,
    NULL, -- health_kind
    NULL, -- health_type
    NULL  -- gender_health
FROM post_text pt
WHERE pt.post_type = 'HEALTH'
AND NOT EXISTS (SELECT 1 FROM health_posts hp WHERE hp.id = pt.id);

-- 1.12 Интерьер (INTERIOR)
INSERT IGNORE INTO interior_posts (id, interior_type, condition_interior)
SELECT
    pt.id,
    NULL, -- interior_type
    NULL  -- condition_interior
FROM post_text pt
WHERE pt.post_type = 'INTERIOR'
AND NOT EXISTS (SELECT 1 FROM interior_posts ip WHERE ip.id = pt.id);

-- 1.13 Спорт (SPORTS)
INSERT IGNORE INTO sports_posts (id, sports_kind, sports_type, gender_sports, condition_sports)
SELECT
    pt.id,
    NULL, -- sports_kind
    NULL, -- sports_type
    NULL, -- gender_sports
    NULL  -- condition_sports
FROM post_text pt
WHERE pt.post_type = 'SPORTS'
AND NOT EXISTS (SELECT 1 FROM sports_posts sp WHERE sp.id = pt.id);

-- 1.14 Инструменты (TOOLS)
INSERT IGNORE INTO tools_posts (id, tools_type, condition_tools)
SELECT
    pt.id,
    NULL, -- tools_type
    NULL  -- condition_tools
FROM post_text pt
WHERE pt.post_type = 'TOOLS'
AND NOT EXISTS (SELECT 1 FROM tools_posts tp WHERE tp.id = pt.id);

-- ============================================================
-- 2. Проверка результата
-- ============================================================

SELECT '✅ Миграция V2 выполнена!' AS Status;

-- Статистика по переносу
SELECT
    'REALTY' AS Category,
    COUNT(*) AS Total,
    (SELECT COUNT(*) FROM realty_posts) AS Migrated
FROM post_text WHERE post_type = 'REALTY'
UNION ALL
SELECT
    'VEHICLE',
    COUNT(*),
    (SELECT COUNT(*) FROM vehicle_posts)
FROM post_text WHERE post_type = 'VEHICLE'
UNION ALL
SELECT
    'ANIMALS',
    COUNT(*),
    (SELECT COUNT(*) FROM animals_posts)
FROM post_text WHERE post_type = 'ANIMALS'
UNION ALL
SELECT
    'APPLIANCES',
    COUNT(*),
    (SELECT COUNT(*) FROM appliances_posts)
FROM post_text WHERE post_type = 'APPLIANCES'
UNION ALL
SELECT
    'AUTO',
    COUNT(*),
    (SELECT COUNT(*) FROM auto_posts)
FROM post_text WHERE post_type = 'AUTO'
UNION ALL
SELECT
    'CHILDRENPR',
    COUNT(*),
    (SELECT COUNT(*) FROM childrenpr_posts)
FROM post_text WHERE post_type = 'CHILDRENPR'
UNION ALL
SELECT
    'CLOTHING',
    COUNT(*),
    (SELECT COUNT(*) FROM clothing_posts)
FROM post_text WHERE post_type = 'CLOTHING'
UNION ALL
SELECT
    'CONSTRUCTIONS',
    COUNT(*),
    (SELECT COUNT(*) FROM constructions_posts)
FROM post_text WHERE post_type = 'CONSTRUCTIONS'
UNION ALL
SELECT
    'ELECTRONICS',
    COUNT(*),
    (SELECT COUNT(*) FROM electronics_posts)
FROM post_text WHERE post_type = 'ELECTRONICS'
UNION ALL
SELECT
    'FOOD',
    COUNT(*),
    (SELECT COUNT(*) FROM food_posts)
FROM post_text WHERE post_type = 'FOOD'
UNION ALL
SELECT
    'HEALTH',
    COUNT(*),
    (SELECT COUNT(*) FROM health_posts)
FROM post_text WHERE post_type = 'HEALTH'
UNION ALL
SELECT
    'INTERIOR',
    COUNT(*),
    (SELECT COUNT(*) FROM interior_posts)
FROM post_text WHERE post_type = 'INTERIOR'
UNION ALL
SELECT
    'SPORTS',
    COUNT(*),
    (SELECT COUNT(*) FROM sports_posts)
FROM post_text WHERE post_type = 'SPORTS'
UNION ALL
SELECT
    'TOOLS',
    COUNT(*),
    (SELECT COUNT(*) FROM tools_posts)
FROM post_text WHERE post_type = 'TOOLS';