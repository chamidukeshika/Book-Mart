// ReadOnlineService.java
package com.ecom.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.ecom.model.ReadOnline;

public interface ReadOnlineService {
	ReadOnline saveDocument(ReadOnline doc);

	Page<ReadOnline> getAllDocuments(String category, int pageNo, int pageSize);

	Page<ReadOnline> searchDocuments(String keyword, int pageNo, int pageSize);

	void deleteDocument(int id);

	ReadOnline getDocumentById(int id);

	List<ReadOnline> getAllDocuments();

	ReadOnline updateDocument(ReadOnline document);
}