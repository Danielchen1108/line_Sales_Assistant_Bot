package com.example.AIrobot.Handler.Customer;

import com.example.AIrobot.Entity.Customer;
import com.example.AIrobot.Service.CustomerService;
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
    private final CustomerService customerService;
    private final CustomerFlowHandler customerFlowHandler;

    public CustomerInputHandler(SessionService sessionService,
                                LineMessageUtil lineMessageUtil,
                                CustomerService customerService,
                                CustomerFlowHandler customerFlowHandler) {
        this.sessionService = sessionService;
        this.lineMessageUtil = lineMessageUtil;
        this.customerService = customerService;
        this.customerFlowHandler = customerFlowHandler;
    }

    public ResponseEntity<String> handleStep(UserSession session, String userId, String userMessage, String replyToken) {
        String input = userMessage.trim();
        String replyText = "";

        switch (session.step) {
            // ===== 新增流程 =====
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
                replyText = "📞 請輸入電話：";
            }
            case ASK_PHONE -> {
                session.phone = input;
                session.step = Step.ASK_REGION;
                replyText = "📍 請輸入地區：";
            }
            case ASK_REGION -> {
                session.region = input;
                session.step = Step.ASK_AGE;
                replyText = "🎂 請輸入年齡，可打 \"@略過\"";
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
                replyText = "💼 請輸入職業，可打 \"@略過\"：";
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
                        + "如正確請輸入「確認」，如需取消請輸入「@取消」或「@上一步」";
            }
            case CONFIRM -> {
                if (input.equals("確認")) {
                    sessionService.removeUserSession(userId);
                    // 這裡要傳四個參數
                    return customerFlowHandler.handleFinalAddConfirmation(session, input, userId, replyToken);
                } else {
                    replyText = "⚠️ 請輸入「確認」";
                    return lineMessageUtil.replyText(replyToken, replyText);
                }
            }



            // ====== 進階流程：全部委派給 flowHandler ======
            case CHOOSE_SAME_NAME_INDEX -> {
                return customerFlowHandler.handleSelectSameNameCustomer(session, input, replyToken, userId);
            }
            case DELETE_CHOOSE_INDEX -> {
                return customerFlowHandler.handleSelectDeleteCustomer(session, input, replyToken, userId);
            }
            case DELETE_CONFIRM -> {
                return customerFlowHandler.handleDeleteConfirmation(session, input, userId, replyToken);
            }
            case UPDATE_CHOOSE_FIELD -> {
                return customerFlowHandler.handleUpdateFieldSelection(session, input, replyToken, userId);
            }
            case UPDATE_ASK_UPDATE_VALUE -> {
                String replyMsg = customerFlowHandler.handleUpdateFieldInput(session, input, userId);
                sessionService.removeUserSession(userId);
                return lineMessageUtil.replyText(replyToken, replyMsg);
            }

           case UPDATE_CONFIRM -> {
                if (input.equals("確認")) {
                    // FlowHandler 統一處理所有 update/AI/格式/DB，外層只管流程
                    return customerFlowHandler.handleUpdateFieldInput(session, session.updateFieldValue, userId, replyToken);
                } else {
                    sessionService.removeUserSession(userId);
                    return lineMessageUtil.replyText(replyToken, "❌ 已取消更新");
                }
            }

            default -> replyText = "⚠️ 系統錯誤，無法處理目前步驟。";
        }

        // 記得 session 狀態要更新
        sessionService.setUserSession(userId, session);
        return lineMessageUtil.replyText(replyToken, replyText);
    }
}
