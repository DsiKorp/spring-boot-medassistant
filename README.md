# iamedassistant

Servicio REST en Spring Boot 4 con asistente médico educativo multi-proveedor. Enruta cada prompt a uno de **seis proveedores LLM** según el campo `model` del request: Google Gemini, Ollama local, OpenAI, Anthropic, Groq y MiniMax (compatible con Anthropic). Integra además un subsistema de gestión de citas médicas accesible como tool por el LLM.

> ⚕️ El sistema responde únicamente con fines **educativos**. No realiza diagnósticos ni prescribe medicamentos. Ver `src/main/resources/prompts/system-prompt.st`.

## Stack

- Java 25 + Spring Boot 4.1.1
- Spring AI 2.0.1 (todos los starters `spring-ai-starter-model-*` activos)
  - `google-genai`, `ollama`, `openai`, `anthropic`
  - Groq y MiniMax reutilizan los starters OpenAI/Anthropic (sus APIs son compatibles)
- Spring Data JPA + PostgreSQL 17
- Lombok
- Virtual threads habilitados
- Reactor (`Flux<String>`) para streaming SSE
- Jackson para DTOs estructurados de salida (Structured Output)

## Proveedores LLM soportados

| Proveedor (routing key) | Backend | API | Variables en `.env` |
|---|---|---|---|
| `gemini` (default) | Google Gemini | nativa Spring AI | `GOOGLE_AI_API_KEY`, `GOOGLE_AI_MODEL` |
| `ollama` | Ollama local | nativa Spring AI | `OLLAMA_AI_URL`, `OLLAMA_AI_MODEL` |
| `openai` | OpenAI nativo | nativa Spring AI | `OPENAI_AI_KEY`, `OPENAI_AI_MODEL`, `OPENAI_AI_BASE_URL` |
| `anthropic` | Anthropic nativo | nativa Spring AI | `ANTHROPIC_AI_KEY`, `ANTHROPIC_AI_MODEL`, `ANTHROPIC_AI_BASE_URL` |
| `groq` | Groq Cloud (OpenAI-compat.) | Spring AI OpenAI + URL Groq | `GROQ_AI_API_KEY`, `GROQ_AI_URL`, `GROQ_AI_MODEL` |
| `minimax` | MiniMax (Anthropic-compat.) | Spring AI Anthropic + URL MiniMax | `MINIMAX_AI_KEY`, `MINIMAX_AI_BASE_URL`, `MINIMAX_AI_MODEL` |

Si `model` viene ausente o con un valor desconocido, se enruta a **Gemini**.

## Requisitos

| Servicio | Dónde | Cómo levantarlo |
|---|---|---|
| PostgreSQL 17 | `localhost:5432` | `docker compose up -d postgress` (servicio nombrado `postgress` por typo en `docker-compose.yml`) |
| Ollama (opcional, solo si usás `model: "ollama"`) | `http://localhost:11434` | Instalar Ollama y descargar los modelos necesarios: `ollama pull llama3.2:3b` (mínimo, chat) y `ollama pull nomic-embed-text` (embeddings). Ver `models.txt` para más modelos. |
| API keys | variables en `.env` | En el archivo `.env` en la raíz del repo. |

Archivo `.env` requerido en la raíz (ver `.env.template` para la plantilla completa):

```env
POSTGRES_USER=...
POSTGRES_PASSWORD=...
POSTGRES_DB=medassistant-db

GOOGLE_AI_API_KEY=...
OLLAMA_AI_URL=http://localhost:11434
OLLAMA_AI_MODEL=llama3.2:3b
OPENAI_AI_KEY=sk-...
ANTHROPIC_AI_KEY=sk-ant-...
GROQ_AI_API_KEY=gsk_...
GROQ_AI_URL=https://api.groq.com/openai/v1
MINIMAX_AI_KEY=sk-cp-...
MINIMAX_AI_BASE_URL=https://api.minimax.io/anthropic
MINIMAX_AI_MODEL=MiniMax-M3[1m]
```

El archivo se carga automáticamente mediante un `EnvironmentPostProcessor` propio (`DotenvEnvironmentPostProcessor`, registrado en `META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports`). `spring-dotenv` está como dependencia pero su auto-config está deshabilitada a propósito para evitar carga doble.

## Comandos

```bash
./mvnw spring-boot:run     # arranca la app en :8080
./mvnw test                # único test: contextLoads() (bootea el contexto completo)
./mvnw package             # genera el JAR
./mvnw clean install -DskipTests   # build sin tests (útil en CI)
```

> ⚠️ `./mvnw test` y `./mvnw spring-boot:run` requieren Postgres arriba y todas las API keys presentes — el contexto se carga con los seis perfiles `ollama, gemini, openai, anthropic, groq, minimax` activos a la vez.

## API

Base: `/api/v1`. Todas reciben `Content-Type: application/json`.

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

Devuelven DTOs tipados (`ConditionSummaryDto`, `SymptomAnalysisDto`, `QueryClassificationDto`, etc.) usando *Structured Output* de Spring AI (`JsonPropertyDescription`).

