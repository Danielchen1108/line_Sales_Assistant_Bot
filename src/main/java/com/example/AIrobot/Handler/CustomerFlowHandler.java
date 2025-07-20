// ✅ CustomerFlowHandler.java
package com.example.AIrobot.Handler;

import com.example.AIrobot.Service.CustomerService;
import com.example.AIrobot.Service.SessionService;
import com.example.AIrobot.Util.LineMessageUtil;
import com.example.AIrobot.Entity.Customer;
import com.example.AIrobot.model.UserSession;
import org.springframework.http.ResponseEntity;

public class CustomerFlowHandler {

    private final SessionService sessionService;
    private final CustomerService customerService;
    private final LineMessageUtil lineMessageUtil;

    public CustomerFlowHandler(SessionService sessionService, CustomerService customerService, LineMessageUtil lineMessageUtil) {
        this.sessionService = sessionService;
        this.customerService = customerService;
        this.lineMessageUtil = lineMessageUtil;
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
}
