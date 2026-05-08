# CLAUDE.md — ANVIL-BELLOWS Stabilization + Agent Presets

## Project

Repository:

`Lootziffer666/ANVIL-BELLOWS`

Main Android project:

`Android/AnvilBellows`

ANVIL-BELLOWS is an Android LLM router app assembled from OPENDORK/ANVIL-BELLOWS concepts.

This handoff has two gates:

1. **Stabilize the existing Android app**
2. **Add Android-native Agent Presets inspired by `SamurAIGPT/Open-Custom-GPT`**

Do not start feature work until the existing app builds cleanly.

---

## Core Interpretation

The user does not want a broad rewrite.

The correct product direction is:

```text
Provider Router
+ Local OpenAI-compatible API
+ Quota/Fallback Logic
+ Chat Overview
+ Agent Presets
+ Project/Context Awareness later
```

Agent Presets should behave like CustomGPT-style reusable chat modes, but they must be implemented inside ANVIL-BELLOWS's own architecture.

Use Open-Custom-GPT as a concept reference only.

Do not import Open-Custom-GPT.

Do not directly control ChatGPT CustomGPTs.

---

## Non-negotiables

- Do not redesign the whole app.
- Do not rename the app.
- Do not perform broad architecture rewrites.
- Do not remove working features to make the build pass.
- Do not “modernize” unrelated code.
- Do not make presets OpenAI-only.
- Do not import the Open-Custom-GPT Next.js app.
- Do not use `dangerouslyAllowBrowser`.
- Do not store API keys in plaintext files such as `db.json`.
- Do not expose API keys client-side.
- Do not build against ChatGPT web/session behavior.
- Do not execute arbitrary user-defined functions in this task.
- Use the Gradle wrapper: `./gradlew`, never plain `gradle`.

---

## Working Directory

Run Android commands from:

```bash
cd Android/AnvilBellows
```

Required verification:

```bash
./gradlew clean
./gradlew assembleDebug
./gradlew test
```

Success may only be claimed if `./gradlew assembleDebug` passes.

---

# Gate 1 — Stabilization / Bugfixing

Before adding Agent Presets, inspect and fix the existing integration problems.

## 1. GitHub Actions duplication and Gradle wrapper discipline

Known issue:

There are duplicate Android build workflows:

- `.github/workflows/android-build.yml`
- `.github/workflows/Build_Debug_APK.yaml`

Keep one canonical workflow, preferably:

- `.github/workflows/android-build.yml`

Delete or disable the duplicate workflow.

In the remaining workflow:

- use `working-directory: Android/AnvilBellows`
- run `./gradlew assembleDebug`
- run `./gradlew test`
- optionally run `./gradlew assembleRelease`
- do not run plain `gradle assembleDebug`
- do not force a different Gradle version than the wrapper
- prefer JDK 17 unless a verified reason exists for JDK 21

## 2. Local API server session safety

Known issue:

`AnvilApiServer` currently creates fake session IDs like:

```kotlin
val sessionId = "api_${System.currentTimeMillis()}"
```

But `ChatUseCase` persists assistant messages through `ConversationRepository.saveMessage(...)`.

`ConversationMessageEntity` has a foreign key to `ConversationSessionEntity`.

Therefore local API calls can crash because the fake session does not exist.

Implement a minimal safe fix.

Preferred option:

- inject `ConversationRepository` and `ProviderConfigDao` into `AnvilApiServer`
- create a real conversation session before calling `ChatUseCase`
- save incoming user messages if appropriate
- pass the real `sessionId` to `ChatUseCase`

Alternative acceptable option:

- add a stateless `ChatUseCase` mode for external API calls
- that mode must not persist session-bound data

Do not silently swallow DB foreign key exceptions.

## 3. First-launch provider seeding race

Known issue:

`AnvilBellowsApp` initializes `DatabaseInitializer` asynchronously in `Application.onCreate()`.

`ChatViewModel` immediately queries enabled providers during init.

On a fresh install, provider retrieval can happen before seeding completes.

Fix this deterministically.

Possible approaches:

- inject `DatabaseInitializer` into `ChatViewModel` and call `initializeIfNeeded()` before querying providers
- or expose/wait for initialization state
- or make provider retrieval robust by ensuring seed completion before first provider query

Do database work off the main thread.

## 4. Dynamic base URL must preserve provider path prefixes

Known issue:

`DynamicBaseUrlInterceptor` only swaps scheme/host/port and can lose provider path prefixes.

Provider base URLs may contain required paths:

```text
https://generativelanguage.googleapis.com/v1beta/openai/
https://api.groq.com/openai/
https://api.cohere.com/compatibility/
https://openrouter.ai/api/
https://router.huggingface.co/hf-inference/
https://open.bigmodel.cn/api/paas/v4/
```

The final request URL must combine provider base path + Retrofit endpoint path.

Example:

```text
base:     https://api.groq.com/openai/
endpoint: v1/chat/completions
final:    https://api.groq.com/openai/v1/chat/completions
```

Avoid duplicate slashes.

Add focused tests if practical.

## 5. Local proxy security default

Known issue:

The local OpenAI-compatible proxy on port `4141` should not be silently exposed without protection.

Preferred default:

- localhost-only binding, or
- required local bearer token stored securely

