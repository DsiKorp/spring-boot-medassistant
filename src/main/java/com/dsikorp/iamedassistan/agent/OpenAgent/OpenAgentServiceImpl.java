package com.dsikorp.iamedassistan.agent.OpenAgent;

import com.dsikorp.iamedassistan.config.ClientResolver;

import com.dsikorp.iamedassistan.tools.AppointmentSearchTool;
import com.dsikorp.iamedassistan.tools.DoctorInfoTool;
import com.dsikorp.iamedassistan.tools.DrugInfoTool;
import com.dsikorp.iamedassistan.tools.PatientInfoTool;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class OpenAgentServiceImpl implements OpenAgentService{

    private final ClientResolver clientResolver;
    private final PatientInfoTool patientInfoTool;
    private final AppointmentSearchTool appointmentSearchTool;
    private final DoctorInfoTool doctorInfoTool;
    private final DrugInfoTool drugInfoTool;

    @Value("classpath:prompts/open-agent.st")
    private Resource agentResource;

    private PromptTemplate agentTemplate;

    @PostConstruct
    void init() {
        agentTemplate = new PromptTemplate(agentResource);
    }

    @Override
    public String execute(String query, String model, Long userId) {

        log.info("Agente abierto — query: {}", query);

        String systemPrompt = agentTemplate.render(Map.of(
                "fechaActual", LocalDate.now().toString()
        ));

        String result = ChatClient.create(clientResolver.resolveModel(model))
                .prompt()
                .system(systemPrompt)
                .user(query)
                .tools(patientInfoTool, appointmentSearchTool, doctorInfoTool, drugInfoTool)
                .toolContext(Map.of("userId", userId, "role", "PATIENT"))
                .call()
                .content();

        log.info("Agente abierto — respuesta generada");

        return result;
    }
}


















