package com.anveshana.search.auth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_otps")
public class EmailOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Stored as BCrypt hash for security
    @Column(nullable = false)
    private String otpHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public EmailOtp() {}

    public EmailOtp(User user, String otpHash, LocalDateTime expiresAt) {
        this.user = user;
        this.otpHash = otpHash;
        this.expiresAt = expiresAt;
    }

    public Long getId()                  { return id; }
    public User getUser()                { return user; }
    public String getOtpHash()           { return otpHash; }
    public LocalDateTime getExpiresAt()  { return expiresAt; }
    public boolean isUsed()              { return used; }
    public LocalDateTime getCreatedAt()  { return createdAt; }

    public void setUsed(boolean used)    { this.used = used; }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
