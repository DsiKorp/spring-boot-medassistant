# iamedassistant

Servicio REST en **Spring Boot 4.1.2 + Java 25 + Spring AI 2.0.2** que expone un **asistente médico educativo**. Encima del chat básico se construyeron cuatro capas: **análisis estructurado**, **agentes** (routing, orquestador multi-worker, cadena de reservas con human-in-the-loop y agente abierto), **RAG con ingesta de documentos** e **integración MCP** (servidor propio + cliente contra servidores externos). Todo el API está detrás de **OAuth2/JWT (Keycloak)**.

> ⚕️ El sistema responde únicamente con fines **educativos**. No realiza diagnósticos ni prescribe medicamentos. Ver `src/main/resources/prompts/system-prompt.st`.

## Stack

- Java 25 + Spring Boot 4.1.2
- Spring AI 2.0.2
  - `google-genai`, `ollama`, `openai`, `anthropic` (chat) + `google-genai-embedding`
  - `spring-ai-starter-vector-store-chroma` y `spring-ai-starter-vector-store-pgvector`
  - `spring-ai-vector-store-advisor` (RAG con `QuestionAnswerAdvisor`)
  - `spring-ai-starter-model-chat-memory-repository-jdbc` (memoria conversacional persistente)
  - `spring-ai-starter-mcp-client` y `spring-ai-starter-mcp-server-webmvc`
  - `spring-ai-tika-document-reader` (ingesta de documentos)
- Spring Data JPA + PostgreSQL 17
- Spring Security OAuth2 Resource Server (JWT) + Keycloak 26.7.4
- MapStruct + Lombok
- Virtual threads habilitados
- Reactor (`Flux<String>`) para streaming SSE
- Jackson para Structured Output (`JsonPropertyDescription`)

## Perfiles de ejecución

El proyecto tiene **dos configuraciones mutually excluyentes**, seleccionadas por el perfil activo en `application.yaml` (`spring.profiles.active`). Hoy el valor activo es **`aws`**.

| | Perfil `aws` (producción) | Perfil por defecto (`!aws`) |
|---|---|---|
| Config | `AssistantConfigAws`, `ClientResolverAws` | `AssistantConfig`, `ClientResolverDefault` |
| LLM | **Solo Gemini** (`resolveModel` lanza excepción para cualquier otro) | **6 proveedores** (gemini, ollama, openai, anthropic, groq, minimax) |
| Vector store | **pgvector** sobre la misma BD (`dimensions=3072`, tabla `medassistant_google`, índice `NONE`) | **Chroma** en `localhost:8000`, una colección por proveedor (`medassistant_google`, `medassistant_ollama`, …) |
| BD | RDS (`RDS_HOST`, `RDS_PORT`, `RDS_DB`, `RDS_USERNAME`, `RDS_PASSWORD`), `ddl-auto: update` | Postgres local, `ddl-auto: create-drop` |
| Schema SQL | `schema-aws.sql` (`CREATE EXTENSION IF NOT EXISTS vector`) | ninguno |
| MCP cliente | **deshabilitado** (`spring.ai.mcp.client.enabled: false`) | activo: filesystem, github, engram |
| Búsqueda RAG | `similarityThreshold 0.7`, `topK 3` | `similarityThreshold 0.5`, `topK 5` |

Para volver al modo multi-proveedor local hay que cambiar `spring.profiles.active` en `application.yaml` (la línea con los seis perfiles está comentada justo arriba).

## Proveedores LLM soportados

Solo disponibles en el perfil por defecto (`!aws`).

