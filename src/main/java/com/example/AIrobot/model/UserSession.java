package com.example.AIrobot.model;

import java.time.LocalDate;
import java.util.List;

import com.example.AIrobot.Entity.Customer;

public class UserSession {
    public enum Step {
        ASK_NAME,
        ASK_IDNUMBER,     // 新增：詢問身分證
        ASK_BIRTHDAY,     // 新增：詢問生日
        ASK_PHONE,
        ASK_REGION,
        ASK_AGE,
        ASK_JOB,
        ASK_PRODUCTS,
        ASK_STATUS,
        CONFIRM,
        DONE,
        CHOOSE_SAME_NAME_INDEX,
        UPDATE_CHOOSE_FIELD,
        UPDATE_ASK_UPDATE_VALUE,
        DELETE_CHOOSE_INDEX,
        DELETE_CONFIRM
    }

    public Step step = Step.ASK_NAME;
    public String name;
    public String idNumber;        // 新增：身分證字號
    public LocalDate birthday;     // 新增：出生年月日（建議 LocalDate）
    public String phone, region, job, productsOwned, status;
    public Integer age;
    public List<Customer> sameNameList;
    public Long selectedCustomerId;
    public int updateFieldIndex;
}
