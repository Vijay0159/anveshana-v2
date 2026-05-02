package com.anveshana.search.auth.controller;

import com.anveshana.search.auth.dto.SignupForm;
import com.anveshana.search.auth.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

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

        try {
            userService.register(
                    form.getFullName().trim(),
                    form.getEmail().trim().toLowerCase(),
                    form.getPassword()
            );
            ra.addFlashAttribute("registered", "Account created successfully. Please log in.");
            return "redirect:/login?registered";
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

    @GetMapping("/login")
    public String loginForm(@RequestParam(value = "registered", required = false) String registered) {
        return "login";
    }
}