| Proveedor (routing key) | Backend | API | Variables en `.env` |
|---|---|---|---|
| `gemini` (default) | Google Gemini | nativa Spring AI | `GOOGLE_AI_API_KEY`, `GOOGLE_AI_MODEL` |
| `ollama` | Ollama local | nativa Spring AI | `OLLAMA_AI_URL`, `OLLAMA_AI_MODEL` |
| `openai` | OpenAI nativo | nativa Spring AI | `OPENAI_AI_KEY`, `OPENAI_AI_MODEL`, `OPENAI_AI_BASE_URL` |
| `anthropic` | Anthropic nativo | nativa Spring AI | `ANTHROPIC_AI_KEY`, `ANTHROPIC_AI_MODEL`, `ANTHROPIC_AI_BASE_URL` |
| `groq` | Groq Cloud (OpenAI-compat.) | Spring AI OpenAI + URL Groq | `GROQ_AI_API_KEY`, `GROQ_AI_URL`, `GROQ_AI_MODEL` |
| `minimax` | MiniMax (Anthropic-compat.) | Spring AI Anthropic + URL MiniMax | `MINIMAX_AI_KEY`, `MINIMAX_AI_BASE_URL`, `MINIMAX_AI_MODEL` |

Si `model` viene ausente o con un valor desconocido, `ClientResolverDefault.normalize()` lo resuelve a **`gemini`**. En el perfil `aws` cualquier valor distinto de `gemini` produce un `IllegalArgumentException` ("Modelo no soportado en producción").

Groq y MiniMax no tienen starter propio: reutilizan los modelos OpenAI/Anthropic con `base-url` alternativas, construidos a mano en `AssistantConfig` para no colisionar con la auto-config nativa.

## Requisitos

| Servicio | Dónde | Cómo levantarlo |
|---|---|---|
| PostgreSQL 17 | `localhost:5432` | `docker compose up -d postgress` (servicio nombrado `postgress` por typo en `docker-compose.yml`) |
| Keycloak 26.7.4 | `localhost:8090` (realm `medassistant`) | `docker compose up -d keycloak` |
| Chroma | `localhost:8000` | `docker compose up -d chroma` (solo perfil `!aws`) |
| pgvector | `localhost:5434` | `docker compose -f docker-compose.aws-test.yml up -d` (simula RDS en local) |
| Ollama (opcional, solo `model: "ollama"`) | `http://localhost:11434` | `ollama pull llama3.2:3b` (chat) y `ollama pull nomic-embed-text` (embeddings). Ver `models.txt`. |
| API keys | variables en `.env` | copiar `.env.template` a `.env` y completar |

`docker-compose.yml` levanta Postgres (5432), Postgres de Keycloak (5433), Keycloak (8090) y Chroma (8000).

### Variables de entorno

`.env` en la raíz del repo, cargado por un `EnvironmentPostProcessor` propio (`DotenvEnvironmentPostProcessor`, registrado en `META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports`). `spring-dotenv` está como dependencia pero su auto-config está deshabilitada a propósito para evitar carga doble.

Variables principales (plantilla completa en `.env.template`):

```env
# Postgres local
POSTGRES_USER=...
POSTGRES_PASSWORD=...
POSTGRES_DB=medassistant-db

# RDS (perfil aws)
RDS_HOST=localhost
RDS_PORT=5434
RDS_DB=medassistant_prod
RDS_USERNAME=postgres
RDS_PASSWORD=...

# Keycloak
POSTGRES_USER_KEYCLOAK=...
POSTGRES_PASSWORD_KEYCLOAK=...
POSTGRES_DB_KEYCLOAK=...
KEYCLOAK_ADMIN=...
KEYCLOAK_ADMIN_PASSWORD=...

# Proveedores LLM
GOOGLE_AI_API_KEY=...
GOOGLE_AI_MODEL=gemini-3.1-flash-lite
OLLAMA_AI_URL=http://localhost:11434
OLLAMA_AI_MODEL=llama3.2:3b
OPENAI_AI_KEY=sk-...
OPENAI_AI_MODEL=gpt-5-mini
OPENAI_AI_BASE_URL=https://api.openai.com
ANTHROPIC_AI_KEY=sk-ant-...
ANTHROPIC_AI_MODEL=claude-haiku-4-5
ANTHROPIC_AI_BASE_URL=https://api.anthropic.com
GROQ_AI_API_KEY=gsk_...
GROQ_AI_URL=https://api.groq.com/openai/v1
GROQ_AI_MODEL=openai/gpt-oss-20b
MINIMAX_AI_KEY=sk-cp-...
MINIMAX_AI_BASE_URL=https://api.minimax.io/anthropic
MINIMAX_AI_MODEL=MiniMax-M3[1m]

# MCP client
GITHUB_TOKEN=...
```

