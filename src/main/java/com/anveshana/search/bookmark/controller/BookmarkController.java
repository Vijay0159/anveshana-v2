package com.anveshana.search.bookmark.controller;

import com.anveshana.search.auth.entity.User;
import com.anveshana.search.auth.service.CustomUserDetails;
import com.anveshana.search.bookmark.entity.Bookmark;
import com.anveshana.search.bookmark.service.BookmarkService;
import com.anveshana.search.search.model.Document;
import com.anveshana.search.search.service.IndexService;
import com.anveshana.search.search.util.HtmlHighlighter;
import com.anveshana.search.search.engine.TextTokenizer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final IndexService indexService;

    public BookmarkController(BookmarkService bookmarkService, IndexService indexService) {
        this.bookmarkService = bookmarkService;
        this.indexService = indexService;
    }

    @PostMapping("/bookmarks/add")
    public String addBookmark(@AuthenticationPrincipal CustomUserDetails principal,
                              @RequestParam("docId") int docId,
                              @RequestParam("path") String path,
                              @RequestParam(value = "title", required = false) String title,
                              @RequestParam(value = "q", required = false) String query,
                              @RequestParam(value = "page", required = false) String page,
                              @RequestParam(value = "redirectTo", required = false) String redirectTo,
                              RedirectAttributes ra) {

        if (principal == null) {
            ra.addFlashAttribute("error", "Please log in to bookmark documents.");
            return "redirect:/login";
        }

        User user = principal.getDomainUser();
        try {
            bookmarkService.addBookmark(user, docId, path, title, query);
            ra.addFlashAttribute("success", "Bookmark added successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("error", "Could not add bookmark. Please try again.");
        }

        if (redirectTo != null && !redirectTo.isBlank()) {
            return "redirect:" + redirectTo;
        }

        return "redirect:/document/" + docId + buildQueryPageParams(query, page);
    }

    @PostMapping("/bookmarks/remove")
    public String removeBookmark(@AuthenticationPrincipal CustomUserDetails principal,
                                 @RequestParam("docId") int docId,
                                 @RequestParam(value = "redirectTo", required = false) String redirectTo,
                                 RedirectAttributes ra) {

        if (principal == null) {
            ra.addFlashAttribute("error", "Please log in to manage bookmarks.");
            return "redirect:/login";
        }

        User user = principal.getDomainUser();
        try {
            bookmarkService.removeBookmark(user, docId);
            ra.addFlashAttribute("success", "Bookmark removed.");
        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("error", "Could not remove bookmark. Please try again.");
        }

        if (redirectTo != null && !redirectTo.isBlank()) {
            return "redirect:" + redirectTo;
        }

        return "redirect:/bookmarks";
    }

    @GetMapping("/bookmarks")
    public String listBookmarks(@AuthenticationPrincipal CustomUserDetails principal,
                                Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = principal.getDomainUser();
        List<Bookmark> bookmarks = bookmarkService.getBookmarksFor(user);
        List<Document> documents = indexService.getDocuments();

        bookmarks.forEach(b -> {
            if (b.getQueryAtBookmark() != null && !b.getQueryAtBookmark().isBlank()) {
                Document doc = documents.stream()
                        .filter(d -> d.getDocId() == b.getDocId())
                        .findFirst()
                        .orElse(null);
                if (doc != null) {
                    String snippet = HtmlHighlighter.highlight(
                            generateSnippet(doc.getContent(), b.getQueryAtBookmark()),
                            TextTokenizer.tokenize(b.getQueryAtBookmark())
                    );
                    b.setSnippet(snippet);
                }
            }
        });

        model.addAttribute("bookmarks", bookmarks);
        model.addAttribute("fullName", user.getFullName());

        return "bookmarks";
    }

    private String generateSnippet(String content, String query) {
        String lowerContent = content.toLowerCase();
        String firstTerm = TextTokenizer.tokenize(query).stream().findFirst().orElse(null);
        int startIdx = 0;
        if (firstTerm != null) {
            int idx = lowerContent.indexOf(firstTerm.toLowerCase());
            if (idx >= 0) startIdx = Math.max(0, idx - 80);
        }
        int endIdx = Math.min(content.length(), startIdx + 260);
        return content.substring(startIdx, endIdx).trim();
    }

    private String buildQueryPageParams(String query, String page) {
        StringBuilder sb = new StringBuilder();
        boolean hasQuery = query != null && !query.isBlank();
        boolean hasPage = page != null && !page.isBlank();

        if (hasQuery || hasPage) {
            sb.append("?");
            if (hasQuery) {
                sb.append("q=").append(URLEncoder.encode(query, StandardCharsets.UTF_8));
            }
            if (hasPage) {
                if (hasQuery) sb.append("&");
                sb.append("page=").append(page);
            }
        }
        return sb.toString();
    }
}
