-- Repara filas históricas que pudieron quedar sin backfill durante una actualización.
-- No modifica estados ya cambiados por los usuarios (DECLINED, LEFT, etc.).
INSERT INTO conversacion_participantes
    (id, conversacion_id, usuario_id, estado, administrador, creado_en, actualizado_en)
SELECT gen_random_uuid(), c.id, c.creado_por, 'ACCEPTED', TRUE, c.creado_en, c.creado_en
FROM conversaciones c
ON CONFLICT (conversacion_id, usuario_id) DO NOTHING;

INSERT INTO conversacion_participantes
    (id, conversacion_id, usuario_id, estado, administrador, creado_en, actualizado_en)
SELECT gen_random_uuid(), c.id, c.destinatario_id, 'PENDING', FALSE, c.creado_en, c.creado_en
FROM conversaciones c
WHERE c.destinatario_id IS NOT NULL
  AND c.destinatario_id <> c.creado_por
ON CONFLICT (conversacion_id, usuario_id) DO NOTHING;
