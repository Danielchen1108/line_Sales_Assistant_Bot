package com.example.AIrobot.Service.ServiceImpl;


import com.example.AIrobot.Entity.Customer;
import com.example.AIrobot.Repository.CustomerRepository;
import com.example.AIrobot.Service.CustomerService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.util.ArrayList;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Autowired
    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public void addCustomer(Customer customer) {
        // 如果有 id 就 update，沒有就新增
        customerRepository.save(customer);
    }

    @Override
    public Customer findCustomerByNameAndCreatedBy(String name, String createdBy) {
        return customerRepository.findByNameAndCreatedBy(name, createdBy).orElse(null);
    }

    @Override
    public List<Customer> getAllCustomersByCreatedBy(String createdBy) {
        return customerRepository.findAllByCreatedBy(createdBy);
    }

    // ====== 建議新的以 id 為主的更新方法 ======
    @Override
    public boolean updateCustomerStatusById(Long id, String newStatus) {
    Customer customer = findById(id);
    if (customer != null) {
        customer.setStatus(newStatus); // 直接改 status 欄位
        customerRepository.save(customer);
        return true;
    }
    return false;
    }


    @Override
    public boolean updateCustomerFieldById(Long id, int fieldIndex, String newValue) {
        Customer customer = findById(id);
        if (customer != null) {
            switch (fieldIndex) {
                case 1 -> customer.setName(newValue);
                case 2 -> customer.setIdNumber(newValue);      // 身分證字號
                case 3 -> {
                    try {
                        customer.setBirthday(LocalDate.parse(newValue));
                    } catch (Exception e) {
                        return false; // 格式錯誤，回傳失敗
                    }
                }
                case 4 -> customer.setPhone(newValue);
                case 5 -> customer.setRegion(newValue);
                case 6 -> {
                    try {
                        customer.setAge(Integer.parseInt(newValue));
                    } catch (Exception e) {
                        return false; // 格式錯誤，回傳失敗
                    }
                }
                case 7 -> customer.setJob(newValue);
                case 8 -> customer.setProductsOwned(newValue);
                case 9 -> customer.setStatus(newValue);
                default -> { return false; }
            }
            System.out.println("id=" + id + ", fieldIndex=" + fieldIndex + ", newValue=" + newValue);
            customerRepository.save(customer);
            return true;
        }
        return false;
    }

    // =========================================

    @Override
    public Customer findById(Long id) {
        return customerRepository.findById(id).orElse(null);
    }

    @Override
    public List<Customer> findAllByNameAndCreatedBy(String name, String createdBy) {
        return customerRepository.findAllByNameAndCreatedBy(name, createdBy);
    }

    @Override
    public boolean deleteCustomerById(Long id, String createdBy) {   
     Customer customer = customerRepository.findById(id).orElse(null);
    // 檢查是否存在且屬於該 user
    if (customer != null && customer.getCreatedBy().equals(createdBy)) {
        customerRepository.deleteById(id);
        return true;
    }
    return false;
    }

    // 保留：舊的（不推薦，會踩多名同名陷阱）
   
}
