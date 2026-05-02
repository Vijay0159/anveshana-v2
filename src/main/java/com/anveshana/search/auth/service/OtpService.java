package com.anveshana.search.auth.service;

import com.anveshana.search.auth.entity.EmailOtp;
import com.anveshana.search.auth.entity.User;
import com.anveshana.search.auth.repository.EmailOtpRepository;
import com.anveshana.search.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class OtpService {

    private final EmailOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private static final int OTP_EXPIRY_MINUTES = 10;
    private final SecureRandom random = new SecureRandom();

    public OtpService(EmailOtpRepository otpRepository,
                      UserRepository userRepository,
                      EmailService emailService,
                      PasswordEncoder passwordEncoder) {
        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Generates a new OTP, stores it hashed, and sends the email.
     * Invalidates any previous OTPs for this user first.
     */
    @Transactional
    public void generateAndSend(User user) {
        // Invalidate all previous OTPs
        otpRepository.invalidateAllForUser(user);

        // Generate 6-digit OTP
        String otp = String.format("%06d", random.nextInt(1_000_000));

        // Hash it before storing
        String otpHash = passwordEncoder.encode(otp);

        // Persist
        EmailOtp emailOtp = new EmailOtp(
                user,
                otpHash,
                LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES)
        );
        otpRepository.save(emailOtp);

        // Send email with plaintext OTP
        emailService.sendOtpEmail(user.getEmail(), user.getFullName(), otp);
    }

    /**
     * Validates the OTP entered by the user.
     * Returns true if valid, marks it used and verifies the account.
     */
    @Transactional
    public OtpResult validate(String email, String rawOtp) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return OtpResult.INVALID;

        EmailOtp latest = otpRepository.findLatestByUser(user).orElse(null);
        if (latest == null) return OtpResult.INVALID;

        if (latest.isExpired()) return OtpResult.EXPIRED;

        if (!passwordEncoder.matches(rawOtp, latest.getOtpHash())) return OtpResult.INVALID;

        // Mark OTP as used
        latest.setUsed(true);
        otpRepository.save(latest);

        // Verify the user's email
        user.setEmailVerified(true);
        userRepository.save(user);

        return OtpResult.SUCCESS;
    }

    public enum OtpResult {
        SUCCESS, EXPIRED, INVALID
    }
}
