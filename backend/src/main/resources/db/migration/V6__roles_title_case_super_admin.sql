-- ============================================================
-- Migración Manual: Nombres de roles en Title Case + Super Administrador
--  - Renombrar los roles existentes a primera letra mayúscula / resto minúsculas
--  - Crear el rol 'Super Administrador' (único que restablece registros)
--  - Asignar el rol 'Super Administrador' al admin por defecto (admin@kineticpulse.com)
--  - Garantizar la columna roles.activo (por si V5 no se ejecutó)
-- TODO es idempotente. Ejecutar sobre la BD existente.
-- ============================================================

-- 1) Renombrar roles existentes a Title Case (idempotente)
UPDATE roles SET nombre_rol = 'Administrador', descripcion = 'Administrador del sistema con acceso total'
 WHERE LOWER(nombre_rol) = 'admin';
UPDATE roles SET nombre_rol = 'Operador', descripcion = 'Puede gestionar eventos, check-in e importaciones'
 WHERE LOWER(nombre_rol) = 'operador';
UPDATE roles SET nombre_rol = 'Monitor', descripcion = 'Acceso de solo lectura al dashboard y reportes'
 WHERE LOWER(nombre_rol) = 'monitor';
UPDATE roles SET nombre_rol = 'Invitado', descripcion = 'Acceso público al catálogo de eventos'
 WHERE LOWER(nombre_rol) = 'invitado';

-- 2) Columna roles.activo por si V5 no se aplicó (idempotente)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'roles' AND column_name = 'activo') THEN
        ALTER TABLE roles ADD COLUMN activo boolean NOT NULL DEFAULT true;
    END IF;
END $$;

-- 3) Insertar rol 'Super Administrador' (idempotente)
INSERT INTO roles (nombre_rol, descripcion, activo)
SELECT 'Super Administrador', 'Rol superior: es el único que puede restablecer registros eliminados', true
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE LOWER(nombre_rol) = 'super administrador'
);

-- 4) Asignar 'Super Administrador' al admin por defecto (idempotente)
INSERT INTO roles_usuarios (id_usuario, id_rol)
SELECT u.id_usuario, sr.id_rol
FROM login l
JOIN usuarios u ON u.id_usuario = l.id_usuario
JOIN roles sr ON LOWER(sr.nombre_rol) = 'super administrador'
WHERE l.email_usuario = 'admin@kineticpulse.com'
  AND NOT EXISTS (
      SELECT 1 FROM roles_usuarios ru
      JOIN roles r ON r.id_rol = ru.id_rol
      WHERE ru.id_usuario = u.id_usuario AND LOWER(r.nombre_rol) = 'super administrador'
  );