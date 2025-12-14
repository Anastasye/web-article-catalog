package com.guap.articlecatalog.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.guap.articlecatalog.model.Article;
import com.guap.articlecatalog.model.User;

public interface ArticleService {
    Article saveArticle(Article article, User user, MultipartFile pdfFile);
    Page<Article> getUserArticles(User user, Pageable pageable);
    Article getArticleById(Long id);
    Article updateArticle(Long id, Article articleDetails, MultipartFile pdfFile);
    void deleteArticle(Long id, User user);
    Page<Article> searchArticles(String author, String topic, String keyword, Pageable pageable);
    Page<Article> searchUserArticles(User user, String query, Pageable pageable);
    List<String> getAllTopics();
    byte[] getPdfFile(Long articleId);
    long getTotalArticlesCount();
    long getUserArticlesCount(User user);
}