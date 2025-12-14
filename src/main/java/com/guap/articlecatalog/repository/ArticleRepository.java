package com.guap.articlecatalog.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.guap.articlecatalog.model.Article;
import com.guap.articlecatalog.model.User;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    Page<Article> findByUser(User user, Pageable pageable);
    List<Article> findByUser(User user);
    
    // Исправленный запрос для общего поиска
    @Query("SELECT a FROM Article a WHERE " +
       "(:author IS NULL OR :author = '' OR LOWER(a.authors) LIKE LOWER(CONCAT('%', :author, '%'))) AND " +
       "(:topic IS NULL OR :topic = '' OR a.topic = :topic) AND " +
       "(:keyword IS NULL OR :keyword = '' OR LOWER(a.keywords) LIKE LOWER(CONCAT('%', :keyword, '%')))")
       Page<Article> searchArticles(@Param("author") String author, 
                                   @Param("topic") String topic, 
                                   @Param("keyword") String keyword, 
                                   Pageable pageable);
    
    @Query("SELECT DISTINCT a.topic FROM Article a WHERE a.topic IS NOT NULL AND a.topic != ''")
    List<String> findAllTopics();
    
    @Query("SELECT a FROM Article a WHERE a.user = :user AND " +
           "(LOWER(a.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.authors) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.keywords) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Article> searchUserArticles(@Param("user") User user, 
                                     @Param("query") String query, 
                                     Pageable pageable);
    
    long countByUser(User user);
}