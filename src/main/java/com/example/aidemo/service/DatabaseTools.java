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

            // Tool 5: 导出产品到 Excel（返回简洁下载链接）
            FunctionCallbackWrapper.<Void, Map<String, Object>>builder(ignored -> {
                try {
                    // Excel 文件通过 /ai/export 端点直接下载
                    String downloadUrl = "/ai/export";
                    String message = "Excel 已生成！请直接访问 http://localhost:8080/ai/export 下载 Excel 文件";
                    
                    return Map.of("status", "success", "message", message, "downloadUrl", downloadUrl);
                } catch (Exception e) {
                    return Map.of("status", "error", "message", "Excel导出失败: " + e.getMessage());
                }
            })
                .withName("exportProductsToExcel")
                .withDescription("导出所有产品到 Excel 文件（无参数）。调用后会返回下载链接，请直接告诉用户访问 http://localhost:8080/ai/export 下载 Excel 文件即可。")
                .withInputType(Void.class)
                .build()
        );
    }

    public record CategoryRequest(String category) {}
}