## Comandos

```bash
./mvnw spring-boot:run     # arranca la app en :8080
./mvnw test                # suite completa: 160 tests unitarios
./mvnw package             # genera el JAR
./mvnw clean install -DskipTests   # build sin tests
```

> ⚠️ `./mvnw test` y `./mvnw spring-boot:run` requieren la BD arriba, Keycloak accesible y todas las API keys presentes: `IamedassistanApplicationTests.contextLoads()` bootea el contexto completo de la aplicación. **El resto de los tests son unitarios puros** (sin contexto de Spring, sin BD, sin red) y corren en segundos.

### Tests

```bash
./mvnw test                                          # toda la suite
./mvnw test -Dtest=AppointmentChainServiceImplTest    # una clase
./mvnw test -Dtest='tools/*Test'                      # un paquete
```

La suite tiene **160 tests** en 33 clases, organizados por capa:

| Área | Clases | Qué cubren |
|---|---|---|
| `service/` | 4 | Búsqueda de médicos, turnos disponibles, reserva, historial de paciente, OpenFDA (con `MockRestServiceServer`) |
| `tools/` | 5 | Las 5 tools del LLM: delegación, control de acceso por rol y por `userId` |
| `agent/` | 8 | Router por tipo, cadena de citas con human-in-the-loop, agente abierto, orquestador y sus 3 workers |
| `config/` | 5 | Resolución de proveedor (los 6 + AWS), advisor de auditoría, memoria, seed de datos |
| `mcp/` | 2 | Recurso de doctores por especialidad y prompt de consulta de síntomas |
| `controller/` | 5 | Routing HTTP, validación del body, DTOs tipados, streaming SSE y propagación del `userId` desde el JWT |
| `dto/`, `util/`, `exception/` | 3 | Validación de `ChatRequestDto`, extracción de roles y traducción de excepciones a códigos HTTP |

Dos helpers en `src/test/java/com/dsikorp/iamedassistan/`:

- **`ChatClientTestSupport`** — construye un `ChatClient` mock con la cadena fluida encadenable, para interceptar el `ChatClient.create(...)` estático que arman los agentes.
- **`MockMvcTestSupport`** — `MockMvc` standalone con un JWT sintético, para probar los controllers sin cargar el contexto ni depender de Keycloak.

Convenciones: JUnit Jupiter + Mockito + AssertJ, `@ExtendWith(MockitoExtension.class)`, `@DisplayName` en español, y los recursos de prompt (`@Value Resource`) se inyectan con `ReflectionTestUtils.setField(...)` antes de llamar al `init()` de cada agente.

## API

Base: `/api/v1`. Todas reciben `Content-Type: application/json` y **exigen un JWT válido** con rol `PATIENT` o `ADMIN`.

### Cuerpo común

```json
{ "prompt": "Texto del usuario", "model": "groq" }
```

- `prompt` obligatorio (validado con `@NotBlank`).
- `model` selecciona proveedor: `"ollama"`, `"openai"`, `"anthropic"`, `"groq"`, `"minimax"`, `"gemini"`. Ausente o desconocido → `gemini` (perfil por defecto).
- El `userId` **no** viene en el body: se lee del claim `userId` del JWT (`jwt.getClaim("userId")`) y se usa como `CONVERSATION_ID` de la memoria y como clave del estado de reservas pendientes.

### Chat (`/api/v1/chat`)

