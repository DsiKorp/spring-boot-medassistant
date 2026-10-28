# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> Documento alineado con `README.md` (guía de uso) y `AGENTS.md` (guía operativa para agentes). Si los tres discrepan, **el código manda**: verificá contra `src/main/` antes de asumir.

---

## Comandos comunes de desarrollo

**Correr la aplicación:** `./mvnw spring-boot:run`
**Ejecutar pruebas:** `./mvnw test`
**Empaquetar JAR:** `./mvnw package`
**Limpiar proyecto:** `./mvnw clean`
**Build sin tests:** `./mvnw clean install -DskipTests`

### Configurar servicios dependientes antes de correr

```bash
docker compose up -d postgress    # PostgreSQL 17 en :5432
docker compose up -d keycloak     # Keycloak 26.7.4 en :8090 (realm "medassistant")
docker compose up -d chroma       # Chroma en :8000 (solo perfil !aws)
docker compose -f docker-compose.aws-test.yml up -d   # pgvector en :5434 (simula RDS, perfil aws)
# Nota: el servicio de Postgres se llama "postgress" por typo en docker-compose.yml, no corríjalo.
```

Además: Ollama corriendo en `localhost:11434` con modelos cargados (roster en `models.txt` en la raíz) y un `.env` en la raíz del repo con las API keys. **Todos los endpoints `/api/v1/**` y `/mcp/**` exigen JWT de Keycloak con rol `PATIENT` o `ADMIN`**, y el token debe traer el claim `userId` (del que dependen la memoria conversacional y el estado de reservas).

### Pruebas

```bash
./mvnw test                                          # suite completa: 160 tests
./mvnw test -Dtest=AppointmentChainServiceImplTest    # una clase
./mvnw test -Dtest='tools/*Test'                      # un paquete
```

La suite tiene **160 tests en 33 clases** (`src/test/java/com/dsikorp/iamedassistan/`), en espejo con `src/main`. Salvo `IamedassistanApplicationTests.contextLoads()` (que bootea el contexto completo y sí necesita BD + Keycloak + API keys), **todo lo demás son unitarios puros**: sin contexto de Spring, sin BD, sin red.

Dos helpers compartidos en el paquete raíz de test:

- **`ChatClientTestSupport`** — `mockChatClient()` devuelve un `ChatClient` mock cuya cadena `prompt() → system() → user() → advisors() → tools() → call()` devuelve siempre el mismo spec. Necesario porque los agentes arman `ChatClient.create(...)` estático y hay que interceptarlo con `mockStatic(ChatClient.class)`.
- **`MockMvcTestSupport`** — `standalone(...)` arma un `MockMvc` standalone con el `AuthenticationPrincipalArgumentResolver` real y un JWT sintético; `asPatient(userId)` lo inyecta en el `SecurityContextHolder` y como principal de la request.

> ⚠️ `mockStatic(ChatClient.class)` es **thread-local**: sirve para el hilo del test, no para los `Schedulers.boundedElastic()`. Por eso el test del orquestador con progreso (SSE) stubbea el `ChatModel` en lugar del `ChatClient` estático.

Convenciones: JUnit Jupiter + Mockito + AssertJ, `@ExtendWith(MockitoExtension.class)`, `@DisplayName` en español, y los `@Value Resource` de prompt se inyectan con `ReflectionTestUtils.setField(...)` antes de invocar el `init()` del agente.

---

## Arquitectura y estructura del proyecto

Servicio REST en **Spring Boot 4.1.2 / Java 25 / Spring AI 2.0.2** que expone un **asistente médico educativo** (no diagnostica ni receta: lo fija `prompts/system-prompt.st`). Tiene cuatro capas:

1. **Chat** (`/api/v1/chat/*`) — texto plano, streaming SSE, y plantillas de explain/symptoms/diagnose/consult.
2. **Análisis estructurado** (`/api/v1/analysis/*`) — DTOs tipados vía *Structured Output* de Spring AI.
3. **Agentes** (`/api/v1/agent/*`) — router por tipo de consulta, orquestador multi-worker, cadena de reservas con human-in-the-loop y agente abierto.
4. **Ingesta documental** (`/api/v1/documents/ingest`, solo `ADMIN`) — RAG sobre Chroma o pgvector.

