CREATE TABLE IF NOT EXISTS conversacion_participantes (
    id UUID PRIMARY KEY,
    conversacion_id UUID NOT NULL REFERENCES conversaciones(id) ON DELETE CASCADE,
    usuario_id UUID NOT NULL,
    estado VARCHAR(16) NOT NULL CHECK (estado IN ('PENDING', 'ACCEPTED', 'DECLINED', 'LEFT', 'REMOVED')),
    administrador BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_conversacion_participante UNIQUE (conversacion_id, usuario_id)
);

CREATE INDEX IF NOT EXISTS ix_participantes_usuario_estado
    ON conversacion_participantes (usuario_id, estado);

-- Migra los chats existentes sin conceder acceso automáticamente a los antiguos destinatarios.
INSERT INTO conversacion_participantes
    (id, conversacion_id, usuario_id, estado, administrador)
SELECT gen_random_uuid(), c.id, c.creado_por, 'ACCEPTED', TRUE
FROM conversaciones c
ON CONFLICT (conversacion_id, usuario_id) DO NOTHING;

INSERT INTO conversacion_participantes
    (id, conversacion_id, usuario_id, estado, administrador)
SELECT gen_random_uuid(), c.id, c.destinatario_id, 'PENDING', FALSE
FROM conversaciones c
WHERE c.destinatario_id IS NOT NULL
  AND c.destinatario_id <> c.creado_por
ON CONFLICT (conversacion_id, usuario_id) DO NOTHING;
