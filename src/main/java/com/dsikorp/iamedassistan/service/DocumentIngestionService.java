package com.dsikorp.iamedassistan.service;

import com.dsikorp.iamedassistan.dto.IngestionResult;
import org.springframework.core.io.Resource;

public interface DocumentIngestionService {

    IngestionResult ingestNewDocument(Resource resource);
}
