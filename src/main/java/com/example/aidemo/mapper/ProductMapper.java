package com.example.aidemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aidemo.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}