package com.example.AIrobot.Service.ServiceImpl;

import com.example.AIrobot.Service.SessionService;
import com.example.AIrobot.model.UserSession;
import com.example.AIrobot.model.AdvisorSession;
import com.example.AIrobot.model.AdminSession;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionServiceImpl implements SessionService {

    // 一般用戶 Session
    private final Map<String, UserSession> userSessionMap = new ConcurrentHashMap<>();

    // 顧問服務 Session
    private final Map<String, AdvisorSession> advisorSessionMap = new ConcurrentHashMap<>();

    // 管理者設定 Session
    private final Map<String, AdminSession> adminSessionMap = new ConcurrentHashMap<>();

    // --- UserSession ---
    @Override
    public UserSession getUserSession(String userId) {
        return userSessionMap.get(userId);
    }

    @Override
    public void setUserSession(String userId, UserSession session) {
        System.out.println("[DEBUG] setUserSession, userId = " + userId + ", step = " + session.step);
        userSessionMap.put(userId, session);
        System.out.println("[DEBUG] userSessionMap keys after set: " + userSessionMap.keySet());
    }

    @Override
    public void removeUserSession(String userId) {
        System.out.println("[DEBUG] removeUserSession called, userId = " + userId);
        userSessionMap.remove(userId);
        System.out.println("[DEBUG] userSessionMap keys after remove: " + userSessionMap.keySet());
    }

    @Override
    public boolean hasUserSession(String userId) {
        return userSessionMap.containsKey(userId);
    }

    // --- AdvisorSession ---
    @Override
    public AdvisorSession getAdvisorSession(String userId) {
        return advisorSessionMap.get(userId);
    }

    @Override
    public void setAdvisorSession(String userId, AdvisorSession session) {
        advisorSessionMap.put(userId, session);
    }

    @Override
    public void removeAdvisorSession(String userId) {
        advisorSessionMap.remove(userId);
    }

    @Override
    public boolean hasAdvisorSession(String userId) {
        return advisorSessionMap.containsKey(userId);
    }

    // --- AdminSession ---
    @Override
    public AdminSession getAdminSession(String userId) {
        return adminSessionMap.get(userId);
    }

    @Override
    public void setAdminSession(String userId, AdminSession session) {
        adminSessionMap.put(userId, session);
    }

    @Override
    public void removeAdminSession(String userId) {
        adminSessionMap.remove(userId);
    }

    @Override
    public boolean hasAdminSession(String userId) {
        return adminSessionMap.containsKey(userId);
    }

    // --- 清空所有 Session ---
    @Override
    public void clearAll() {
        userSessionMap.clear();
        advisorSessionMap.clear();
        adminSessionMap.clear();
    }
}
