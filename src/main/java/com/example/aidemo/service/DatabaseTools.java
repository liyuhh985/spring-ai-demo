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

            // Tool 5: 导出产品到 Excel（支持按条件筛选和排序）
            FunctionCallbackWrapper.<ExportRequest, Map<String, Object>>builder(request -> {
                try {
                    List<Product> products;
                    
                    // 根据 category 筛选
                    if (request.category() != null && !request.category().isEmpty()) {
                        products = databaseService.getProductsByCategory(request.category());
                    } else {
                        products = databaseService.getAllProducts();
                    }
                    
                    // 根据 sortBy 排序
                    if (request.sortBy() != null && !request.sortBy().isEmpty()) {
                        products = switch (request.sortBy()) {
                            case "price-asc" -> databaseService.getProductsByPriceAsc();
                            case "price-desc" -> databaseService.getProductsByPriceDesc();
                            case "sales-asc" -> {
                                var sorted = databaseService.getProductsBySales();
                                java.util.Collections.reverse(sorted);
                                yield sorted;
                            }
                            case "sales-desc" -> databaseService.getProductsBySales();
                            default -> products;
                        };
                    }
                    
                    // 根据 limit 限制数量
                    if (request.limit() != null && request.limit() > 0 && products.size() > request.limit()) {
                        products = products.subList(0, request.limit());
                    }
                    
                    // 构建下载 URL（带查询参数）
                    StringBuilder urlBuilder = new StringBuilder("/ai/export?exportData=true");
                    if (request.category() != null && !request.category().isEmpty()) {
                        urlBuilder.append("&category=").append(request.category());
                    }
                    if (request.sortBy() != null && !request.sortBy().isEmpty()) {
                        urlBuilder.append("&sortBy=").append(request.sortBy());
                    }
                    if (request.limit() != null && request.limit() > 0) {
                        urlBuilder.append("&limit=").append(request.limit());
                    }
                    
                    String downloadUrl = urlBuilder.toString();
                    String message = String.format("Excel 已生成！共导出 %d 条数据。请直接访问 http://localhost:8080%s 下载 Excel 文件", 
                        products.size(), downloadUrl);
                    
                    // 将数据存入共享上下文（供导出端点使用）
                    ExportContext.setProducts(products);
                    
                    return Map.of("status", "success", "message", message, "downloadUrl", downloadUrl, "count", products.size());
                } catch (Exception e) {
                    return Map.of("status", "error", "message", "Excel导出失败: " + e.getMessage());
                }
            })
                .withName("exportProductsToExcel")
                .withDescription("导出产品到 Excel 文件，支持按类别筛选、排序方式和数量限制。参数说明：category-产品类别（如电子产品、服装等），sortBy-排序方式（price-asc价格升序，price-desc价格降序，sales-asc销量升序，sales-desc销量降序），limit-导出数量限制。调用后会返回下载链接，请直接告诉用户访问返回的下载链接下载 Excel 文件即可。")
                .withInputType(ExportRequest.class)
                .build()
        );
    }

    public record CategoryRequest(String category) {}

    /**
     * 导出请求参数
     * @param category 类别筛选（可选，不填则导出全部）
     * @param sortBy 排序方式：price-asc, price-desc, sales-asc, sales-desc
     * @param limit 导出数量限制（可选）
     */
    public record ExportRequest(
        String category,
        String sortBy,
        Integer limit
    ) {}
    
    /**
     * 导出上下文，用于在 function callback 和 HTTP endpoint 之间传递数据
     */
    public static class ExportContext {
        private static final ThreadLocal<List<Product>> products = new ThreadLocal<>();
        
        public static void setProducts(List<Product> list) {
            products.set(list);
        }
        
        public static List<Product> getProducts() {
            return products.get();
        }
        
        public static void clear() {
            products.remove();
        }
    }
}
