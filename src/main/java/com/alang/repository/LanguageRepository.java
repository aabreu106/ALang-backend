package com.alang.repository;

import com.alang.entity.Language;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Language repository.
 *
 * TODO: Seed this table with supported languages
 * TODO: Add migration script (Flyway or Liquibase)
 */
@Repository
public interface LanguageRepository extends JpaRepository<Language, String> {

    /**
     * Get all fully supported languages (for frontend dropdown).
     */
    List<Language> findByFullySupportedTrue();
}
