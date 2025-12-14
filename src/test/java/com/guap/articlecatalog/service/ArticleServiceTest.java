package com.guap.articlecatalog.service;

import com.guap.articlecatalog.model.Article;
import com.guap.articlecatalog.model.User;
import com.guap.articlecatalog.repository.ArticleRepository;
import com.guap.articlecatalog.service.impl.ArticleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ArticleServiceTest {
    
    @Mock
    private ArticleRepository articleRepository;
    
    @InjectMocks
    private ArticleServiceImpl articleService;
    
    private Article testArticle;
    private Article anotherArticle;
    private User testUser;
    private User anotherUser;
    
    @BeforeEach
    void setUp() {
        // Тестовые пользователи
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        
        anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setUsername("anotheruser");
        
        // Основная тестовая статья
        testArticle = new Article();
        testArticle.setId(1L);
        testArticle.setTitle("Test Article");
        testArticle.setAuthors("Test Author");
        testArticle.setPublicationYear(2024);
        testArticle.setKeywords("test, java, spring");
        testArticle.setTopic("Programming");
        testArticle.setPdfFileName("test.pdf");
        testArticle.setPdfFilePath("test.pdf");
        testArticle.setFileSize(1024L);
        testArticle.setUser(testUser);
        
        // Другая статья для тестов
        anotherArticle = new Article();
        anotherArticle.setId(2L);
        anotherArticle.setTitle("Another Article");
        anotherArticle.setAuthors("Another Author");
        anotherArticle.setPublicationYear(2023);
        anotherArticle.setKeywords("database, sql");
        anotherArticle.setTopic("Database");
        anotherArticle.setPdfFileName("another.pdf");
        anotherArticle.setPdfFilePath("another.pdf");
        anotherArticle.setUser(anotherUser);
    }
    
    @Test
    void testSaveArticle_Success() {
        // Подготовка тестовых данных
        MultipartFile pdfFile = new MockMultipartFile(
            "test.pdf", 
            "test.pdf", 
            "application/pdf", 
            "PDF content".getBytes()
        );
        
        Article articleToSave = new Article();
        articleToSave.setTitle("Test Article");
        articleToSave.setAuthors("Test Author");
        
        // Используем Reflection для установки uploadDir
        try {
            var field = articleService.getClass().getDeclaredField("uploadDir");
            field.setAccessible(true);
            field.set(articleService, "./uploads");
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException e) {
            fail("Не удалось установить uploadDir: " + e.getMessage());
        }
        
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> {
            Article saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setPdfFileName("test.pdf");
            saved.setPdfFilePath("test.pdf");
            saved.setFileSize(pdfFile.getSize());
            saved.setUser(testUser);
            return saved;
        });
        
        Article savedArticle = articleService.saveArticle(articleToSave, testUser, pdfFile);
        
        assertNotNull(savedArticle);
        assertEquals("Test Article", savedArticle.getTitle());
        assertEquals(testUser, savedArticle.getUser());
        verify(articleRepository, times(1)).save(any(Article.class));
    }
    
    @Test
    void testSaveArticle_NullFile() {
        Article articleToSave = new Article();
        articleToSave.setTitle("Test Article");
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            articleService.saveArticle(articleToSave, testUser, null);
        });
        
        assertEquals("PDF файл не может быть пустым", exception.getMessage());
        verify(articleRepository, never()).save(any(Article.class));
    }
    
    @Test
    void testSaveArticle_TooLargeFile() {
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11 MB
        MultipartFile largeFile = new MockMultipartFile(
            "large.pdf", 
            "large.pdf", 
            "application/pdf", 
            largeContent
        );
        
        Article articleToSave = new Article();
        articleToSave.setTitle("Test Article");
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            articleService.saveArticle(articleToSave, testUser, largeFile);
        });
        
        assertEquals("Файл слишком большой (максимум 10 MB)", exception.getMessage());
        verify(articleRepository, never()).save(any(Article.class));
    }
    
    @Test
    void testSaveArticle_InvalidFileType() {
        MultipartFile invalidFile = new MockMultipartFile(
            "test.txt", 
            "test.txt", 
            "text/plain", 
            "Not a PDF".getBytes()
        );
        
        Article articleToSave = new Article();
        articleToSave.setTitle("Test Article");
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            articleService.saveArticle(articleToSave, testUser, invalidFile);
        });
        
        assertTrue(exception.getMessage().contains("Файл должен быть в формате PDF"));
        verify(articleRepository, never()).save(any(Article.class));
    }
    
    @Test
    void testGetArticleById_Exists() {
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));
        
        Article foundArticle = articleService.getArticleById(1L);
        
        assertNotNull(foundArticle);
        assertEquals("Test Article", foundArticle.getTitle());
        assertEquals("Test Author", foundArticle.getAuthors());
        assertEquals(2024, foundArticle.getPublicationYear());
    }
    
    @Test
    void testGetArticleById_NotExists() {
        when(articleRepository.findById(99L)).thenReturn(Optional.empty());
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            articleService.getArticleById(99L);
        });
        
        assertEquals("Статья не найдена", exception.getMessage());
    }
    
    @Test
    void testGetUserArticles() {
        List<Article> articles = Arrays.asList(testArticle);
        Page<Article> articlePage = new PageImpl<>(articles);
        
        when(articleRepository.findByUser(eq(testUser), any(Pageable.class))).thenReturn(articlePage);
        
        Page<Article> result = articleService.getUserArticles(testUser, PageRequest.of(0, 10));
        
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Test Article", result.getContent().get(0).getTitle());
        assertEquals(testUser, result.getContent().get(0).getUser());
    }
    
    @Test
    void testGetUserArticles_Empty() {
        Page<Article> emptyPage = new PageImpl<>(List.of());
        
        when(articleRepository.findByUser(eq(testUser), any(Pageable.class))).thenReturn(emptyPage);
        
        Page<Article> result = articleService.getUserArticles(testUser, PageRequest.of(0, 10));
        
        assertNotNull(result);
        assertEquals(0, result.getContent().size());
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testSearchArticles() {
        List<Article> articles = Arrays.asList(testArticle, anotherArticle);
        Page<Article> articlePage = new PageImpl<>(articles);
        
        when(articleRepository.searchArticles(eq("Author"), eq("Programming"), eq("java"), any(Pageable.class)))
            .thenReturn(articlePage);
        
        Page<Article> result = articleService.searchArticles("Author", "Programming", "java", PageRequest.of(0, 10));
        
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
    }
    
    @Test
    void testSearchArticles_NullParameters() {
        List<Article> articles = Arrays.asList(testArticle);
        Page<Article> articlePage = new PageImpl<>(articles);
        
        when(articleRepository.searchArticles(eq(null), eq(null), eq(null), any(Pageable.class)))
            .thenReturn(articlePage);
        
        Page<Article> result = articleService.searchArticles(null, null, null, PageRequest.of(0, 10));
        
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }
    
    @Test
    void testSearchUserArticles() {
        List<Article> articles = Arrays.asList(testArticle);
        Page<Article> articlePage = new PageImpl<>(articles);
        
        when(articleRepository.searchUserArticles(eq(testUser), eq("test"), any(Pageable.class)))
            .thenReturn(articlePage);
        
        Page<Article> result = articleService.searchUserArticles(testUser, "test", PageRequest.of(0, 10));
        
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Test Article", result.getContent().get(0).getTitle());
    }
    
    @Test
    void testGetAllTopics() {
        List<String> topics = Arrays.asList("Programming", "Science", "Technology", "Database");
        
        when(articleRepository.findAllTopics()).thenReturn(topics);
        
        List<String> result = articleService.getAllTopics();
        
        assertNotNull(result);
        assertEquals(4, result.size());
        assertTrue(result.contains("Programming"));
        assertTrue(result.contains("Database"));
        assertFalse(result.contains("Unknown"));
    }
    
    @Test
    void testGetUserArticlesCount() {
        when(articleRepository.countByUser(testUser)).thenReturn(5L);
        
        long count = articleService.getUserArticlesCount(testUser);
        
        assertEquals(5L, count);
    }
    
    @Test
    void testGetTotalArticlesCount() {
        when(articleRepository.count()).thenReturn(100L);
        
        long count = articleService.getTotalArticlesCount();
        
        assertEquals(100L, count);
    }
    
    @Test
    void testDeleteArticle_Success() {
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));
        doNothing().when(articleRepository).delete(any(Article.class));
        
        articleService.deleteArticle(1L, testUser);
        
        verify(articleRepository, times(1)).delete(testArticle);
    }
    
    @Test
    void testDeleteArticle_WrongUser() {
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            articleService.deleteArticle(1L, anotherUser);
        });
        
        assertEquals("Вы не можете удалить чужую статью", exception.getMessage());
        verify(articleRepository, never()).delete(any(Article.class));
    }
    
    @Test
    void testDeleteArticle_NotFound() {
        when(articleRepository.findById(99L)).thenReturn(Optional.empty());
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            articleService.deleteArticle(99L, testUser);
        });
        
        assertEquals("Статья не найдена", exception.getMessage());
        verify(articleRepository, never()).delete(any(Article.class));
    }
    
    @Test
    void testUpdateArticle_Success() {
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));
        when(articleRepository.save(any(Article.class))).thenReturn(testArticle);
        
        Article updatedDetails = new Article();
        updatedDetails.setTitle("Updated Title");
        updatedDetails.setAuthors("Updated Author");
        updatedDetails.setPublicationYear(2025);
        
        Article updatedArticle = articleService.updateArticle(1L, updatedDetails, null);
        
        assertNotNull(updatedArticle);
        verify(articleRepository, times(1)).save(any(Article.class));
    }
    
    @Test
    void testUpdateArticle_NotFound() {
        when(articleRepository.findById(99L)).thenReturn(Optional.empty());
        
        Article updatedDetails = new Article();
        updatedDetails.setTitle("Updated Title");
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            articleService.updateArticle(99L, updatedDetails, null);
        });
        
        assertEquals("Статья не найдена", exception.getMessage());
        verify(articleRepository, never()).save(any(Article.class));
    }
}