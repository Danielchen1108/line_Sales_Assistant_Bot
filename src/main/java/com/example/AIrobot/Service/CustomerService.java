package com.example.AIrobot.Service;

import java.util.List;

import com.example.AIrobot.Entity.Customer;

public interface CustomerService {
    void addCustomer(Customer customer); // 新增 or 更新
    Customer findCustomerByNameAndCreatedBy(String name, String createdBy); // 只抓第一筆，不建議用於更新
    List<Customer> getAllCustomersByCreatedBy(String createdBy); // 單一 user 的全部客戶
    List<Customer> findAllByNameAndCreatedBy(String name, String createdBy); // 多個同名用於列表
    Customer findById(Long id); // 以 id 查唯一客戶

    // 以下是以 id 為主的更新
    boolean updateCustomerStatusById(Long id, String newStatus);

    // 你如果有「直接改電話/地區」等欄位，也建議都用 id
    boolean updateCustomerFieldById(Long id, int fieldIndex, String newValue); // ex: 1:姓名, 2:電話, ...

    boolean deleteCustomerById(Long id, String createdBy); // 加 createdBy 做安全驗證
}
