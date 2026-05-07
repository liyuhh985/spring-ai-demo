-- 创建商品表
CREATE TABLE IF NOT EXISTS product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    price DECIMAL(10,2),
    stock INT,
    sales INT DEFAULT 0
);

-- 删除已存在的数据（如果有）
DELETE FROM product;

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