package com.example.AIrobot.model;

import java.util.ArrayList;
import java.util.List;

import com.example.AIrobot.Entity.Customer;

public class AdvisorSession {
    public enum SessionStep {
        ASK_TARGET_NAME,      // 等待輸入顧問服務對象名字
        CONVERSATION          // 跟 AI 對話階段
    }

    private String userId;                       // LINE 使用者 ID
    private String targetName;                   // 目前顧問對象名字
    private Customer targetCustomer;             // 查詢到的 Customer 資料
    private SessionStep step;                    // 流程狀態
    private List<String> chatHistory = new ArrayList<>(); // 問答紀錄

    public AdvisorSession(String userId) {
        this.userId = userId;
        this.step = SessionStep.ASK_TARGET_NAME;
    }

    public AdvisorSession() {
        this.step = SessionStep.ASK_TARGET_NAME;
    }

    // --- Getter & Setter ---
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public Customer getTargetCustomer() { return targetCustomer; }
    public void setTargetCustomer(Customer targetCustomer) { this.targetCustomer = targetCustomer; }

    public SessionStep getStep() { return step; }
    public void setStep(SessionStep step) { this.step = step; }

    public List<String> getChatHistory() { return chatHistory; }
    public void setChatHistory(List<String> chatHistory) { this.chatHistory = chatHistory; }
    public void addChat(String msg) { this.chatHistory.add(msg); }
}
