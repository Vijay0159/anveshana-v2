package com.anveshana.search.search.controller;

import com.anveshana.search.auth.entity.User;
import com.anveshana.search.auth.service.CustomUserDetails;
import com.anveshana.search.bookmark.service.BookmarkService;
import com.anveshana.search.search.engine.TextTokenizer;
import com.anveshana.search.search.model.Document;
import com.anveshana.search.search.model.SearchResult;
import com.anveshana.search.search.service.IndexService;
import com.anveshana.search.search.service.SearchService;
import com.anveshana.search.search.util.HtmlHighlighter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class SearchController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final SearchService searchService;
    private final IndexService indexService;
    private final BookmarkService bookmarkService;

    public SearchController(SearchService searchService,
                            IndexService indexService,
                            BookmarkService bookmarkService) {
        this.searchService = searchService;
        this.indexService = indexService;
        this.bookmarkService = bookmarkService;
    }

    @GetMapping("/")
    public String home(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            model.addAttribute("bookmarks", bookmarkService.getBookmarksFor(currentUser));
            model.addAttribute("fullName", currentUser.getFullName());
        }
        return "search";
    }

    @GetMapping("/search")
    public String search(
            @RequestParam("q") String q,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
            Model model
    ) {
        List<SearchResult> allResults = searchService.search(q);

        int totalCount = allResults.size();
        int totalPages = totalCount == 0 ? 1 : (int) Math.ceil((double) totalCount / size);

        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalCount);

        List<SearchResult> pageResults = totalCount == 0
                ? Collections.emptyList()
                : allResults.subList(fromIndex, toIndex);

        List<SearchResult> pageResultsWithSnippets = pageResults.stream()
                .map(r -> addSnippet(r, q))
                .collect(Collectors.toList());

        model.addAttribute("query", q);
        model.addAttribute("results", pageResultsWithSnippets);
        model.addAttribute("count", totalCount);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", totalPages);

        User currentUser = getCurrentUser();
        if (currentUser != null) {
            model.addAttribute("bookmarks", bookmarkService.getBookmarksFor(currentUser));
            model.addAttribute("fullName", currentUser.getFullName());
        }

        return "results";
    }

    @GetMapping("/document/{docId}")
    public String documentView(
            @PathVariable("docId") int docId,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            Model model
    ) {
        List<Document> documents = indexService.getDocuments();

        Document doc = documents.stream()
                .filter(d -> d.getDocId() == docId)
                .findFirst()
                .orElse(null);

        if (doc == null) {
            model.addAttribute("message", "Document not found");
            return "document";
        }

        String content = doc.getContent();
        if (q != null && !q.isBlank()) {
            List<String> terms = TextTokenizer.tokenize(q);
            content = HtmlHighlighter.highlight(content, terms);
        }

        model.addAttribute("docId", doc.getDocId());
        model.addAttribute("path", doc.getPath());
        model.addAttribute("content", content);
        model.addAttribute("query", q);
        model.addAttribute("page", page);
        model.addAttribute("hasValidQuery", q != null && !q.trim().isEmpty());

        User currentUser = getCurrentUser();
        boolean isBookmarked = currentUser != null && bookmarkService.isBookmarked(currentUser, docId);
        model.addAttribute("isBookmarked", isBookmarked);

        return "document";
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private SearchResult addSnippet(SearchResult result, String query) {
        List<Document> documents = indexService.getDocuments();

        Document doc = documents.stream()
                .filter(d -> d.getDocId() == result.getDocId())
                .findFirst()
                .orElse(null);

        if (doc == null) {
            result.setSnippet("");
            return result;
        }

        String raw = doc.getContent();
        String lowerRaw = raw.toLowerCase();
        String firstTerm = TextTokenizer.tokenize(query).stream().findFirst().orElse(null);

        int startIdx = 0;
        if (firstTerm != null && !firstTerm.isBlank()) {
            int idx = lowerRaw.indexOf(firstTerm.toLowerCase());
            if (idx >= 0) startIdx = Math.max(0, idx - 80);
        }

        int endIdx = Math.min(raw.length(), startIdx + 260);
        String snippetText = raw.substring(startIdx, endIdx).trim();

        List<String> terms = TextTokenizer.tokenize(query);
        String highlightedSnippet = HtmlHighlighter.highlight(snippetText, terms);

        result.setSnippet(highlightedSnippet);
        return result;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails cud) {
            return cud.getDomainUser();
        }
        return null;
    }
}
