# iamedassistant

Servicio REST en Spring Boot que expone un asistente médico educativo. Permite enviar prompts a uno de dos proveedores LLM (Google Gemini o Ollama local) según el campo `model` de cada request.

> ⚕️ El sistema responde únicamente con fines **educativos**. No realiza diagnósticos ni prescribe medicamentos. Ver `src/main/resources/prompts/system-prompt.st`.

## Stack

- Java 25 + Spring Boot 4.1.0
- Spring AI 2.0.0 (`spring-ai-starter-model-google-genai` + `spring-ai-starter-model-ollama`)
- Spring Data JPA + PostgreSQL 17
- Lombok
- Virtual threads habilitados (Spring Boot 4)
- Reactor (`Flux<String>`) para streaming SSE

## Requisitos

| Servicio | Dónde | Cómo levantarlo |
|---|---|---|
| PostgreSQL 17 | `localhost:5432` | `docker compose up -d postgress` (servicio nombrado `postgress` por typo en `docker-compose.yml`) |
| Ollama | `http://localhost:11434` | Instalar Ollama y `ollama pull llama3.2:3b` (mínimo). Ver `models.txt` para más modelos. |
| Google Gemini API key | variable `GOOGLE_AI_API_KEY` | En el archivo `.env` en la raíz del repo. |

Archivo `.env` requerido en la raíz (está gitignored, ver `.env` para ejemplo):

```env
POSTGRES_USER=...
POSTGRES_PASSWORD=...
POSTGRES_DB=medassistant-db
GOOGLE_AI_API_KEY=...
```

El archivo se carga automáticamente mediante un `EnvironmentPostProcessor` propio (`DotenvEnvironmentPostProcessor`, registrado en `META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports`).

## Comandos

```bash
./mvnw spring-boot:run     # arranca la app en :8080
./mvnw test                # único test: contextLoads() (bootea el contexto completo)
./mvnw package             # genera el JAR
```

> ⚠️ `./mvnw test` y `./mvnw spring-boot:run` requieren que los tres servicios (Postgres, Ollama, Gemini API key válida) estén disponibles — el contexto se carga con ambos perfiles `ollama` y `gemini` activos.

## API

Base: `/api/v1/chat`. Todas reciben `Content-Type: application/json`.

| Método | Path | Descripción |
|---|---|---|
| `POST` | `/api/v1/chat` | Chat básico (texto plano) |
| `POST` | `/api/v1/chat/stream` | Chat en streaming (SSE, `text/event-stream`) |
| `POST` | `/api/v1/chat/explain` | Explica una condición médica en lenguaje accesible |
| `POST` | `/api/v1/chat/symptoms` | Análisis de síntomas con enfoque *few-shot* |
| `POST` | `/api/v1/chat/diagnose` | Diagnóstico con cadena de pensamiento (*chain-of-thought*) |
| `POST` | `/api/v1/chat/consult` | Consulta general con system prompt + plantilla |

### Cuerpo

```json
{ "prompt": "Texto del usuario", "model": "ollama" }
```

- `prompt` obligatorio (validado con `@NotBlank`).
- `model` selecciona el backend: `"ollama"` → modelo local; cualquier otro valor (incluido `"gemini"` o ausente) → Gemini.

### Ejemplos

```bash
# Chat con Ollama
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"prompt":"¿Qué es la hipertensión?","model":"ollama"}'

# Chat con Gemini (cualquier model distinto de "ollama")
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"prompt":"¿Qué es la hipertensión?"}'

# Streaming (SSE)
curl -N -X POST http://localhost:8080/api/v1/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"prompt":"Escribí un poema corto sobre Java","model":"ollama"}'

# Análisis de síntomas
curl -X POST http://localhost:8080/api/v1/chat/symptoms \
  -H "Content-Type: application/json" \
  -d '{"prompt":"Tengo fiebre 38°C y dolor de garganta desde hace 3 días"}'
```

## Estructura

```
src/main/java/com/dsikorp/iamedassistan/
├── IamedassistanApplication.java
├── controller/ChatController.java
├── service/AssistantService.java          # interfaz
├── service/AssistantServiceImpl.java      # enruta gemini/ollama + plantillas
├── config/AssistantConfig.java            # beans geminiClient, ollamaClient
├── config/DotenvEnvironmentPostProcessor.java
├── dto/ChatRequestDto.java
├── TestChatClient.java                    # scratch comentado, ignorar
└── TestLlmCall.java                       # scratch comentado, ignorar

src/main/resources/
├── application.yaml                       # perfiles activos: ollama, gemini
├── application-ollama.yml                 # http://localhost:11434, modelo por defecto
├── application-gemini.yml                 # GOOGLE_AI_API_KEY, gemini-3.1-flash-lite
├── META-INF/spring/...EnvironmentPostProcessor.imports
└── prompts/
    ├── system-prompt.st          # inyectado vía .defaultSystem(...) en ambos beans
    ├── explain-condition.st      # placeholder {condicion}
    ├── symptom-analysis.st       # placeholder {sintomas} (few-shot)
    ├── diagnosis-cot.st          # placeholder {sintomas} (chain-of-thought)
    └── consultation.st           # placeholder {consulta}
```

## Notas y particularidades

- **Dos proveedores LLM arrancan siempre.** Ambos starters (`google-genai` y `ollama`) están en el classpath y los perfiles `ollama,gemini` están activos en `application.yaml`. Los excludes de auto-config están comentados en `application-*.yml` a propósito — no los "arregles" descomentándolos.
- **Esquema efímero.** `spring.jpa.hibernate.ddl-auto=create-drop` reconstruye las tablas en cada arranque. No hay Flyway/Liquibase configurado; no los introduzcas silenciosamente.
- **Lombok obligatorio.** `pom.xml` ya configura `lombok` como annotation processor en `compile` y `test-compile`. No añadir de nuevo.
- **Placeholders en español.** Las plantillas `.st` usan `{condicion}`, `{sintomas}`, `{consulta}` (sin tildes). Mantener alineados con `AssistantServiceImpl.init()`.
- **System prompt centralizado.** El único lugar para modificar el tono/reglas globales es `prompts/system-prompt.st` (se inyecta en los dos `ChatClient`).
- **Carga de `.env` doble.** `spring-dotenv` está como dependencia pero solo el `EnvironmentPostProcessor` propio está registrado. No actives el autoconfig de `spring-dotenv` o `.env` se cargará dos veces.
- **Sin CI.** No hay workflows en `.github/`, no hay lint adicional al de Spring Boot starter defaults.
- **VS Code.** `.vscode/launch.json` (gitignored) pasa `.env` al launcher; IDE debug usa esa config automáticamente. `./mvnw` no — se cubre con el `EnvironmentPostProcessor`.