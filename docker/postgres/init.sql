CREATE TABLE IF NOT EXISTS app_users(
    ID BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    USERNAME TEXT NOT NULL UNIQUE,
    FIRST_NAME TEXT NOT NULL,
    LAST_NAME TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS products(
    ID BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    NAME TEXT NOT NULL,
    DESCRIPTION TEXT,
    PRICE NUMERIC(20, 5) NOT NULL
);

CREATE TABLE IF NOT EXISTS product_reviews(
    ID BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    USER_ID BIGINT NOT NULL,
    PRODUCT_ID BIGINT NOT NULL,
    RATING INTEGER NOT NULL,
    CREATED_AT TIMESTAMP NOT NULL,
    COMMENT TEXT,
    CONSTRAINT product_reviews_app_users_fk FOREIGN KEY (USER_ID) REFERENCES app_users(ID) ON DELETE NO ACTION,
    CONSTRAINT product_reviews_products_fk FOREIGN KEY (PRODUCT_ID) REFERENCES products(ID) ON DELETE NO ACTION
);

CREATE UNIQUE INDEX IF NOT EXISTS product_reviews_user_id_product_id_idx ON product_reviews(USER_ID, PRODUCT_ID);

ALTER TABLE product_reviews REPLICA IDENTITY FULL;

CREATE PUBLICATION product_reviews_publication FOR TABLE product_reviews;

SELECT pg_create_logical_replication_slot('postgres_debezium', 'pgoutput');

INSERT INTO app_users (username, first_name, last_name)
VALUES
    ('Alex', 'Alexander', 'AlexLastName'),
    ('John', 'Doe', 'JohnLastName'),
    ('Bob', 'Bobby', 'BobLastName'),
    ('Kate', 'Katie', 'KateLastName'),
    ('Lex', 'Lexie', 'LexLastName'),
    ('Billy', 'Bill', 'BillLastName');

INSERT INTO products(NAME, DESCRIPTION, PRICE)
VALUES
    ('Product One', 'Description One', 1.0),
    ('Product Two', 'Description Two', 2.0),
    ('Product Three', 'Description Three', 3.0),
    ('Product Four', 'Description Four', 4.0),
    ('Product Five', 'Description Five', 5.0),
    ('Product Six', 'Description Six', 6.0);


