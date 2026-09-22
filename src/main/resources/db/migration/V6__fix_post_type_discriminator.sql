-- V6: Обновление post_type для перенесённых записей

UPDATE post_text pt
SET pt.post_type = 'AUTO'
WHERE pt.post_type = 'DEFAULT'
  AND EXISTS (SELECT 1 FROM auto_posts ap WHERE ap.id = pt.id);