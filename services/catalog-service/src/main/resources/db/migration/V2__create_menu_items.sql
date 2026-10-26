CREATE TABLE menu_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    restaurant_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(1000) NULL,
    price DECIMAL(10, 2) NOT NULL,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_menu_items_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurants (id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_menu_items_price_non_negative CHECK (price >= 0),
    INDEX idx_menu_items_restaurant_catalog
        (restaurant_id, deleted_at, available, name, id)
);
