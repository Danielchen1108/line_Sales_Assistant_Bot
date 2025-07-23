// ✅ CustomerFlowHandler.java
package com.example.AIrobot.Handler.Customer;

import com.example.AIrobot.Service.CustomerService;
import com.example.AIrobot.Service.OpenAiService;
import com.example.AIrobot.Service.SessionService;
import com.example.AIrobot.Util.LineMessageUtil;
import com.example.AIrobot.Entity.Customer;
import com.example.AIrobot.model.UserSession;

import java.util.List;

import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class CustomerFlowHandler {

    private final SessionService sessionService;
    private final CustomerService customerService;
    private final LineMessageUtil lineMessageUtil;
    private final OpenAiService openAiService;

    public CustomerFlowHandler(SessionService sessionService, CustomerService customerService, LineMessageUtil lineMessageUtil,OpenAiService openAiService) {
        this.sessionService = sessionService;
        this.customerService = customerService;
        this.lineMessageUtil = lineMessageUtil;
        this.openAiService = openAiService;
    }

    /**
     * 根據當前使用者的 session 狀態 (step)，分派對應的流程處理邏輯。
     * <p>
     * 適用於更新、刪除、選擇欄位等進階互動流程，透過 session.step 進行 switch 控制，
     * 搭配對應方法處理使用者的輸入與流程推進。
     * <p>
     * 若輸入為 @取消，將清除 session 並回覆終止訊息。
     *
     * @param userId 使用者的 LINE ID
     * @param userMessage 使用者輸入的文字內容
     * @param replyToken 用於回覆 LINE 的回應 token
     * @return ResponseEntity 文字回覆結果
     */


     public ResponseEntity<String> handleSession(String userId, String userMessage, String replyToken) {
        if (!sessionService.hasUserSession(userId)) {
            return null; // 不屬於本流程
        }
        UserSession session = sessionService.getUserSession(userId);

        if (userMessage.trim().equals("@取消")) {
            sessionService.removeUserSession(userId);
            return lineMessageUtil.replyText(replyToken, "✅ 已取消操作，回到主選單。");
        }

        switch (session.step) {
            case CHOOSE_SAME_NAME_INDEX -> {
                return handleSelectSameNameCustomer(session, userMessage, replyToken, userId);
            }
            case DELETE_CHOOSE_INDEX -> {
                return handleSelectDeleteCustomer(session, userMessage, replyToken, userId);
            }
            case DELETE_CONFIRM -> {
                sessionService.removeUserSession(userId);
                return handleDeleteConfirmation(session, userMessage, userId, replyToken);
            }
            case UPDATE_CHOOSE_FIELD -> {
                sessionService.setUserSession(userId, session);
                return handleUpdateFieldSelection(session, userMessage, replyToken, userId);
            }
            case UPDATE_ASK_UPDATE_VALUE -> {
                sessionService.removeUserSession(userId);
                return handleUpdateFieldInput(session, userMessage, userId, replyToken);
            }
            default -> {
                return lineMessageUtil.replyText(replyToken, "⚠️ 發生未知錯誤，請重新操作。或輸入 @取消。");
            }
        }
    }

    /**
     * 使用者選擇同名顧客時處理流程。
     */
    public ResponseEntity<String> handleSelectSameNameCustomer(UserSession session, String userMessage, String replyToken, String userId) {
        try {
            int idx = Integer.parseInt(userMessage.trim());
            if (idx >= 1 && idx <= session.sameNameList.size()) {
                Customer selected = session.sameNameList.get(idx - 1);
                session.selectedCustomerId = selected.getId();
                session.step = UserSession.Step.UPDATE_CHOOSE_FIELD;
                sessionService.setUserSession(userId, session);
                return lineMessageUtil.replyText(replyToken, "請問要更新哪個資料？請輸入數字：\n1. 姓名\n2. 身分證字號\n3. 出生年月日\n4. 電話\n5. 地區\n6. 年齡\n7. 職業\n8. 已購險種\n9. 狀態\n(隨時輸入 @取消 可中止)");
            } else {
                return lineMessageUtil.replyText(replyToken, "請輸入 1~" + session.sameNameList.size() + " 之間的數字。");
            }
        } catch (Exception e) {
            return lineMessageUtil.replyText(replyToken, "請輸入序號數字！");
        }
    }

    /**
     * 使用者選擇要刪除的顧客資料
     */
    public ResponseEntity<String> handleSelectDeleteCustomer(UserSession session, String userMessage, String replyToken, String userId) {
        try {
            int idx = Integer.parseInt(userMessage.trim());
            if (idx >= 1 && idx <= session.sameNameList.size()) {
                Customer selected = session.sameNameList.get(idx - 1);
                session.selectedCustomerId = selected.getId();
                session.step = UserSession.Step.DELETE_CONFIRM;
                sessionService.setUserSession(userId, session);
                return lineMessageUtil.replyText(replyToken, "🗑️ 確定要刪除這筆資料嗎？請輸入『確認』或輸入其他內容取消");
            } else {
                return lineMessageUtil.replyText(replyToken, "請輸入 1~" + session.sameNameList.size() + " 之間的數字。");
            }
        } catch (Exception e) {
            return lineMessageUtil.replyText(replyToken, "請輸入序號數字！");
        }
    }

    /**
     * 刪除資料確認
     */
    public ResponseEntity<String> handleDeleteConfirmation(UserSession session, String userMessage, String userId, String replyToken) {
        if (userMessage.trim().equals("確認")) {
            boolean deleted = customerService.deleteCustomerById(session.selectedCustomerId, userId);
            return lineMessageUtil.replyText(replyToken, deleted ? "✅ 已刪除成功" : "❌ 刪除失敗，請稍後再試。");
        } else {
            return lineMessageUtil.replyText(replyToken, "已取消刪除");
        }
    }

    /**
     * 選擇欲修改的欄位
     */
    public ResponseEntity<String> handleUpdateFieldSelection(UserSession session, String userMessage, String replyToken, String userId) {
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
                return ResponseEntity.ok("請輸入新的 " + fieldName + "：");
            } else {
                return ResponseEntity.ok("請輸入 1~9 之間的數字。");
            }
        } catch (Exception e) {
            return ResponseEntity.ok("請輸入數字（1~9）！");
        }
    }

    /**
     * 接收更新值，並儲存資料
     */
    public ResponseEntity<String> handleUpdateFieldInput(UserSession session, String userMessage, String userId, String replyToken) {
        boolean updated = customerService.updateCustomerFieldById(session.selectedCustomerId, session.updateFieldIndex, userMessage);

        return lineMessageUtil.replyText(replyToken, updated ? "✅ 修改成功" : "❌ 修改失敗");
    }

    /**
     * 最後新增確認後儲存資料
     */
    public ResponseEntity<String> handleFinalAddConfirmation(UserSession session, String userMessage, String userId, String replyToken) {
        if (userMessage.trim().equals("確認")) {
            Customer customer = new Customer();
            customer.setName(session.name);
            customer.setIdNumber(session.idNumber);
            customer.setBirthday(session.birthday);
            customer.setPhone(session.phone);
            customer.setRegion(session.region);
            customer.setAge(session.age);
            customer.setJob(session.job);
            customer.setProductsOwned(session.productsOwned);
            customer.setStatus(session.status);
            customer.setCreatedBy(userId);

            customerService.addCustomer(customer);
            return lineMessageUtil.replyText(replyToken, "✅ 顧客新增成功！");
        } else {
            return lineMessageUtil.replyText(replyToken, "❌ 已取消新增。\n如需重新開始請輸入 @新增");
        }
    }

    /*
     * 處理使用者輸入的欄位值，根據 UserSession 中選擇的欄位 (updateFieldIndex)
     * 隊指定顧客進行欄位更新，並呼叫OpenAI重新分析後儲存
     * @param session 當前使用者的對話 Session，包含顧客 ID 與欲更新欄位 index
     * @param userMessage 使用者輸入的新欄位值
     * @param userId 使用者 LINE ID（用來綁定資料建立者）
     * @return 回傳更新結果文字
     */

    public String handleUpdateFieldInput(UserSession session, String userMessage, String userId) {
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

            // 回傳格式略
            return formatCustomerReply(customer);
        } else {
            return "⚠️ 查無顧客！";
        }
    }

    
            /**
         * 將顧客資料格式化為多行的 LINE 回覆文字。
         * 若資料為 null，將顯示「未填」或「AI尚未分析」等預設說明。
         *
         * @param customer 顧客物件，需包含基本欄位與 AI 分析結果
         * @return 回傳格式化後的顧客資訊文字
         */

    private String formatCustomerReply(Customer customer) {
        String updateTime = customer.getUpdatedAt() != null
                ? customer.getUpdatedAt().toLocalDate().toString() : "無";
        return "✅ 顧客已更新：\n"
                + "👤 姓名：" + customer.getName() + "\n"
                + "🆔 身分證字號：" + (customer.getIdNumber() == null ? "未填" : customer.getIdNumber()) + "\n"
                + "🎂 出生年月日：" + (customer.getBirthday() == null ? "未填" : customer.getBirthday()) + "\n"
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
    }


            /**
         * 處理新增顧客流程的最後確認步驟。
         * 
         * 將 UserSession 中的暫存資料組成一個 Customer 實體，
         * 初步儲存後呼叫 OpenAI API 進行潛力分析，並將分析結果更新到 Customer 資料中，
         * 再次儲存後回傳格式化的顧客資訊訊息。
         *
         * @param session 使用者的暫存對話資料（含欄位內容）
         * @param userId 使用者的 LINE ID（作為顧客 createdBy）
         * @return 顯示已新增顧客的詳細資料與 AI 分析結果
         */


    public String handleFinalAddConfirmation(UserSession session, String userId) {
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

        return formatCustomerReply(customer);
    }

    /**
     * 啟動更新流程：當輸入 @更新 + 姓名 指令時，查找所有同名顧客，
     * 並引導使用者輸入欲修改的那一筆資料的編號。
     *
     * @param userId 使用者 ID（用於資料綁定）
     * @param name 欲更新的顧客姓名（可能有多筆）
     * @param replyToken LINE 回覆用 token
     * @return 回覆選擇清單，並等待使用者輸入編號
     */


    public ResponseEntity<String> handleSelectSameNameCustomer(String userId, String name, String replyToken) {
            List<Customer> list = customerService.findAllByNameAndCreatedBy(name, userId);
            if (list == null || list.isEmpty()) {
                 lineMessageUtil.replyText(replyToken, "❌ 查無顧客：" + name);
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
             lineMessageUtil.replyText(replyToken, sb.toString());
            return ResponseEntity.ok("OK");
        }
        
                /**
         * 啟動刪除流程，根據使用者輸入的姓名查找所有同名顧客，
         * 並引導使用者輸入欲刪除的顧客編號。
         * @刪除
         * @param userId 使用者 LINE ID
         * @param name 查詢的顧客姓名（可能有多筆同名）
         * @param replyToken 回覆用的 token
         * @return 顯示選擇列表並等待使用者輸入編號
         */


            public ResponseEntity<String>  handleSelectDeleteCustomer(String userId, String name, String replyToken) {
            List<Customer> list = customerService.findAllByNameAndCreatedBy(name, userId);
            if (list == null || list.isEmpty()) {
                lineMessageUtil.replyText(replyToken, "❌ 查無顧客：" + name);
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
            lineMessageUtil.replyText(replyToken, sb.toString());
            return ResponseEntity.ok("OK");
        }

}
