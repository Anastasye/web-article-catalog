package com.guap.articlecatalog.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.multipart.MultipartFile;

import com.guap.articlecatalog.model.User;

public interface UserService extends UserDetailsService {
    User saveUser(User user);
    
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findAllUsers();
    User updateUserProfile(Long userId, User userDetails);

    void uploadAvatar(Long userId, MultipartFile avatarFile);
    
    User findById(Long id);
    
    List<User> searchUsers(String query);
}