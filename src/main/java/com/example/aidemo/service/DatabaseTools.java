package com.example.aidemo.service;

import com.alibaba.excel.EasyExcel;
import com.example.aidemo.entity.Product;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Database Tools - Spring AI 1.0.0-M4 Function Calling 实现
 */
@Component
public class DatabaseTools {

    private final DatabaseService databaseService;

    public DatabaseTools(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    /**
     * 获取所有可用的 Tool Callbacks（给 ChatClient 用）
     */
    public List<FunctionCallback> getTools() {
        return List.of(
            // Tool 1: 按价格升序查询（无需参数）
            FunctionCallbackWrapper.<Void, Map<String, Object>>builder(ignored -> {
                var products = databaseService.getProductsByPriceDesc();
                java.util.Collections.reverse(products);
                return Map.of("status", "success", "data", products);
            })
                .withName("getProductsByPriceAsc")
                .withDescription("获取按价格升序的产品列表，最便宜的产品排在前面。用于回答最便宜、价格最低等相关问题。")
                .withInputType(Void.class)
                .build(),

            // Tool 2: 按销量排序查询（无需参数）
            FunctionCallbackWrapper.<Void, Map<String, Object>>builder(ignored -> {
                var products = databaseService.getProductsBySales();
                return Map.of("status", "success", "data", products);
            })
                .withName("getProductsBySales")
                .withDescription("获取按销量排序的产品列表，销量最高的排在前面。用于回答销量最好、最受欢迎等相关问题。")
                .withInputType(Void.class)
                .build(),

            // Tool 3: 按类别查询（需要类别参数）
            FunctionCallbackWrapper.<CategoryRequest, Map<String, Object>>builder(request -> {
                var products = databaseService.getProductsByCategory(request.category());
                return Map.of("status", "success", "data", products);
            })
                .withName("getProductsByCategory")
                .withDescription("根据产品类别查询产品列表")
                .withInputType(CategoryRequest.class)
                .build(),

            // Tool 4: 获取销售统计（无需参数）
            FunctionCallbackWrapper.<Void, Map<String, Object>>builder(ignored -> {
                var summary = databaseService.getSalesSummary();
                return Map.of("status", "success", "data", summary);
            })
                .withName("getSalesSummary")
                .withDescription("获取销售统计数据，包括总销售额、总订单数、平均订单金额等")
                .withInputType(Void.class)
                .build(),

            // Tool 5: 导出产品到 Excel（无参数，使用固定路径）
            FunctionCallbackWrapper.<Void, Map<String, Object>>builder(ignored -> {
                try {
                    var products = databaseService.getAllProducts();
                    // 固定导出路径
                    String filePath = "C:/temp/products.xlsx";
                    
                    // 自动创建目录
                    new File("C:/temp").mkdirs();
                    
                    EasyExcel.write(filePath, Product.class)
                        .sheet("产品列表")
                        .doWrite(products);
                    
                    return Map.of("status", "success", "message", "Excel导出成功", "filePath", filePath);
                } catch (Exception e) {
                    return Map.of("status", "error", "message", "Excel导出失败: " + e.getMessage());
                }
            })
                .withName("exportProductsToExcel")
                .withDescription("导出所有产品到 Excel 文件，文件将保存在 C:/temp/products.xlsx")
                .withInputType(Void.class)
                .build()
        );
    }

    public record CategoryRequest(String category) {}
}
