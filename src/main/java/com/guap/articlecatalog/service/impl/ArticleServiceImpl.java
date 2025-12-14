package com.guap.articlecatalog.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.guap.articlecatalog.model.Article;
import com.guap.articlecatalog.model.User;
import com.guap.articlecatalog.repository.ArticleRepository;
import com.guap.articlecatalog.service.ArticleService;

@Service
@Transactional
public class ArticleServiceImpl implements ArticleService {
    
    private static final Logger logger = LoggerFactory.getLogger(ArticleServiceImpl.class);
    
    private final ArticleRepository articleRepository;
    
    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;
    
    public ArticleServiceImpl(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }
    
    private Path getUploadPath() {
        return Paths.get(uploadDir, "articles");
    }
    
    private void ensureUploadDirectoryExists() throws IOException {
        Path uploadPath = getUploadPath();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
    }
    
    @Override
    public Article saveArticle(Article article, User user, MultipartFile pdfFile) {
        try {
            logger.debug("=== DEBUG: saveArticle called ===");
            logger.debug("Article title: {}", article.getTitle());
            logger.debug("User: {}", user.getUsername());
            logger.debug("PDF file: {}", pdfFile.getOriginalFilename());
            logger.debug("PDF size: {}", pdfFile.getSize());
            
            // Проверка файла (MultipartFile всегда не-null в Spring MVC)
            if (pdfFile.isEmpty()) {
                throw new IllegalArgumentException("PDF файл не может быть пустым");
            }
            
            // Проверка размера
            long maxSize = 10 * 1024 * 1024; // 10 MB
            if (pdfFile.getSize() > maxSize) {
                throw new IllegalArgumentException("Файл слишком большой (максимум 10 MB)");
            }
            
            // Проверка типа
            String contentType = pdfFile.getContentType();
            logger.debug("Content type: {}", contentType);
            
            if (contentType == null || !contentType.equals("application/pdf")) {
                throw new IllegalArgumentException("Файл должен быть в формате PDF. Получен: " + contentType);
            }
            
            // Получаем оригинальное имя файла безопасно
            String originalFilename = pdfFile.getOriginalFilename();
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                throw new IllegalArgumentException("Имя файла не может быть пустым");
            }
            
            // Создаем директорию если нет
            Path uploadDir = getUploadPath();
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                logger.debug("Created directory: {}", uploadDir.toAbsolutePath());
            }
            
            // Генерируем уникальное имя файла безопасно
            int lastDotIndex = originalFilename.lastIndexOf('.');
            String fileExtension = lastDotIndex > 0 
                ? originalFilename.substring(lastDotIndex) 
                : ".pdf";
            
            String uniqueFilename = System.currentTimeMillis() + "_" + 
                                UUID.randomUUID().toString().substring(0, 8) + 
                                fileExtension;
            
            Path filePath = uploadDir.resolve(uniqueFilename);
            
            // Сохраняем файл
            Files.write(filePath, pdfFile.getBytes());
            logger.debug("File saved to: {}", filePath.toAbsolutePath());
            
            // Устанавливаем свойства статьи
            article.setUser(user);
            article.setPdfFileName(originalFilename);
            article.setPdfFilePath(filePath.toString());
            article.setFileSize(pdfFile.getSize());
            
            // Сохраняем в базу
            Article savedArticle = articleRepository.save(article);
            logger.debug("Article saved to DB with ID: {}", savedArticle.getId());
            
