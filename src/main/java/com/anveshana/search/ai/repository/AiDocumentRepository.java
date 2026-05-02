package com.anveshana.search.ai.repository;

import com.anveshana.search.ai.entity.AiDocument;
import com.anveshana.search.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiDocumentRepository extends JpaRepository<AiDocument, Long> {

    List<AiDocument> findByUserOrderByCreatedAtDesc(User user);

    Optional<AiDocument> findByUserAndQuery(User user, String query);

    boolean existsByUserAndQuery(User user, String query);
}
