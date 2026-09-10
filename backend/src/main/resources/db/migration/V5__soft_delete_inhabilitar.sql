-- ============================================================
-- Migración Manual: Soft-delete (eliminar) vs inhabilitar
--  - 'Inhabilitar'  -> estado 'Inactivo'  (permanece visible)
--  - 'Eliminar'     -> estado 'Eliminado' (soft-delete, se oculta)
--  - Insertar estados 'Eliminado' para USUARIO, LOGIN y REGISTRO_ASISTENCIA
--  - Convertir registros de asistencia marcados 'Inactivo' (soft-delete
--    anterior) al nuevo marcador 'Eliminado'
--  - Agregar columna roles.activo (soft-delete de roles)
-- Ejecutar sobre la BD existente.
-- ============================================================

-- 1) Insertar estados 'Eliminado' (idempotente)
INSERT INTO estados (tipo_estado, nombre_estado, descripcion_estado)
SELECT v.tipo, v.nombre, v.descripcion
FROM (VALUES
    ('USUARIO',            'Eliminado', 'Usuario eliminado (soft-delete)'),
    ('LOGIN',              'Eliminado', 'Login eliminado (soft-delete)'),
    ('REGISTRO_ASISTENCIA','Eliminado', 'Registro eliminado (soft-delete)')
) AS v(tipo, nombre, descripcion)
WHERE NOT EXISTS (
    SELECT 1 FROM estados e
    WHERE e.tipo_estado = v.tipo AND LOWER(e.nombre_estado) = 'eliminado'
);

-- 2) Convertir registros de asistencia inactivados (soft-delete anterior)
--    al nuevo marcador 'Eliminado'
UPDATE registro_asistencia
SET id_estado = (SELECT e2.id_estado FROM estados e2
                 WHERE e2.tipo_estado = 'REGISTRO_ASISTENCIA'
                   AND LOWER(e2.nombre_estado) = 'eliminado'
                 LIMIT 1)
WHERE id_estado = (SELECT e1.id_estado FROM estados e1
                   WHERE e1.tipo_estado = 'REGISTRO_ASISTENCIA'
                     AND LOWER(e1.nombre_estado) = 'inactivo'
                   LIMIT 1);

-- 3) Columna roles.activo (idempotente)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'roles' AND column_name = 'activo') THEN
        ALTER TABLE roles ADD COLUMN activo boolean NOT NULL DEFAULT true;
    END IF;
END $$;