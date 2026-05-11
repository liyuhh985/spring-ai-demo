package com.example.aidemo;

import com.alibaba.excel.EasyExcel;
import com.example.aidemo.entity.Product;
import com.example.aidemo.service.DatabaseService;
import com.example.aidemo.service.DatabaseTools;
import com.example.aidemo.service.DatabaseTools.ExportContext;
import com.example.aidemo.service.VectorStoreService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/ai")
public class ChatController {

    private final ChatClient chatClient;
    private final DatabaseService databaseService;
    private final VectorStoreService vectorStoreService;

    private static final String SYSTEM_PROMPT = """
            You are an e-commerce data analyst.
            
            You can use these tools when user asks for data:
            - getProductsByPriceAsc: Get products sorted by price (ascending)
            - getProductsBySales: Get products sorted by sales (descending)  
            - getSalesSummary: Get sales statistics
            """;

    public ChatController(ChatClient.Builder chatClientBuilder, 
                          DatabaseService databaseService,
                          VectorStoreService vectorStoreService,
                          DatabaseTools databaseTools) {
        // 使用 defaultFunctions 注册工具
        List<FunctionCallback> tools = databaseTools.getTools();
        this.chatClient = chatClientBuilder
                .defaultFunctions(tools.toArray(new FunctionCallback[0]))
                .build();

        this.databaseService = databaseService;
        this.vectorStoreService = vectorStoreService;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .call()
                .content();
    }

    @GetMapping("/init-knowledge-base")
    public String initKnowledgeBase() {
        initKnowledgeBaseAsync();
        return "Knowledge base update started...";
    }

    @Async
    public void initKnowledgeBaseAsync() {
        try {
            var products = databaseService.getAllProducts();
            for (var product : products) {
                String content = String.format("Product: %s, Price: %.2f, Category: %s, Sales: %d",
                        product.getName(), product.getPrice().doubleValue(), 
                        product.getCategory(), product.getSales());
                vectorStoreService.saveDocument("product_" + product.getId(), content);
            }
            System.out.println("Knowledge base initialized! " + products.size() + " products imported");
        } catch (Exception e) {
            System.out.println("Init failed: " + e.getMessage());
        }
    }

    @GetMapping("/test-search")
    public String testSearch(@RequestParam String query, 
                            @RequestParam(defaultValue = "10") int topK,
                            @RequestParam(defaultValue = "0.0") float threshold) {
        try {
            List<String> results = vectorStoreService.search(query, topK, threshold);
            StringBuilder response = new StringBuilder();
            response.append("Query: ").append(query).append("\n");
            response.append("Results: ").append(results.size()).append("\n");
            response.append(String.join("\n---\n", results));
            return response.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/export")
    public void exportExcel(
            HttpServletResponse response,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false, defaultValue = "false") boolean exportData) {
        try {
            List<Product> products;
            
            // 优先从 ExportContext 获取数据（通过 AI function callback 设置）
            List<Product> contextProducts = ExportContext.getProducts();
            if (contextProducts != null && !contextProducts.isEmpty()) {
                products = contextProducts;
                ExportContext.clear(); // 清理上下文
            } else {
                // 直接通过 URL 参数查询
                if (category != null && !category.isEmpty()) {
                    products = databaseService.getProductsByCategory(category);
                } else {
                    products = databaseService.getAllProducts();
                }
                
                // 排序
                if (sortBy != null && !sortBy.isEmpty()) {
                    products = switch (sortBy) {
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
                
                // 限制数量
                if (limit != null && limit > 0 && products.size() > limit) {
                    products = products.subList(0, limit);
                }
            }
            
            // 添加日志确认查询成功
            System.out.println("Export: Found " + products.size() + " products");
            
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=products.xlsx");
            
            // 手动定义表头
            List<List<String>> head = new ArrayList<>();
            head.add(Arrays.asList("ID"));
            head.add(Arrays.asList("产品名称"));
            head.add(Arrays.asList("类别"));
            head.add(Arrays.asList("价格"));
            head.add(Arrays.asList("库存"));
            head.add(Arrays.asList("销量"));
            
            // 写入数据
            List<List<Object>> data = new ArrayList<>();
            for (Product p : products) {
                List<Object> row = Arrays.asList(p.getId(), p.getName(), p.getCategory(), p.getPrice(), p.getStock(), p.getSales());
                data.add(row);
            }
            
            EasyExcel.write(response.getOutputStream())
                .head(head)
                .sheet("产品列表")
                .doWrite(data);
        } catch (Exception e) {
            System.out.println("Export failed: " + e.getMessage());
        }
    }
}