| Método | Path | Descripción |
|---|---|---|
| `POST` | `/api/v1/chat` | Chat básico (texto plano) |
| `POST` | `/api/v1/chat/stream` | Chat en streaming SSE (`text/event-stream; charset=UTF-8`) |
| `POST` | `/api/v1/chat/explain` | Explica una condición médica en lenguaje accesible |
| `POST` | `/api/v1/chat/symptoms` | Análisis de síntomas con enfoque *few-shot* |
| `POST` | `/api/v1/chat/diagnose` | Diagnóstico con cadena de pensamiento (*chain-of-thought*) |
| `POST` | `/api/v1/chat/consult` | Consulta general con system prompt + plantilla |

### Análisis estructurado (`/api/v1/analysis`)

Devuelven DTOs tipados (`ConditionSummaryDto`, `SymptomAnalysisDto`, `QueryClassificationDto`) usando *Structured Output* de Spring AI (`JsonPropertyDescription`).

| Método | Path | Descripción |
|---|---|---|
| `POST` | `/api/v1/analysis/condition` | Resumen estructurado de una condición |
| `POST` | `/api/v1/analysis/conditions` | Lista de condiciones relacionadas |
| `POST` | `/api/v1/analysis/symptoms` | Análisis estructurado de síntomas |
| `POST` | `/api/v1/analysis/classify` | Clasificación (severidad, urgencia, tipo) — base del router |

### Agentes (`/api/v1/agent`)

| Método | Path | Descripción |
|---|---|---|
| `POST` | `/api/v1/agent/routing` | Clasifica la consulta y la enruta a la plantilla especializada |
| `POST` | `/api/v1/agent/orchestrator` | Tres workers en paralelo + síntesis final |
| `POST` | `/api/v1/agent/orchestrator/progress` | Igual que el anterior, pero SSE con eventos de progreso |
| `POST` | `/api/v1/agent/chain/appointment` | Extrae → busca → selecciona turno (deja la reserva pendiente) |
| `POST` | `/api/v1/agent/chain/appointment/confirm` | Confirma y persiste la reserva pendiente |
| `POST` | `/api/v1/agent/chain/appointment/cancel` | Descarta la reserva pendiente |
| `POST` | `/api/v1/agent/open` | Agente abierto con todas las tools |

### Documentos (`/api/v1/documents`)

| Método | Path | Descripción |
|---|---|---|
| `POST` | `/api/v1/documents/ingest` | Ingesta un documento (multipart `file`) al vector store. **Solo `ADMIN`** |

### MCP (`/api/v1/mcp`)

| Método | Path | Descripción |
|---|---|---|
| `POST` | `/api/v1/mcp/demo` | Chat con las tools del cliente MCP conectadas. Solo perfil `!aws` |

Además, el **servidor MCP** propio se expone en `/mcp` (ruta por defecto del starter webmvc), con protocolo `STATELESS`.

### Autenticación

Todos los endpoints bajo `/api/v1/**` y `/mcp/**` requieren `ROLE_PATIENT` o `ROLE_ADMIN`. Los roles se extraen del claim `realm_access.roles` del JWT de Keycloak.

```bash
# 1. Obtener token (el client-id debe existir en el realm y tener un mapper para el atributo userId)
TOKEN=$(curl -s -X POST "http://localhost:8090/realms/medassistant/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=<client-id>" \
  -d "username=<user>" -d "password=<pass>" | jq -r .access_token)

# 2. Llamar la API
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt":"¿Qué es la hipertensión?","model":"gemini"}'
```

> El claim `userId` del token **debe** existir: sin él `jwt.getClaim("userId")` devuelve `null` y la memoria conversacional y la cadena de reservas quedan sin clave por usuario.

## Curls

### Chat por proveedor

```bash
# Gemini (default, sin model o model="gemini")
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt":"¿Qué es la hipertensión?"}'

# Ollama local
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt":"¿Qué es la hipertensión?","model":"ollama"}'

# OpenAI / Anthropic / Groq / MiniMax
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt":"¿Qué es la diabetes?","model":"anthropic"}'
```

### Streaming SSE

```bash
curl -N -X POST http://localhost:8080/api/v1/chat/stream \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt":"Escribí un poema corto sobre Java","model":"ollama"}'
```

