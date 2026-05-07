package com.example.aidemo;

import com.example.aidemo.service.DeepSeekService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
public class ChatController {

    private final DeepSeekService deepSeekService;

    public ChatController(DeepSeekService deepSeekService) {
        this.deepSeekService = deepSeekService;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        try {
            return deepSeekService.chat(message);
        } catch (Exception e) {
            return "错误: " + e.getMessage();
        }
    }
}