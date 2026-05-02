package com.anveshana.search.auth.repository;

import com.anveshana.search.auth.entity.EmailOtp;
import com.anveshana.search.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {

    // Get the latest unused, unexpired OTP for a user
    @Query("SELECT o FROM EmailOtp o WHERE o.user = :user AND o.used = false " +
           "ORDER BY o.createdAt DESC")
    Optional<EmailOtp> findLatestByUser(User user);

    // Invalidate all previous OTPs for a user before issuing a new one
    @Modifying
    @Transactional
    @Query("UPDATE EmailOtp o SET o.used = true WHERE o.user = :user")
    void invalidateAllForUser(User user);
}
