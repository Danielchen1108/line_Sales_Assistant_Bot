// ✅ CustomerCommandHandler.java
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
import java.util.List;
import java.util.stream.Collectors;
@Component
public class CustomerCommandHandler {

    private final SessionService sessionService;
    private final LineMessageUtil lineMessageUtil;
    private final CustomerService customerService;
    private final CustomerFlowHandler customerFlowHandler;

    public CustomerCommandHandler(SessionService sessionService, LineMessageUtil lineMessageUtil,CustomerService customerService,CustomerFlowHandler customerFlowHandler) {

        this.sessionService = sessionService;
        this.lineMessageUtil = lineMessageUtil;
        this.customerService = customerService;
        this.customerFlowHandler = customerFlowHandler;
    }

        public ResponseEntity<String> handleCommand(String userId, String userMessage, String replyToken) {
            userMessage = userMessage.trim();

            // --- 📌 @選單 ---
            if (userMessage.equals("@選單")) {
                return lineMessageUtil.replyText(replyToken,
                    """
                    【功能選單】
                    @新增
                    @查詢 姓名
                    @更新 姓名
                    @刪除 姓名
                    @列出所有客戶
                    @列出成交率最高的人數 N
                    @顧問服務
                    @取消
                    @選單
                    """);
            }

            // --- 🧾 單步指令 ---
            if (userMessage.equals("@新增")) {
                return handleStartNew(userId, replyToken);
            }

            if (userMessage.startsWith("@查詢")) {
                String name = userMessage.replaceFirst("@查詢", "").trim();
                return handleQueryCustomer(userId, name, replyToken);
            }

            if (userMessage.equals("@列出所有客戶")) {
                return handleListAllCustomers(userId, replyToken);
            }

            if (userMessage.startsWith("@列出成交率最高的人數")) {
                String numStr = userMessage.replace("@列出成交率最高的人數", "").trim();
                int limit = 10;
                try {
                    limit = Integer.parseInt(numStr);
                    if (limit <= 0) limit = 10;
                } catch (Exception e) { /* fallback limit = 10 */ }
                return handleTopCustomers(userId, limit, replyToken);
            }

            // --- 🔁 多步驟流程轉交給 FlowHandler ---
            if (userMessage.startsWith("@更新")) {
                String name = userMessage.replaceFirst("@更新", "").trim();
                return customerFlowHandler.handleSelectSameNameCustomer(userId, name, replyToken);
            }

            if (userMessage.startsWith("@刪除")) {
                String name = userMessage.replaceFirst("@刪除", "").trim();
                return customerFlowHandler.handleSelectDeleteCustomer(userId, name, replyToken);
            }

            // --- ❌ 無效指令 ---
            return lineMessageUtil.replyText(replyToken, "❌ 無效指令，請輸入 @選單 查看功能列表");
        }



    //---------  新增流程 ----------
    public ResponseEntity<String> handleStartNew(String userId, String replyToken) {
        UserSession session = new UserSession();
        session.step = Step.ASK_NAME;
        sessionService.setUserSession(userId, session);
        return lineMessageUtil.replyText(replyToken, "👤 請輸入顧客姓名：");
    }

    // 查詢指定姓名的顧客清單
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
                .append("🆔 身分證字號：").append(c.getIdNumber()!= null ? c.getIdNumber() : "未填").append("\n") // ← 這邊原本錯誤
                .append("🎂 出生年月日：").append(c.getBirthday() != null ? c.getBirthday().toString() : "未填").append("\n") 
                .append("📞 電話：").append(c.getPhone() != null ? c.getPhone() : "未填").append("\n")
                .append("📍 地區：").append(c.getRegion() != null ? c.getRegion() : "未填").append("\n")
                .append("🔥 成交機會：").append(c.getPotentialLevel() != null ? c.getPotentialLevel() : "AI尚未分析").append("\n")
                .append("📝 狀態：").append(c.getStatus() != null ? c.getStatus() : "未填").append("\n")
                .append("ID：").append(c.getId())
                .append("\n----------------\n");
            }
        }

        lineMessageUtil.replyText(replyToken, sb.toString()); 
        return ResponseEntity.ok("OK");
    }

            /**
         * 查詢該使用者建立的所有顧客，並以文字列表格式回傳最多 20 筆資料。
         *
         * @param userId 使用者的 LINE ID（作為建立者身份）
         * @param replyToken 用於回覆 LINE 訊息
         * @return 顧客列表的訊息內容
         */


    public ResponseEntity<String>handleListAllCustomers(String userId, String replyToken) {
      List<Customer> allList = customerService.getAllCustomersByCreatedBy(userId); // 只撈該 user 建立的

            if (allList == null || allList.isEmpty()) {
                 lineMessageUtil.replyText(replyToken, "尚無顧客資料。");
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

            lineMessageUtil.replyText(replyToken, sb.toString());
            return ResponseEntity.ok("OK");
        }
    public ResponseEntity<String> handleTopCustomers(String createdBy, int limit, String replyToken) {
       List<Customer> allList = customerService.getAllCustomersByCreatedBy(createdBy);

        if (allList == null || allList.isEmpty()) {
            lineMessageUtil.replyText(replyToken, "尚無客戶資料。");
            return ResponseEntity.ok("OK");
        }

        

        // 過濾有有效分數（含「分」字的數字）
        List<Customer> validList = allList.stream()
                .filter(c -> c.getPotentialLevel() != null && isNumeric(c.getPotentialLevel()))
                .collect(Collectors.toList());


        if (validList.isEmpty()) {
            lineMessageUtil.replyText(replyToken, "目前沒有分析出成交分數的顧客資料。");
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

        lineMessageUtil.replyText(replyToken, sb.toString());
        return ResponseEntity.ok("OK");
    }
    
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

    
   
}
