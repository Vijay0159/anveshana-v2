package com.anveshana.search.ai.controller;

import com.anveshana.search.ai.entity.AiDocument;
import com.anveshana.search.ai.service.AiDocumentService;
import com.anveshana.search.auth.entity.User;
import com.anveshana.search.auth.service.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class AiLibraryController {

    private final AiDocumentService aiDocumentService;

    public AiLibraryController(AiDocumentService aiDocumentService) {
        this.aiDocumentService = aiDocumentService;
    }

    /**
     * POST /ai-results/save
     * Saves an AI result to the user's library.
     */
    @PostMapping("/ai-results/save")
    public String save(@AuthenticationPrincipal CustomUserDetails principal,
                       @RequestParam("query") String query,
                       @RequestParam("content") String content,
                       @RequestParam(value = "redirectTo", required = false) String redirectTo,
                       RedirectAttributes ra) {

        if (principal == null) return "redirect:/login";

        User user = principal.getDomainUser();
        try {
            aiDocumentService.save(user, query, content);
            ra.addFlashAttribute("success", "Saved to your AI Library.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Could not save. Please try again.");
        }

        if (redirectTo != null && !redirectTo.isBlank()) {
            return "redirect:" + redirectTo;
        }
        return "redirect:/ai-library";
    }

    /**
     * POST /ai-results/delete
     * Deletes a saved AI document from the user's library.
     */
    @PostMapping("/ai-results/delete")
    public String delete(@AuthenticationPrincipal CustomUserDetails principal,
                         @RequestParam("id") Long id,
                         RedirectAttributes ra) {

        if (principal == null) return "redirect:/login";

        User user = principal.getDomainUser();
        try {
            aiDocumentService.delete(id, user);
            ra.addFlashAttribute("success", "Document removed from your AI Library.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Could not delete. Please try again.");
        }

        return "redirect:/ai-library";
    }

    /**
     * GET /ai-library
     * Shows all saved AI documents for the logged-in user.
     */
    @GetMapping("/ai-library")
    public String library(@AuthenticationPrincipal CustomUserDetails principal,
                          Model model) {

        if (principal == null) return "redirect:/login";

        User user = principal.getDomainUser();
        List<AiDocument> docs = aiDocumentService.getAllForUser(user);

        model.addAttribute("docs", docs);
        model.addAttribute("fullName", user.getFullName());

        return "ai-library";
    }

    /**
     * GET /saved-search?q=
     * Searches the user's saved AI documents using the inverted index.
     */
    @GetMapping("/saved-search")
    public String savedSearch(@AuthenticationPrincipal CustomUserDetails principal,
                              @RequestParam("q") String query,
                              Model model) {

        if (principal == null) return "redirect:/login";

        User user = principal.getDomainUser();
        List<AiDocumentService.SavedSearchResult> results =
                aiDocumentService.search(query, user);

        model.addAttribute("query", query);
        model.addAttribute("results", results);
        model.addAttribute("count", results.size());
        model.addAttribute("fullName", user.getFullName());

        return "saved-results";
    }
}
