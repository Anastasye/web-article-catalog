package com.guap.articlecatalog.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.guap.articlecatalog.model.User;
import com.guap.articlecatalog.service.UserService;

import jakarta.validation.Valid;

@Controller
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    
    public AuthController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }
    
    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }
    
    @GetMapping("/register")
    public String showRegistrationPage(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }
    
    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user,
                              BindingResult bindingResult,
                              Model model) {
        try {
            if (bindingResult.hasErrors()) {
                model.addAttribute("errorMessage", "Пожалуйста, исправьте ошибки в форме");
                return "register";
            }
            
            // Проверка уникальности логина
            if (userService.findByUsername(user.getUsername()).isPresent()) {
                model.addAttribute("errorMessage", "Пользователь с таким логином уже существует");
                return "register";
            }
            
            // Проверка уникальности email
            if (userService.findByEmail(user.getEmail()).isPresent()) {
                model.addAttribute("errorMessage", "Пользователь с таким email уже существует");
                return "register";
            }
            
            // Шифрование пароля
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            
            // Сохранение пользователя
            User savedUser = userService.saveUser(user);
            
            logger.info("Зарегистрирован новый пользователь: {}", savedUser.getUsername());
            model.addAttribute("successMessage", "Регистрация успешна! Теперь вы можете войти в систему.");
            
            return "redirect:/login?success";
            
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "register";
        } catch (Exception e) {
            logger.error("Ошибка при регистрации пользователя", e);
            model.addAttribute("errorMessage", "Произошла ошибка при регистрации. Попробуйте позже.");
            return "register";
        }
    }
}
