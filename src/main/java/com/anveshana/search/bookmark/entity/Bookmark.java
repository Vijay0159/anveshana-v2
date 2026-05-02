package com.anveshana.search.bookmark.entity;

import com.anveshana.search.auth.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookmarks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "doc_id"}))
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "doc_id", nullable = false)
    private Integer docId;

    @Column(nullable = false, length = 1024)
    private String path;

    @Column(length = 512)
    private String title;

    @Column(name = "query_at_bookmark", length = 500)
    private String queryAtBookmark;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Transient
    private String snippet;

    public Bookmark() {
        this.createdAt = LocalDateTime.now();
    }

    public Bookmark(User user, Integer docId, String path, String title, String queryAtBookmark) {
        this.user = user;
        this.docId = docId;
        this.path = path;
        this.title = title;
        this.queryAtBookmark = queryAtBookmark;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Integer getDocId() { return docId; }
    public void setDocId(Integer docId) { this.docId = docId; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getQueryAtBookmark() { return queryAtBookmark; }
    public void setQueryAtBookmark(String queryAtBookmark) { this.queryAtBookmark = queryAtBookmark; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public String getSnippet() { return snippet; }
    public void setSnippet(String snippet) { this.snippet = snippet; }

    @Override
    public String toString() {
        return "Bookmark{id=" + id + ", docId=" + docId + ", title='" + title + '\''
                + ", path='" + path + '\'' + ", user=" + (user != null ? user.getId() : null)
                + ", createdAt=" + createdAt + '}';
    }
}
