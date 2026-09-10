-- ============================================================
-- Migración Manual: Normalización de estados
--  - Nombres de estado -> "Mayúscula inicial por palabra, resto minúsculas"
--    (Ej: 'ACTIVO' -> 'Activo', 'COMPLETADO_CON_ERRORES' -> 'Completado Con Errores')
--  - Renombra columnas inconsistentes: id_estados -> id_estado,
--    "Descripcion_estado" -> descripcion_estado
--  - Índice único normalizado sobre (tipo_estado, LOWER(nombre_estado))
-- Ejecutar sobre la BD existente (diseño post V3).
-- ============================================================

-- 1) Unificar el caso de los nombres de estado (PostgreSQL INITCAP)
UPDATE estados
SET nombre_estado = INITCAP(REPLACE(TRIM(nombre_estado), '_', ' '))
WHERE nombre_estado IS NOT NULL
  AND nombre_estado <> INITCAP(REPLACE(TRIM(nombre_estado), '_', ' '));

-- 2) Renombrar RegistroAsistencia.id_estados -> id_estado (idempotente)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'registro_asistencia'
                 AND column_name = 'id_estados')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_name = 'registro_asistencia'
                         AND column_name = 'id_estado') THEN
        ALTER TABLE registro_asistencia RENAME COLUMN id_estados TO id_estado;
    END IF;
END $$;

-- 3) Renombrar Estado."Descripcion_estado" -> descripcion_estado (idempotente)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'estados'
                 AND column_name = 'Descripcion_estado')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_name = 'estados'
                         AND column_name = 'descripcion_estado') THEN
        ALTER TABLE estados RENAME COLUMN "Descripcion_estado" TO descripcion_estado;
    END IF;
END $$;

-- 4) Índice único normalizado (case-insensitive) sobre (tipo_estado, nombre_estado)
--    El nombre del índice difiere del de la constraint que genera Hibernate.
CREATE UNIQUE INDEX IF NOT EXISTS uk_estados_tipo_nombre_idx
    ON estados (tipo_estado, LOWER(nombre_estado));

-- 5) Precaución con duplicados históricos antes del índice (si aplica)
--    Si el paso 4 fallara por duplicados, eliminar los registros duplicados
--    conservando el de menor id:
--    DELETE FROM estados e
--    WHERE e.id_estado NOT IN (
--        SELECT MIN(id_estado) FROM estados GROUP BY tipo_estado, LOWER(nombre_estado)
--    );