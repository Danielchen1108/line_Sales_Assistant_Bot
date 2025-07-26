package com.example.AIrobot.Controller;

import com.example.AIrobot.Handler.AdvisorHandler;
import com.example.AIrobot.Handler.Customer.CustomerCommandHandler;
import com.example.AIrobot.Handler.Customer.CustomerHandler;
import com.example.AIrobot.Handler.Customer.CustomerSessionHandler;
import com.example.AIrobot.Service.SessionService;
import com.example.AIrobot.Util.LineMessageUtil;
import com.example.AIrobot.model.AdvisorSession;
import com.example.AIrobot.Handler.AdminHandler; // 加上 import

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/webhook")
public class LineWebhookController {

    
    private final AdvisorHandler advisorHandler;
    private final AdminHandler adminHandler;
    private final SessionService sessionService;
    private final LineMessageUtil lineMessageUtil;
    private final CustomerCommandHandler customerCommandHandler;
    private final CustomerSessionHandler customerSessionHandler;

    public LineWebhookController(
        CustomerSessionHandler customerSessionHandler,
        CustomerCommandHandler customerCommandHandler,
        CustomerHandler customerHandler,
        AdvisorHandler advisorHandler,
        SessionService sessionService,
        LineMessageUtil lineMessageUtil,
        AdminHandler adminHandler
    ) {
        this.customerSessionHandler = customerSessionHandler;
        this.customerCommandHandler = customerCommandHandler;
        this.advisorHandler = advisorHandler;
        this.sessionService = sessionService;
        this.lineMessageUtil = lineMessageUtil;
        this.adminHandler = adminHandler;
    }

    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody String requestBody) {
        try {
            JSONObject event = new JSONObject(requestBody)
                    .getJSONArray("events")
                    .getJSONObject(0);
            String replyToken = event.getString("replyToken");
            String userMessage = event.getJSONObject("message").getString("text").trim();
            String userId = event.getJSONObject("source").getString("userId");

            // 1. 管理者流程
            if (userMessage.equals("@設定管理者") || sessionService.hasAdminSession(userId)) {
                return adminHandler.handleAdminSession(userId, userMessage, replyToken);
            }

            // 2. 顧問流程
            if (userMessage.startsWith("@顧問服務") || sessionService.hasAdvisorSession(userId)) {
                if (!sessionService.hasAdvisorSession(userId)) {
                    sessionService.setAdvisorSession(userId, new AdvisorSession());
                }
                return advisorHandler.handleAdvisorSession(userId, userMessage, replyToken);
            }

            // 3. 客戶多步驟流程
            if (sessionService.hasUserSession(userId)) {
                // 多步流程進行中，僅允許 @上一步/@取消 等特殊指令，其他則回提示
                if (userMessage.startsWith("@") && 
                    !userMessage.equals("@取消") && 
                    !userMessage.equals("@上一步")&&
                    !userMessage.equals("@略過")) {
                    return replyText(replyToken, "請先完成當前操作或輸入 @取消 結束本次流程");
                }
                return customerSessionHandler.handle(userId, userMessage, replyToken);
            }

            // 4. 所有 @開頭指令 → 全丟給 CommandHandler，@選單也在裡面集中處理
            if (userMessage.startsWith("@")) {
                return customerCommandHandler.handleCommand(userId, userMessage, replyToken);
            }

            // 5. 無效輸入
            return replyText(replyToken, "⚠️ 請輸入正確的指令，例如 @新增 或 @查詢 姓名");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok("OK");
        }
    }

    private ResponseEntity<String> replyText(String replyToken, String message) {
        lineMessageUtil.sendLineReply(replyToken, message);
        return ResponseEntity.ok("OK");
    }
}