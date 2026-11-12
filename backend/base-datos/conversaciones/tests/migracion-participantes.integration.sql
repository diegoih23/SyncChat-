\set ON_ERROR_STOP on
BEGIN;
CREATE SCHEMA syncchat_migration_test;
SET search_path TO syncchat_migration_test, public;

-- Esquema legado anterior a la columna del destinatario.
CREATE TABLE conversaciones (
    id UUID PRIMARY KEY,
    nombre VARCHAR(160) NOT NULL,
    creado_por UUID NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO conversaciones (id, nombre, creado_por, creado_en) VALUES
    ('10000000-0000-0000-0000-000000000001', 'Grupo con invitado', '20000000-0000-0000-0000-000000000001', '2024-01-01T10:00:00Z'),
    ('10000000-0000-0000-0000-000000000002', 'Solo creador', '20000000-0000-0000-0000-000000000002', '2024-02-01T10:00:00Z'),
    ('10000000-0000-0000-0000-000000000003', 'Auto destinatario', '20000000-0000-0000-0000-000000000003', '2024-03-01T10:00:00Z'),
    ('10000000-0000-0000-0000-000000000004', 'Backfill faltante', '20000000-0000-0000-0000-000000000004', '2024-04-01T10:00:00Z');

\ir ../002-destinatario.sql
UPDATE conversaciones SET destinatario_id = '20000000-0000-0000-0000-000000000099'
WHERE id = '10000000-0000-0000-0000-000000000001';
UPDATE conversaciones SET destinatario_id = NULL
WHERE id = '10000000-0000-0000-0000-000000000002';
UPDATE conversaciones SET destinatario_id = creado_por
WHERE id = '10000000-0000-0000-0000-000000000003';
UPDATE conversaciones SET destinatario_id = '20000000-0000-0000-0000-000000000098'
WHERE id = '10000000-0000-0000-0000-000000000004';

\ir ../003-participantes.sql

-- Simula un proceso que ya registró una respuesta y un backfill incompleto.
UPDATE conversacion_participantes SET estado = 'DECLINED'
WHERE conversacion_id = '10000000-0000-0000-0000-000000000001'
  AND usuario_id = '20000000-0000-0000-0000-000000000099';
DELETE FROM conversacion_participantes
WHERE conversacion_id = '10000000-0000-0000-0000-000000000004'
  AND usuario_id = '20000000-0000-0000-0000-000000000098';

\ir ../004-backfill-participantes.sql
\ir ../003-participantes.sql
\ir ../004-backfill-participantes.sql

DO $$
DECLARE
    participantes_total INTEGER;
    conversaciones_total INTEGER;
BEGIN
    SELECT count(*) INTO conversaciones_total FROM conversaciones;
    SELECT count(*) INTO participantes_total FROM conversacion_participantes;
    IF conversaciones_total <> 4 THEN
        RAISE EXCEPTION 'La migración alteró o perdió conversaciones: %', conversaciones_total;
    END IF;
    IF participantes_total <> 6 THEN
        RAISE EXCEPTION 'Backfill no idempotente o incompleto: se esperaban 6 participantes, hay %', participantes_total;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM conversacion_participantes
        WHERE conversacion_id = '10000000-0000-0000-0000-000000000001'
          AND usuario_id = '20000000-0000-0000-0000-000000000001'
          AND estado = 'ACCEPTED' AND administrador
    ) THEN RAISE EXCEPTION 'El creador no fue migrado como administrador aceptado'; END IF;
    IF NOT EXISTS (
        SELECT 1 FROM conversacion_participantes
        WHERE conversacion_id = '10000000-0000-0000-0000-000000000001'
          AND usuario_id = '20000000-0000-0000-0000-000000000099'
          AND estado = 'DECLINED' AND NOT administrador
    ) THEN RAISE EXCEPTION 'La migración sobrescribió la respuesta del invitado'; END IF;
    IF NOT EXISTS (
        SELECT 1 FROM conversacion_participantes
        WHERE conversacion_id = '10000000-0000-0000-0000-000000000004'
          AND usuario_id = '20000000-0000-0000-0000-000000000098'
          AND estado = 'PENDING' AND NOT administrador
          AND creado_en = '2024-04-01T10:00:00Z'
    ) THEN RAISE EXCEPTION 'El destinatario faltante no se restauró como pendiente'; END IF;
    IF EXISTS (
        SELECT 1 FROM conversacion_participantes
        WHERE conversacion_id = '10000000-0000-0000-0000-000000000003'
          AND usuario_id = '20000000-0000-0000-0000-000000000003'
        GROUP BY conversacion_id, usuario_id HAVING count(*) > 1
    ) THEN RAISE EXCEPTION 'Se duplicó el creador cuando el destinatario es él mismo'; END IF;
END $$;

ROLLBACK;
