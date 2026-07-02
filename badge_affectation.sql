CREATE TABLE IF NOT EXISTS badge_affectation (
    numeroserie  VARCHAR(255) NOT NULL,
    affectee_a   VARCHAR(255) NOT NULL,
    updated_at   TIMESTAMP,
    updated_by   VARCHAR(255),
    printed_at   TIMESTAMP,
    CONSTRAINT badge_affectation_pkey PRIMARY KEY (numeroserie)
);