| Método | Path | Descripción |
|---|---|---|
| `POST` | `/api/v1/analysis/condition` | Resumen estructurado de una condición |
| `POST` | `/api/v1/analysis/conditions` | Lista de condiciones relacionadas |
| `POST` | `/api/v1/analysis/symptoms` | Análisis estructurado de síntomas |
| `POST` | `/api/v1/analysis/classify` | Clasificación de la consulta (severidad, urgencia, tipo) |

### Cuerpo común

```json
{ "prompt": "Texto del usuario", "model": "groq" }
```

- `prompt` obligatorio (validado con `@NotBlank`).
- `model` selecciona proveedor: `"ollama"`, `"openai"`, `"anthropic"`, `"groq"`, `"minimax"`. Cualquier otro valor (incluido ausente) → Gemini.

### Logs automáticos por request

Cada llamada no-streaming loggea con `log.info`:

```
[LLM response] modelo efectivo=openai/gpt-oss-20b
[LLM response] tokens prompt=124 | completion=387 | total=511
[LLM response] finish reason=STOP
```

Adicionalmente, `ClientResolver` loggea el proveedor y el modelo específico que se va a usar:

```
ClientResolver Proveedor modelo: groq
ClientResolver modelo específico: openai/gpt-oss-20b
```

> El streaming (`/chat/stream`) no emite metadata porque los chunks llegan incrementalmente; usar `/chat` cuando se necesiten tokens.

## Curls

### Chat por proveedor

```bash
# Gemini (default, sin model o model="gemini")
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"prompt":"¿Qué es la hipertensión?"}'

curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"prompt":"¿Qué es la hipertensión?","model":"gemini"}'

# Ollama local
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"prompt":"¿Qué es la hipertensión?","model":"ollama"}'

# OpenAI
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"prompt":"¿Qué es la hipertensión?","model":"openai"}'

# Anthropic
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"prompt":"¿Qué es la hipertensión?","model":"anthropic"}'

# Groq (OpenAI-compat.)
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"prompt":"¿Qué es la diabetes?","model":"groq"}'

# MiniMax (Anthropic-compat.)
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"prompt":"¿Qué es la diabetes?","model":"minimax"}'
```

### Streaming SSE

```bash
# Streaming con Ollama
curl -N -X POST http://localhost:8080/api/v1/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"prompt":"Escribí un poema corto sobre Java","model":"ollama"}'

# Streaming con Gemini (ver headers y status)
curl -N -i -X POST http://localhost:8080/api/v1/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"prompt":"Hola","model":"gemini"}'
```

`curl -N` (o `--no-buffer`) desactiva el buffering para ver los chunks del stream en tiempo real.

### Plantillas del chat

```bash
# Explicar condición
curl -X POST http://localhost:8080/api/v1/chat/explain \
  -H "Content-Type: application/json" \
  -d '{"prompt":"diabetes tipo 2"}'

# Análisis de síntomas
curl -X POST http://localhost:8080/api/v1/chat/symptoms \
  -H "Content-Type: application/json" \
  -d '{"prompt":"Tengo fiebre 38°C y dolor de garganta desde hace 3 días"}'

# Diagnóstico con chain-of-thought
curl -X POST http://localhost:8080/api/v1/chat/diagnose \
  -H "Content-Type: application/json" \
  -d '{"prompt":"dolor torácico, mareo, sudor frío"}'

# Consulta general
curl -X POST http://localhost:8080/api/v1/chat/consult \
  -H "Content-Type: application/json" \
  -d '{"prompt":"¿qué alimentos debo evitar con hipertensión?"}'
```

### Análisis estructurado

```bash
# Resumen de una condición
curl -X POST http://localhost:8080/api/v1/analysis/condition \
  -H "Content-Type: application/json" \
  -d '{"prompt":"asma bronquial"}'

# Lista de condiciones relacionadas
curl -X POST http://localhost:8080/api/v1/analysis/conditions \
  -H "Content-Type: application/json" \
  -d '{"prompt":"enfermedad pulmonar obstructiva crónica"}'

# Análisis estructurado de síntomas (devuelve SymptomAnalysisDto)
curl -X POST http://localhost:8080/api/v1/analysis/symptoms \
  -H "Content-Type: application/json" \
  -d '{"prompt":"cefalea unilateral pulsátil, fotofobia, náuseas"}'

# Clasificación (devuelve QueryClassificationDto con SeverityEnum/UrgencyEnum/QueryTypeEnum)
curl -X POST http://localhost:8080/api/v1/analysis/classify \
  -H "Content-Type: application/json" \
  -d '{"prompt":"me duele mucho el pecho y me cuesta respirar"}'
```

## Tools y subsistema MedAssistant

El LLM tiene acceso a dos `@Tool` declarados en `tool/`:

- **`AppointmentSearchTool`** — busca turnos disponibles por doctor/fecha.
- **`DoctorInfoTool`** — devuelve información de un doctor por ID.