Encima: **integración MCP** (servidor propio + cliente stdio) y **seguridad OAuth2/Keycloak**.

### Estructura del código (src/main/java/com/dsikorp/iamedassistan/)

```
├── IamedassistanApplication.java         # Spring Boot app (sin @EntityScan: las entities están en model/)
├── controller/
│   ├── ChatController.java               # /api/v1/chat/* (text plano, streaming SSE, explain/symptoms/diagnose/consult)
│   ├── AnalysisController.java           # /api/v1/analysis/* (condition/conditions/symptoms/classify)
│   ├── AgentController.java              # /api/v1/agent/* (routing, orchestrator, chain/appointment, open)
│   ├── DocumentController.java           # /api/v1/documents/ingest — @PreAuthorize ADMIN
│   └── McpDemoController.java            # /api/v1/mcp/demo — solo perfil !aws
├── agent/
│   ├── RoutingWorkflowServiceimpl.java    # clasifica la consulta y la enruta (5 tipos)
│   ├── AppointmentChainServiceImpl.java   # extract → search → select → confirm/cancel (human-in-the-loop)
│   ├── PendingBookingStore.java           # reservas pendientes por userId (in-memory, ConcurrentHashMap)
│   ├── OpenAgent/OpenAgentServiceImpl.java
│   └── orchestrator/
│       ├── OrchestratorServiceImpl.java   # 3 workers + síntesis (variante SSE con progreso)
│       ├── ClinicalWorker.java
│       ├── HistoryWorker.java
│       └── BibliographyWorker.java        # RAG: similaritySearch sobre el vector store
├── service/
│   ├── AssistantService.java / Impl       # chat + streaming + plantillas .st
│   ├── AnalysisService.java / Impl        # structured output
│   ├── DoctorService / AppointmentService / PatientService / DrugInfoService
│   └── DocumentIngestionService(+AwsImpl) # ingesta + troceo a vector store
├── config/
│   ├── AssistantConfig.java              # @Profile("!aws"): 6 ChatClient + 6 VectorStore (Chroma) + ChromaApi
│   ├── AssistantConfigAws.java           # @Profile("aws"): geminiClient + PgVectorStore (dims 3072)
│   ├── ClientResolver.java               # interfaz: resolve / resolveModel / resolveVectorStore
│   ├── ClientResolverDefault.java        # @Profile("!aws") — 6 proveedores
│   ├── ClientResolverAws.java            # @Profile("aws") — solo Gemini, el resto lanza excepción
│   ├── MemoryConfig.java                 # MessageWindowChatMemory(10) sobre repositorio JDBC
│   ├── MedicalAuditAdvisor.java          # CallAdvisor + StreamAdvisor (logging de auditoría)
│   ├── SecurityConfig.java               # OAuth2 Resource Server, JWT, STATELESS
│   ├── AppConfig.java                    # RestClient de OpenFDA
│   ├── DataLoader.java                   # CommandLineRunner: 5 doctores, 20 turnos, 3 pacientes
│   └── DotenvEnvironmentPostProcessor    # carga .env una sola vez (autoconfig de spring-dotenv desactivada a propósito)
├── mcp/
│   ├── DoctorResourceProvider.java        # @McpResource doctor://{specialty}
│   └── SymptomConsultationPromptProvider.java  # @McpPrompt symptom-consultation
├── model/                                 # JPA entities con Lombok: Doctor, Patient, Appointment
├── repository/                            # Spring Data: findBySpecialtyIgnoreCase, findByDoctorIdInAndDateAndAvailableTrue, etc.
├── tools/                                 # 5 @Tool: AppointmentSearch, AppointmentBooking, DoctorInfo, PatientInfo, DrugInfo
├── mapper/                                # MapStruct: DoctorInfoMapper, PatientInfoMapper
├── dto/                                   # ChatRequestDto, DTOs de análisis, agent/, DrugInfo, DoctorInfo, PatientInfo
├── exception/GlobalExceptionHandler.java   # RestClient→503, DateTimeParse/IllegalArgument→400, AccessDenied→403
└── util/SecurityUtils.java                # extractRole(Authentication) → PATIENT | ADMIN | UNKNOWN
```

