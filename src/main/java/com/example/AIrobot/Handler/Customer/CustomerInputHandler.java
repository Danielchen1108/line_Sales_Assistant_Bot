package com.example.AIrobot.Handler.Customer;

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


    public CustomerInputHandler(SessionService sessionService, LineMessageUtil lineMessageUtil,CustomerService customerService) {
        this.sessionService = sessionService;
        this.lineMessageUtil = lineMessageUtil;
        this.customerService = customerService;
    }
    //新增流程
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

             
        // === 更新流程 ===
            case CHOOSE_SAME_NAME_INDEX -> {
                try {
                    int index = Integer.parseInt(input);
                    if (index < 1 || index > session.sameNameList.size()) {
                        return lineMessageUtil.replyText(replyToken, "❌ 請輸入有效的編號（1 ~ " + session.sameNameList.size() + "）");
                    }
                    session.selectedCustomer = session.sameNameList.get(index - 1);
                    session.selectedCustomerId = session.selectedCustomer.getId();
                    session.step = UserSession.Step.UPDATE_CHOOSE_FIELD;
                    replyText = "請輸入要更新的欄位編號：\n1. 姓名\n2. 身分證字號\n3. 出生年月日\n4. 電話\n5. 地區\n6. 年齡\n7. 職業\n8. 已購險種\n9. 狀態";
                } catch (Exception e) {
                    return lineMessageUtil.replyText(replyToken, "❌ 請輸入正確的數字編號！");
                }
            }

            case UPDATE_CHOOSE_FIELD -> {
                try {
                    int field = Integer.parseInt(input);
                    if (field < 1 || field > 9) {
                        return lineMessageUtil.replyText(replyToken, "❌ 請輸入1~9之間的數字。");
                    }
                    session.updateFieldIndex = field;
                    session.step = UserSession.Step.UPDATE_ASK_UPDATE_VALUE;
                    String fieldName = switch (field) {
                        case 1 -> "姓名";
                        case 2 -> "身分證字號";
                        case 3 -> "出生年月日";
                        case 4 -> "電話";
                        case 5 -> "地區";
                        case 6 -> "年齡";
                        case 7 -> "職業";
                        case 8 -> "已購險種";
                        case 9 -> "狀態";
                        default -> "";
                    };
                    replyText = "請輸入新的 " + fieldName + "：";
                } catch (Exception e) {
                    replyText = "❌ 請輸入1~9之間的數字。";
                }
            }

            case UPDATE_ASK_UPDATE_VALUE -> {
                switch (session.updateFieldIndex) {
                   
                    case 1 -> session.selectedCustomer.setName(input);
                    case 2 -> session.selectedCustomer.setIdNumber(input);
                    case 3 -> {
                        try {
                            session.selectedCustomer.setBirthday(LocalDate.parse(input));
                        } catch (Exception e) {
                            return lineMessageUtil.replyText(replyToken, "⚠️ 日期格式錯誤，請用 yyyy-MM-dd。");
                        }
                    }
                    case 4 -> session.selectedCustomer.setPhone(input);
                    case 5 -> session.selectedCustomer.setRegion(input);
                    case 6 -> {
                        try {
                            session.selectedCustomer.setAge(Integer.parseInt(input));
                        } catch (Exception e) {
                            return lineMessageUtil.replyText(replyToken, "⚠️ 請輸入數字。");
                        }
                    }
                    case 7 -> session.selectedCustomer.setJob(input);
                    case 8 -> session.selectedCustomer.setProductsOwned(input);
                    case 9 -> session.selectedCustomer.setStatus(input);
                }
                 session.updateFieldValue = input;
                session.step = UserSession.Step.UPDATE_CONFIRM;
                replyText = "請輸入「確認」儲存修改，或「@取消」放棄。";
            }

           case UPDATE_CONFIRM -> {
                if (input.equals("確認")) {
                    boolean success = customerService.updateCustomerFieldById(
                        session.selectedCustomerId,
                        session.updateFieldIndex,
                        session.updateFieldValue  // 你可以在 UPDATE_ASK_UPDATE_VALUE case 裡暫存新值到這
                    );
                    sessionService.removeUserSession(userId);
                    replyText = success ? "✅ 資料已更新！" : "❌ 更新失敗，請稍後再試";
                    return lineMessageUtil.replyText(replyToken, replyText);
                } else {
                    sessionService.removeUserSession(userId);
                    replyText = "❌ 已取消更新";
                    return lineMessageUtil.replyText(replyToken, replyText);
                }
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
