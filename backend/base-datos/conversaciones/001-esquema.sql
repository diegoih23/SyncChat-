CREATE TABLE IF NOT EXISTS conversaciones (
    id UUID PRIMARY KEY,
    nombre VARCHAR(160) NOT NULL,
    creado_por UUID NOT NULL,
    destinatario_id UUID,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