Ambos están registrados como `defaultTools(...)` en cada bean `ChatClient` de `AssistantConfig`, así que el modelo puede invocarlos automáticamente cuando la consulta del usuario lo requiera (ej. *"Tengo dolor de cabeza, ¿qué especialista me conviene y cuándo hay turno?"*).

Las tools delegan en `DoctorService` / `AppointmentService` (`service/`), que operan sobre entidades JPA `Doctor`, `Patient`, `Appointment` (en `model/`) con sus repositorios en `repository/`. `DataLoader` (`service/`) es un `CommandLineRunner` que siembra 5 doctores, ~30 turnos y pacientes demo en cada arranque (compatible con `ddl-auto: create-drop`).

## Estructura

```
src/main/java/com/dsikorp/iamedassistan/
├── IamedassistanApplication.java          # @EntityScan Doctor/Patient/Appointment
├── controller/
│   ├── ChatController.java                # /api/v1/chat/*
│   └── AnalysisController.java            # /api/v1/analysis/*
├── service/
│   ├── AssistantService.java              # interfaz asistente
│   ├── AssistantServiceImpl.java          # enruta gemini/ollama/openai/anthropic/groq/minimax
│   ├── AnalysisService.java               # interfaz análisis estructurado
│   ├── AnalysisServiceImpl.java
│   ├── DoctorService.java
│   ├── AppointmentService.java
│   └── DataLoader.java                    # CommandLineRunner, siembra datos demo
├── config/
│   ├── AssistantConfig.java               # 6 beans ChatClient + defaultTools
│   ├── ClientResolver.java                # routing + log del modelo específico
│   └── DotenvEnvironmentPostProcessor.java
├── model/                                 # Doctor, Patient, Appointment (JPA + Lombok)
├── repository/                            # Spring Data JPA repositories
├── tool/                                  # AppointmentSearchTool, DoctorInfoTool (@Tool)
├── dto/                                   # ChatRequestDto + DTOs de análisis
├── TestChatClient.java                    # scratch comentado (ChatClient → metadata)
└── TestLlmCall.java                       # scratch comentado (ChatModel → metadata)

src/main/resources/
├── application.yaml                       # perfiles activos: ollama, gemini, openai, anthropic, groq, minimax
├── application-ollama.yml
├── application-gemini.yml
├── application-openai.yml
├── application-anthropic.yml
├── application-groq.yml
├── application-minimax.yml
├── META-INF/spring/...EnvironmentPostProcessor.imports
└── prompts/
    ├── system-prompt.st          # inyectado vía .defaultSystem(...) en los 6 beans
    ├── explain-condition.st      # placeholder {condicion}
    ├── symptom-analysis.st       # placeholder {sintomas} (few-shot)
    ├── diagnosis-cot.st          # placeholder {sintomas} (chain-of-thought)
    └── consultation.st           # placeholder {consulta}
```

## Notas y particularidades

- **Seis proveedores arrancan siempre.** Los starters `google-genai`, `ollama`, `openai`, `anthropic` están en el classpath y los seis perfiles están activos en `application.yaml`. Los excludes de auto-config están comentados en los `application-*.yml` a propósito — no los "arregles" descomentándolos.
- **Groq y MiniMax sin starter propio.** Reutilizan los starters OpenAI/Anthropic porque exponen APIs compatibles; los beans `groqClient` y `minimaxClient` se construyen a mano en `AssistantConfig` apuntando a sus `base-url` respectivas. No colisionan con los beans nativos porque cada uno es un bean con nombre distinto.
- **Esquema efímero.** `spring.jpa.hibernate.ddl-auto: create-drop` reconstruye las tablas en cada arranque. No hay Flyway/Liquibase configurado; no los introduzcas silenciosamente.
- **Lombok obligatorio.** `pom.xml` ya configura `lombok` como annotation processor en `compile` y `test-compile`. No añadir de nuevo.
- **Jackson explícito.** `jackson-annotations` está declarado como dependencia directa para que los DTOs con `JsonPropertyDescription` (Structured Output) resuelvan siempre, incluso en IDEs con caché obsoleta.
- **Placeholders en español.** Las plantillas `.st` usan `{condicion}`, `{sintomas}`, `{consulta}` (sin tildes). Mantener alineados con `AssistantServiceImpl.init()`.
- **System prompt centralizado.** El único lugar para modificar el tono/reglas globales es `prompts/system-prompt.st` (se inyecta en los seis `ChatClient` con `{currentDate}` reemplazado en runtime).
- **Carga de `.env` única.** `spring-dotenv` está como dependencia pero solo el `EnvironmentPostProcessor` propio está registrado. No actives el autoconfig de `spring-dotenv` o `.env` se cargará dos veces.
- **Sin CI.** No hay workflows en `.github/`, no hay lint adicional al de Spring Boot starter defaults.
- **VS Code.** `.vscode/launch.json` (gitignored) pasa `.env` al launcher; IDE debug usa esa config automáticamente. `./mvnw` no — se cubre con el `EnvironmentPostProcessor`.