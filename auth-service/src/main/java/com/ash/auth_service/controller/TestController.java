package com.ash.auth_service.controller;

import com.ash.auth_service.entity.User;
import com.ash.auth_service.repository.UserRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/test")
public class TestController {
    private final UserRepository userRepository;

    public TestController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/create")
    public User createUser(){
        User u1= User.builder()
                .userId("USR_"+ UUID.randomUUID())
                .build();
        return userRepository.save(u1);
    }
}
