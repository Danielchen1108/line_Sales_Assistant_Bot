package com.example.AIrobot.Handler;

import com.example.AIrobot.Entity.Admin;
import com.example.AIrobot.Repository.AdminRepository;
import com.example.AIrobot.Service.SessionService;
import com.example.AIrobot.Util.LineMessageUtil;
import com.example.AIrobot.model.AdminSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class AdminHandler {

    private final SessionService sessionService;
    private final AdminRepository adminRepository;
    private final LineMessageUtil lineMessageUtil;

    @Autowired
    public AdminHandler(SessionService sessionService,
                        AdminRepository adminRepository,
                        LineMessageUtil lineMessageUtil) {
        this.sessionService = sessionService;
        this.adminRepository = adminRepository;
        this.lineMessageUtil = lineMessageUtil;
    }

    public ResponseEntity<String> handleAdminSession(String userId, String userMessage, String replyToken) {

        AdminSession adminSession = sessionService.getAdminSession(userId);
        if (adminSession == null) {
            lineMessageUtil.sendLineReply(replyToken, "⚠️ 請先輸入 @設定管理者 開始設定流程");
            return ResponseEntity.ok("OK");
        }

        switch (adminSession.getStep()) {
            case ASK_EMAIL -> {
                adminSession.setEmail(userMessage.trim());
                adminSession.setStep(AdminSession.Step.ASK_NAME);
                lineMessageUtil.sendLineReply(replyToken, "🙋‍♂️ 請輸入管理者姓名：");
            }

            case ASK_NAME -> {
                adminSession.setName(userMessage.trim());
                adminSession.setStep(AdminSession.Step.CONFIRM_DONE);

                Optional<Admin> optionalAdmin = adminRepository.findById(userId);
                Admin admin = optionalAdmin.orElseGet(() -> {
                    Admin newAdmin = new Admin();
                    newAdmin.setId(userId);
                    newAdmin.setCreatedAt(LocalDateTime.now());
                    return newAdmin;
                });

                admin.setEmail(adminSession.getEmail());
                admin.setName(adminSession.getName());
                admin.setLastActiveAt(LocalDateTime.now());

                adminRepository.save(admin);

                String reply = "✅ 管理者設定完成，資料已儲存：\n"
                             + "📧 Email: " + admin.getEmail() + "\n"
                             + "🙋‍♂️ 姓名: " + admin.getName();

                lineMessageUtil.sendLineReply(replyToken, reply);
                sessionService.removeAdminSession(userId);
            }

            case CONFIRM_DONE -> {
                lineMessageUtil.sendLineReply(replyToken, "✅ 設定已完成，若需重新設定請再次輸入 @設定管理者");
            }
        }

        return ResponseEntity.ok("OK");
    }
}
