--liquibase formatted sql

--changeset dev:1.0.0-01-create-orders
--comment: Create orders table and indexes if not exists
CREATE TABLE IF NOT EXISTS orders (
                                      id BIGSERIAL PRIMARY KEY,
                                      customer_id VARCHAR(100) NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    failure_reason VARCHAR(64),
    failure_message VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
    );

-- Create indexes for performance (idempotent)
CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);

--rollback DROP TABLE IF EXISTS orders CASCADE;

--changeset dev:1.0.0-02-create-order-items
--comment: Create order_items table and indexes if not exists
CREATE TABLE IF NOT EXISTS order_items (
                                           id BIGSERIAL PRIMARY KEY,
                                           order_id BIGINT NOT NULL,
                                           product_id VARCHAR(100) NOT NULL,
    product_type VARCHAR(32) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    metadata JSONB,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
    REFERENCES orders(id) ON DELETE CASCADE
    );

-- Create indexes for performance (idempotent)
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items(product_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_type ON order_items(product_type);
CREATE INDEX IF NOT EXISTS idx_order_items_metadata ON order_items USING GIN (metadata);

--rollback DROP TABLE IF EXISTS order_items CASCADE;

--changeset dev:1.0.0-03-add-failure-message-to-order-items
--comment: Add failure_message and failure_reason to order_items
ALTER TABLE order_items
    ADD COLUMN IF NOT EXISTS failure_message VARCHAR(255),
    ADD COLUMN IF NOT EXISTS failure_reason  VARCHAR(255);



