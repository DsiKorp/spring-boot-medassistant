package com.dsikorp.iamedassistan.agent.OpenAgent;

public interface OpenAgentService {
    String execute(String query, String model, Long userId);
}
