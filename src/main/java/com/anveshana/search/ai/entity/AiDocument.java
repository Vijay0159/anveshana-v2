package com.anveshana.search.ai.entity;

import com.anveshana.search.auth.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_documents")
public class AiDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 500)
    private String query;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AiDocument() {
        this.createdAt = LocalDateTime.now();
    }

    public AiDocument(User user, String query, String title, String content) {
        this.user = user;
        this.query = query;
        this.title = title;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId()                  { return id; }
    public User getUser()                { return user; }
    public String getQuery()             { return query; }
    public String getTitle()             { return title; }
    public String getContent()           { return content; }
    public LocalDateTime getCreatedAt()  { return createdAt; }

    public void setUser(User user)       { this.user = user; }
    public void setQuery(String query)   { this.query = query; }
    public void setTitle(String title)   { this.title = title; }
    public void setContent(String content){ this.content = content; }
}
