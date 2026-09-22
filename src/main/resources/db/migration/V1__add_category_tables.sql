-- ============================================================
-- Миграция V1: Добавление категорийных таблиц для товаров
-- ============================================================

-- ============================================================
-- Добавление поля post_type в post_text
-- ============================================================
ALTER TABLE post_text ADD COLUMN post_type VARCHAR(20) DEFAULT 'DEFAULT';
CREATE INDEX idx_post_type ON post_text(post_type);

-- ============================================================
-- realty_posts и vehicle_posts (Hibernate ожидает их через @Table)
-- ============================================================
CREATE TABLE IF NOT EXISTS realty_posts (
                                            id BIGINT NOT NULL PRIMARY KEY,
                                            property_type VARCHAR(255),
    rooms_count INT,
    floors_count INT,
    floors_count_house INT,
    has_elevator BIT(1),
    build_year INT,
    floors_apartment INT,
    material_up VARCHAR(255),
    repair_up VARCHAR(255),
    house_area INT,
    land_area INT,
    material_house VARCHAR(255),
    repair_house VARCHAR(255),
    distance_to_city_house INT,
    distance_to_city INT,
    land_area_garage INT,
    land_area_commercial INT,
    FOREIGN KEY (id) REFERENCES post_text(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS vehicle_posts (
                                             id BIGINT NOT NULL PRIMARY KEY,
                                             vehicle_type VARCHAR(255),
    vehicle_origin VARCHAR(255),
    vehicle_year INT,
    mileage INT,
    number_of_owners INT,
    FOREIGN KEY (id) REFERENCES post_text(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Остальные таблицы-наследники
-- ============================================================
CREATE TABLE IF NOT EXISTS animals_posts (
                                             id BIGINT NOT NULL PRIMARY KEY,
                                             animals_kind VARCHAR(255),
    FOREIGN KEY (id) REFERENCES post_text(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS appliances_posts (
                                                id BIGINT NOT NULL PRIMARY KEY,
                                                appliances_type VARCHAR(255),
    condition_appliances VARCHAR(255),
    FOREIGN KEY (id) REFERENCES post_text(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS auto_posts (
                                          id BIGINT NOT NULL PRIMARY KEY,
                                          auto_type VARCHAR(255),
    condition_auto VARCHAR(255),
    FOREIGN KEY (id) REFERENCES post_text(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS childrenpr_posts (
                                                id BIGINT NOT NULL PRIMARY KEY,
                                                gender_childrenpr VARCHAR(255),
    condition_childrenpr VARCHAR(255),
    FOREIGN KEY (id) REFERENCES post_text(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS clothing_posts (
                                              id BIGINT NOT NULL PRIMARY KEY,
                                              clothing_type VARCHAR(255),
    gender_clothing VARCHAR(255),
    condition_clothing VARCHAR(255),
    FOREIGN KEY (id) REFERENCES post_text(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS constructions_posts (
                                                   id BIGINT NOT NULL PRIMARY KEY,
                                                   constructions_type VARCHAR(255),
    FOREIGN KEY (id) REFERENCES post_text(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS electronics_posts (
                                                 id BIGINT NOT NULL PRIMARY KEY,
                                                 electronics_type VARCHAR(255),
    condition_electronics VARCHAR(255),
    FOREIGN KEY (id) REFERENCES post_text(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS food_posts (
                                          id BIGINT NOT NULL PRIMARY KEY,
                                          food_kind VARCHAR(255),
    FOREIGN KEY (id) REFERENCES post_text(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS health_posts (
                                            id BIGINT NOT NULL PRIMARY KEY,
                                            health_kind VARCHAR(255),
    health_type VARCHAR(255),
    gender_health VARCHAR(255),
    FOREIGN KEY (id) REFERENCES post_text(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS interior_posts (
                                              id BIGINT NOT NULL PRIMARY KEY,
                                              interior_type VARCHAR(255),
    condition_interior VARCHAR(255),
    FOREIGN KEY (id) REFERENCES post_text(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sports_posts (
                                            id BIGINT NOT NULL PRIMARY KEY,
                                            sports_kind VARCHAR(255),
    sports_type VARCHAR(255),
    gender_sports VARCHAR(255),
    condition_sports VARCHAR(255),
    FOREIGN KEY (id) REFERENCES post_text(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tools_posts (
                                           id BIGINT NOT NULL PRIMARY KEY,
                                           tools_type VARCHAR(255),
    condition_tools VARCHAR(255),
    FOREIGN KEY (id) REFERENCES post_text(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Индексы для новых таблиц
-- ============================================================
CREATE INDEX idx_animals_kind ON animals_posts(animals_kind);
CREATE INDEX idx_appliances_type ON appliances_posts(appliances_type);
CREATE INDEX idx_appliances_condition ON appliances_posts(condition_appliances);
CREATE INDEX idx_auto_type ON auto_posts(auto_type);
CREATE INDEX idx_auto_condition ON auto_posts(condition_auto);
CREATE INDEX idx_clothing_type ON clothing_posts(clothing_type);
CREATE INDEX idx_clothing_gender ON clothing_posts(gender_clothing);
CREATE INDEX idx_electronics_type ON electronics_posts(electronics_type);
CREATE INDEX idx_health_kind ON health_posts(health_kind);
CREATE INDEX idx_sports_kind ON sports_posts(sports_kind);
CREATE INDEX idx_tools_type ON tools_posts(tools_type);