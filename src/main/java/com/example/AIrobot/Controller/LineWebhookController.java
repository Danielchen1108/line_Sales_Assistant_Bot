package com.example.AIrobot.Controller;

import com.example.AIrobot.Handler.CustomerHandler;
import com.example.AIrobot.Handler.AdvisorHandler;
import com.example.AIrobot.Service.SessionService;
import com.example.AIrobot.Util.LineMessageUtil;
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

    private final CustomerHandler customerHandler;
    private final AdvisorHandler advisorHandler;
    private final SessionService sessionService;
    private final LineMessageUtil lineMessageUtil;
    private final AdminHandler adminHandler; 

    public LineWebhookController(
        CustomerHandler customerHandler,
        AdvisorHandler advisorHandler,
        SessionService sessionService,
        LineMessageUtil lineMessageUtil,
        AdminHandler adminHandler  // 🔧 建構式注入
) {
    this.customerHandler = customerHandler;
    this.advisorHandler = advisorHandler;
    this.sessionService = sessionService;
    this.lineMessageUtil = lineMessageUtil;
    this.adminHandler = adminHandler; // 🔧 設定
}

    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody String requestBody) {
        try {
            // 解析 LINE event
            JSONObject event = new JSONObject(requestBody)
                    .getJSONArray("events")
                    .getJSONObject(0);
            String replyToken = event.getString("replyToken");
            String userMessage = event.getJSONObject("message").getString("text");
            String userId = event.getJSONObject("source").getString("userId");

            if (!sessionService.hasAdminSession(userId)) {
            sessionService.setAdminSession(userId, new com.example.AIrobot.model.AdminSession());
            lineMessageUtil.sendLineReply(replyToken, "📧 請輸入管理者 Email：");
            return ResponseEntity.ok("OK");
            }

            
            // --- Admin 設定流程 ---
            if (userMessage.trim().equals("@設定管理者")) {
                sessionService.setAdminSession(userId, new com.example.AIrobot.model.AdminSession());
                lineMessageUtil.sendLineReply(replyToken, "📧 請輸入管理者 Email：");
                return ResponseEntity.ok("OK");
            }

            if (sessionService.hasAdminSession(userId)) {
                return adminHandler.handleAdminSession(userId, userMessage, replyToken);
            }

            // --- 多步驟流程判斷（有 Session） ---
            if (sessionService.hasAdvisorSession(userId)) {
                // 進入顧問服務流程
                return advisorHandler.handleAdvisorSession(userId, userMessage, replyToken);
            }
            if (sessionService.hasUserSession(userId)) {
                // 進入新增/查詢/編輯顧客多步驟流程
                return customerHandler.handleSession(userId, userMessage, replyToken);
            }

            // --- 無 Session 的主指令判斷 ---
            if (userMessage.trim().equals("@新增")) {
                sessionService.setUserSession(userId, new com.example.AIrobot.model.UserSession());
                someMethod(replyToken, "👤 請輸入顧客姓名：");
                return ResponseEntity.ok("OK");
            }
            if (userMessage.trim().startsWith("@查詢")) {
                // 範例：@查詢 王小明
                String name = userMessage.replaceFirst("@查詢", "").trim();
                return customerHandler.handleQueryCustomer(userId, name, replyToken);
            }
            if (userMessage.trim().startsWith("@更新")) {
                String name = userMessage.replaceFirst("@更新", "").trim();
                return customerHandler.handleUpdateCustomer(userId, name, replyToken);
            }
            if (userMessage.trim().startsWith("@刪除")) {
                String name = userMessage.replaceFirst("@刪除", "").trim();
                return customerHandler.handleDeleteCustomer(userId, name, replyToken);
            }
            if (userMessage.trim().equals("@列出所有客戶")) {
                return customerHandler.handleListAllCustomers(userId, replyToken);
            }
            if (userMessage.trim().startsWith("@列出成交率最高的人數")) {
                // 範例：@列出成交率最高的人數 5
                String numStr = userMessage.replace("@列出成交率最高的人數", "").trim();
                int limit = 10;
                try {
                    limit = Integer.parseInt(numStr);
                    if (limit <= 0) limit = 10;
                } catch (Exception e) { /* 預設10 */ }
                return customerHandler.handleTopCustomers(userId, limit, replyToken);
            }
            if (userMessage.trim().startsWith("@顧問服務")) {
                sessionService.setAdvisorSession(userId, new com.example.AIrobot.model.AdvisorSession());
                return advisorHandler.handleAdvisorSession(userId, userMessage, replyToken);
            }
            if (userMessage.trim().equals("@選單")) {
                someMethod(replyToken,
                    "【功能選單】\n" +
                    "@新增\n" +
                    "@查詢 姓名\n" +
                    "@更新 姓名\n" +
                    "@刪除 姓名\n" +
                    "@列出所有客戶\n" +
                    "@列出成交率最高的人數 N\n" +
                    "@顧問服務\n" +
                    "@取消\n" +
                    "@選單"
                );
                return ResponseEntity.ok("OK");
            }


            // 若指令未識別
            someMethod(replyToken, "請輸入正確的指令（如 @新增、@查詢 姓名、@顧問服務 ...）");
            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok("OK");
        }
    }

    // 共用 LINE 訊息回覆
    public void someMethod(String replyToken,String replyText){
            lineMessageUtil.sendLineReply(replyToken, replyText);
    }
}
