package com.guap.articlecatalog.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "articles")
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Название статьи не может быть пустым")
    @Column(nullable = false)
    private String title;
    
    @NotBlank(message = "Авторы не могут быть пустыми")
    @Column(nullable = false, length = 500)
    private String authors;
    
    private Integer publicationYear;
    
    @Column(length = 1000)
    private String keywords;
    
    private String topic;
    
    @Column(nullable = false)
    private String pdfFilePath;
    
    private String pdfFileName;
    private Long fileSize;
    private LocalDateTime uploadDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    public Article() {
        this.uploadDate = LocalDateTime.now();
    }
    
    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getAuthors() { return authors; }
    public void setAuthors(String authors) { this.authors = authors; }
    
    public Integer getPublicationYear() { return publicationYear; }
    public void setPublicationYear(Integer publicationYear) { this.publicationYear = publicationYear; }
    
    public String getKeywords() { return keywords; }
    
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    
    public String getPdfFilePath() { return pdfFilePath; }
    public void setPdfFilePath(String pdfFilePath) { this.pdfFilePath = pdfFilePath; }
    
    public String getPdfFileName() { return pdfFileName; }
    public void setPdfFileName(String pdfFileName) { this.pdfFileName = pdfFileName; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public LocalDateTime getUploadDate() { return uploadDate; }
    public void setUploadDate(LocalDateTime uploadDate) { this.uploadDate = uploadDate; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}