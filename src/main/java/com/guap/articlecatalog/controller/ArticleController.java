package com.guap.articlecatalog.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.guap.articlecatalog.model.Article;
import com.guap.articlecatalog.model.User;
import com.guap.articlecatalog.service.ArticleService;
import com.guap.articlecatalog.service.UserService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/articles")
public class ArticleController {
    
    private static final Logger logger = LoggerFactory.getLogger(ArticleController.class);
    
    private final ArticleService articleService;
    private final UserService userService;
    
    public ArticleController(ArticleService articleService, UserService userService) {
        this.articleService = articleService;
        this.userService = userService;
    }
    
    private User getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + username));
    }
    
    // 2.3 Система поиска - Все статьи с поиском
    @GetMapping
    public String listArticles(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(required = false) String author,
                              @RequestParam(required = false) String topic,
                              @RequestParam(required = false) String keyword,
                              Model model,
                              Authentication authentication) {
        try {
            // Убрали неиспользуемую переменную currentUser
            Pageable pageable = PageRequest.of(page, 10, Sort.by("uploadDate").descending());
            Page<Article> articlesPage = articleService.searchArticles(author, topic, keyword, pageable);
            
            model.addAttribute("articles", articlesPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", articlesPage.getTotalPages());
            model.addAttribute("totalItems", articlesPage.getTotalElements());
            
            List<String> topics = articleService.getAllTopics();
            model.addAttribute("topics", topics);
            model.addAttribute("selectedAuthor", author);
            model.addAttribute("selectedTopic", topic);
            model.addAttribute("selectedKeyword", keyword);
            
            return "articles/list";
        } catch (Exception e) {
            logger.error("Ошибка при получении списка статей", e);
            model.addAttribute("errorMessage", "Произошла ошибка при загрузке статей");
            return "articles/list";
        }
    }
    
    // 2.2 Управление статьями - Мои статьи
    @GetMapping("/my")
public String listMyArticles(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(required = false) String query,
                            Model model,
                            Authentication authentication) {
    try {
        // Получаем текущего пользователя
        String username = authentication.getName();
        User currentUser = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + username));
        
        Pageable pageable = PageRequest.of(page, 10, Sort.by("uploadDate").descending());
        Page<Article> articlesPage;
        
        if (query != null && !query.trim().isEmpty()) {
            articlesPage = articleService.searchUserArticles(currentUser, query, pageable);
            model.addAttribute("searchQuery", query);
        } else {
            articlesPage = articleService.getUserArticles(currentUser, pageable);
        }
        
        model.addAttribute("articles", articlesPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", articlesPage.getTotalPages());
        model.addAttribute("totalItems", articlesPage.getTotalElements());
        
        long userArticlesCount = articleService.getUserArticlesCount(currentUser);
        model.addAttribute("userArticlesCount", userArticlesCount);
        
        List<String> topics = articleService.getAllTopics();
        model.addAttribute("topics", topics);
        
        return "articles/my-list";
        
    } catch (Exception e) {
        logger.error("Ошибка при получении списка статей пользователя", e);
        model.addAttribute("errorMessage", "Произошла ошибка при загрузке ваших статей");
        return "articles/my-list";
    }
}
    
    
    // 3.2 Добавление новой статьи -
    @GetMapping("/add")
    public String showAddArticleForm(Model model) {
        model.addAttribute("article", new Article());
        
        List<String> topics = articleService.getAllTopics();
        model.addAttribute("topics", topics);
        
        return "articles/add";
    }
    
    // 3.2 Добавление новой статьи - 
    @PostMapping("/add")
    public String addArticle(@RequestParam("pdfFile") MultipartFile pdfFile,
                            @RequestParam("title") String title,
                            @RequestParam("authors") String authors,
                            @RequestParam(value = "publicationYear", required = false) Integer publicationYear,
                            @RequestParam(value = "keywords", required = false) String keywords,
                            @RequestParam(value = "topic", required = false) String topic,
                            Model model,
                            Authentication authentication) {
        
        try {
            logger.debug("=== DEBUG: Starting article addition ===");
            logger.debug("Title: {}", title);
            logger.debug("Authors: {}", authors);
            logger.debug("PDF File: {}", pdfFile.getOriginalFilename());
            logger.debug("PDF Size: {}", pdfFile.getSize());
            
            // Базовые проверки
            if (pdfFile.isEmpty()) {
                model.addAttribute("errorMessage", "Пожалуйста, загрузите PDF файл");
                model.addAttribute("topics", articleService.getAllTopics());
                return "articles/add";
            }
            
            if (title == null || title.trim().isEmpty()) {
                model.addAttribute("errorMessage", "Название статьи обязательно");
                model.addAttribute("topics", articleService.getAllTopics());
                return "articles/add";
            }
            
            if (authors == null || authors.trim().isEmpty()) {
                model.addAttribute("errorMessage", "Авторы обязательны");
                model.addAttribute("topics", articleService.getAllTopics());
                return "articles/add";
            }
            
            // Создаем статью
            Article article = new Article();
            article.setTitle(title.trim());
            article.setAuthors(authors.trim());
            
            if (publicationYear != null) {
                article.setPublicationYear(publicationYear);
            }
            
            if (keywords != null && !keywords.trim().isEmpty()) {
                article.setKeywords(keywords.trim());
            }
            
            if (topic != null && !topic.trim().isEmpty()) {
                article.setTopic(topic.trim());
            }
            
            logger.debug("=== DEBUG: Article object created ===");
            
            // Получаем текущего пользователя
            String username = authentication.getName();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + username));
            
            logger.debug("=== DEBUG: User found: {} ===", currentUser.getUsername());
            
            // Сохраняем статью
            Article savedArticle = articleService.saveArticle(article, currentUser, pdfFile);
            
            logger.debug("=== DEBUG: Article saved with ID: {} ===", savedArticle.getId());
            
            return "redirect:/articles/my?success";
            
        } catch (Exception e) {
            logger.error("=== ERROR: Failed to add article ===", e);
            
            model.addAttribute("errorMessage", "Ошибка при добавлении статьи: " + 
                            (e.getMessage() != null ? e.getMessage() : "Неизвестная ошибка"));
            model.addAttribute("topics", articleService.getAllTopics());
            return "articles/add";
        }
    }
    // 3.4 Просмотр статьи
    @GetMapping("/view/{id}")
    public String viewArticle(@PathVariable Long id, Model model, Authentication authentication) {
        try {
            Article article = articleService.getArticleById(id);
            User currentUser = getCurrentUser(authentication);
            
            model.addAttribute("article", article);
            model.addAttribute("isOwner", article.getUser().getId().equals(currentUser.getId()));
            
            return "articles/view";
        } catch (Exception e) {
            logger.error("Ошибка при просмотре статьи", e);
            model.addAttribute("errorMessage", "Статья не найдена");
            return "redirect:/articles";
        }
    }
    
    // 3.4 Редактирование статьи - Форма
    @GetMapping("/edit/{id}")
    public String showEditArticleForm(@PathVariable Long id, Model model, Authentication authentication) {
        try {
            Article article = articleService.getArticleById(id);
            User currentUser = getCurrentUser(authentication);
            
            // Проверка прав доступа
            if (!article.getUser().getId().equals(currentUser.getId())) {
                model.addAttribute("errorMessage", "У вас нет прав для редактирования этой статьи");
                return "redirect:/articles/my";
            }
            
            model.addAttribute("article", article);
            
            List<String> topics = articleService.getAllTopics();
            model.addAttribute("topics", topics);
            
            return "articles/edit";
        } catch (Exception e) {
            logger.error("Ошибка при редактировании статьи", e);
            model.addAttribute("errorMessage", "Статья не найдена");
            return "redirect:/articles/my";
        }
    }
    
    // 3.4 Редактирование статьи - Обработка
    @PostMapping("/edit/{id}")
    public String updateArticle(@PathVariable Long id,
                               @Valid @ModelAttribute("article") Article articleDetails,
                               BindingResult bindingResult,
                               @RequestParam(value = "pdfFile", required = false) MultipartFile pdfFile,
                               Model model,
                               Authentication authentication) {
        try {
            if (bindingResult.hasErrors()) {
                List<String> topics = articleService.getAllTopics();
                model.addAttribute("topics", topics);
                model.addAttribute("errorMessage", "Пожалуйста, исправьте ошибки в форме");
                return "articles/edit";
            }
            
            User currentUser = getCurrentUser(authentication);
            Article updatedArticle = articleService.updateArticle(id, articleDetails, pdfFile);
            
            logger.info("Статья обновлена: {} (ID: {}) пользователем {}", 
                       updatedArticle.getTitle(), id, currentUser.getUsername());
            
            return "redirect:/articles/my?success";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            List<String> topics = articleService.getAllTopics();
            model.addAttribute("topics", topics);
            return "articles/edit";
        } catch (Exception e) {
            logger.error("Ошибка при обновлении статьи", e);
            model.addAttribute("errorMessage", "Произошла ошибка при обновлении статьи");
            List<String> topics = articleService.getAllTopics();
            model.addAttribute("topics", topics);
            return "articles/edit";
        }
    }
    
    // 2.2 Удаление статьи
    @PostMapping("/delete/{id}")
    public String deleteArticle(@PathVariable Long id, Authentication authentication) {
        try {
            User currentUser = getCurrentUser(authentication);
            articleService.deleteArticle(id, currentUser);
            
            logger.info("Статья удалена (ID: {}) пользователем {}", id, currentUser.getUsername());
            
            return "redirect:/articles/my?deleted";
        } catch (IllegalArgumentException e) {
            logger.error("Ошибка при удалении статьи: {}", e.getMessage());
            return "redirect:/articles/my?error=" + e.getMessage();
        } catch (Exception e) {
            logger.error("Ошибка при удалении статьи", e);
            return "redirect:/articles/my?error=Произошла ошибка при удалении статьи";
        }
    }
    
    // 3.4 Скачивание PDF
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadPdf(@PathVariable Long id) {
        try {
            Article article = articleService.getArticleById(id);
            byte[] pdfData = articleService.getPdfFile(id);
            
            ByteArrayResource resource = new ByteArrayResource(pdfData);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                           "attachment; filename=\"" + article.getPdfFileName() + "\"")
                    .contentLength(pdfData.length)
                    .body(resource);
        } catch (Exception e) {
            logger.error("Ошибка при скачивании файла", e);
            return ResponseEntity.notFound().build();
        }
    }
}