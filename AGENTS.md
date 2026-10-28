# AGENTS.md

Spring Boot 4.1.2 / Java 25 / Spring AI 2.0.2 REST service: **iamedassistant**, a medical *educational* assistant. Four layers: chat API, structured analysis, agent workflows, and document ingestion (RAG). MCP on both sides (own server + stdio client). All endpoints sit behind OAuth2/JWT (Keycloak).

> **The LLM must never diagnose or prescribe.** Tone/rules live only in `src/main/resources/prompts/system-prompt.st`.

## Stack & layout

- Package root: `com.dsikorp.iamedassistan` (note the typo — `iamedassistan`, not `iamedassistant`). It is intentional; don't "fix" it.
- `controller/` — `ChatController` (`/api/v1/chat*`), `AnalysisController` (`/api/v1/analysis*`), `AgentController` (`/api/v1/agent/*`), `DocumentController` (`/api/v1/documents/ingest`, ADMIN only), `McpDemoController` (`/api/v1/mcp/demo`, `!aws` only).
- `agent/` — `RoutingWorkflowServiceimpl` (query classifier + route dispatch), `AppointmentChainServiceImpl` (3-step booking + human-in-the-loop), `PendingBookingStore` (in-memory pending bookings), `OpenAgent/OpenAgentServiceImpl`, `orchestrator/` (`OrchestratorServiceImpl` + `ClinicalWorker`, `HistoryWorker`, `BibliographyWorker`).
- `config/` — `AssistantConfig` (`@Profile("!aws")`), `AssistantConfigAws` (`@Profile("aws")`), `ClientResolver` + `ClientResolverDefault` + `ClientResolverAws`, `MemoryConfig`, `MedicalAuditAdvisor`, `SecurityConfig`, `AppConfig` (OpenFDA `RestClient`), `DataLoader` (CommandLineRunner seed), `DotenvEnvironmentPostProcessor`.
- `mcp/` — `DoctorResourceProvider` (`@McpResource doctor://{specialty}`), `SymptomConsultationPromptProvider` (`@McpPrompt symptom-consultation`).
- `service/` — `AssistantService(Impl)`, `AnalysisService(Impl)`, `DoctorService`, `AppointmentService`, `PatientService`, `DrugInfoService`, `DocumentIngestionService` + `DocumentIngestionServiceAwsImpl`.
- `tools/` — 5 `@Tool` components: `AppointmentSearchTool`, `AppointmentBookingTool`, `DoctorInfoTool`, `PatientInfoTool`, `DrugInfoTool`. (Package was renamed `tool/` → `tools/`; use the plural.)
- `model/` — JPA entities `Doctor`, `Patient`, `Appointment` (plain `@Id Long id`, no `@GeneratedValue`; IDs come from the seed).
- `repository/` — Spring Data JPA. `DoctorRepository.findBySpecialtyIgnoreCase`, `findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrSpecialtyContainingIgnoreCase`; `AppointmentRepository.findByDoctorIdInAndDateAndAvailableTrue` / `…AndStartTimeAndAvailableTrue`.
- `mapper/` — MapStruct: `DoctorInfoMapper`, `PatientInfoMapper` (`componentModel = "spring"`).
- `dto/` — `ChatRequestDto` (`prompt` `@NotBlank`, `model` optional), analysis DTOs (`ConditionSummaryDto`, `SymptomAnalysisDto`, `QueryClassificationDto` + `SeverityEnum`/`UrgencyEnum`/`QueryTypeEnum`), `agent/` DTOs (`AppointmentRequest`, `AppointmentSelection`), `DrugInfo`, `DoctorInfo`, `PatientInfo`, `AppointmentInfo`.
- `exception/GlobalExceptionHandler.java`, `util/SecurityUtils.java`.
- `TestChatClient.java`, `TestLlmCall.java` — dead scratch files in the main package, both commented out. Ignore them.
- Resources: `application.yaml` (active profile **`aws`**), `application-aws.yml`, `application-{ollama,gemini,openai,anthropic,groq,minimax}.yml`, `schema-aws.sql`, `prompts/*.st` (16 templates).

## Profiles — the single most important thing to get right

Two **mutually exclusive** configurations, selected by `spring.profiles.active` (currently `aws`; the six-profile line above it is commented out).

| | `aws` | `!aws` (default) |
|---|---|---|
| Config | `AssistantConfigAws`, `ClientResolverAws` | `AssistantConfig`, `ClientResolverDefault` |
| LLMs | Gemini only — anything else throws `IllegalArgumentException` | 6 providers: gemini, ollama, openai, anthropic, groq, minimax |
| Vector store | pgvector on the app DB (`dimensions=3072`, table `medassistant_google`, index `NONE`) | Chroma at `localhost:8000`, one collection per provider |
| DB | RDS via `RDS_*`, `ddl-auto: update`, `schema-aws.sql` | local Postgres, `ddl-auto: create-drop` |
| MCP client | disabled | enabled (filesystem, github, engram) |
| RAG tuning | threshold 0.7, topK 3 | threshold 0.5, topK 5 |

