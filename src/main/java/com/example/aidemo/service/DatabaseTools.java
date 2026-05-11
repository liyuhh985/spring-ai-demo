package com.example.aidemo.service;

import com.alibaba.excel.EasyExcel;
import com.example.aidemo.entity.Product;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.stereotype.Component;

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

            // Tool 5: 导出产品到 Excel（返回 data URL）
            FunctionCallbackWrapper.<Void, Map<String, Object>>builder(ignored -> {
                try {
                    var products = databaseService.getAllProducts();
                    
                    // 在内存中生成 Excel
                    java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
                    
                    EasyExcel.write(outputStream, Product.class)
                        .sheet("产品列表")
                        .doWrite(products);
                    
                    // 转换为 Base64
                    byte[] excelBytes = outputStream.toByteArray();
                    String base64 = java.util.Base64.getEncoder().encodeToString(excelBytes);
                    
                    // 返回 data URL 格式，用户可直接在浏览器打开下载
                    String downloadUrl = "data:application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;base64," + base64;
                    
                    return Map.of("status", "success", "downloadUrl", downloadUrl, "filename", "products.xlsx");
                } catch (Exception e) {
                    return Map.of("status", "error", "message", "Excel导出失败: " + e.getMessage());
                }
            })
                .withName("exportProductsToExcel")
                .withDescription("导出所有产品到 Excel 文件（无参数）。返回 data URL 格式的 Excel 下载链接，AI 可以直接将 downloadUrl 提供给用户，用户在浏览器中打开该链接即可自动下载 Excel 文件。")
                .withInputType(Void.class)
                .build()
        );
    }

    public record CategoryRequest(String category) {}
}
