package com.example.AIrobot.Handler.Customer;

import com.example.AIrobot.Service.CustomerService;
import com.example.AIrobot.Service.OpenAiService;
import com.example.AIrobot.Service.SessionService;
import com.example.AIrobot.Util.LineMessageUtil;
import com.example.AIrobot.Entity.Customer;
import com.example.AIrobot.model.UserSession;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class CustomerHandler {

    private final CustomerService customerService;
    private final OpenAiService openAiService;
    private final SessionService sessionService;
    private final LineMessageUtil lineMessageUtil;
    

    public CustomerHandler(CustomerService customerService, OpenAiService openAiService, SessionService sessionService , LineMessageUtil lineMessageUtil) {
        this.customerService = customerService;
        this.openAiService = openAiService;
        this.sessionService = sessionService;
        this.lineMessageUtil = lineMessageUtil;
    }

    public ResponseEntity<String> handleSession(String userId, String userMessage, String replyToken) {
        if (!sessionService.hasUserSession(userId)) {
            return null; // 不屬於本流程
        }
        UserSession session = sessionService.getUserSession(userId);
        String replyText = "";

        if (userMessage.trim().equals("@取消")) {
            sessionService.removeUserSession(userId);
            someMethod(replyToken, "✅ 已取消操作，回到主選單。");
            return ResponseEntity.ok("OK");
        }

        switch (session.step) {
            // --------- 更新流程 ---------
            case CHOOSE_SAME_NAME_INDEX -> {
                replyText = handleChooseSameNameIndex(session, userMessage);
                sessionService.setUserSession(userId, session);
                 someMethod(replyToken, replyText);
                return ResponseEntity.ok("OK");
            }
            case DELETE_CHOOSE_INDEX -> {
                replyText = handleDeleteChooseIndex(session, userMessage);
                sessionService.setUserSession(userId, session);
                someMethod(replyToken, replyText);
                return ResponseEntity.ok("OK");
            }
            case DELETE_CONFIRM -> {
                replyText = handleDeleteConfirm(session, userMessage, userId);
                sessionService.removeUserSession(userId);
                 someMethod(replyToken, replyText);
                return ResponseEntity.ok("OK");
            }
            case UPDATE_CHOOSE_FIELD -> {
                replyText = handleUpdateChooseField(session, userMessage);
                sessionService.setUserSession(userId, session);
                 someMethod(replyToken, replyText);
                return ResponseEntity.ok("OK");
            }
            case UPDATE_ASK_UPDATE_VALUE -> {
                replyText = handleUpdateAskUpdateValue(session, userMessage, userId);
                sessionService.removeUserSession(userId);
                 someMethod(replyToken, replyText);
                return ResponseEntity.ok("OK");
            }
            // --------- 新增流程 ---------
            
                case ASK_NAME -> {
                    session.name = userMessage.trim();
                    session.step = UserSession.Step.ASK_IDNUMBER;   // 下個步驟
                    sessionService.setUserSession(userId, session);
                    replyText = "🆔 請輸入身分證字號：\n(或輸入\"@略過\"或\"@取消\")";
                }
                case ASK_IDNUMBER -> {
                    String input = userMessage.trim();
                    if (input.equals("@略過")) {
                        session.idNumber = null;
                        session.step = UserSession.Step.ASK_BIRTHDAY;
                        sessionService.setUserSession(userId, session);
                        replyText = "🎂 請輸入出生年月日（例如：1990-01-01）：";
                    } else if (!input.matches("^[A-Z][0-9]{9}$")) {
                        // 格式不對，重來
                        replyText = "❌ 身分證字號格式錯誤，請重新輸入（例如：A123456789）。若無法提供，請輸入 @略過";
                    } else {
                        // 格式正確
                        session.idNumber = input;
                        session.step = UserSession.Step.ASK_BIRTHDAY;
                        sessionService.setUserSession(userId, session);
                        replyText = "🎂 請輸入出生年月日（例如：1990-01-01）：";
                    }
                    someMethod(replyToken, replyText);
                    return ResponseEntity.ok("OK");
}
                case ASK_BIRTHDAY -> {
                    if (userMessage.trim().equals("@略過")) {
                        session.birthday = null;
                    } else {
                        try {
                            session.birthday = LocalDate.parse(userMessage.trim());
                        } catch (Exception e) {
                            replyText = "⚠️ 日期格式錯誤，請用 yyyy-MM-dd，例如：1990-01-01";
                            someMethod(replyToken, replyText);
                            return ResponseEntity.ok("OK");
                        }
                    }
                    session.step = UserSession.Step.ASK_PHONE;
                    sessionService.setUserSession(userId, session);
                    replyText = "📞 請輸入電話：\n(或輸入\"@取消\")";
                }
                case ASK_PHONE -> {
                    session.phone = userMessage.trim();
                    session.step = UserSession.Step.ASK_REGION;
                    sessionService.setUserSession(userId, session);
                    replyText = "📍 請輸入地區：\n(或輸入\"@取消\")";
                }
                // ... 後續照原本邏輯繼續
                case ASK_REGION -> {
                    session.region = userMessage.trim();
                    session.step = UserSession.Step.ASK_AGE;
                    sessionService.setUserSession(userId, session);
                    replyText = "🎂 請輸入年齡可打\"@略過\"\n(或輸入\"@取消\")";
                }
                case ASK_AGE -> {
                    if (userMessage.trim().equals("@略過")) session.age = null;
                    else {
                        try { session.age = Integer.parseInt(userMessage.trim()); }
                        catch(Exception e) { session.age = null; }
                    }
                    session.step = UserSession.Step.ASK_JOB;
                    sessionService.setUserSession(userId, session);
                    replyText = "💼 請輸入職業可打 \"@略過\"\n(或輸入\"@取消\")：";
                }
                case ASK_JOB -> {
                    session.job = userMessage.trim().equals("@略過") ? null : userMessage.trim();
                    session.step = UserSession.Step.ASK_PRODUCTS;
                    sessionService.setUserSession(userId, session);
                    replyText = "🛡️ 請輸入已購險種（多個用逗號分隔，沒填可打 @略過）：";
                }
                case ASK_PRODUCTS -> {
                    session.productsOwned = userMessage.trim().equals("@略過") ? null : userMessage.trim();
                    session.step = UserSession.Step.ASK_STATUS;
                    sessionService.setUserSession(userId, session);
                    replyText = "📝 請輸入客戶目前狀態或需求：";
                }
                case ASK_STATUS -> {
                    session.status = userMessage.trim();
                    session.step = UserSession.Step.CONFIRM;
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
                            + "如正確請輸入\"確認\"，如需取消請輸入\"@取消\"";
                }
                case CONFIRM -> {
                    if (userMessage.trim().equals("確認")) {
                        replyText = handleAddConfirm(session, userId);
                    } else {
                        replyText = "已取消新增。";
                    }
                    sessionService.removeUserSession(userId);
                    someMethod(replyToken, replyText);
                    return ResponseEntity.ok("OK");
                }
                default -> {}
            }

         someMethod(replyToken, replyText);
        return ResponseEntity.ok("OK");
    }

    // ---- 拆出各流程method ----
    //已新增至flow
    private String handleChooseSameNameIndex(UserSession session, String userMessage) {
        try {
            int idx = Integer.parseInt(userMessage.trim());
            if (idx >= 1 && idx <= session.sameNameList.size()) {
                Customer selected = session.sameNameList.get(idx - 1);
                session.selectedCustomerId = selected.getId();
                session.step = UserSession.Step.UPDATE_CHOOSE_FIELD;
                return "請問要更新哪個資料？請輸入數字：\n1. 姓名\n2. 身分證字號\n3. 出生年月日\n4. 電話\n5. 地區\n6. 年齡\n7. 職業\n8. 已購險種\n9. 狀態\n(隨時輸入 @取消 可中止)";
            } else {
                return "請輸入 1~" + session.sameNameList.size() + " 之間的數字。";
            }
        } catch (Exception e) {
            return "請輸入序號數字！";
        }
    }
    //已新增至flow
    private String handleDeleteChooseIndex(UserSession session, String userMessage) {
        try {
            int idx = Integer.parseInt(userMessage.trim());
            if (idx >= 1 && idx <= session.sameNameList.size()) {
                Customer target = session.sameNameList.get(idx - 1);
                session.selectedCustomerId = target.getId();
                session.step = UserSession.Step.DELETE_CONFIRM;
                return "請問你確認要刪除：\n"
                        + "姓名：" + target.getName() + "\n"
                        + "電話：" + (target.getPhone() == null ? "無" : target.getPhone()) + "\n"
                        + "如要刪除請輸入「確認」，取消請輸入 @取消";
            } else {
                return "請輸入有效的編號！";
            }
        } catch (Exception e) {
            return "請輸入有效的編號！";
        }
    }
    //已新增至flow
    private String handleDeleteConfirm(UserSession session, String userMessage, String userId) {
        String replyText;
        if (userMessage.trim().equals("確認")) {
            boolean deleted = customerService.deleteCustomerById(session.selectedCustomerId, userId);
            if (deleted) {
                replyText = "✅ 已刪除成功";
            } else {
                replyText = "❌ 刪除失敗，請稍後再試。";
            }
        } else {
            replyText = "已取消刪除";
        }
        return replyText;
    }
    //已新增至flow
    private String handleUpdateChooseField(UserSession session, String userMessage) {
        try {
            int field = Integer.parseInt(userMessage.trim());
            if (field >= 1 && field <= 9) {
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
                return "請輸入新的 " + fieldName + "：";
            } else {
                return "請輸入 1~9 之間的數字。";
            }
        } catch (Exception e) {
            return "請輸入數字（1~9）！";
        }
    }
    //已新增至flow
    private String handleUpdateAskUpdateValue(UserSession session, String userMessage, String userId) {
            String value = userMessage.trim();
            int idx = session.updateFieldIndex;
            Customer customer = customerService.findById(session.selectedCustomerId);
            boolean updated = false;
            String fieldName = "";
            if (customer != null) {
                switch (idx) {
                    case 1 -> { customer.setName(value); fieldName = "姓名"; updated = true; }
                    case 2 -> { customer.setIdNumber(value); fieldName = "身分證字號"; updated = true; }
                    case 3 -> {
                        try {
                            // 生日格式驗證，建議 yyyy-MM-dd
                            customer.setBirthday(java.time.LocalDate.parse(value));
                            fieldName = "(西元)出生年月日";
                            updated = true;
                        } catch (Exception e) {
                            return "⚠️ 生日格式錯誤，請用 yyyy-MM-dd，例如：1990-01-01";
                        }
                    }
                    case 4 -> { customer.setPhone(value); fieldName = "電話"; updated = true; }
                    case 5 -> { customer.setRegion(value); fieldName = "地區"; updated = true; }
                    case 6 -> {
                        try { customer.setAge(Integer.parseInt(value)); }
                        catch(Exception e) { customer.setAge(null); }
                        fieldName = "年齡"; updated = true;
                    }
                    case 7 -> { customer.setJob(value); fieldName = "職業"; updated = true; }
                    case 8 -> { customer.setProductsOwned(value); fieldName = "已購險種"; updated = true; }
                    case 9 -> { customer.setStatus(value); fieldName = "狀態"; updated = true; }
                }
                if (updated) {
                    try {
                        String resultJson = openAiService.analyzeCustomerPotential(customer);
                        JSONObject result = new JSONObject(resultJson);
                        customer.setPotentialLevel(result.optString("成交機會", "未知"));
                        customer.setAiComment(result.optString("評價", "無"));
                        customer.setAiProductAdvice(result.optString("建議產品","無"));
                        customer.setAiFollowUp(result.optString("後續建議","無"));
                        customer.setAiTags(result.optString("標籤","無"));
                        customerService.addCustomer(customer);
                    } catch (Exception e) {
                        customer.setPotentialLevel("未知");
                        customer.setAiComment("AI 分析失敗");
                        customer.setAiProductAdvice("沒有建議");
                        customer.setAiFollowUp("沒有建議");
                        customer.setAiTags("沒有標籤");
                        customerService.addCustomer(customer);
                    }
                }
                String updateTime = customer.getUpdatedAt() != null
                        ? customer.getUpdatedAt().toLocalDate().toString() : "無";
                return "✅ 顧客已更新：\n"
                        + "👤 姓名：" + customer.getName() + "\n"
                        + "🆔 身分證字號：" + (customer.getIdNumber() == null ? "未填" : customer.getIdNumber()) + "\n"
                        + "🎂 出生年月日：" + (customer.getBirthday() == null ? "未填" : customer.getBirthday().toString()) + "\n"
                        + "📞 電話：" + customer.getPhone() + "\n"
                        + "📍 地區：" + customer.getRegion() + "\n"
                        + "🎂 年齡：" + (customer.getAge() == null ? "未填" : customer.getAge()) + "\n"
                        + "💼 職業：" + (customer.getJob() == null ? "未填" : customer.getJob()) + "\n"
                        + "🛡️ 已購險種：" + (customer.getProductsOwned() == null ? "未填" : customer.getProductsOwned()) + "\n"
                        + "📝 狀態：" + customer.getStatus() + "\n"
                        + "🔥 成交機會：" + (customer.getPotentialLevel() != null ? customer.getPotentialLevel() : "AI尚未分析") + "\n"
                        + "🤖 評價：" + (customer.getAiComment() != null ? customer.getAiComment() : "AI尚未分析") + "\n"
                        + "🛒 建議產品：" + (customer.getAiProductAdvice() != null ? customer.getAiProductAdvice() : "AI尚未分析") + "\n"
                        + "📌 後續建議：" + (customer.getAiFollowUp() != null ? customer.getAiFollowUp() : "AI尚未分析") + "\n"
                        + "🏷️ 標籤：" + (customer.getAiTags() != null ? customer.getAiTags() : "AI尚未分析") + "\n"
                        + "最後更新時間：" + updateTime;
            } else {
                return "⚠️ 查無顧客！";
            }
        }

    //已新增至flow
    private String handleAddConfirm(UserSession session, String userId) {
    // 身分證只允許 1個大寫字母 + 9個數字
    

        Customer customer = new Customer();
        customer.setName(session.name);
        customer.setIdNumber(session.idNumber);
        customer.setBirthday(session.birthday);
        customer.setPhone(session.phone);
        customer.setRegion(session.region);
        customer.setStatus(session.status);
        customer.setAge(session.age);
        customer.setJob(session.job);
        customer.setProductsOwned(session.productsOwned);
        customer.setCreatedBy(userId);

        customerService.addCustomer(customer);
        try {
            String resultJson = openAiService.analyzeCustomerPotential(customer);
            JSONObject result = new JSONObject(resultJson);
            customer.setPotentialLevel(result.optString("成交機會", "未知"));
            customer.setAiComment(result.optString("評價", "無"));
            customer.setAiProductAdvice(result.optString("建議產品", "無"));
            customer.setAiFollowUp(result.optString("後續建議", "無"));
            customer.setAiTags(result.optString("標籤", "無"));
            customerService.addCustomer(customer);
        } catch (Exception e) {
            customer.setPotentialLevel("未知");
            customer.setAiComment("AI 分析失敗");
            customer.setAiProductAdvice("沒有建議");
            customer.setAiFollowUp("沒有建議");
            customer.setAiTags("沒有標籤");
            customerService.addCustomer(customer);
        }
        String updateTime = customer.getUpdatedAt() != null
                ? customer.getUpdatedAt().toLocalDate().toString()
                : "無";
        return "✅ 顧客已新增：\n"
                + "👤 姓名：" + customer.getName() + "\n"
                + "🆔 身分證字號：" + (customer.getIdNumber() == null ? "未填" : customer.getIdNumber()) + "\n"
                + "🎂 出生年月日：" + (customer.getBirthday() == null ? "未填" : customer.getBirthday().toString()) + "\n"
                + "📞 電話：" + customer.getPhone() + "\n"
                + "📍 地區：" + customer.getRegion() + "\n"
                + "🎂 年齡：" + (customer.getAge() == null ? "未填" : customer.getAge()) + "\n"
                + "💼 職業：" + (customer.getJob() == null ? "未填" : customer.getJob()) + "\n"
                + "🛡️ 已購險種：" + (customer.getProductsOwned() == null ? "未填" : customer.getProductsOwned()) + "\n"
                + "📝 狀態：" + customer.getStatus() + "\n"
                + "🌟 成交機會：" + (customer.getPotentialLevel() != null ? customer.getPotentialLevel() : "AI尚未分析") + "\n"
                + "🤖 評價：" + (customer.getAiComment() != null ? customer.getAiComment() : "AI尚未分析") + "\n"
                + "🛒 建議產品：" + (customer.getAiProductAdvice() != null ? customer.getAiProductAdvice() : "AI尚未分析") + "\n"
                + "📌 後續建議：" + (customer.getAiFollowUp() != null ? customer.getAiFollowUp() : "AI尚未分析") + "\n"
                + "🏷️ 標籤：" + (customer.getAiTags() != null ? customer.getAiTags() : "AI尚未分析") + "\n"
                + "最後更新時間：" + updateTime;
    }


        //查詢客戶 
        //已新增至Command
         public ResponseEntity<String> handleQueryCustomer(String userId, String name, String replyToken) {
            List<Customer> list = customerService.findAllByNameAndCreatedBy(name, userId);
            StringBuilder sb = new StringBuilder();

            if (list == null || list.isEmpty()) {
                sb.append("❌ 查無客戶：").append(name);
            } else {
                sb.append("查詢到 ").append(list.size()).append(" 筆同名客戶：\n");
                for (int i = 0; i < list.size(); i++) {
                    Customer c = list.get(i);
                    sb.append("【第 ").append(i + 1).append(" 筆】\n")
                    .append("👤 姓名：").append(c.getName() != null ? c.getName() : "未填").append("\n")
                    .append("🆔 身分證字號：").append(c.getIdNumber()!= null ? c.getName() : "未填").append("\n")
                    .append("🎂 出生年月日：").append(c.getBirthday() != null ? c.getBirthday().toString() : "未填").append("\n") 
                    .append("📞 電話：").append(c.getPhone() != null ? c.getPhone() : "未填").append("\n")
                    .append("📍 地區：").append(c.getRegion() != null ? c.getRegion() : "未填").append("\n")
                    .append("🔥 成交機會：").append(c.getPotentialLevel() != null ? c.getPotentialLevel() : "AI尚未分析").append("\n")
                    .append("📝 狀態：").append(c.getStatus() != null ? c.getStatus() : "未填").append("\n")
                    .append("ID：").append(c.getId())
                    .append("\n----------------\n");
                }
            }
            someMethod(replyToken, sb.toString());
            return ResponseEntity.ok("OK");
        }

            // // 補充：身分證遮蔽 function
            // private String maskId(String idNumber) {
            //     if (idNumber == null || idNumber.length() != 10) return "未填";
            //     return idNumber.substring(0, 3) + "****" + idNumber.substring(7);
            // }


    
    //更新
    //已新增至flow
            public ResponseEntity<String> handleUpdateCustomer(String userId, String name, String replyToken) {
            List<Customer> list = customerService.findAllByNameAndCreatedBy(name, userId);
            if (list == null || list.isEmpty()) {
                 someMethod(replyToken, "❌ 查無顧客：" + name);
                return ResponseEntity.ok("OK");
            }
            UserSession session = new UserSession();
            session.step = UserSession.Step.CHOOSE_SAME_NAME_INDEX;
            session.sameNameList = list;
            sessionService.setUserSession(userId, session);

            StringBuilder sb = new StringBuilder();
            sb.append("查到多筆同名顧客，請輸入欲更新的編號：\n");
            for (int i = 0; i < list.size(); i++) {
                Customer c = list.get(i);
                sb.append((i + 1)).append(". ")
                .append(c.getName()).append(" / ")
                .append(c.getPhone() == null ? "未填" : c.getPhone())
                .append(" / ").append(c.getRegion() == null ? "未填" : c.getRegion())
                .append("\n");
            }
             someMethod(replyToken, sb.toString());
            return ResponseEntity.ok("OK");
        }
        //刪除
        //已新增至flow
            public ResponseEntity<String> handleDeleteCustomer(String userId, String name, String replyToken) {
            List<Customer> list = customerService.findAllByNameAndCreatedBy(name, userId);
            if (list == null || list.isEmpty()) {
                 someMethod(replyToken, "❌ 查無顧客：" + name);
                return ResponseEntity.ok("OK");
            }
            UserSession session = new UserSession();
            session.step = UserSession.Step.DELETE_CHOOSE_INDEX;
            session.sameNameList = list;
            sessionService.setUserSession(userId, session);

            StringBuilder sb = new StringBuilder();
            sb.append("查到多筆同名顧客，請輸入欲刪除的編號：\n");
            for (int i = 0; i < list.size(); i++) {
                Customer c = list.get(i);
                sb.append((i + 1)).append(". ")
                .append(c.getName()).append(" / ")
                .append(c.getPhone() == null ? "未填" : c.getPhone())
                .append(" / ").append(c.getRegion() == null ? "未填" : c.getRegion())
                .append("\n");
            }
            someMethod(replyToken, sb.toString());
            return ResponseEntity.ok("OK");
        }

            //列表
            //已新增至command
            public ResponseEntity<String> handleListAllCustomers(String userId, String replyToken) {
            List<Customer> allList = customerService.getAllCustomersByCreatedBy(userId); // 只撈該 user 建立的

            if (allList == null || allList.isEmpty()) {
                 someMethod(replyToken, "尚無顧客資料。");
                return ResponseEntity.ok("OK");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("【所有客戶列表】\n");
            int maxDisplay = 20; // 最多顯示20筆（可調整）
            for (int i = 0; i < allList.size() && i < maxDisplay; i++) {
                Customer c = allList.get(i);
                sb.append("👤 ").append(c.getName() != null ? c.getName() : "未填")
                .append("\n📞 ").append(c.getPhone() != null ? c.getPhone() : "未填")
                .append("\n📍 ").append(c.getRegion() != null ? c.getRegion() : "未填")
                .append("\n🔥 ").append(c.getPotentialLevel() != null ? c.getPotentialLevel() : "AI尚未分析")
                .append("\n----------------\n");
            }

            if (allList.size() > maxDisplay) {
                sb.append("......(僅顯示前 ").append(maxDisplay).append(" 筆)\n");
            }

             someMethod(replyToken, sb.toString());
            return ResponseEntity.ok("OK");
        }

        //已新增至command
            public ResponseEntity<String> handleTopCustomers(String createdBy, int limit, String replyToken) {
                List<Customer> allList = customerService.getAllCustomersByCreatedBy(createdBy);

                if (allList == null || allList.isEmpty()) {
                    someMethod(replyToken, "尚無客戶資料。");
                    return ResponseEntity.ok("OK");
                }

              

                // 過濾有有效分數（含「分」字的數字）
                List<Customer> validList = allList.stream()
                        .filter(c -> c.getPotentialLevel() != null && isNumeric(c.getPotentialLevel()))
                        .collect(Collectors.toList());


                if (validList.isEmpty()) {
                    someMethod(replyToken, "目前沒有分析出成交分數的顧客資料。");
                    return ResponseEntity.ok("OK");
                }

                // 依分數排序（將「分」字去掉）
                validList.sort((c1, c2) -> 
                    Integer.compare(
                        Integer.parseInt(c2.getPotentialLevel().replace("分", "").trim()),
                        Integer.parseInt(c1.getPotentialLevel().replace("分", "").trim())
                    )
                );

                StringBuilder sb = new StringBuilder();
                sb.append("【成交率最高前 ").append(limit).append(" 位顧客】\n");
                for (int i = 0; i < validList.size() && i < limit; i++) {
                    Customer c = validList.get(i);
                    sb.append("NO.").append(i + 1)
                        .append("｜👤").append(c.getName() != null ? c.getName() : "未填")
                        .append("\n📞").append(c.getPhone() != null ? c.getPhone() : "未填")
                        .append("\n🔥成交分數：").append(c.getPotentialLevel())
                        .append(c.getAiComment() != null ? "\n🤖" + c.getAiComment() : "")
                        .append("\n----------------\n");
                }

                someMethod(replyToken, sb.toString());
                return ResponseEntity.ok("OK");
            }

            //已新增至command
            private boolean isNumeric(String str) {
                if (str == null || str.trim().isEmpty()) return false;
                str = str.trim().replace("分", "");
                try {
                    Integer.parseInt(str);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            }


        public void someMethod(String replyToken, String replyText){
            lineMessageUtil.sendLineReply(replyToken, replyText);
        }
    
}
