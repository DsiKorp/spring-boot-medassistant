package com.dsikorp.iamedassistan.agent;

public interface RoutingWorkflowService {
    String routeQuery(String query, String model, Long userId);
}
