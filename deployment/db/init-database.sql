--Dentro de la conexión de postgres
DO
$$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'capability_user') THEN
        CREATE ROLE capability_user LOGIN PASSWORD 'vaca1234';
    END IF;
END
$$;

CREATE DATABASE onclass_capability OWNER capability_user;
