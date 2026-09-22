-- ============================================================
-- Миграция V8: Добавление недостающих индексов для фильтров
-- Дата: 2026-09-07
-- Описание: Индексы для полей, используемых в фильтрации
-- ============================================================

-- ============================================================
-- 1. childrenpr_posts (Детские товары)
-- ============================================================
CREATE INDEX idx_childrenpr_gender ON childrenpr_posts(gender_childrenpr(191));
CREATE INDEX idx_childrenpr_condition ON childrenpr_posts(condition_childrenpr(191));

-- ============================================================
-- 2. clothing_posts (Одежда) - добавляем недостающий
-- ============================================================
-- already: idx_clothing_type, idx_clothing_gender
CREATE INDEX idx_clothing_condition ON clothing_posts(condition_clothing(191));

-- ============================================================
-- 3. constructions_posts (Стройматериалы)
-- ============================================================
CREATE INDEX idx_constructions_type ON constructions_posts(constructions_type(191));

-- ============================================================
-- 4. electronics_posts (Электроника) - добавляем недостающий
-- ============================================================
-- already: idx_electronics_type
CREATE INDEX idx_electronics_condition ON electronics_posts(condition_electronics(191));

-- ============================================================
-- 5. food_posts (Продукты)
-- ============================================================
CREATE INDEX idx_food_kind ON food_posts(food_kind(191));

-- ============================================================
-- 6. health_posts (Здоровье) - добавляем недостающие
-- ============================================================
-- already: idx_health_kind
CREATE INDEX idx_health_type ON health_posts(health_type(191));
CREATE INDEX idx_health_gender ON health_posts(gender_health(191));

-- ============================================================
-- 7. interior_posts (Интерьер)
-- ============================================================
CREATE INDEX idx_interior_type ON interior_posts(interior_type(191));
CREATE INDEX idx_interior_condition ON interior_posts(condition_interior(191));

-- ============================================================
-- 8. sports_posts (Спорт) - добавляем недостающие
-- ============================================================
-- already: idx_sports_kind
CREATE INDEX idx_sports_type ON sports_posts(sports_type(191));
CREATE INDEX idx_sports_gender ON sports_posts(gender_sports(191));
CREATE INDEX idx_sports_condition ON sports_posts(condition_sports(191));

-- ============================================================
-- 9. tools_posts (Инструменты) - добавляем недостающий
-- ============================================================
-- already: idx_tools_type
CREATE INDEX idx_tools_condition ON tools_posts(condition_tools(191));

-- ============================================================
-- 10. Составные индексы с ограничением длины
-- ============================================================

-- Для clothing: поиск по типу + полу + состоянию
-- Используем префиксы для VARCHAR колонок
CREATE INDEX idx_clothing_combo ON clothing_posts(
                                                  clothing_type(100),
                                                  gender_clothing(50),
                                                  condition_clothing(50)
    );

-- Для sports: поиск по виду + типу + полу + состоянию
CREATE INDEX idx_sports_combo ON sports_posts(
                                              sports_kind(100),
                                              sports_type(50),
                                              gender_sports(50),
                                              condition_sports(50)
    );

-- Для health: поиск по виду + типу + полу
CREATE INDEX idx_health_combo ON health_posts(
                                              health_kind(100),
                                              health_type(50),
                                              gender_health(50)
    );

-- Для childrenpr: поиск по полу + состоянию
CREATE INDEX idx_childrenpr_combo ON childrenpr_posts(
                                                      gender_childrenpr(50),
                                                      condition_childrenpr(50)
    );

-- ============================================================
-- 11. Проверка успешности
-- ============================================================
SELECT 'Migration V5 completed: All missing indexes added!' AS Status;