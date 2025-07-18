package com.example.AIrobot.Service;

import com.example.AIrobot.model.UserSession;
import com.example.AIrobot.model.AdvisorSession;
import com.example.AIrobot.model.AdminSession;

public interface SessionService {

    // --- User Session ---
    UserSession getUserSession(String userId);

    void setUserSession(String userId, UserSession session);

    void removeUserSession(String userId);

    boolean hasUserSession(String userId);

    // --- Advisor Session ---
    AdvisorSession getAdvisorSession(String userId);

    void setAdvisorSession(String userId, AdvisorSession session);

    void removeAdvisorSession(String userId);

    boolean hasAdvisorSession(String userId);

    // --- Admin Session ---
    AdminSession getAdminSession(String userId);

    void setAdminSession(String userId, AdminSession session);

    void removeAdminSession(String userId);

    boolean hasAdminSession(String userId);

    // --- (進階) 清空全部 Session ---
    void clearAll();
}
