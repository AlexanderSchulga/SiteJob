-- #####################################################################
-- Migration 5: Перенос данных во все дочерние таблицы
-- Исключены: realty, motor, sports, farming, books, fleamarket
-- #####################################################################

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. CONSTRUCTIONS (Строительные материалы)
-- ============================================================
INSERT IGNORE INTO constructions_posts (id, constructions_type)
SELECT
    pt.id,
    NULL AS constructions_type
FROM post_text pt
WHERE pt.category = 'constructions';

-- ============================================================
-- 2. AUTO (Автозапчасти и автотовары)
-- ============================================================
INSERT IGNORE INTO auto_posts (id, auto_type, condition_auto)
SELECT
    pt.id,
    NULL AS auto_type,
    NULL AS condition_auto
FROM post_text pt
WHERE pt.category = 'auto';

-- ============================================================
-- 3. INTERIOR (Дом и интерьер)
-- ============================================================
INSERT IGNORE INTO interior_posts (id, interior_type, condition_interior)
SELECT
    pt.id,
    NULL AS interior_type,
    NULL AS condition_interior
FROM post_text pt
WHERE pt.category = 'interior';

-- ============================================================
-- 4. TOOLS (Инструменты)
-- ============================================================
INSERT IGNORE INTO tools_posts (id, tools_type, condition_tools)
SELECT
    pt.id,
    NULL AS tools_type,
    NULL AS condition_tools
FROM post_text pt
WHERE pt.category = 'tools';

-- ============================================================
-- 5. APPLIANCES (Бытовая техника)
-- ============================================================
INSERT IGNORE INTO appliances_posts (id, appliances_type, condition_appliances)
SELECT
    pt.id,
    NULL AS appliances_type,
    NULL AS condition_appliances
FROM post_text pt
WHERE pt.category = 'appliances';

-- ============================================================
-- 6. ELECTRONICS (Электроника и гаджеты)
-- ============================================================
INSERT IGNORE INTO electronics_posts (id, electronics_type, condition_electronics)
SELECT
    pt.id,
    NULL AS electronics_type,
    NULL AS condition_electronics
FROM post_text pt
WHERE pt.category = 'electronics';

-- ============================================================
-- 7. CLOTHING (Одежда и обувь)
-- ============================================================
INSERT IGNORE INTO clothing_posts (id, clothing_type, gender_clothing, condition_clothing)
SELECT
    pt.id,
    NULL AS clothing_type,
    NULL AS gender_clothing,
    NULL AS condition_clothing
FROM post_text pt
WHERE pt.category = 'clothing';

-- ============================================================
-- 8. CHILDREN (Детские товары)
-- ============================================================
INSERT IGNORE INTO childrenpr_posts (id, gender_childrenpr, condition_childrenpr)
SELECT
    pt.id,
    NULL AS gender_childrenpr,
    NULL AS condition_childrenpr
FROM post_text pt
WHERE pt.category = 'childrenPr';

-- ============================================================
-- 9. FOOD (Продукты питания)
-- ============================================================
INSERT IGNORE INTO food_posts (id, food_kind)
SELECT
    pt.id,
    NULL AS food_kind
FROM post_text pt
WHERE pt.category = 'food';

-- ============================================================
-- 10. HEALTH (Здоровье и красота)
-- ============================================================
INSERT IGNORE INTO health_posts (id, health_kind, health_type, gender_health)
SELECT
    pt.id,
    NULL AS health_kind,
    NULL AS health_type,
    NULL AS gender_health
FROM post_text pt
WHERE pt.category = 'health';

-- ============================================================
-- 11. ANIMALS (Флора и фауна)
-- ============================================================
INSERT IGNORE INTO animals_posts (id, animals_kind)
SELECT
    pt.id,
    NULL AS animals_kind
FROM post_text pt
WHERE pt.category = 'animals';

SET FOREIGN_KEY_CHECKS = 1;