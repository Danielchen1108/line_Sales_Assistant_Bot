package com.example.AIrobot.Service;

import com.example.AIrobot.model.UserSession;
import com.example.AIrobot.model.AdvisorSession;

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

    // --- (進階) 清空全部Session ---
    void clearAll();
}
