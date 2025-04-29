// ReadOnlineRepository.java
package com.ecom.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.model.ReadOnline;

public interface ReadOnlineRepository extends JpaRepository<ReadOnline, Integer> {
	Page<ReadOnline> findByCategoryContainingIgnoreCase(String categoryKeyword, Pageable pageable);

	Page<ReadOnline> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description,
			Pageable pageable);
}