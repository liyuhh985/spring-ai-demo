package com.example.aidemo;

import com.example.aidemo.service.DatabaseService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
public class ChatController {

    private final ChatClient chatClient;
    private final DatabaseService databaseService;

    // System Prompt：告诉 AI 它的角色和能力
    private static final String SYSTEM_PROMPT = """
            你是一个资深的电商数据分析师，擅长数据分析、用户增长和营销策略。
            你的回答要专业、简洁、有数据支撑。
            当用户问销售或价格相关问题时，必须先调用相关工具获取真实数据，再回答。
            """;

    public ChatController(ChatClient.Builder chatClientBuilder, DatabaseService databaseService) {
        this.chatClient = chatClientBuilder.build();
        this.databaseService = databaseService;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        // 根据用户问题，自动调用数据库
        String lowerMessage = message.toLowerCase();
        String dataContext = "";
        
        try {
            // 情况1：问销售排名（最好、top、销量等）
            if (lowerMessage.contains("销售") || lowerMessage.contains("top") || 
                lowerMessage.contains("最好") || lowerMessage.contains("销量") ||
                lowerMessage.contains("哪个") || lowerMessage.contains("畅销")) {
                var products = databaseService.getProductsBySales();
                dataContext = "按销售额排序的商品数据：" + products;
            } 
            // 情况2：问价格（价格最高、单&#9;&#9;&#9;&#9;价最高、最贵等）
            else if (lowerMessage.contains("价格") || lowerMessage.contains("贵") || 
                     lowerMessage.contains("便宜") || lowerMessage.contains("多少钱") ||
                     lowerMessage.contains("售价") || lowerMessage.contains("单价")) {
                var products = databaseService.getProductsByPriceDesc();
                dataContext = "按价格排序的商品数据：" + products;
            }
            // 情况3：问统计汇总
            else if (lowerMessage.contains("统计") || lowerMessage.contains("汇总")) {
                var summary = databaseService.getSalesSummary();
                dataContext = "销售统计数据：" + summary;
            }
            // 情况4：按分类查询
            else if (lowerMessage.contains("电子") || lowerMessage.contains("服装") || 
                     lowerMessage.contains("家电")) {
                String category = lowerMessage.contains("电子") ? "电子产品" : 
                                 lowerMessage.contains("服装") ? "服装" : "家电";
                var products = databaseService.getProductsByCategory(category);
                dataContext = category + "分类的商品数据：" + products;
            }
        } catch (Exception e) {
            dataContext = "数据库查询失败: " + e.getMessage();
        }
        
        // 构建完整的问题（包含数据库上下文）
        String fullMessage = message + "\n\n参考数据：" + dataContext;
        
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(fullMessage)
                .call()
                .content();
    }
}