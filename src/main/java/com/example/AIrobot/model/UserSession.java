package com.example.AIrobot.model;

import java.time.LocalDate;
import java.util.List;
import com.example.AIrobot.Entity.Customer;

public class UserSession {
    public enum Step {
        // ===== 新增流程 =====
        ASK_NAME,
        ASK_IDNUMBER,     
        ASK_BIRTHDAY,     
        ASK_PHONE,
        ASK_REGION,
        ASK_AGE,
        ASK_JOB,
        ASK_PRODUCTS,
        ASK_STATUS,
        CONFIRM,           // 新增流程資料確認

        // ===== 更新流程 =====
        CHOOSE_SAME_NAME_INDEX,   // 選同名
        UPDATE_CHOOSE_FIELD,      // 選擇要更新的欄位
        UPDATE_ASK_UPDATE_VALUE,  // 輸入新值
        UPDATE_CONFIRM,           // 確認更新

        // ===== 刪除流程 =====
        DELETE_CHOOSE_INDEX,      // 選擇同名欲刪除的 index
        DELETE_CONFIRM,           // 刪除前確認

        DONE                      // 結束
    }

    // ========== 新增、更新流程通用資料 ==========
    public String name;
    public String idNumber;    
    public LocalDate birthday;     
    public String phone, region, job, productsOwned, status;
    public Integer age;

    // ========== 同名名單（給更新/刪除流程用） ==========
    public List<Customer> sameNameList;       // 查到多筆同名時的暫存
    public Long selectedCustomerId;           // 被選中要處理的那筆資料ID
    public Customer selectedCustomer;         // 若你直接存整個物件（更新用）

    // ========== 更新流程 ==========
    public int updateFieldIndex;             // 更新：使用者選的欄位編號（1~9）
    public String updateFieldValue;

    // ========== 刪除流程 ==========
    // 無特殊屬性，通常用 selectedCustomerId 即可（若需多步刪除流程）

    // ========== 當前進度 ==========
    public Step step = Step.ASK_NAME;

    // ...可擴充其它業務屬性...
}
