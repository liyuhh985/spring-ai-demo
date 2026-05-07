package com.example.aidemo.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.aidemo.entity.Product;
import com.example.aidemo.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 数据库查询服务
 * 供 AI 调用来查询数据库
 */
@Service
public class DatabaseService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 查询所有商品
     */
    public List<Product> getAllProducts() {
        return productMapper.selectList(null);
    }

    /**
     * 按销售额排序查询商品
     */
    public List<Product> getProductsBySales() {
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("sales");
        return productMapper.selectList(wrapper);
    }

    /**
     * 按价格排序查询商品（从高到低）
     */
    public List<Product> getProductsByPriceDesc() {
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("price");
        return productMapper.selectList(wrapper);
    }

    /**
     * 按分类查询商品
     */
    public List<Product> getProductsByCategory(String category) {
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        wrapper.eq("category", category);
        return productMapper.selectList(wrapper);
    }

    /**
     * 获取销售统计
     */
    public Map<String, Object> getSalesSummary() {
        String sql = "SELECT COUNT(*) as total_products, SUM(sales) as total_sales, AVG(price) as avg_price FROM product";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
        return result.get(0);
    }
}