`curl -N` (o `--no-buffer`) desactiva el buffering para ver los chunks en tiempo real.

### Plantillas del chat

```bash
curl -X POST http://localhost:8080/api/v1/chat/explain \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt":"diabetes tipo 2"}'

curl -X POST http://localhost:8080/api/v1/chat/symptoms \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt":"Tengo fiebre 38°C y dolor de garganta desde hace 3 días"}'

curl -X POST http://localhost:8080/api/v1/chat/diagnose \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt":"dolor torácico, mareo, sudor frío"}'

curl -X POST http://localhost:8080/api/v1/chat/consult \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt":"¿qué alimentos debo evitar con hipertensión?"}'
```

### Análisis estructurado

```bash
curl -X POST http://localhost:8080/api/v1/analysis/condition \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt":"asma bronquial"}'

curl -X POST http://localhost:8080/api/v1/analysis/conditions \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt":"enfermedad pulmonar obstructiva crónica"}'

curl -X POST http://localhost:8080/api/v1/analysis/symptoms \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt":"cefalea unilateral pulsátil, fotofobia, náuseas"}'

curl -X POST http://localhost:8080/api/v1/analysis/classify \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt":"me duele mucho el pecho y me cuesta respirar"}'
```

### Agentes

```bash
# Router: clasifica y responde con la ruta especializada
curl -X POST http://localhost:8080/api/v1/agent/routing \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt":"me duele mucho el pecho y me cuesta respirar"}'

# Orquestador (síncrono y con progreso SSE)
curl -X POST http://localhost:8080/api/v1/agent/orchestrator \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt":"fiebre, tos seca y dolor muscular"}'

curl -N -X POST http://localhost:8080/api/v1/agent/orchestrator/progress \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt":"fiebre, tos seca y dolor muscular"}'

# Cadena de reservas (3 pasos: extraer → seleccionar → confirmar)
curl -X POST http://localhost:8080/api/v1/agent/chain/appointment \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt":"quiero un turno de cardiología mañana"}'

curl -X POST http://localhost:8080/api/v1/agent/chain/appointment/confirm \
  -H "Authorization: Bearer $TOKEN"

curl -X POST http://localhost:8080/api/v1/agent/chain/appointment/cancel \
  -H "Authorization: Bearer $TOKEN"

# Agente abierto con tools
curl -X POST http://localhost:8080/api/v1/agent/open \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt":"¿qué medicamentos hay para la diabetes y quién es mi médico de cabecera?"}'
```

### Ingesta documental (ADMIN)

```bash
curl -X POST http://localhost:8080/api/v1/documents/ingest \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -F "file=@/ruta/al/documento.pdf"
```

## Tools

Viven en `tools/` (paquete plural) y son `@Component` con métodos `@Tool` anotados. Se registran como `defaultTools(...)` en los beans `ChatClient` de `AssistantConfig` y `AssistantConfigAws`, y además se pasan explícitamente en el agente abierto.

| Tool | Método | Qué hace |
|---|---|---|
| `AppointmentSearchTool` | `searchAppointments(specialty, date)` | Turnos disponibles por especialidad y fecha (`yyyy-MM-dd`) |
| `AppointmentBookingTool` | `bookAppointment(specialty, date, time, patientId)` | Reserva un turno y marca `available = false` |
| `DoctorInfoTool` | `searchDoctors(query)` | Busca médicos por nombre, apellido o especialidad |
| `PatientInfoTool` | `getPatientInfo(patientId)` | Historial clínico del paciente |
| `DrugInfoTool` | `getDrugInfo(drugName)` | Ficha del medicamento contra la API pública OpenFDA (`api.fda.gov`) |

Los tools delegan en `DoctorService` / `AppointmentService` / `PatientService` / `DrugInfoService` (`service/`), que operan sobre las entidades JPA `Doctor`, `Patient`, `Appointment` (`model/`) y sus repositorios (`repository/`). Las conversiones entidad↔DTO se hacen con MapStruct (`mapper/`).

