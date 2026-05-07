-- 创建商品表
CREATE TABLE IF NOT EXISTS product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    price DECIMAL(10,2),
    stock INT,
    sales INT DEFAULT 0
);

-- 创建订单表
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT,
    amount DECIMAL(10,2),
    quantity INT,
    order_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 插入测试数据 - 商品
INSERT INTO product (name, category, price, stock, sales) VALUES
('iPhone 15', '电子产品', 5999.00, 100, 50),
('MacBook Pro', '电子产品', 12999.00, 30, 15),
('AirPods Pro', '电子产品', 1999.00, 200, 120),
('小米手机', '电子产品', 2999.00, 150, 80),
('联想笔记本', '电子产品', 5999.00, 50, 25),
('运动鞋', '服装', 599.00, 300, 180),
('T恤', '服装', 99.00, 500, 350),
('牛仔裤', '服装', 299.00, 200, 100),
('咖啡机', '家电', 1299.00, 80, 40),
('电饭煲', '家电', 399.00, 120, 60);

-- 插入测试数据 - 订单
INSERT INTO orders (product_id, amount, quantity, order_time) VALUES
(1, 5999.00, 1, '2024-05-01 10:00:00'),
(3, 1999.00, 2, '2024-05-01 11:30:00'),
(2, 12999.00, 1, '2024-05-02 09:15:00'),
(4, 2999.00, 1, '2024-05-02 14:20:00'),
(6, 599.00, 1, '2024-05-03 16:45:00'),
(7, 99.00, 3, '2024-05-03 18:00:00'),
(5, 5999.00, 1, '2024-05-04 10:30:00'),
(8, 299.00, 2, '2024-05-04 15:10:00'),
(9, 1299.00, 1, '2024-05-05 11:00:00'),
(10, 399.00, 2, '2024-05-05 13:30:00'),
(1, 5999.00, 1, '2024-05-06 09:00:00'),
(3, 1999.00, 1, '2024-05-06 14:00:00'),
(2, 12999.00, 1, '2024-05-07 10:30:00'),
(4, 2999.00, 2, '2024-05-07 16:00:00'),
(6, 599.00, 2, '2024-05-07 19:00:00');