### Perfiles de ejecución (mutuamente excluyentes)

El perfil activo está en `application.yaml` → `spring.profiles.active`, hoy **`aws`**. La línea con los seis perfiles está comentada justo arriba.

| | `aws` | `!aws` (por defecto) |
|---|---|---|
| Config | `AssistantConfigAws`, `ClientResolverAws` | `AssistantConfig`, `ClientResolverDefault` |
| LLM | **solo Gemini** | **6 proveedores**: gemini, ollama, openai, anthropic, groq, minimax |
| Vector store | **pgvector** en la BD de la app (dims 3072, tabla `medassistant_google`, índice `NONE`) | **Chroma** en `:8000`, una colección por proveedor |
| BD | RDS (`RDS_*`), `ddl-auto: update`, `schema-aws.sql` | Postgres local, `ddl-auto: create-drop` |
| MCP cliente | **deshabilitado** | activo: filesystem, github, engram |
| RAG | threshold 0.7, topK 3 | threshold 0.5, topK 5 |

**Si tocás routing de proveedor, wiring de vector store o parámetros de RAG, actualizá los dos perfiles.**

### Flujo del asistente LLM

1. **Request → Controller:** `ChatController` recibe `ChatRequestDto` (`prompt` con `@NotBlank`, `model` opcional) y lee `userId` del claim `userId` del JWT. El body **nunca** lleva el userId.
2. **Routing:** `ClientResolver.resolve(model)` / `.resolveModel(model)` eligen el bean. `model` ausente o desconocido → `gemini` (en `!aws`); en `aws` cualquier valor distinto de `gemini` lanza `IllegalArgumentException`.
3. **Advisors:** los beans `ChatClient` traen `QuestionAnswerAdvisor` (RAG) y `MedicalAuditAdvisor` (auditoría) como `defaultAdvisors`, y las 5 tools como `defaultTools`.
4. **Respuesta:** texto plano o SSE con Reactor (`Flux<String>`).

### Los cuatro agentes

- **Router** (`RoutingWorkflowServiceimpl`): clasifica con `AnalysisService.classifyQuery()` y despacha a `EMERGENCY`, `SYMPTOM_REPORT`, `GENERAL_QUESTION`, `PRESCRIPTION_REQUEST` u `OFF_TOPIC` (este último responde sin llamar al modelo). Cada ruta usa su template `routing-*.st` y pasa por `MedicalAuditAdvisor`.
- **Orquestador** (`OrchestratorServiceImpl`): lanza ClinicalWorker + HistoryWorker + BibliographyWorker y sintetiza con `orchestrator-synthesis.st`. La variante `/orchestrator/progress` emite SSE con marcadores `[PROGRESO] …`.
- **Cadena de citas** (`AppointmentChainServiceImpl`): extrae con `appointment-extraction.st` → `AppointmentRequest`; busca turnos; selecciona con `appointment-confirmation.st` → `AppointmentSelection`, que queda **pendiente** en `PendingBookingStore`; `/confirm` revalida disponibilidad y persiste, `/cancel` descarta.
- **Agente abierto** (`OpenAgentServiceImpl`): un `ChatClient` con las tools de consulta (paciente, turnos, médicos, medicamentos), prompt `open-agent.st` y `toolContext` con `userId` y `role`.

> ⚠️ **Los agentes arman su propio `ChatClient.create(clientResolver.resolveModel(model))`**, así que **no heredan** `defaultTools`, `QuestionAnswerAdvisor` ni `defaultSystem`. `MedicalAuditAdvisor` lo agregan a mano donde hace falta. Si un agente nuevo necesita RAG o tools, pasalos explícitamente.

### Sub-sistema de gestión de citas y tools

