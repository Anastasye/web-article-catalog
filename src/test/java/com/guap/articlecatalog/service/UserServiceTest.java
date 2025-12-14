package com.guap.articlecatalog.service;

import com.guap.articlecatalog.model.User;
import com.guap.articlecatalog.repository.UserRepository;
import com.guap.articlecatalog.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        // Тестовый пользователь
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword123");
        testUser.setFullName("Test User");
    }
    
    @Test
    void testLoadUserByUsername_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        UserDetails userDetails = userService.loadUserByUsername("testuser");
        
        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("encodedPassword123", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("USER")));
    }
    
    @Test
    void testLoadUserByUsername_NotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());
        
        Exception exception = assertThrows(UsernameNotFoundException.class, () -> {
            userService.loadUserByUsername("nonexistent");
        });
        
        assertEquals("Пользователь не найден: nonexistent", exception.getMessage());
    }
    
    @Test
    void testSaveUser_Success() {
        // Создаем пользователя для сохранения
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("new@example.com");
        newUser.setPassword("password123");
        newUser.setFullName("New User");
        
        // Настраиваем моки
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            user.setPassword("encodedPassword");
            return user;
        });
        
        User savedUser = userService.saveUser(newUser);
        
        assertNotNull(savedUser);
        assertEquals("newuser", savedUser.getUsername());
        assertEquals("encodedPassword", savedUser.getPassword());
        verify(passwordEncoder, times(1)).encode("password123");
        verify(userRepository, times(1)).save(any(User.class));
    }
    
    @Test
    void testSaveUser_UsernameAlreadyExists() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        User newUser = new User();
        newUser.setUsername("testuser");
        newUser.setEmail("new@example.com");
        newUser.setPassword("password123");
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.saveUser(newUser);
        });
        
        assertEquals("Пользователь с таким логином уже существует", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void testSaveUser_EmailAlreadyExists() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("test@example.com");
        newUser.setPassword("password123");
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.saveUser(newUser);
        });
        
        assertEquals("Email уже используется", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void testFindByUsername_UserExists() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        Optional<User> foundUser = userService.findByUsername("testuser");
        
        assertTrue(foundUser.isPresent());
        assertEquals("testuser", foundUser.get().getUsername());
        assertEquals("test@example.com", foundUser.get().getEmail());
    }
    
    @Test
    void testFindByUsername_UserNotExists() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());
        
        Optional<User> foundUser = userService.findByUsername("nonexistent");
        
        assertFalse(foundUser.isPresent());
    }
    
    @Test
    void testFindByEmail_UserExists() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        
        Optional<User> foundUser = userService.findByEmail("test@example.com");
        
        assertTrue(foundUser.isPresent());
        assertEquals("test@example.com", foundUser.get().getEmail());
        assertEquals("testuser", foundUser.get().getUsername());
    }
    
    @Test
    void testFindByEmail_UserNotExists() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());
        
        Optional<User> foundUser = userService.findByEmail("nonexistent@example.com");
        
        assertFalse(foundUser.isPresent());
    }
    
    @Test
    void testFindAllUsers() {
        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setUsername("anotheruser");
        
        List<User> users = Arrays.asList(testUser, anotherUser);
        when(userRepository.findAll()).thenReturn(users);
        
        List<User> allUsers = userService.findAllUsers();
        
        assertEquals(2, allUsers.size());
        verify(userRepository, times(1)).findAll();
    }
    
    @Test
    void testUpdateUserProfile_Success() {
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("testuser");
        existingUser.setEmail("old@example.com");
        existingUser.setFullName("Old Name");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        
        User updatedDetails = new User();
        updatedDetails.setFullName("New Name");
        updatedDetails.setEmail("new@example.com");
        
        User updatedUser = userService.updateUserProfile(1L, updatedDetails);
        
        assertNotNull(updatedUser);
        assertEquals("New Name", updatedUser.getFullName());
        assertEquals("new@example.com", updatedUser.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }
    
    @Test
    void testUpdateUserProfile_SameEmail() {
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("testuser");
        existingUser.setEmail("test@example.com");
        existingUser.setFullName("Old Name");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        
        User updatedDetails = new User();
        updatedDetails.setFullName("Updated Name");
        updatedDetails.setEmail("test@example.com"); // Тот же email
        
        User updatedUser = userService.updateUserProfile(1L, updatedDetails);
        
        assertNotNull(updatedUser);
        assertEquals("Updated Name", updatedUser.getFullName());
        assertEquals("test@example.com", updatedUser.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }
    
    @Test
    void testUpdateUserProfile_EmailAlreadyUsed() {
        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setEmail("used@example.com");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("used@example.com")).thenReturn(Optional.of(anotherUser));
        
        User updatedDetails = new User();
        updatedDetails.setFullName("Updated Name");
        updatedDetails.setEmail("used@example.com");
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.updateUserProfile(1L, updatedDetails);
        });
        
        assertEquals("Email уже используется", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void testUpdateUserProfile_UserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        
        User updatedDetails = new User();
        updatedDetails.setFullName("Updated Name");
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.updateUserProfile(99L, updatedDetails);
        });
        
        assertEquals("Пользователь не найден", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void testSearchUsers_WithQuery() {
        when(userRepository.searchUsers("test")).thenReturn(Arrays.asList(testUser));
        
        List<User> foundUsers = userService.searchUsers("test");
        
        assertEquals(1, foundUsers.size());
        assertEquals("testuser", foundUsers.get(0).getUsername());
        verify(userRepository, times(1)).searchUsers("test");
    }
    
    @Test
    void testSearchUsers_EmptyQuery() {
        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setUsername("anotheruser");
        
        List<User> users = Arrays.asList(testUser, anotherUser);
        when(userRepository.findAll()).thenReturn(users);
        
        List<User> foundUsers = userService.searchUsers("");
        
        assertEquals(2, foundUsers.size());
        verify(userRepository, times(1)).findAll();
    }
    
    @Test
    void testFindById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        User foundUser = userService.findById(1L);
        
        assertNotNull(foundUser);
        assertEquals(1L, foundUser.getId());
        assertEquals("testuser", foundUser.getUsername());
    }
    
    @Test
    void testFindById_NotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.findById(99L);
        });
        
        assertEquals("Пользователь не найден", exception.getMessage());
    }
}