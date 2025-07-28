package com.example.AIrobot.Handler;

import com.example.AIrobot.Service.SessionService;
import com.example.AIrobot.Util.LineMessageUtil;
import com.example.AIrobot.Service.OpenAiService;
import com.example.AIrobot.Service.CustomerService;
import com.example.AIrobot.Entity.Customer;
import com.example.AIrobot.model.AdvisorSession;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class AdvisorHandler {

   
    private final SessionService sessionService;
    private final OpenAiService openAiService;
    private final CustomerService customerService;
    private final LineMessageUtil lineMessageUtil;
    

    public AdvisorHandler(SessionService sessionService, OpenAiService openAiService, CustomerService customerService,LineMessageUtil lineMessageUtil) {
        this.sessionService = sessionService;
        this.openAiService = openAiService;
        this.customerService = customerService;
        this.lineMessageUtil = lineMessageUtil;
    }

            public ResponseEntity<String> handleAdvisorSession(String userId, String userMessage, String replyToken) {
                if (!sessionService.hasAdvisorSession(userId)) {
                    return null; // 不屬於顧問服務流程
                }
                AdvisorSession advisorSession = sessionService.getAdvisorSession(userId);
                String replyText = "";

                // 通用取消指令
                if (userMessage.trim().equals("@取消")) {
                    sessionService.removeAdvisorSession(userId);
                    someMethod(replyToken, "✅ 顧問服務已取消。");
                    return ResponseEntity.ok("OK");
                }

                switch (advisorSession.getStep()) {
                    case ASK_TARGET_NAME -> {
                      
                        String name = userMessage.trim();
                        if (name.startsWith("@顧問服務")) {
                            name = name.replaceFirst("@顧問服務", "").trim();
                        }
                        List<Customer> list = customerService.findAllByNameAndCreatedBy(name, userId);
                        if (list.isEmpty()) {
                            System.out.println("查詢姓名: " + name + "，userId: " + userId);
                            replyText = "查無顧客：" + name + "，請重新輸入姓名。";
                        } else {
                            Customer target = list.get(0); // 若多名同名可再擴充選擇
                            advisorSession.setTargetCustomer(target);
                            advisorSession.setTargetName(name);
                            advisorSession.setStep(AdvisorSession.SessionStep.CONVERSATION);
                            replyText = "顧問服務已鎖定「" + name + "」，你可以問任何想問的話題～\n(如:「我該如何開啟話題？」)";
                        }
                        sessionService.setAdvisorSession(userId, advisorSession);
                        someMethod(replyToken, replyText);
                        return ResponseEntity.ok("OK");
                    }
                    case CONVERSATION -> {
                        // ✅ 若包含 @，但不是 @顧問服務，就離開顧問模式
                    if (userMessage.contains("@") && !userMessage.trim().startsWith("@顧問服務")) {
                        sessionService.removeAdvisorSession(userId);
                        someMethod(replyToken, "👋 已離開顧問服務，進入其他指令模式。");
                        return ResponseEntity.ok("OK");
                    }

                        Customer c = advisorSession.getTargetCustomer();
                        String aiReply = openAiService.advisorSuggest(userMessage, c);
                        replyText = "🤖 AI建議：\n" + aiReply;
                        someMethod(replyToken, replyText);
                        return ResponseEntity.ok("OK");
                    }
                }
                    

        // fallback
        someMethod(replyToken, "發生未知錯誤，請輸入 @取消 結束流程。");
        return ResponseEntity.ok("OK");
    }

    public void someMethod(String replyToken,String replyText){
        lineMessageUtil.sendLineReply(replyToken,replyText);
    }

}
