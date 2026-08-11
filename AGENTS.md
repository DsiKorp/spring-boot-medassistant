# iamedassistant

Spring Boot 4.1 / Java 25 / Spring AI 2.0 REST service. Exposes a medical-assistant chat API that fans out to two LLM providers (Google Gemini + local Ollama) selected per request.

## Stack & layout

- Package root: `com.dsikorp.iamedassistan` (note: typo'd — `iamedassistan`, not `iamedassistant`).
  - `IamedassistanApplication` — main entry.
  - `controller/ChatController` — `/api/v1/chat*` endpoints (`chat`, `chat/stream` SSE, `explain`, `symptoms`, `diagnose`, `consult`).
  - `service/AssistantServiceImpl` — routes between two `ChatClient` beans based on the `model` request field: `"ollama"` → `ollamaClient`, anything else → `geminiClient` (see `resolveClient`).
  - `config/AssistantConfig` — declares the two named `ChatClient` beans (`geminiClient`, `ollamaClient`), each pre-loaded with `prompts/system-prompt.st`.
  - `config/DotenvEnvironmentPostProcessor` — custom `.env` loader registered via `src/main/resources/META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports`. Runs at `HIGHEST_PRECEDENCE`.
  - `dto/ChatRequestDto` — `{ prompt: @NotBlank, model: String }`.
  - `TestChatClient`, `TestLlmCall` — dead scratch files in the main package, both commented out (`@Component`); ignore unless cleaning up.
- Resources: `application.yaml` (active profiles: `ollama, gemini`), `application-ollama.yml`, `application-gemini.yml`, prompts in `prompts/*.st`.

## Commands

- Run app: `./mvnw spring-boot:run` — listens on `:8080`, virtual threads enabled.
- Tests: `./mvnw test` — only test is `IamedassistanApplicationTests.contextLoads()`, which boots the full context.
- Package: `./mvnw package`.
- No Spotless / Checkstyle / additional linters configured; only Spring Boot starter defaults.

## Runtime prerequisites

Missing any of these makes the app fail to start (both AI profiles load eagerly, JPA eager):

1. PostgreSQL on `localhost:5432` — `docker compose up -d postgress` (note the typo in the service name in `docker-compose.yml`; container is `medassistant-db-container`, volume `medassistant-data`). Schema is `create-drop`, so it is rebuilt on every boot.
2. Ollama on `http://localhost:11434` with at least `llama3.2:3b` pulled (see `models.txt` for the local model roster).
3. `.env` at repo root with `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`, `GOOGLE_AI_API_KEY`. Sample values are committed in `.env` (gitignored).
4. Same prerequisites for `./mvnw test` — `contextLoads()` wires both `ChatModel`s and the `DataSource`.

## Quirks

- Two parallel AI starters (`spring-ai-starter-model-google-genai` + `spring-ai-starter-model-ollama`) are on the classpath and both profile YAMLs are active; both `ChatClient` beans are constructed at startup. The auto-config excludes are commented out in both `application-*.yml` — don't naively "fix" by uncommenting them, both are intentional.
- Lombok is required; `maven-compiler-plugin` already wires `lombok` as annotation processor for both `compile` and `test-compile` executions. Do not add it elsewhere.
- Prompt template placeholders are Spanish keys: `{condicion}`, `{sintomas}`, `{consulta}` — keep them aligned with the `.st` files.
- Hibernate `ddl-auto: create-drop` and `show-sql: true` — no migration tool is wired; do not introduce Flyway/Liquibase silently.
- `.vscode/launch.json` (gitignored) passes `.env` to the Spring Boot launcher; IDE runs use it automatically. `./mvnw` runs do not — the custom `EnvironmentPostProcessor` covers both.
- `spring-dotenv` (v5.1.0) is also on the classpath, but only the custom `DotenvEnvironmentPostProcessor` is registered in `EnvironmentPostProcessor.imports`, so don't expect dotenv-library semantics; if you wire its autoconfig you'll double-load `.env`.
- No CI workflows, no `opencode.json`, no `AGENTS.md`/`CLAUDE.md` history to inherit from.

## Conventions

- REST API under `/api/v1/...`; keep new endpoints versioned the same way.
- Sync responses return `ResponseEntity<String>`; streaming only `/chat/stream` uses `Flux<String>` with `text/event-stream; charset=UTF-8`.
- System prompt lives only in `prompts/system-prompt.st` and is injected via `.defaultSystem(...)` in `AssistantConfig`. Per-feature prompts (`explain-condition`, `symptom-analysis`, `diagnosis-cot`, `consultation`) are rendered with `PromptTemplate` in `AssistantServiceImpl.init()` (`@PostConstruct`) and addressed by their parameter keys.
