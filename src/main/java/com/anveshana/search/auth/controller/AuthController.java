package com.anveshana.search.auth.controller;

import com.anveshana.search.auth.dto.SignupForm;
import com.anveshana.search.auth.entity.User;
import com.anveshana.search.auth.service.OtpService;
import com.anveshana.search.auth.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;
    private final OtpService otpService;

    public AuthController(UserService userService, OtpService otpService) {
        this.userService = userService;
        this.otpService = otpService;
    }

    // -------------------------------------------------------------------------
    // Signup
    // -------------------------------------------------------------------------

    @GetMapping("/signup")
    public String signupForm(Model model) {
        if (!model.containsAttribute("userForm")) {
            model.addAttribute("userForm", new SignupForm());
        }
        return "signup";
    }

    @PostMapping("/signup")
    public String handleSignup(@ModelAttribute("userForm") SignupForm form,
                               RedirectAttributes ra) {
        if (form.getFullName() == null || form.getFullName().isBlank()
                || form.getEmail() == null || form.getEmail().isBlank()
                || form.getPassword() == null || form.getPassword().length() < 6) {
            ra.addFlashAttribute("userForm", form);
            ra.addFlashAttribute("error",
                    "Please fill all fields. Password must be at least 6 characters.");
            return "redirect:/signup";
        }

        String email = form.getEmail().trim().toLowerCase();

        try {
            User user = userService.register(
                    form.getFullName().trim(),
                    email,
                    form.getPassword()
            );

            // Send OTP email
            otpService.generateAndSend(user);

            ra.addFlashAttribute("info",
                    "Account created! We've sent a 6-digit verification code to " + email);
            return "redirect:/verify-email?email=" + email;

        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("userForm", form);
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/signup";
        } catch (Exception ex) {
            ra.addFlashAttribute("userForm", form);
            ra.addFlashAttribute("error", "Something went wrong. Please try again.");
            return "redirect:/signup";
        }
    }

    // -------------------------------------------------------------------------
    // Email Verification
    // -------------------------------------------------------------------------

    @GetMapping("/verify-email")
    public String verifyEmailForm(@RequestParam("email") String email, Model model) {
        model.addAttribute("email", email);
        return "verify-email";
    }

    @PostMapping("/verify-email")
    public String handleVerify(@RequestParam("email") String email,
                               @RequestParam("otp") String otp,
                               RedirectAttributes ra) {
        OtpService.OtpResult result = otpService.validate(email.trim().toLowerCase(), otp.trim());

        switch (result) {
            case SUCCESS -> {
                userService.enableUser(email.trim().toLowerCase());
                ra.addFlashAttribute("success",
                        "Email verified successfully! You can now log in.");
                return "redirect:/login?verified";
            }
            case EXPIRED -> {
                ra.addFlashAttribute("error",
                        "Your code has expired. Please request a new one.");
                return "redirect:/verify-email?email=" + email;
            }
            case INVALID -> {
                ra.addFlashAttribute("error",
                        "Invalid code. Please check and try again.");
                return "redirect:/verify-email?email=" + email;
            }
        }
        return "redirect:/verify-email?email=" + email;
    }

    // -------------------------------------------------------------------------
    // Resend OTP
    // -------------------------------------------------------------------------

    @PostMapping("/resend-otp")
    public String resendOtp(@RequestParam("email") String email,
                            RedirectAttributes ra) {
        String normalizedEmail = email.trim().toLowerCase();
        User user = userService.findByEmail(normalizedEmail);

        if (user == null || user.isEmailVerified()) {
            ra.addFlashAttribute("error", "No pending verification found for this email.");
            return "redirect:/login";
        }

        try {
            otpService.generateAndSend(user);
            ra.addFlashAttribute("info", "A new verification code has been sent to " + normalizedEmail);
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Could not send email. Please try again.");
        }

        return "redirect:/verify-email?email=" + normalizedEmail;
    }

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    @GetMapping("/login")
    public String loginForm(@RequestParam(value = "verified", required = false) String verified,
                            @RequestParam(value = "registered", required = false) String registered) {
        return "login";
    }
}
