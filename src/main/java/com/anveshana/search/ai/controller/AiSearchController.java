package com.anveshana.search.ai.controller;

import com.anveshana.search.ai.service.AiDocumentService;
import com.anveshana.search.ai.service.AiSearchService;
import com.anveshana.search.auth.entity.User;
import com.anveshana.search.auth.service.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AiSearchController {

    private final AiSearchService aiSearchService;
    private final AiDocumentService aiDocumentService;

    public AiSearchController(AiSearchService aiSearchService,
                              AiDocumentService aiDocumentService) {
        this.aiSearchService = aiSearchService;
        this.aiDocumentService = aiDocumentService;
    }

    @GetMapping("/ai-search")
    public String aiSearch(@RequestParam("q") String query,
                           @AuthenticationPrincipal CustomUserDetails principal,
                           Model model) {
        model.addAttribute("query", query);

        try {
            AiSearchService.AiSearchResult result = aiSearchService.search(query);
            model.addAttribute("aiContent", result.getContent());
            model.addAttribute("fromCache", result.isFromCache());
            model.addAttribute("filePath", result.getFilePath());
            model.addAttribute("error", null);

            // Check if user already saved this query
            if (principal != null) {
                User user = principal.getDomainUser();
                model.addAttribute("alreadySaved",
                        aiDocumentService.isSaved(user, query));
            } else {
                model.addAttribute("alreadySaved", false);
            }

        } catch (Exception e) {
            model.addAttribute("aiContent", null);
            model.addAttribute("alreadySaved", false);
            model.addAttribute("error", "Could not fetch AI results: " + e.getMessage());
        }

        return "ai-results";
    }

    /**
     * Logout endpoint that cleans up AI-generated session files before logging out.
     */
    @PostMapping("/logout-and-clear")
    public String logoutAndClear(HttpServletRequest request,
                                 HttpServletResponse response,
                                 Authentication authentication) {
        aiSearchService.clearSessionFiles();

        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }

        return "redirect:/login?logout";
    }
}
