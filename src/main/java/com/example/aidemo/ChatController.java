package com.example.aidemo;

import com.example.aidemo.service.QianfanService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
public class ChatController {

    private final QianfanService qianfanService;

    public ChatController(QianfanService qianfanService) {
        this.qianfanService = qianfanService;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        try {
            return qianfanService.chat(message);
        } catch (Exception e) {
            return "错误: " + e.getMessage();
        }
    }

    @GetMapping("/chat/{model}")
    public String chat(@RequestParam String message, @PathVariable String model) {
        try {
            return qianfanService.chat(model, message);
        } catch (Exception e) {
            return "错误: " + e.getMessage();
        }
    }
}