**When you change provider routing, vector store wiring, or RAG parameters, update both configs.**

## Commands

- Run: `./mvnw spring-boot:run` — `:8080`, virtual threads enabled.
- Tests: `./mvnw test` — **160 unit tests** across 33 classes (plus the `contextLoads()` integration smoke test). Only `IamedassistanApplicationTests.contextLoads()` boots the full Spring context; everything else is a pure unit test with no DB, no network and no Spring context.
- Package: `./mvnw package`. Build without tests: `./mvnw clean install -DskipTests`.
- No Spotless / Checkstyle / extra linters — only Spring Boot starter defaults. **No CI workflows** in `.github/`.

## Runtime prerequisites

Missing any of these fails startup (every provider bean, both vector stores, and the DataSource are eager):

1. PostgreSQL 17 on `localhost:5432` — `docker compose up -d postgress` (the service name typo `postgress` is intentional; container `postgress-medassistant-db-container`, volume `medassistant-data`).
2. Keycloak 26.7.4 on `localhost:8090`, realm `medassistant` — `docker compose up -d keycloak`. Its own Postgres is on `5433`.
3. Chroma on `localhost:8000` — `docker compose up -d chroma` (only needed for `!aws`).
4. pgvector on `localhost:5434` to simulate RDS — `docker compose -f docker-compose.aws-test.yml up -d` (only needed for `aws`).
5. Ollama on `http://localhost:11434` with `llama3.2:3b` pulled (see `models.txt`) — only if you use `model: "ollama"`.
6. `.env` at repo root (gitignored). `spring-dotenv` is on the classpath but only the custom `DotenvEnvironmentPostProcessor` is registered — so `.env` semantics are *ours*, not the library's.

These prerequisites are needed for `./mvnw spring-boot:run` and for `IamedassistanApplicationTests.contextLoads()`. The other 159 unit tests need none of them.

## Security model

- `SecurityConfig` = OAuth2 Resource Server, JWT, `STATELESS`, CSRF disabled.
- Roles come from the `realm_access.roles` claim, prefixed to `ROLE_*` by a custom `JwtAuthenticationConverter`.
- `/api/v1/**` and `/mcp/**` require `PATIENT` or `ADMIN`; everything else requires authentication. `DocumentController.ingestDocument` additionally carries `@PreAuthorize("hasRole('ADMIN')")`.
- **The `userId` comes from the JWT, never from the request body.** Every controller does `jwt.getClaim("userId")`. That value is the `ChatMemory.CONVERSATION_ID` and the `PendingBookingStore` key. If the claim is absent, memory and booking state silently lose their per-user partition.
- Request body is only `{ "prompt": String, "model": String }`. `model` selects the provider; unknown/absent → `gemini` (in `!aws`).

## Quirks