            return savedArticle;
            
        } catch (IOException e) {
            logger.error("Ошибка при сохранении файла", e);
            throw new RuntimeException("Ошибка при сохранении файла: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            logger.error("Ошибка валидации файла", e);
            throw e; // Перебрасываем дальше
        } catch (Exception e) {
            logger.error("Неизвестная ошибка при сохранении статьи", e);
            throw new RuntimeException("Неизвестная ошибка при сохранении статьи: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Page<Article> getUserArticles(User user, Pageable pageable) {
        return articleRepository.findByUser(user, pageable);
    }
    
    @Override
    public Article getArticleById(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Статья не найдена"));
    }
    
    @Override
    public Article updateArticle(Long id, Article articleDetails, MultipartFile pdfFile) {
        Article article = getArticleById(id);
        
        // Обновление полей
        if (articleDetails.getTitle() != null && !articleDetails.getTitle().trim().isEmpty()) {
            article.setTitle(articleDetails.getTitle());
        }
        if (articleDetails.getAuthors() != null && !articleDetails.getAuthors().trim().isEmpty()) {
            article.setAuthors(articleDetails.getAuthors());
        }
        if (articleDetails.getPublicationYear() != null) {
            article.setPublicationYear(articleDetails.getPublicationYear());
        }
        if (articleDetails.getKeywords() != null) {
            article.setKeywords(articleDetails.getKeywords());
        }
        if (articleDetails.getTopic() != null) {
            article.setTopic(articleDetails.getTopic());
        }
        
        // Обновление файла, если предоставлен
        if (pdfFile != null && !pdfFile.isEmpty()) {
            if (pdfFile.getSize() > 10 * 1024 * 1024) {
                throw new IllegalArgumentException("Файл слишком большой (максимум 10 MB)");
            }
            
            // Проверка типа файла с защитой от null
            String contentType = pdfFile.getContentType();
            if (contentType == null || !contentType.equals("application/pdf")) {
                throw new IllegalArgumentException("Файл должен быть в формате PDF. Получен: " + contentType);
            }
            
            try {
                ensureUploadDirectoryExists();
                
                // Удаление старого файла
                if (article.getPdfFilePath() != null) {
                    Path oldFilePath = Paths.get(article.getPdfFilePath());
                    Files.deleteIfExists(oldFilePath);
                }
                
                // Сохранение нового файла
                String originalFilename = pdfFile.getOriginalFilename();
                String fileExtension = originalFilename != null && originalFilename.contains(".")
                        ? originalFilename.substring(originalFilename.lastIndexOf("."))
                        : ".pdf";
                
                String uniqueFileName = UUID.randomUUID() + fileExtension;
                Path filePath = getUploadPath().resolve(uniqueFileName);
                Files.write(filePath, pdfFile.getBytes());
                
                article.setPdfFileName(originalFilename);
                article.setPdfFilePath(filePath.toString());
                article.setFileSize(pdfFile.getSize());
                
            } catch (IOException e) {
                throw new RuntimeException("Ошибка при обновлении файла", e);
            }
        }
        
        return articleRepository.save(article);
    }
    
    @Override
    public void deleteArticle(Long id, User user) {
        Article article = getArticleById(id);
        
        // Проверка прав доступа
        if (!article.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Вы не можете удалить чужую статью");
        }
        
        // Удаление файла (если существует)
        if (article.getPdfFilePath() != null) {
            try {
                Path filePath = Paths.get(article.getPdfFilePath());
                if (Files.exists(filePath)) {
                    Files.deleteIfExists(filePath);
                }
            } catch (IOException e) {
                logger.warn("Не удалось удалить файл статьи: {}", e.getMessage());
            }
        }
        
        articleRepository.delete(article);
        
        logger.info("Статья удалена: {} (ID: {}) пользователем {}", 
                   article.getTitle(), id, user.getUsername());
    }
    
    @Override
    public Page<Article> searchArticles(String author, String topic, String keyword, Pageable pageable) {
        return articleRepository.searchArticles(author, topic, keyword, pageable);
    }
    
    @Override
    public Page<Article> searchUserArticles(User user, String query, Pageable pageable) {
        return articleRepository.searchUserArticles(user, query, pageable);
    }
    
    @Override
    public List<String> getAllTopics() {
        return articleRepository.findAllTopics();
    }
    
    @Override
    public byte[] getPdfFile(Long articleId) {
        Article article = getArticleById(articleId);
        
        if (article.getPdfFilePath() == null) {
            throw new IllegalArgumentException("Файл статьи не найден");
        }
        
        try {
            Path filePath = Paths.get(article.getPdfFilePath());
            if (!Files.exists(filePath)) {
                throw new IllegalArgumentException("Файл не существует: " + filePath);
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при чтении файла", e);
        }
    }
    
    @Override
    public long getTotalArticlesCount() {
        return articleRepository.count();
    }
    
    @Override
    public long getUserArticlesCount(User user) {
        return articleRepository.countByUser(user);
    }
}