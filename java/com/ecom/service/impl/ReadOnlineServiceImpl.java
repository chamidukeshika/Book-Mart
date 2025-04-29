// ReadOnlineServiceImpl.java
package com.ecom.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.ecom.model.ReadOnline;
import com.ecom.repository.ReadOnlineRepository;
import com.ecom.service.ReadOnlineService;

@Service
public class ReadOnlineServiceImpl implements ReadOnlineService {

	@Autowired
	private ReadOnlineRepository repo;

	@Override
	public ReadOnline saveDocument(ReadOnline doc) {
		return repo.save(doc);
	}

	@Override
	public Page<ReadOnline> getAllDocuments(String category, int pageNo, int pageSize) {
		PageRequest pageable = PageRequest.of(pageNo, pageSize);
		return repo.findByCategoryContainingIgnoreCase(category, pageable);
	}

	@Override
	public Page<ReadOnline> searchDocuments(String query, int pageNo, int pageSize) {
		PageRequest pageable = PageRequest.of(pageNo, pageSize);
		return repo.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query, pageable);
	}

	@Override
	public void deleteDocument(int id) {
		repo.deleteById(id);
	}

	@Override
	public ReadOnline getDocumentById(int id) {
		return repo.findById(id).orElse(null);
	}

	@Override
	public ReadOnline updateDocument(ReadOnline document) {
		return repo.save(document);
	}
}