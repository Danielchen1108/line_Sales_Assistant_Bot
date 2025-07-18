package com.example.AIrobot.Service.ServiceImpl;

import com.example.AIrobot.Entity.Admin;
import com.example.AIrobot.Repository.AdminRepository;
import com.example.AIrobot.Service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AdminServiceImpl implements AdminService {

    private final AdminRepository userRepository;

    @Autowired
    public AdminServiceImpl(AdminRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Admin createUser(Admin user) {
        return userRepository.save(user);
    }

    @Override
    public Optional<Admin> getUserById(String id) {
        return userRepository.findById(id);
    }

    @Override
    public boolean existsById(String id) {
        return userRepository.existsById(id);
    }

    @Override
    public Admin updateLastActiveTime(String id) {
        Optional<Admin> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            Admin user = optionalUser.get();
            user.setLastActiveAt(LocalDateTime.now());
            return userRepository.save(user);
        }
        return null;
    }

    @Override
    public Admin updateLineStatus(String id, String newStatus) {
        Optional<Admin> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            Admin user = optionalUser.get();
            user.setLineStatus(newStatus);
            return userRepository.save(user);
        }
        return null;
    }
}
