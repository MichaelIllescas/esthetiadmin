package com.esthetiadmin.backend.catalog.repository;

import com.esthetiadmin.backend.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByOrderByNameAsc();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
