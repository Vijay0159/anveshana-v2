package com.anveshana.search.bookmark.service;

import com.anveshana.search.auth.entity.User;
import com.anveshana.search.bookmark.entity.Bookmark;
import com.anveshana.search.bookmark.repository.BookmarkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;

    public BookmarkService(BookmarkRepository bookmarkRepository) {
        this.bookmarkRepository = bookmarkRepository;
    }

    public List<Bookmark> getBookmarksFor(User user) {
        return bookmarkRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public boolean isBookmarked(User user, int docId) {
        return bookmarkRepository.findByUserAndDocId(user, docId).isPresent();
    }

    @Transactional
    public void addBookmark(User user, int docId, String path, String title, String query) {
        if (bookmarkRepository.findByUserAndDocId(user, docId).isPresent()) {
            return;
        }
        Bookmark b = new Bookmark(user, docId, path, title, query);
        bookmarkRepository.save(b);
    }

    @Transactional
    public void removeBookmark(User user, int docId) {
        bookmarkRepository.findByUserAndDocId(user, docId)
                .ifPresent(bookmarkRepository::delete);
    }
}
