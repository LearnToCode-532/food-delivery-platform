CREATE TABLE restaurants (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(1000) NULL,
    address VARCHAR(500) NULL,
    phone_number VARCHAR(30) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_restaurants_name_id (name, id)
);
