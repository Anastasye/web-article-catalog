package com.guap.articlecatalog.controller;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.guap.articlecatalog.model.User;
import com.guap.articlecatalog.service.UserService;

@Controller
@RequestMapping("/user")
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    private User getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + username));
    }
    
    // 2.4 Публичные профили пользователей - Мой профиль
    @GetMapping("/profile")
    public String showProfile(Model model, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        model.addAttribute("user", currentUser);
        
        // Форматируем дату регистрации (только дата без времени)
        if (currentUser.getRegistrationDate() != null) {
            String formattedDate = currentUser.getRegistrationDate()
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            model.addAttribute("formattedRegistrationDate", formattedDate);
        }
        
        return "profile";
    }
    
    // 3.1 Регистрация и вход - Обновление профиля
    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam(value = "fullName", required = false) String fullName,
                            @RequestParam(value = "email", required = false) String email,
                            Model model,
                            Authentication authentication) {
        try {
            logger.debug("Обновление профиля: fullName={}, email={}", fullName, email);
            
            User currentUser = getCurrentUser(authentication);
            
            // Создаем объект с обновленными данными
            User userDetails = new User();
            if (fullName != null && !fullName.trim().isEmpty()) {
                userDetails.setFullName(fullName.trim());
            }
            
            if (email != null && !email.trim().isEmpty()) {
                userDetails.setEmail(email.trim());
            }
            
            // Обновляем профиль
            User updatedUser = userService.updateUserProfile(currentUser.getId(), userDetails);
            
            model.addAttribute("user", updatedUser);
            model.addAttribute("successMessage", "Профиль успешно обновлен");
            
            // Форматируем дату регистрации
            if (updatedUser.getRegistrationDate() != null) {
                String formattedDate = updatedUser.getRegistrationDate()
                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                model.addAttribute("formattedRegistrationDate", formattedDate);
            }
            
            logger.info("Профиль пользователя {} обновлен", currentUser.getUsername());
            
            return "profile";
            
        } catch (IllegalArgumentException e) {
            logger.error("Ошибка валидации при обновлении профиля", e);
            model.addAttribute("errorMessage", e.getMessage());
            return "profile";
        } catch (Exception e) {
            logger.error("Ошибка при обновлении профиля", e);
            model.addAttribute("errorMessage", "Произошла ошибка при обновлении профиля");
            return "profile";
        }
    }
    
    // 3.1 Регистрация и вход - Загрузка аватара
    @PostMapping("/profile/upload-avatar")
    public String uploadAvatar(@RequestParam("avatarFile") MultipartFile avatarFile,
                              Model model,
                              Authentication authentication) {
        try {
            User currentUser = getCurrentUser(authentication);
            userService.uploadAvatar(currentUser.getId(), avatarFile);
            
            model.addAttribute("successMessage", "Аватар успешно загружен");
            model.addAttribute("user", currentUser);
            
            // Форматируем дату регистрации
            if (currentUser.getRegistrationDate() != null) {
                String formattedDate = currentUser.getRegistrationDate()
                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                model.addAttribute("formattedRegistrationDate", formattedDate);
            }
            
            logger.info("Аватар пользователя {} обновлен", currentUser.getUsername());
            
            return "redirect:/user/profile";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "profile";
        } catch (Exception e) {
            logger.error("Ошибка при загрузке аватара", e);
            model.addAttribute("errorMessage", "Произошла ошибка при загрузке аватара");
            return "profile";
        }
    }
    
    // 3.5 Просмотр профилей других пользователей - Поиск пользователей
    @GetMapping("/search")
    public String searchUsers(@RequestParam(value = "query", required = false) String query,
                             Model model) {
        List<User> users;
        
        if (query == null || query.trim().isEmpty()) {
            users = userService.findAllUsers();
        } else {
            users = userService.searchUsers(query);
        }
        
        model.addAttribute("users", users);
        model.addAttribute("query", query);
        return "user-search";
    }
    
    // 2.4 Публичные профили пользователей - Просмотр профиля другого пользователя
    @GetMapping("/{id}")
    public String viewUserProfile(@PathVariable Long id, Model model, Authentication authentication) {
        try {
            User user = userService.findById(id);
            User currentUser = getCurrentUser(authentication);
            
            model.addAttribute("viewedUser", user);
            model.addAttribute("isOwnProfile", user.getId().equals(currentUser.getId()));
            
            // Форматируем дату регистрации
            if (user.getRegistrationDate() != null) {
                String formattedDate = user.getRegistrationDate()
                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                model.addAttribute("formattedRegistrationDate", formattedDate);
            }
            
            return "user-profile";
        } catch (Exception e) {
            logger.error("Ошибка при просмотре профиля пользователя", e);
            model.addAttribute("errorMessage", "Пользователь не найден");
            return "redirect:/";
        }
    }
}