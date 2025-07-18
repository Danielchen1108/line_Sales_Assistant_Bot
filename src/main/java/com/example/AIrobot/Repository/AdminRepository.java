package com.example.AIrobot.Repository;

import com.example.AIrobot.Entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminRepository extends JpaRepository<Admin, String> {

    // 依照 email 查找使用者（非必要，但常見）
    Admin findByEmail(String email);

    // 可加：判斷使用者是否已存在
    boolean existsById(String id);
}