LAN exposure must be explicit.

Do not break the intended local OpenAI-compatible API use case.

## 6. Packaging/resource conflicts

`app/build.gradle.kts` already excludes duplicate META-INF resources.

Do not broaden excludes blindly.

Only change packaging rules if the build reports a specific resource conflict.

---

# Gate 2 — Agent Presets Feature

Only begin this gate after Gate 1 builds.

## Product Goal

Add an Android-native Agent Preset feature.

The user should eventually be able to open the chat overview and choose something like:

```text
Normal Chat
Repo Auditor
Prompt Smith
PRD Sharpener
Custom Agent
```

A preset should define reusable chat behavior:

- name
- description
- system instructions
- optional default provider/model
- optional tool flags
- optional function/action schema JSON for future use

The preset should shape the chat context before messages are sent to the existing LLM router.

## Open-Custom-GPT Mapping

Use this mapping only as conceptual guidance:

```text
Open-Custom-GPT assistant name       -> AgentPreset.name
Open-Custom-GPT instructions         -> AgentPreset.systemPrompt
Open-Custom-GPT retrieval/files      -> future fileScopeIds / FILE_CONTEXT
Open-Custom-GPT functions/actions    -> functionSchemaJson, stored but not executed yet
Open-Custom-GPT embedded chat        -> ANVIL-BELLOWS chat overview / local API later
Open-Custom-GPT Assistants API object -> do not depend on it
```

## Data Model

Create a persistent Room-backed Agent Preset model.

Suggested entity:

```kotlin
@Entity(tableName = "agent_presets")
data class AgentPresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val systemPrompt: String,
    val defaultProviderId: String? = null,
    val defaultModelId: String? = null,
    val enabledToolIdsJson: String = "[]",
    val fileScopeIdsJson: String = "[]",
    val functionSchemaJson: String? = null,
    val isPinned: Boolean = false,
    val isBuiltIn: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)
```

Suggested domain model:

```kotlin
data class AgentPreset(
    val id: String,
    val name: String,
    val description: String,
    val systemPrompt: String,
    val defaultProviderId: String?,
    val defaultModelId: String?,
    val enabledToolIds: List<String>,
    val fileScopeIds: List<String>,
    val functionSchemaJson: String?,
    val isPinned: Boolean,
    val isBuiltIn: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
```

Required persistence pieces:

- `AgentPresetEntity`
- `AgentPresetDao`
- repository layer
- Room database registration
- DI provider
- migration or safe schema version handling

Use existing Room patterns.

## Seed Presets

Seed a small safe set if no presets exist:

```text
General Chat
Repo Auditor
Prompt Smith
PRD Sharpener
```

Keep prompts generic.

Do not include private project lore.

## Chat Session Integration

Add optional preset association to chat sessions:

```kotlin
presetId: String?
```

When a new chat is started from a preset:

- create a session with `presetId`
- keep normal chat possible with `presetId = null`
- store/display selected preset name where appropriate

## Chat Context Construction

When `ChatUseCase` sends messages for a session with a preset:

1. Load the preset.
2. Prepend a system message using `preset.systemPrompt`.
3. Add memory/context if existing logic already does so.
4. Add user-visible chat messages.

Do not save the system prompt as a visible normal chat message.

Do not duplicate system prompts on repeated sends.

Provider handoff must preserve this constructed message list.

## Provider / Model Behavior

If preset has no default provider/model:

- use existing provider ranking/fallback.

If preset has default provider/model:

- try preferred provider/model first when available
- if unavailable, over quota, or failing, fall back through existing handoff logic
- do not hard-fail unless a strict mode already exists

Do not make Agent Presets OpenAI-only.

## Tool Model

For this step, implement conservative tool storage only.

Examples:

```kotlin
enum class AgentToolId {
    LOCAL_MEMORY,
    WIKI_EXPORT,
    CODE_HELP,
    REPO_AUDIT,
    FILE_CONTEXT
}
```

Or use string IDs if that fits existing architecture.

Function/action JSON:

- store it
- validate syntax
- display it back for editing
- do not execute it yet
- do not make arbitrary network calls from it

## UI Scope

Keep UI minimal.

Required surfaces:

1. Chat overview:
   - Normal Chat
   - Built-in presets
   - Custom presets
   - Create Agent button

2. Preset editor:
   - Name
   - Description
   - Instructions/System Prompt
   - Optional default provider
   - Optional default model
   - Optional tool toggles
   - Optional function schema JSON text area

Validation:

- name not blank
- system prompt not blank
- function schema JSON empty or syntactically valid

Use existing Compose/Material3 patterns.

Do not introduce a webview-based builder.

---

## Final Verification

Run:

```bash
cd Android/AnvilBellows
./gradlew clean
./gradlew assembleDebug
./gradlew test
```

If a command fails:

1. fix the actual error
2. do not hide it
3. do not upgrade dependencies randomly
4. rerun the failing task
5. rerun full verification

---

## Deliverables

Report:

1. Files changed
2. Bugfixes completed
3. New entities/DAOs/models
4. UI changes
5. How preset selection starts a chat
6. How systemPrompt is injected
7. How default provider/model is handled
8. Verification commands and results
9. Remaining risks

Do not claim success unless `./gradlew assembleDebug` passes.
