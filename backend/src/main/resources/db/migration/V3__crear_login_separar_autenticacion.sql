-- ============================================================
-- Migración Manual: Tabla Login (autenticación separada del perfil)
-- El estado del login y del usuario es FK a `estados`.
-- Ejecutar UNA sola vez contra la BD existente.
-- (No hay Flyway configurado; ddl-auto=update ya crea las columnas.)
-- ============================================================

-- 1) Asegurar que existan los estados de LOGIN y USUARIO
INSERT INTO estados (nombre_estado, descripcion_estado, tipo_estado)
SELECT 'Activo',    'Estado inicial de la cuenta de acceso', 'LOGIN'
WHERE NOT EXISTS (SELECT 1 FROM estados WHERE tipo_estado = 'LOGIN' AND LOWER(nombre_estado) = 'activo');

INSERT INTO estados (nombre_estado, descripcion_estado, tipo_estado)
SELECT 'Inactivo',  'Cuenta desactivada por el administrador', 'LOGIN'
WHERE NOT EXISTS (SELECT 1 FROM estados WHERE tipo_estado = 'LOGIN' AND LOWER(nombre_estado) = 'inactivo');

INSERT INTO estados (nombre_estado, descripcion_estado, tipo_estado)
SELECT 'Bloqueado', 'Cuenta bloqueada por intentos fallidos', 'LOGIN'
WHERE NOT EXISTS (SELECT 1 FROM estados WHERE tipo_estado = 'LOGIN' AND LOWER(nombre_estado) = 'bloqueado');

INSERT INTO estados (nombre_estado, descripcion_estado, tipo_estado)
SELECT 'Activo',    'Perfil de usuario activo', 'USUARIO'
WHERE NOT EXISTS (SELECT 1 FROM estados WHERE tipo_estado = 'USUARIO' AND LOWER(nombre_estado) = 'activo');

INSERT INTO estados (nombre_estado, descripcion_estado, tipo_estado)
SELECT 'Inactivo',  'Perfil de usuario inactivo', 'USUARIO'
WHERE NOT EXISTS (SELECT 1 FROM estados WHERE tipo_estado = 'USUARIO' AND LOWER(nombre_estado) = 'inactivo');

INSERT INTO estados (nombre_estado, descripcion_estado, tipo_estado)
SELECT 'Bloqueado', 'Perfil de usuario bloqueado', 'USUARIO'
WHERE NOT EXISTS (SELECT 1 FROM estados WHERE tipo_estado = 'USUARIO' AND LOWER(nombre_estado) = 'bloqueado');

-- 2) Poblar `login` a partir de `usuarios` (credenciales históricas)
--    El estado se mapea desde el estado USUARIO antiguo al estado LOGIN equivalente.
INSERT INTO login (id_usuario, email_usuario, contrasena_usuario, id_estado, intentos_fallidos, created_at, updated_at)
SELECT u.id_usuario,
       u.email_usuario,
       u.contrasena_usuario,
       le.id_estado,
       0,
       NOW(),
       NOW()
FROM usuarios u
LEFT JOIN estados ue ON ue.id_estado = u.id_estado
LEFT JOIN estados le ON le.tipo_estado = 'LOGIN'
                    AND LOWER(le.nombre_estado) = CASE
                        WHEN ue.nombre_estado ILIKE '%bloqu%' THEN 'bloqueado'
                        WHEN ue.nombre_estado ILIKE '%inactiv%' THEN 'inactivo'
                        ELSE 'activo'
                    END
WHERE NOT EXISTS (
    SELECT 1 FROM login l WHERE l.id_usuario = u.id_usuario
)
ON CONFLICT (id_usuario) DO NOTHING;

-- 3) Columnas de credenciales que ya viven SOLO en `login`:
--    se limpian de `usuarios`. La FK id_estado se CONSERVA (estado del perfil).
ALTER TABLE usuarios ALTER COLUMN email_usuario DROP NOT NULL;
ALTER TABLE usuarios ALTER COLUMN contrasena_usuario DROP NOT NULL;

ALTER TABLE usuarios DROP COLUMN IF EXISTS email_usuario;
ALTER TABLE usuarios DROP COLUMN IF EXISTS contrasena_usuario;

-- 4) Limpieza: si una versión anterior de esta migración creó `activo`,
--    se elimina porque el estado del perfil ahora vive en `id_estado`.
ALTER TABLE usuarios DROP COLUMN IF EXISTS activo;

-- 5) Restaurar intentos/bloqueos desde el modelo antiguo (si aún existe la tabla)
ALTER TABLE login ADD COLUMN IF NOT EXISTS fecha_bloqueo TIMESTAMP;
UPDATE login l
SET intentos_fallidos = ub.intentos_fallidos,
    fecha_bloqueo     = ub.fecha_bloqueo,
    id_estado         = COALESCE((SELECT le.id_estado FROM estados le
                                   WHERE le.tipo_estado = 'LOGIN' AND LOWER(le.nombre_estado) = 'bloqueado'),
                                  l.id_estado)
FROM usuarios_bloqueo ub
WHERE ub.id_usuario = l.id_usuario
  AND ub.fecha_bloqueo IS NOT NULL;

DROP TABLE IF EXISTS usuarios_bloqueo;