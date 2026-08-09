# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## Comandos comunes de desarrollo

**Correr la aplicación:** `./mvnw spring-boot:run`  
**Ejecutar pruebas:** `./mvnw test`  
**Empaquetar JAR:** `./mvnw package`  
**Limpiar proyecto:** `./mvnw clean`  

### Configurar servicios dependientes antes de correr

Levantar PostgreSQL (Docker):
```bash
docker compose up -d postgress
# Nota: el servicio se llama "postgress" por typo en docker-compose.yml, no corríjalo.
```

Asegurarse que Ollama está corriendo con modelos cargados (`localhost:11434`), y tener `.env` con `GOOGLE_AI_API_KEY`. Los perfiles `ollama,gemini` están activos simultáneamente en `application.yaml`; los excludes comentados a propósito deben permanecer así.

### Ejecutar una sola prueba (único test actual)
```bash
./mvnw test -Dtest=IamedassistanApplicationTests#contextLoads
```

---

## Arquitectura y estructura del proyecto

Este es un **servicio REST en Spring Boot 4** que implementa dos sub-sistemas:  
1. **iamedassistant**: asistente médico educativo con doble proveedor LLM (Google Gemini + Ollama local).  
2. **MedAssistant** (herramientas): gestión de citas médicas integrada como tool para el asistente.

### Estructura del código (src/main/java/com/dsikorp/iamedassistan/)
```
├── IamedassistanApplication.java         # Spring Boot app + EntityScanEnable con Doctor/Patient/Appointment
├── controller/
│   ├── ChatController.java               # /api/v1/chat/* (text plano, streaming SSE, explain/symptoms/diagnose/consult)
│   └── AnalysisController.java           # /api/v1/analysis/* (summarizeConditions/listRelatedSymptoms/analyze/analyzeSymptom/classifyQuery)
├── service/
│   ├── AssistantService.java             # interfaz para asistente LLM + plantillas (.st prompts en classpath:prompts/)
│   └── AnalysisServiceImpl.java          # implementation con ChatClient gemini|ollama según el campo model del request. Los beans de cliente se configuran abajo, los excludes comentados permanecen así a propósito.
├── service/DoctorService.java & AppointmentService.java       # interfaces para herramientas (bookAppointment/findAvailableAppointments)  
├── config/
│   ├── AssistantConfig.java              # beans geminiClient + ollamaClient con system-prompt.st y appointmentSearchTool como defaultTools; los excludes comentados de auto-config permanecen así a propósito.
│   └── DotenvEnvironmentPostProcessor    # EnvironmentPostProcessor propio para cargar .env una sola vez (spring-dotenv dependencia instalada pero solo este post-processor activo)  
├── model/                                 # JPA entities con Lombok: Doctor, Patient, Appointment
├── repository/                            # Spring Data repositories simples con count/saveAll/fetchByIds/findAvailableAppointmentsByDoctorIdDate/etc.
├── tool/                                  # @Component @Tool para herramientas disponibles en ChatClient: appointmentSearchTool y doctorInfo (buscar doctores por ID)  
└── dto/                                   # DTOs de entrada/salida con Lombok Records (@NotBlank prompt, model opcional): AppointmentInfo, DoctorInfo, + análisis médico
```

### Flujo del asistente LLM iamedassistant
1. **Request → Controller:** ChatController recibe `ChatRequestDto` (`prompt`, campo `model`: `"ollama"` o cualquier otro para Gemini).  
2. **Routing en AssistantServiceImpl.chat():** el método inspecciona `request.model()` y llama al cliente adecuado: `ollamaClient || geminiClient`.  
3. **Respuesta:** Stream SSE con Reactor (`Flux<String>`) para respuestas en tiempo real o texto plano sincronizado.

### Sub-sistema de gestión de citas MedAssistant
- Los métodos `DoctorService`/`AppointmentService` se invocan como tools desde el LLM cuando un usuario pregunta sobre disponibilidad o reservar turnos médicos.  
- El controlador AnalysisController expone endpoints `/condition`, `/conditions`, `/symptoms`, `/classify` con respuestas en DTOs estructurados (SeverityEnum/UrgencyEnum/QueryTypeEnum).  
- DataLoader implementa CommandLineRunner para cargar datos de prueba: 5 doctores, ~30 turnos distribuidos por días y timeslots, + pacientes demo.

### Configuración dual LLM
**Dos proveedores arrancan siempre:** ambos starters (`google-genai` / `ollama`) están en pom.xml con perfiles activos en application.yaml; los excludes comentados a propósito — no descomentarlos para evitar carga doble o conflictos de auto-config.  
- **Gemini backend:** usa API key desde variable de entorno GOOGLE_AI_API_KEY, modelo default: `gemini-3.1-flash-lite`.  
- **Ollama local:** llama a http://localhost:11434 con modelos recomendados en src/main/resources/models.txt (llama3.2:3b, mistral).  

### Prompts y plantillas (.st files)
Los prompts se guardan como `.st` en `src/main/resources/prompts/`. Los placeholders `{currentDate}`, `{condicion}`, `{sintomas}` son reemplazados dinámicamente por la clase que lee el recurso, no directamente dentro de ChatClient. System-prompt centralizado para reglas/tone globales; plantillas específicas para explain-condition/symptom-analysis/diagnosis-cot/consultation.

### Schema del DB
`spring.jpa.hibernate.ddl-auto=create-drop`: esquemas efímeros reconstruidos en cada arranque, sin Flyway/Liquibase — no los introduzcas silenciosamente. Los repositories tienen métodos como `findAvailableAppointmentsByDoctorIdDate`, `fetchByIds`, etc.

---

## Archivos clave por tarea típica

| Objetivo | Archivo(s) a editar/mover o crear |
|----------|--------------------------------------|
| Añadir una nueva función de chat (endpoint REST + LLM call + streaming SSE) | 1. ChatController.java: añadir `@PostMapping` mapeado en `/api/v1/chat/...`. 2. AssistantServiceImpl.java: implementar método en interfaz que rutea geminiClient \| ollamaClient según model param y devuelva texto plano o Flux<String> para stream. 3. Configurar @Bean del ChatClient correspondiente con tools si es necesario (AssistantConfig). |
| Añadir una nueva tool disponible para el LLM | Crear archivo en `tool/` con `@Component + @Tool(description="...")`, método parametrizado con @ToolParam y anotar servicio existente como dependencia. Registrarlo automáticamente por scan de componentes — no requiere anotación explícita adicional. |
| Ajustar comportamiento global del asistente (tono/reglas) | Editar `prompts/system-prompt.st`; cambia placeholder `{currentDate}` para lógica temporal específica en lugar del método que lee el archivo. No editar directamente dentro de las llamadas a LLM, usa los placeholders y la inyección por bean. |
| Ajustar prompt específico (explicación/ diagnóstico) | Editar template `.st` correspondiente (`explain-condition.st`, `symptom-analysis.st`) para incluir ejemplos few-shot o instrucciones paso a paso; el servicio reemplaza `{condicion}`, `{sintomas}` en init(). |

---

## Notas sobre configuración especial

- **Lombok:** annotation processors ya configurados en pom.xml (compile y test-compiles). No añadir más.  
- **.env único .env** cargado vía DotenvEnvironmentPostProcessor propio, evita carga doble con autoconfig de spring-dotenv comentada a propósito.  
- **No hay CI**, no workflows en `.github/`.
