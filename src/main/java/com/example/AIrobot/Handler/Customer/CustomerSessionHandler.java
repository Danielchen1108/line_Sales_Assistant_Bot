package com.example.AIrobot.Handler.Customer;

import com.example.AIrobot.Service.SessionService;
import com.example.AIrobot.Util.LineMessageUtil;
import com.example.AIrobot.model.UserSession;
import com.example.AIrobot.model.UserSession.Step;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class CustomerSessionHandler {

    private final SessionService sessionService;
    private final LineMessageUtil lineMessageUtil;
    private final CustomerInputHandler customerInputHandler;

    public CustomerSessionHandler(SessionService sessionService,
                                  LineMessageUtil lineMessageUtil,
                                  CustomerInputHandler customerInputHandler) {
        this.sessionService = sessionService;
        this.lineMessageUtil = lineMessageUtil;
        this.customerInputHandler = customerInputHandler;
    }

    public ResponseEntity<String> handle(String userId, String userMessage, String replyToken) {
        UserSession session = sessionService.getUserSession(userId);
        String input = userMessage.trim();

        // 1️⃣ @取消：結束流程
        if (input.equals("@取消")) {
            sessionService.removeUserSession(userId); // 或設為 null/清除狀態
            return lineMessageUtil.replyText(replyToken, "❌ 已取消本次操作。如需重新開始請輸入 @新增");
        }

        // 2️⃣ @上一步：回退一格並清空該欄位
        if (input.equals("@上一步")) {
            Step prevStep = getPreviousStep(session.step);
            if (prevStep == null) {
                return lineMessageUtil.replyText(replyToken, "⚠️ 已經在第一步，無法再往前囉！");
            }
            clearCurrentStepValue(session, session.step); // 清掉這一格
            session.step = prevStep;
            sessionService.setUserSession(userId, session);
            return lineMessageUtil.replyText(replyToken, getStepQuestion(prevStep));
        }

        // 3️⃣ 其它交由 InputHandler (略過也是在 inputHandler 處理)
        return customerInputHandler.handleStep(session, userId, userMessage, replyToken);
    }

    // 回傳上一題步驟
    private Step getPreviousStep(Step currentStep) {
        return switch (currentStep) {
            case ASK_IDNUMBER -> Step.ASK_NAME;
            case ASK_BIRTHDAY -> Step.ASK_IDNUMBER;
            case ASK_PHONE -> Step.ASK_BIRTHDAY;
            case ASK_REGION -> Step.ASK_PHONE;
            case ASK_AGE -> Step.ASK_REGION;
            case ASK_JOB -> Step.ASK_AGE;
            case ASK_PRODUCTS -> Step.ASK_JOB;
            case ASK_STATUS -> Step.ASK_PRODUCTS;
            case CONFIRM -> Step.ASK_STATUS;
            default -> null;
        };
    }

    // 回到上一題自動清空這一題資料
    private void clearCurrentStepValue(UserSession session, Step step) {
        switch (step) {
            case ASK_NAME -> session.name = null;
            case ASK_IDNUMBER -> session.idNumber = null;
            case ASK_BIRTHDAY -> session.birthday = null;
            case ASK_PHONE -> session.phone = null;
            case ASK_REGION -> session.region = null;
            case ASK_AGE -> session.age = null;
            case ASK_JOB -> session.job = null;
            case ASK_PRODUCTS -> session.productsOwned = null;
            case ASK_STATUS -> session.status = null;
            default -> {}
        }
    }

    // 根據步驟取得問題
    private String getStepQuestion(Step step) {
        return switch (step) {
            case ASK_NAME -> "👤 請輸入顧客姓名：";
            case ASK_IDNUMBER -> "🆔 請輸入身分證字號：\n(或輸入\"@略過\"、\"@取消\"或\"@上一步\")";
            case ASK_BIRTHDAY -> "🎂 請輸入出生年月日（例如：1990-01-01）：";
            case ASK_PHONE -> "📞 請輸入電話：\n(或輸入\"@取消\"或\"@上一步\")";
            case ASK_REGION -> "📍 請輸入地區：\n(或輸入\"@取消\"或\"@上一步\")";
            case ASK_AGE -> "🎂 請輸入年齡，可打 \"@略過\"\n(或輸入\"@取消\"或\"@上一步\")";
            case ASK_JOB -> "💼 請輸入職業，可打 \"@略過\"\n(或輸入\"@取消\"或\"@上一步\")：";
            case ASK_PRODUCTS -> "🛡️ 請輸入已購險種（多個用逗號分隔，沒填可打 @略過）：";
            case ASK_STATUS -> "📝 請輸入客戶目前狀態或需求：";
            default -> "";
        };
    }
}
