package com.guap.articlecatalog.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.guap.articlecatalog.model.User;
import com.guap.articlecatalog.repository.UserRepository;
import com.guap.articlecatalog.service.UserService;

import jakarta.annotation.PostConstruct;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    // PasswordEncoder будет автоматически внедрен из SecurityConfig
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @PostConstruct
    public void initTestUser() {
        try {
            if (userRepository.findByUsername("testuser").isEmpty()) {
                User testUser = new User();
                testUser.setUsername("testuser");
                testUser.setEmail("test@example.com");
                testUser.setPassword(passwordEncoder.encode("password123"));
                testUser.setFullName("Тестовый Пользователь");
                testUser.getRoles().add("USER");
                
                userRepository.save(testUser);
                logger.info("✅ Тестовый пользователь создан: testuser / password123");
            }
        } catch (Exception e) {
            logger.error("Ошибка при создании тестового пользователя", e);
        }
    }
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("Попытка входа пользователя: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + username));
        
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }
    

    @Override
    public User saveUser(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Пользователь с таким логином уже существует");
        }
        
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email уже используется");
        }
        
        if (!user.getPassword().startsWith("$2a$")) { // Проверяем, что пароль уже не зашифрован
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        
        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }
        user.getRoles().add("USER");
        
        User savedUser = userRepository.save(user);
        logger.info("Пользователь сохранен: {} (ID: {})", user.getUsername(), savedUser.getId());
        
        return savedUser;
    }
    
    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    @Override
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }
    
    @Override
    public User updateUserProfile(Long userId, User userDetails) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        
        if (userDetails.getFullName() != null && !userDetails.getFullName().isEmpty()) {
            user.setFullName(userDetails.getFullName());
        }
        
        if (userDetails.getEmail() != null && !userDetails.getEmail().isEmpty() 
                && !userDetails.getEmail().equals(user.getEmail())) {
            if (userRepository.findByEmail(userDetails.getEmail()).isPresent()) {
                throw new IllegalArgumentException("Email уже используется");
            }
            user.setEmail(userDetails.getEmail());
        }
        
        return userRepository.save(user);
    }
    
    @Override
    public void uploadAvatar(Long userId, MultipartFile avatarFile) {
        if (avatarFile == null || avatarFile.isEmpty()) {
            throw new IllegalArgumentException("Файл не загружен");
        }
        
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
            
            if (avatarFile.getSize() > 2 * 1024 * 1024) {
                throw new IllegalArgumentException("Файл слишком большой (макс. 2 MB)");
            }
            
            String contentType = avatarFile.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("Файл должен быть изображением");
            }
            
            Path avatarDir = Paths.get("./uploads/avatars");
            if (!Files.exists(avatarDir)) {
                Files.createDirectories(avatarDir);
            }
            
            String originalFileName = avatarFile.getOriginalFilename();
            if (originalFileName == null || originalFileName.isEmpty()) {
                throw new IllegalArgumentException("Имя файла не может быть пустым");
            }
            
            String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String uniqueFileName = "avatar_" + userId + "_" + UUID.randomUUID() + fileExtension;
            
            Path filePath = avatarDir.resolve(uniqueFileName);
            Files.write(filePath, avatarFile.getBytes());
            
            user.setAvatarPath("/uploads/avatars/" + uniqueFileName);
            userRepository.save(user);
            
            logger.info("Аватар загружен для пользователя ID: {}", userId);
            
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при сохранении аватара", e);
        }
    }
    
    @Override
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
    }
    
    @Override
    public List<User> searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return userRepository.findAll();
        }
        return userRepository.searchUsers(query.trim());
    }
}