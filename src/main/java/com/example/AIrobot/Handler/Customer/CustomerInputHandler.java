package com.example.AIrobot.Handler.Customer;

import com.example.AIrobot.Service.SessionService;
import com.example.AIrobot.Util.LineMessageUtil;
import com.example.AIrobot.model.UserSession;
import com.example.AIrobot.model.UserSession.Step;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class CustomerInputHandler {

    private final SessionService sessionService;
    private final LineMessageUtil lineMessageUtil;

    public CustomerInputHandler(SessionService sessionService, LineMessageUtil lineMessageUtil) {
        this.sessionService = sessionService;
        this.lineMessageUtil = lineMessageUtil;
    }

    public ResponseEntity<String> handleStep(UserSession session, String userId, String userMessage, String replyToken) {
        String input = userMessage.trim();
        String replyText = "";

        switch (session.step) {
            case ASK_NAME -> {
                session.name = input;
                session.step = Step.ASK_IDNUMBER;
                replyText = "🆔 請輸入身分證字號：\n(或輸入\"@略過\"、\"@取消\"或\"@上一步\")";
            }
            case ASK_IDNUMBER -> {
                if (input.equals("@略過")) {
                    session.idNumber = null;
                    session.step = Step.ASK_BIRTHDAY;
                    replyText = "🎂 請輸入出生年月日（例如：1990-01-01）：";
                } else if (!input.matches("^[A-Z][0-9]{9}$")) {
                    replyText = "❌ 身分證字號格式錯誤，請重新輸入（例如：A123456789）。若無法提供，請輸入 @略過";
                    return lineMessageUtil.replyText(replyToken, replyText);
                } else {
                    session.idNumber = input;
                    session.step = Step.ASK_BIRTHDAY;
                    replyText = "🎂 請輸入出生年月日（例如：1990-01-01）：";
                }
            }
            case ASK_BIRTHDAY -> {
                if (input.equals("@略過")) {
                    session.birthday = null;
                } else {
                    try {
                        session.birthday = LocalDate.parse(input);
                    } catch (Exception e) {
                        replyText = "⚠️ 日期格式錯誤，請用 yyyy-MM-dd，例如：1990-01-01";
                        return lineMessageUtil.replyText(replyToken, replyText);
                    }
                }
                session.step = Step.ASK_PHONE;
                replyText = "📞 請輸入電話：\n(或輸入\"@取消\"或\"@上一步\")";
            }
            case ASK_PHONE -> {
                session.phone = input;
                session.step = Step.ASK_REGION;
                replyText = "📍 請輸入地區：\n(或輸入\"@取消\"或\"@上一步\")";
            }
            case ASK_REGION -> {
                session.region = input;
                session.step = Step.ASK_AGE;
                replyText = "🎂 請輸入年齡，可打 \"@略過\"\n(或輸入\"@取消\"或\"@上一步\")";
            }
            case ASK_AGE -> {
                if (input.equals("@略過")) {
                    session.age = null;
                } else {
                    try {
                        session.age = Integer.parseInt(input);
                    } catch (Exception e) {
                        session.age = null;
                    }
                }
                session.step = Step.ASK_JOB;
                replyText = "💼 請輸入職業，可打 \"@略過\"\n(或輸入\"@取消\"或\"@上一步\")：";
            }
            case ASK_JOB -> {
                session.job = input.equals("@略過") ? null : input;
                session.step = Step.ASK_PRODUCTS;
                replyText = "🛡️ 請輸入已購險種（多個用逗號分隔，沒填可打 @略過）：";
            }
            case ASK_PRODUCTS -> {
                session.productsOwned = input.equals("@略過") ? null : input;
                session.step = Step.ASK_STATUS;
                replyText = "📝 請輸入客戶目前狀態或需求：";
            }
            case ASK_STATUS -> {
                session.status = input;
                session.step = Step.CONFIRM;
                replyText = "✅ 請確認資料：\n"
                        + "姓名：" + session.name + "\n"
                        + "身分證字號：" + (session.idNumber == null ? "未填" : session.idNumber) + "\n"
                        + "出生年月日：" + (session.birthday == null ? "未填" : session.birthday) + "\n"
                        + "電話：" + session.phone + "\n"
                        + "地區：" + session.region + "\n"
                        + "年齡：" + (session.age == null ? "未填" : session.age) + "\n"
                        + "職業：" + (session.job == null ? "未填" : session.job) + "\n"
                        + "已購險種：" + (session.productsOwned == null ? "未填" : session.productsOwned) + "\n"
                        + "狀態：" + session.status + "\n"
                        + "如正確請輸入\"確認\"，如需取消請輸入\"@取消\"或\"@上一步\"";
            }
            default -> {
                replyText = "⚠️ 系統錯誤，無法處理目前步驟。";
            }
        }

        // 更新 session 並回應
        sessionService.setUserSession(userId, session);
        return lineMessageUtil.replyText(replyToken, replyText);
    }
}