- Las tools viven en `tools/` (paquete **plural**; antes era `tool/`) y son `@Component` + `@Tool` + `@ToolParam`: `AppointmentSearchTool`, `AppointmentBookingTool`, `DoctorInfoTool`, `PatientInfoTool`, `DrugInfoTool`.
- Se registran como `defaultTools(...)` en `AssistantConfig` **y** en `AssistantConfigAws` (si agregás una, actualizá los dos).
- Delegan en `DoctorService` / `AppointmentService` / `PatientService` / `DrugInfoService` (`service/`), que operan sobre las entidades JPA de `model/` vía `repository/`. Las conversiones entidad↔DTO las hace MapStruct (`mapper/`).
- `DrugInfoTool` consulta la API pública OpenFDA (`api.fda.gov`) a través del `RestClient` de `AppConfig`; trunca los campos a 500 caracteres.
- `DataLoader` (`config/`) es un `CommandLineRunner` que siembra 5 doctores, 20 turnos (5 días × 4 slots) y 3 pacientes demo.

### RAG e ingesta documental

- `QuestionAnswerAdvisor` está montado en todos los beans `ChatClient` con un `PromptTemplate` propio que **permite caer en las tools o en el conocimiento general** cuando los documentos no alcanzan (el template default forzaría "no puedo responder").
- `BibliographyWorker` hace su propia `similaritySearch` (`topK=3`, `similarityThreshold=0.3`) sobre el vector store del proveedor resuelto.
- Ingesta: `DocumentController` → `DocumentIngestionService` (Chroma) o `DocumentIngestionServiceAwsImpl` (pgvector); Tika lee el archivo y se trocea.

### MCP

- **Servidor** (`spring-ai-starter-mcp-server-webmvc`, protocolo `STATELESS`, nombre `medassistant-mcp`): expone el recurso `doctor://{specialty}` y el prompt `symptom-consultation`. El endpoint webmvc por defecto es **`/mcp`** (`spring.ai.mcp.server.streamable-http.mcp-endpoint`), y `SecurityConfig` lo protege con `PATIENT`/`ADMIN`.
- **Cliente** (`spring-ai-starter-mcp-client`, solo `!aws`): conexiones stdio a `filesystem` (lee `./src/main/resources/prompts`), `github` (usa `GITHUB_TOKEN`) y `engram`.

### Seguridad

- `SecurityConfig` monta un **OAuth2 Resource Server** con JWT, sesiones `STATELESS` y CSRF deshabilitado. Los roles salen del claim `realm_access.roles`, prefijados con `ROLE_` por un `JwtAuthenticationConverter` propio.
- Reglas: `/api/v1/**` y `/mcp/**` → `PATIENT` o `ADMIN`; el resto → `authenticated`. `DocumentController.ingestDocument` además lleva `@PreAuthorize("hasRole('ADMIN')")`.
- `SecurityUtils.extractRole()` simplifica a `PATIENT`/`ADMIN` y se pasa al servicio de chat.

### Prompts y plantillas (.st files)

Los prompts se guardan como `.st` en `src/main/resources/prompts/` (**16 plantillas**). Los placeholders se reemplazan con `PromptTemplate` en un `@PostConstruct init()`, nunca dentro del `ChatClient`. System-prompt centralizado para reglas/tone globales.

Placeholders en español **sin tildes**: `{currentDate}`, `{condicion}`, `{sintomas}`, `{consulta}`, `{pedido}`, `{turnos}`, `{documentos}`, `{analisisClinico}`, `{historial}`, `{bibliografia}`, `{fechaActual}`.

### Schema del DB

- Perfil `!aws`: `ddl-auto: create-drop` → esquemas efímeros reconstruidos en cada arranque.
- Perfil `aws`: `ddl-auto: update` + `schema-aws.sql` (`CREATE EXTENSION IF NOT EXISTS vector`).
- **No hay Flyway/Liquibase** — no los introduzcas silenciosamente.
- `DataLoader` tiene una guarda `if (doctorRepository.count() > 0) return;` porque en `aws` el schema persiste entre arranques: sin ella el seed se duplica en cada restart.

---

## Archivos clave por tarea típica

