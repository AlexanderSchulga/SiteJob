

-- 1. Убираем NOT NULL с phone (регистрация без телефона)
ALTER TABLE users MODIFY COLUMN phone VARCHAR(255) NULL;

-- 2. Добавляем UNIQUE на nameuse (никнейм уникален)
ALTER TABLE users ADD CONSTRAINT uk_users_nameuse UNIQUE (nameuse);