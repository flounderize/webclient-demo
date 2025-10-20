-- 插入测试用户数据
INSERT INTO user (user_id, username, email, age) VALUES 
('user001', '张三', 'zhangsan@example.com', 28),
('user002', '李四', 'lisi@example.com', 32),
('user003', '王五', 'wangwu@example.com', 25);

-- 插入测试推荐数据
INSERT INTO recommendation (user_id, item_id, score, reason) VALUES 
('user001', 'item001', 0.95, '基于用户历史浏览记录推荐'),
('user001', 'item002', 0.87, '相似用户也喜欢'),
('user002', 'item003', 0.92, '热门商品推荐');