## Los cuatro agentes

### 1. Router — `agent/RoutingWorkflowServiceimpl.java`

Clasifica la consulta con `AnalysisService.classifyQuery()` y la despacha por tipo:

| Tipo | Plantilla | System prompt |
|---|---|---|
| `EMERGENCY` | `routing-emergency.st` | especialista en urgencias, respuesta breve y directa |
| `SYMPTOM_REPORT` | `routing-symptom.st` | especialista en análisis de síntomas |
| `GENERAL_QUESTION` | `routing-general.st` | médico educador |
| `PRESCRIPTION_REQUEST` | `routing-prescription.st` | farmacólogo clínico (informa, no receta) |
| `OFF_TOPIC` | — | respuesta determinística, **sin llamada al modelo** |

Todas las rutas pasan por el `MedicalAuditAdvisor`.

### 2. Orquestador — `agent/orchestrator/OrchestratorServiceImpl.java`

Ejecuta tres workers y sintetiza:

- **ClinicalWorker** — análisis clínico de los síntomas (`worker-clinical.st`).
- **HistoryWorker** — historial del paciente desde la BD.
- **BibliographyWorker** — búsqueda semántica en el vector store (`topK=3`, `similarityThreshold=0.3`) e interpretación (`worker-bibliography.st`).

La síntesis final usa `orchestrator-synthesis.st` con los cuatro placeholders `{analisisClinico}`, `{historial}`, `{bibliografia}`, `{sintomas}`. La variante `/orchestrator/progress` emite SSE con marcadores `[PROGRESO] …` entre etapas.

### 3. Cadena de citas (human-in-the-loop) — `agent/AppointmentChainServiceImpl.java`

1. **Extrae** — `appointment-extraction.st` → entidad `AppointmentRequest` (especialidad, fecha) vía `.entity(...)`.
2. **Busca** — turnos disponibles en la BD.
3. **Selecciona** — `appointment-confirmation.st` → `AppointmentSelection`, que queda **pendiente** en `PendingBookingStore` (in-memory, `ConcurrentHashMap` indexado por `userId`).
4. **Confirma** (`/confirm`) — revalida que el turno siga disponible y lo persiste; o **cancela** (`/cancel`) — descarta la selección.

### 4. Agente abierto — `agent/OpenAgent/OpenAgentServiceImpl.java`

Un solo `ChatClient` con las cuatro tools de consulta (paciente, turnos, médicos, medicamentos), prompt de sistema `open-agent.st` (con `{fechaActual}`) y contexto de tool inyectado (`userId`, `role`).

## RAG e ingesta documental

- **Ingesta**: `DocumentController` → `DocumentIngestionService` → Tika lee el archivo, se trocea y se indexa en los vector stores. Detecta duplicados por similitud antes de insertar. El perfil `aws` usa `DocumentIngestionServiceAwsImpl` (solo pgvector); el resto, `DocumentIngestionServiceImpl` (Chroma).
- **Consulta**: `QuestionAnswerAdvisor` está montado como `defaultAdvisor` en todos los beans `ChatClient`, con un `PromptTemplate` propio que, a diferencia del default, **permite caer en las tools o en el conocimiento general** cuando los documentos no alcanzan.
- `BibliographyWorker` hace su propia `similaritySearch` sobre el vector store del proveedor resuelto.

## MCP

**Servidor** (`spring-ai-starter-mcp-server-webmvc`, protocolo `STATELESS`, nombre `medassistant-mcp`):

- Recurso `doctor://{specialty}` → `mcp/DoctorResourceProvider.java` lista doctores por especialidad.
- Prompt `symptom-consultation` → `mcp/SymptomConsultationPromptProvider.java` (requiere el argumento `symptom`).

**Cliente** (`spring-ai-starter-mcp-client`, solo perfil `!aws`): conexiones stdio a `filesystem` (lee `./src/main/resources/prompts`), `github` (usa `GITHUB_TOKEN`) y `engram`. `McpDemoController` expone `/api/v1/mcp/demo` para probar las tools del cliente.

