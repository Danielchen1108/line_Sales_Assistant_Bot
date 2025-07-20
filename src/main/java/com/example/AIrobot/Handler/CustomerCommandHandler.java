// ✅ CustomerCommandHandler.java
package com.example.AIrobot.Handler;

import com.example.AIrobot.Service.SessionService;
import com.example.AIrobot.Util.LineMessageUtil;
import com.example.AIrobot.model.UserSession;
import com.example.AIrobot.model.UserSession.Step;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

public class CustomerCommandHandler {

    private final SessionService sessionService;
    private final LineMessageUtil lineMessageUtil;

    public CustomerCommandHandler(SessionService sessionService, LineMessageUtil lineMessageUtil) {
        this.sessionService = sessionService;
        this.lineMessageUtil = lineMessageUtil;
    }

    //---------  新增流程 ----------
    public ResponseEntity<String> handleStartNew(String userId, String replyToken) {
        UserSession session = new UserSession();
        session.step = Step.ASK_NAME;
        sessionService.setUserSession(userId, session);
        return lineMessageUtil.replyText(replyToken, "👤 請輸入顧客姓名：");
    }

    public ResponseEntity<String> handleUserSession(String userId, String userMessage, String replyToken) {
        UserSession session = sessionService.getUserSession(userId);
        String replyText = "";

        // 🔙 處理上一步指令
        if (userMessage.trim().equals("@上一步")) {
            switch (session.step) {
                case ASK_IDNUMBER -> session.step = Step.ASK_NAME;
                case ASK_BIRTHDAY -> session.step = Step.ASK_IDNUMBER;
                case ASK_PHONE -> session.step = Step.ASK_BIRTHDAY;
                case ASK_REGION -> session.step = Step.ASK_PHONE;
                case ASK_AGE -> session.step = Step.ASK_REGION;
                case ASK_JOB -> session.step = Step.ASK_AGE;
                case ASK_PRODUCTS -> session.step = Step.ASK_JOB;
                case ASK_STATUS -> session.step = Step.ASK_PRODUCTS;
                case CONFIRM -> session.step = Step.ASK_STATUS;
                default -> {
                    return lineMessageUtil.replyText(replyToken, "⚠️ 無法返回上一步。");
                }
            }
            sessionService.setUserSession(userId, session);
            return lineMessageUtil.replyText(replyToken, "🔙 已返回上一步，請重新輸入。");
        }

        switch (session.step) {
            case ASK_NAME -> {
                session.name = userMessage.trim();
                session.step = Step.ASK_IDNUMBER;
                sessionService.setUserSession(userId, session);
                replyText = "🆔 請輸入身分證字號：\n(或輸入\"@略過\"、\"@取消\"或\"@上一步\")";
            }
            case ASK_IDNUMBER -> {
                String input = userMessage.trim();
                if (input.equals("@略過")) {
                    session.idNumber = null;
                    session.step = Step.ASK_BIRTHDAY;
                    sessionService.setUserSession(userId, session);
                    replyText = "🎂 請輸入出生年月日（例如：1990-01-01）：";
                } else if (!input.matches("^[A-Z][0-9]{9}$")) {
                    replyText = "❌ 身分證字號格式錯誤，請重新輸入（例如：A123456789）。若無法提供，請輸入 @略過";
                } else {
                    session.idNumber = input;
                    session.step = Step.ASK_BIRTHDAY;
                    sessionService.setUserSession(userId, session);
                    replyText = "🎂 請輸入出生年月日（例如：1990-01-01）：";
                }
                return lineMessageUtil.replyText(replyToken, replyText);
            }
            case ASK_BIRTHDAY -> {
                if (userMessage.trim().equals("@略過")) {
                    session.birthday = null;
                } else {
                    try {
                        session.birthday = LocalDate.parse(userMessage.trim());
                    } catch (Exception e) {
                        replyText = "⚠️ 日期格式錯誤，請用 yyyy-MM-dd，例如：1990-01-01";
                        return lineMessageUtil.replyText(replyToken, replyText);
                    }
                }
                session.step = Step.ASK_PHONE;
                sessionService.setUserSession(userId, session);
                replyText = "📞 請輸入電話：\n(或輸入\"@取消\"或\"@上一步\")";
            }
            case ASK_PHONE -> {
                session.phone = userMessage.trim();
                session.step = Step.ASK_REGION;
                sessionService.setUserSession(userId, session);
                replyText = "📍 請輸入地區：\n(或輸入\"@取消\"或\"@上一步\")";
            }
            case ASK_REGION -> {
                session.region = userMessage.trim();
                session.step = Step.ASK_AGE;
                sessionService.setUserSession(userId, session);
                replyText = "🎂 請輸入年齡可打\"@略過\"\n(或輸入\"@取消\"或\"@上一步\")";
            }
            case ASK_AGE -> {
                if (userMessage.trim().equals("@略過")) session.age = null;
                else {
                    try { session.age = Integer.parseInt(userMessage.trim()); }
                    catch(Exception e) { session.age = null; }
                }
                session.step = Step.ASK_JOB;
                sessionService.setUserSession(userId, session);
                replyText = "💼 請輸入職業可打 \"@略過\"\n(或輸入\"@取消\"或\"@上一步\")：";
            }
            case ASK_JOB -> {
                session.job = userMessage.trim().equals("@略過") ? null : userMessage.trim();
                session.step = Step.ASK_PRODUCTS;
                sessionService.setUserSession(userId, session);
                replyText = "🛡️ 請輸入已購險種（多個用逗號分隔，沒填可打 @略過）：";
            }
            case ASK_PRODUCTS -> {
                session.productsOwned = userMessage.trim().equals("@略過") ? null : userMessage.trim();
                session.step = Step.ASK_STATUS;
                sessionService.setUserSession(userId, session);
                replyText = "📝 請輸入客戶目前狀態或需求：";
            }
            case ASK_STATUS -> {
                session.status = userMessage.trim();
                session.step = Step.CONFIRM;
                sessionService.setUserSession(userId, session);
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
        }
        return lineMessageUtil.replyText(replyToken, replyText);
    }
}
