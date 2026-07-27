--Dentro de la nueva conexión de onclass_capability

CREATE SCHEMA IF NOT EXISTS onclass_capability AUTHORIZATION capability_user;

CREATE TABLE IF NOT EXISTS onclass_capability.capabilities (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(50) NOT NULL,
    description      VARCHAR(90) NOT NULL,
    status           VARCHAR(10) NOT NULL DEFAULT 'CREATING',
    technology_count INTEGER NOT NULL DEFAULT 0,
    version          BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS capabilities_name_lower_unique_idx
    ON onclass_capability.capabilities (LOWER(name));

CREATE TABLE IF NOT EXISTS onclass_capability.capability_bootcamps (
    id            BIGSERIAL PRIMARY KEY,
    bootcamp_id   BIGINT NOT NULL,
    capability_id BIGINT NOT NULL REFERENCES onclass_capability.capabilities(id),
    UNIQUE (bootcamp_id, capability_id)
);