## Seguridad

- `SecurityConfig` monta un **OAuth2 Resource Server** con JWT, sesiones `STATELESS` y CSRF deshabilitado.
- `JwtAuthenticationConverter` lee los roles de `realm_access.roles` y los prefija con `ROLE_`.
- Reglas: `/api/v1/**` y `/mcp/**` → `PATIENT` o `ADMIN`; el resto → `authenticated`.
- `DocumentController.ingestDocument` lleva `@PreAuthorize("hasRole('ADMIN')")`.
- `SecurityUtils.extractRole()` simplifica `ROLE_PATIENT`/`ROLE_ADMIN` a `PATIENT`/`ADMIN` y se pasa al servicio de chat.
- `GlobalExceptionHandler` traduce `RestClientException` → 503, `DateTimeParseException` → 400, `IllegalArgumentException` → 400 y `AccessDeniedException` → 403.

## Estructura

```
src/main/java/com/dsikorp/iamedassistan/
├── IamedassistanApplication.java
├── controller/
│   ├── ChatController.java                # /api/v1/chat/*
│   ├── AnalysisController.java            # /api/v1/analysis/*
│   ├── AgentController.java               # /api/v1/agent/*
│   ├── DocumentController.java            # /api/v1/documents/ingest (ADMIN)
│   └── McpDemoController.java             # /api/v1/mcp/demo (!aws)
├── agent/
│   ├── RoutingWorkflowServiceimpl.java    # router por tipo de consulta
│   ├── AppointmentChainServiceImpl.java   # cadena de reservas + HITL
│   ├── PendingBookingStore.java           # reservas pendientes por userId
│   ├── OpenAgent/OpenAgentServiceImpl.java
│   └── orchestrator/
│       ├── OrchestratorServiceImpl.java   # 3 workers + síntesis
│       ├── ClinicalWorker.java
│       ├── HistoryWorker.java
│       └── BibliographyWorker.java        # RAG
├── service/
│   ├── AssistantServiceImpl.java          # chat + streaming + plantillas
│   ├── AnalysisServiceImpl.java           # structured output
│   ├── DoctorService / AppointmentService / PatientService / DrugInfoService
│   └── DocumentIngestionService(+AwsImpl)
├── config/
│   ├── AssistantConfig.java               # @Profile("!aws"): 6 ChatClient + 6 VectorStore
│   ├── AssistantConfigAws.java            # @Profile("aws"): geminiClient + PgVectorStore
│   ├── ClientResolver.java                # interfaz
│   ├── ClientResolverDefault.java         # @Profile("!aws")
│   ├── ClientResolverAws.java             # @Profile("aws")
│   ├── MemoryConfig.java                  # MessageWindowChatMemory(10) sobre JDBC
│   ├── MedicalAuditAdvisor.java           # CallAdvisor + StreamAdvisor (logging)
│   ├── SecurityConfig.java                # OAuth2 JWT
│   ├── AppConfig.java                     # RestClient de OpenFDA
│   └── DataLoader.java                    # CommandLineRunner, seed demo
├── mcp/
│   ├── DoctorResourceProvider.java        # @McpResource doctor://{specialty}
│   └── SymptomConsultationPromptProvider.java  # @McpPrompt
├── model/                                 # Doctor, Patient, Appointment (JPA + Lombok)
├── repository/                            # Spring Data JPA
├── tools/                                 # 5 @Tool (AppointmentSearch/Booking, Doctor, Patient, Drug)
├── mapper/                                # MapStruct: DoctorInfoMapper, PatientInfoMapper
├── dto/                                   # ChatRequestDto, DTOs de análisis, agent, etc.
├── exception/GlobalExceptionHandler.java
└── util/SecurityUtils.java

src/main/resources/
├── application.yaml                       # perfil activo: aws
├── application-aws.yml                    # RDS + pgvector + solo Gemini
├── application-{ollama,gemini,openai,anthropic,groq,minimax}.yml
├── schema-aws.sql                         # CREATE EXTENSION vector
├── prompts/                               # 16 plantillas .st
└── META-INF/spring/...EnvironmentPostProcessor.imports
```

