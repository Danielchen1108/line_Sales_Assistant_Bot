package com.example.AIrobot.Service;

import com.example.AIrobot.Entity.Admin;

import java.util.Optional;

public interface AdminService {

    Admin createUser(Admin user);

    Optional<Admin> getUserById(String id);

    boolean existsById(String id);

    Admin updateLastActiveTime(String id);

    Admin updateLineStatus(String id, String newStatus);
}
