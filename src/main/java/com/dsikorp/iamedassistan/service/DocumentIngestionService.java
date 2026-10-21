package com.dsikorp.iamedassistan.service;

import org.springframework.core.io.Resource;

public interface DocumentIngestionService {

    String ingestNewDocument(Resource resource);
}