- **Agents bypass the configured `ChatClient` beans.** `RoutingWorkflow`, `Orchestrator`, the booking chain and the open agent all do `ChatClient.create(clientResolver.resolveModel(model))` on the fly, so they do **not** inherit `defaultTools`, `QuestionAnswerAdvisor` or `defaultSystem`. `MedicalAuditAdvisor` is attached manually only where needed. If a new agent needs RAG or tools, pass them explicitly — don't assume the bean defaults apply.
- **`MessageChatMemoryAdvisor` is commented out inside the `ChatClient` beans** (it would throw `conversationId cannot be null` on endpoints that don't set one). It is attached per-request **only in `AssistantServiceImpl`** (`chat`, `chatStream`) via `.advisors(a -> a.advisors(memoryAdvisor).param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))`. `AnalysisServiceImpl` sets the `CONVERSATION_ID` param but **does not** attach the advisor — the `/analysis` endpoints currently have no conversational memory. Don't assume otherwise when debugging context loss.
- **`PendingBookingStore` is in-memory** (`ConcurrentHashMap` keyed by `userId`). Pending selections die on restart. Don't describe it as persisted.
- **`DataLoader` seeds only when empty.** `if (doctorRepository.count() > 0) return;` — required because the `aws` profile uses `ddl-auto: update` and the schema persists between restarts. Seed content: 5 doctors, 20 appointments (5 days × 4 slots, `available = count % 3 != 2`), 3 patients.
- **`QuestionAnswerAdvisor` uses a custom `PromptTemplate`** that lets the model fall back to tools or general knowledge when the retrieved context is empty — the default template would force a "no puedo responder". Keep that intent when editing.
- **Hibernate `ddl-auto` differs per profile** (`create-drop` local, `update` on AWS). No Flyway/Liquibase is wired; do not introduce one silently.
- **Lombok + MapStruct are already configured** as annotation processors for both `compile` and `test-compile` in `pom.xml`. Do not add them again.
- **`jackson-annotations` is a direct dependency on purpose** so the `JsonPropertyDescription` Structured Output DTOs always resolve.
- **Groq and MiniMax have no starter of their own.** They reuse the OpenAI/Anthropic models with alternative base URLs, built by hand in `AssistantConfig` so they don't collide with the native auto-config beans.
- **The AWS profile excludes several auto-configs** (Ollama, OpenAI, Anthropic chat/embedding/image/audio/moderation, Chroma, pgvector auto-config) because those starters create beans even without API keys and abort startup. The pgvector store is assembled manually in `AssistantConfigAws`. Don't re-enable them "for cleanliness".
- **Prompt template placeholders are unaccented Spanish keys**: `{currentDate}`, `{condicion}`, `{sintomas}`, `{consulta}`, `{pedido}`, `{turnos}`, `{documentos}`, `{analisisClinico}`, `{historial}`, `{bibliografia}`, `{fechaActual}`. Keep them aligned with the `Map.of(...)` calls that render them.
- **The system prompt lives only in `prompts/system-prompt.st`**, injected via `.defaultSystem(...)` with `{currentDate}` replaced at startup. Don't inline tone/rules into Java.
- **No CI, no `opencode.json`.**

## Conventions

- REST under `/api/v1/...`; keep new endpoints versioned the same way.
- Sync responses return `ResponseEntity<String>` (or a typed DTO for the structured endpoints). Streaming is `Flux<String>` with `produces = "text/event-stream; charset=UTF-8"` — used by `/chat/stream` and `/agent/orchestrator/progress`.
- Controllers are thin: they read `jwt.getClaim("userId")` / `SecurityUtils.extractRole(authentication)` and delegate to a service. Business logic belongs in `service/` or `agent/`.
- `PromptTemplate` is built once in `@PostConstruct void init()` from a `@Value("classpath:prompts/….st") Resource` and rendered with `Map.of(...)`.
- Tools are `@Component` + `@Tool(description = "…")` with `@ToolParam` parameters; register them via `defaultTools(...)` on the `ChatClient` and/or `.tools(...)` per call.
- New JPA entities go in `model/` with Lombok (`@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`); repository in `repository/`; entity↔DTO conversion in `mapper/` with MapStruct.
- Domain errors: throw `IllegalArgumentException` / `DateTimeParseException` — `GlobalExceptionHandler` maps them to 400. `RestClientException` → 503, `AccessDeniedException` → 403.

## Where to change what

| Goal | File(s) |
|---|---|
| Global tone / safety rules | `prompts/system-prompt.st` (only place) |
| Per-feature prompt | the matching `prompts/*.st` + its `Map.of(...)` render call |
| New chat endpoint | `ChatController` + `AssistantService(Impl)` |
| New analysis endpoint | `AnalysisController` + `AnalysisService(Impl)` |
| New agent behaviour | `agent/` — add a service, expose it in `AgentController` |
| New tool for the LLM | new class in `tools/` with `@Component` + `@Tool`; register in `AssistantConfig` **and** `AssistantConfigAws` |
| Provider routing | `ClientResolverDefault` / `ClientResolverAws` (+ the matching `ChatClient` bean) |
| Vector store / RAG tuning | `AssistantConfig` / `AssistantConfigAws` |
| Security rules | `SecurityConfig` (+ `SecurityUtils` for role extraction) |
| MCP server surface | `mcp/` (`@McpResource` / `@McpPrompt`) |
| MCP client connections | `application.yaml` → `spring.ai.mcp.client.stdio.connections` |
| Seed data | `config/DataLoader.java` |
| DB schema changes | entity in `model/` + `schema-aws.sql` if the extension is needed |

## Verification

- `./mvnw test` runs the full suite: 160 unit tests (fast, no infra) plus the `contextLoads()` smoke test that boots the whole context.
- When you touch a class that has a test, run that test class specifically (`-Dtest=<Class>Test`). When you add a class under `src/main`, add the mirrored `<Class>Test` under `src/test` in the same package — the suite is organised as a mirror of `src/main`.
- Manual: get a Keycloak token for realm `medassistant` with a `userId` attribute on the user, then `curl` against `:8080` with `Authorization: Bearer $TOKEN`. Without the `userId` claim, memory and booking state are not partitioned per user.
