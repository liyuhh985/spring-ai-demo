package com.example.aidemo.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {
    @ExcelProperty("ID")
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @ExcelProperty("产品名称")
    private String name;
    
    @ExcelProperty("类别")
    private String category;
    
    @ExcelProperty("价格")
    private BigDecimal price;
    
    @ExcelProperty("库存")
    private Integer stock;
    
    @ExcelProperty("销量")
    private Integer sales;
}