package com.anveshana.search.bookmark.repository;

import com.anveshana.search.auth.entity.User;
import com.anveshana.search.bookmark.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    List<Bookmark> findByUserOrderByCreatedAtDesc(User user);
    Optional<Bookmark> findByUserAndDocId(User user, Integer docId);
}
