package com.guap.articlecatalog.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.guap.articlecatalog.service.ArticleService;

@Controller
public class HomeController {
    
    private final ArticleService articleService;
    
    public HomeController(ArticleService articleService) {
        this.articleService = articleService;
    }
    
    @GetMapping("/")
    public String home(Model model) {
        try {
            long totalArticles = articleService.getTotalArticlesCount();
            model.addAttribute("totalArticles", totalArticles);
        } catch (Exception e) {
            model.addAttribute("totalArticles", 0);
        }
        return "index";
    }
}