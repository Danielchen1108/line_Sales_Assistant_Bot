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

        // 🔙 處理 @上一步
        if (userMessage.trim().equals("@上一步")) {
            Step prevStep = getPreviousStep(session.step);
            if (prevStep == null) {
                return lineMessageUtil.replyText(replyToken, "⚠️ 無法返回上一步。");
            }
            session.step = prevStep;
            sessionService.setUserSession(userId, session);
            return lineMessageUtil.replyText(replyToken, "🔙 已返回上一步，請重新輸入。");
        }

        // 依 step 分流交由 CustomerInputHandler 處理
        return customerInputHandler.handleStep(session, userId, userMessage, replyToken);
    }

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
}