### Plantillas de prompt

Todas en `src/main/resources/prompts/`, renderizadas con `PromptTemplate`. Placeholders en español (sin tildes):

| Archivo | Placeholders |
|---|---|
| `system-prompt.st` | `{currentDate}` |
| `explain-condition.st` | `{condicion}` |
| `symptom-analysis.st` | `{sintomas}` |
| `diagnosis-cot.st` | `{sintomas}` |
| `consultation.st` | `{consulta}` |
| `structured-analysis.st` | `{sintomas}` |
| `routing-{emergency,symptom,general,prescription}.st` | `{consulta}` |
| `worker-clinical.st` | `{sintomas}` |
| `worker-bibliography.st` | `{documentos}`, `{sintomas}` |
| `orchestrator-synthesis.st` | `{analisisClinico}`, `{historial}`, `{bibliografia}`, `{sintomas}` |
| `appointment-extraction.st` | `{pedido}` |
| `appointment-confirmation.st` | `{pedido}`, `{turnos}` |
| `open-agent.st` | `{fechaActual}` |

## Notas y particularidades

- **Dos configuraciones por perfil.** `aws` y `!aws` son excluyentes y cada una tiene su propio `ChatClient`, `VectorStore` y `ClientResolver`. Al tocar uno, revisá el otro.
- **Los agentes no usan los `ChatClient` configurados.** `RoutingWorkflow`, `Orchestrator`, la cadena de citas y el agente abierto arman `ChatClient.create(clientResolver.resolveModel(model))` sobre la marcha, así que **no heredan** `defaultTools`, `QuestionAnswerAdvisor` ni `defaultSystem`; el `MedicalAuditAdvisor` lo agregan a mano solo donde hace falta. Si querés que un agente use RAG o tools, hay que pasarlos explícitamente.
- **Memoria conversacional por usuario.** `MessageWindowChatMemory` con ventana de 10 mensajes y repositorio JDBC (`initialize-schema: always`). El `MessageChatMemoryAdvisor` está comentado en los beans de `ChatClient` (rompería los endpoints sin `conversationId`) y se aplica **por request solo en `AssistantServiceImpl`** (`chat` y `chatStream`), con `CONVERSATION_ID = userId`. `AnalysisServiceImpl` fija el `CONVERSATION_ID` pero **no** agrega el advisor: los endpoints de `/analysis` hoy no conservan memoria entre turnos.
- **`userId` sale del JWT.** Todos los endpoints leen `jwt.getClaim("userId")`; el body solo lleva `prompt` y `model`.
- **`PendingBookingStore` es in-memory.** El estado de "turno pendiente de confirmar" se pierde al reiniciar la app.
- **OpenFDA es una API externa sin caché.** `DrugInfoServiceImpl` la consulta por `RestClient` en cada llamada y trunca los campos a 500 caracteres.
- **Esquema efímero en local.** `ddl-auto: create-drop` reconstruye las tablas en cada arranque (perfil `!aws`). En `aws` es `update` y `DataLoader` tiene una guarda `if (doctorRepository.count() > 0) return;` para no duplicar el seed. No hay Flyway/Liquibase: no los introduzcas silenciosamente.
- **Lombok y MapStruct ya están configurados** como annotation processors en `pom.xml` (`compile` y `test-compile`). No los agregues de nuevo.
- **Jackson explícito.** `jackson-annotations` es dependencia directa para que los DTOs con `JsonPropertyDescription` resuelvan siempre.
- **Carga de `.env` única.** No actives el autoconfig de `spring-dotenv` o `.env` se cargará dos veces.
- **Sin CI.** No hay workflows en `.github/` ni linters adicionales a los defaults de Spring Boot.
