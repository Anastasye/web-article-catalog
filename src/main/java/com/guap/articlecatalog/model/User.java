package com.guap.articlecatalog.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Логин не может быть пустым")
    @Size(min = 3, max = 50, message = "Логин должен содержать от 3 до 50 символов")
    @Column(unique = true, nullable = false)
    private String username;
    
    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Некорректный формат email")
    @Column(unique = true, nullable = false)
    private String email;
    
    @NotBlank(message = "Пароль не может быть пустым")
    @Size(min = 6, message = "Пароль должен содержать минимум 6 символов")
    private String password;
    
    @NotBlank(message = "ФИО не может быть пустым")
    @Size(max = 100, message = "ФИО не должно превышать 100 символов")
    private String fullName;
    
    private String avatarPath;
    private LocalDateTime registrationDate;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Article> articles = new HashSet<>();
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles = new HashSet<>();
    
    public User() {
        this.registrationDate = LocalDateTime.now();
        this.roles.add("USER");
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }
    
    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDateTime registrationDate) { this.registrationDate = registrationDate; }
    
    public Set<Article> getArticles() { return articles; }
    public void setArticles(Set<Article> articles) { this.articles = articles; }
    
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
}