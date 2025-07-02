package com.example.AIrobot.ServiceImpl;

import com.example.AIrobot.Service.SessionService;
import com.example.AIrobot.model.UserSession;
import com.example.AIrobot.model.AdvisorSession;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionServiceImpl implements SessionService {

    // 一般用戶 Session
    private final Map<String, UserSession> userSessionMap = new ConcurrentHashMap<>();
    // 顧問服務 Session
    private final Map<String, AdvisorSession> advisorSessionMap = new ConcurrentHashMap<>();

    // --- UserSession ---
    @Override
    public UserSession getUserSession(String userId) {
        return userSessionMap.get(userId);
    }

    @Override
    public void setUserSession(String userId, UserSession session) {
        userSessionMap.put(userId, session);
    }

    @Override
    public void removeUserSession(String userId) {
        userSessionMap.remove(userId);
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

    // --- 清空所有Session（可選功能） ---
    @Override
    public void clearAll() {
        userSessionMap.clear();
        advisorSessionMap.clear();
    }
}