| Objetivo | Archivo(s) a editar/mover o crear |
|----------|--------------------------------------|
| Añadir una nueva función de chat (endpoint REST + LLM call + streaming SSE) | 1. `ChatController.java`: añadir `@PostMapping` en `/api/v1/chat/...`. 2. `AssistantService.java` + `AssistantServiceImpl.java`: implementar el método usando `clientResolver.resolve(model)` y devolver texto plano o `Flux<String>` para stream. 3. Si necesita tool o advisor, configurar el bean `ChatClient` correspondiente en `AssistantConfig` (y en `AssistantConfigAws`). |
| Añadir una nueva tool disponible para el LLM | Crear archivo en **`tools/`** con `@Component + @Tool(description="...")` y parámetros `@ToolParam`. Registrarla en `defaultTools(...)` de `AssistantConfig` **y** `AssistantConfigAws`. El scan de componentes la detecta sola; no requiere anotación extra. |
| Añadir un agente nuevo | Crear el service en `agent/`, inyectarlo en `AgentController`. Recordar que `ChatClient.create(...)` **no** trae tools/advisors por defecto: agregalos explícitamente si los necesita. |
| Ajustar comportamiento global del asistente (tono/reglas) | Editar `prompts/system-prompt.st`; el placeholder `{currentDate}` se sustituye en runtime. No editar directamente dentro de las llamadas a LLM: usá los placeholders y la inyección por bean. |
| Ajustar prompt específico (explicación / diagnóstico / routing) | Editar el template `.st` correspondiente (`explain-condition.st`, `symptom-analysis.st`, `routing-*.st`, `worker-*.st`, `orchestrator-synthesis.st`, `appointment-*.st`, `open-agent.st`) y alinear los `Map.of(...)` del render. |
| Cambiar el proveedor LLM o el routing | `ClientResolverDefault` / `ClientResolverAws` y el bean `ChatClient` del proveedor. Recordá actualizar **ambos perfiles** si el cambio aplica a los dos. |
| Tocar RAG o el vector store | `AssistantConfig` / `AssistantConfigAws` (threshold, topK, colección, tabla). |
| Cambiar reglas de seguridad o roles | `SecurityConfig.java` (+ `SecurityUtils` para la extracción de rol). |
| Cambiar la superficie del servidor MCP | `mcp/` (`@McpResource` / `@McpPrompt`). |
| Cambiar las conexiones del cliente MCP | `application.yaml` → `spring.ai.mcp.client.stdio.connections`. |
| Cambiar los datos de arranque | `config/DataLoader.java`. |

---

## Notas sobre configuración especial

- **Lombok y MapStruct:** annotation processors ya configurados en `pom.xml` (compile y test-compile). No añadir más. `jackson-annotations` es dependencia directa a propósito, para que los DTOs con `JsonPropertyDescription` (Structured Output) resuelvan siempre.
- **`.env` único:** cargado vía el `DotenvEnvironmentPostProcessor` propio; el autoconfig de `spring-dotenv` está desactivado a propósito para evitar carga doble. No lo actives.
- **Memoria conversacional:** `MessageWindowChatMemory` con ventana de 10 mensajes sobre repositorio JDBC. El `MessageChatMemoryAdvisor` está **comentado** en los beans de `ChatClient` (rompería los endpoints sin `conversationId`) y se aplica **por request solo en `AssistantServiceImpl`** (`chat`, `chatStream`), con `CONVERSATION_ID = userId`. `AnalysisServiceImpl` fija el `CONVERSATION_ID` pero **no** agrega el advisor: los endpoints `/analysis` hoy no conservan memoria.
- **`userId` siempre viene del JWT**, nunca del body. Es la clave de la memoria y del `PendingBookingStore`.
- **`PendingBookingStore` es in-memory:** el estado de "turno pendiente de confirmar" se pierde al reiniciar.
- **Excludes de auto-config:** el perfil `aws` excluye Ollama, OpenAI, Anthropic, Chroma y pgvector auto-config porque esos starters crean beans aun sin API key y abortan el arranque; el `PgVectorStore` se arma a mano en `AssistantConfigAws`. No los re-actives "por limpieza".
- **Groq y MiniMax no tienen starter propio:** reutilizan los modelos OpenAI/Anthropic con `base-url` alternativas, construidos a mano para no colisionar con los beans nativos.
- **Sin CI**, no hay workflows en `.github/` ni linters adicionales a los defaults de Spring Boot.
- **No hay `AGENTS.md`/`CLAUDE.md` desactualizado que heredear**: `README.md`, `AGENTS.md` y este archivo están alineados con el código actual.
