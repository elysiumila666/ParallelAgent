# ParallelAgent

> A human-aware Android agent that works with you, not over you.

Most phone agents assume exclusive control of the device: they open apps, navigate interfaces, and occupy the foreground while completing a task.

ParallelAgent explores a different interaction model:

**The user owns the foreground. The agent owns the background.**

Instead of blindly executing every request, ParallelAgent reasons about the required capability and lets an Android-side runtime decide whether the task should execute immediately, wait for foreground access, or request explicit user approval.

---

## The Problem

Existing GUI-based phone agents often behave as if the phone belongs entirely to the agent.

This creates a fundamental conflict: while an agent is navigating an app, the human cannot comfortably continue using the same screen.

ParallelAgent treats the phone as a shared environment between two actors:

- the human user
- the AI agent

The key problem is therefore not only:

> "How can an agent operate a phone?"

but also:

> "When should an agent act, wait, or ask?"

---

## Core Idea

ParallelAgent separates **planning** from **execution policy**.

The LLM decides **what capability is needed**.

The Android runtime decides **whether and when that capability should execute**.


<img width="1536" height="1024" alt="C91B83E2-24A0-415C-9231-52C16C789C06" src="https://github.com/user-attachments/assets/7fcfe172-5f6c-4088-9c9d-7d7f1ef94d1a" />


This prevents the LLM from directly deciding whether an action is safe to execute.

---

## Human-Aware Scheduling

Each capability declares two important properties:

```text
Resource requirement:
- NETWORK
- OS_BACKGROUND
- FOREGROUND_UI

External side effect:
- true
- false
```

The runtime uses these properties together with the current phone state to make a scheduling decision.

### 1. EXECUTE

Safe background operations can run without interrupting the user.

Example:

```text
"What's the current CNY to AUD exchange rate?"
        ↓
exchange_rate
        ↓
NETWORK + no external side effect
        ↓
EXECUTE
```

The user can remain inside another app while the result appears in the Agent Island.

---

### 2. DEFER

A task requiring foreground UI access is deferred when the human is actively using the foreground.

Example:

```text
"Find the Xiaohongshu post I viewed today."
        ↓
xiaohongshu_gui
        ↓
FOREGROUND_UI
        ↓
Foreground currently occupied by the user
        ↓
DEFER
```

The agent displays:

```text
Waiting · Xiaohongshu is in use
```

instead of taking over the screen.

---

### 3. ASK_APPROVAL

Actions with external side effects require explicit human approval.

Example:

```text
"Send the local file '何昕芮简历' to my WeChat contact Elysium."
        ↓
wechat_send_file
        ↓
External side effect = true
        ↓
ASK_APPROVAL
```

The Agent Island displays:

```text
Approval required
```

While approval is pending:

```text
Volume+ → Approve
Volume− → Reject
```

The current prototype records an approved WeChat action as:

```text
Approved · Queued
```

The actual WeChat GUI file-sending executor is not implemented in this prototype. The interface intentionally does not claim that the file has been sent.

---

## Interaction Model

ParallelAgent is designed so that the user does not need to leave their current app to interact with the agent.

### Wake Agent

Double-press:

```text
Volume+
Volume+
```

The agent records the voice request and captures the foreground application context.

### Abort

Double-press:

```text
Volume-
Volume-
```

### Approval Mode

When the runtime enters `WAITING_APPROVAL`:

```text
Single Volume+ → Approve
Single Volume− → Reject
```

A system overlay ("Agent Island") exposes the agent's current state without taking over the foreground UI.

---

## Architecture

```text
                    ┌─────────────────────┐
                    │     Human User      │
                    └──────────┬──────────┘
                               │ Voice
                               ↓
                    ┌─────────────────────┐
                    │        ASR          │
                    │  DashScope / Qwen   │
                    └──────────┬──────────┘
                               ↓
                    ┌─────────────────────┐
                    │    LLM Planner      │
                    │      DeepSeek       │
                    └──────────┬──────────┘
                               │ Tool Call
                               ↓
                    ┌─────────────────────┐
                    │    Tool Registry    │
                    │ capability metadata │
                    └──────────┬──────────┘
                               ↓
                    ┌─────────────────────┐
                    │   Agent Scheduler   │
                    └──────────┬──────────┘
                               │
               ┌───────────────┼────────────────┐
               ↓               ↓                ↓
           EXECUTE           DEFER        ASK_APPROVAL
               │               │                │
               ↓               ↓                ↓
         Tool Executor     Wait for UI      Human decision
```

The architecture deliberately separates four concerns:

1. **Planner** — decides which capability matches the user's intent.
2. **Tool Registry** — describes the capabilities available to the agent.
3. **Scheduler** — decides whether and when a capability may execute.
4. **Executor** — performs the actual operation.

New tools can therefore be added to the registry without hard-coding their names into the LLM system prompt.

---

## Tool Registry

Capabilities are represented as structured tools rather than hard-coded natural-language rules.

For example:

```text
exchange_rate
├── resource: NETWORK
└── external side effect: false

xiaohongshu_gui
├── resource: FOREGROUND_UI
└── external side effect: false

wechat_send_file
├── resource: FOREGROUND_UI
└── external side effect: true
```

The same tool metadata is used both to expose capabilities to the LLM planner and to inform runtime scheduling.

This keeps capability selection separate from execution policy.

---

## Planner vs Runtime

ParallelAgent deliberately does not let the LLM control the entire execution process.

The responsibilities are separated:

```text
LLM Planner
"What capability does this request require?"
        ↓
Tool Registry
"What capabilities exist?"
        ↓
Agent Scheduler
"Can this capability execute now?"
        ↓
Tool Executor
"How is the capability actually performed?"
```

For example, DeepSeek may determine that a request requires `wechat_send_file`.

It does **not** decide whether sending the file is safe.

Instead, the Android runtime reads:

```text
hasExternalSideEffect = true
```

and independently produces:

```text
ASK_APPROVAL
```

This makes the runtime policy deterministic and inspectable rather than relying entirely on LLM judgment.

---

## Unsupported Capabilities

The agent does not pretend that every understood request can be executed.

If the LLM determines that none of the registered tools can satisfy a request, the planner returns:

```text
NO_TOOL
```

The Agent Island displays:

```text
Agent · Capability unavailable
```

For example, if no email tool is registered:

```text
"Send an email for me."
        ↓
Intent understood
        ↓
No matching registered capability
        ↓
NO_TOOL
```

This prevents unsupported requests from crashing the runtime or being falsely reported as completed.

---

## Implemented Prototype

The current prototype demonstrates three different scheduling decisions:

| User Request | Tool | Runtime Decision | Result |
|---|---|---|---|
| Check CNY/AUD exchange rate | `exchange_rate` | `EXECUTE` | Real network result |
| Find a previously viewed Xiaohongshu post | `xiaohongshu_gui` | `DEFER` | Waits instead of taking over foreground |
| Send a local file to a WeChat contact | `wechat_send_file` | `ASK_APPROVAL` | Approve/reject workflow; approved action is queued |

Together, these scenarios demonstrate that the agent does not apply the same execution strategy to every request.

Instead, execution depends on:

- the capability selected by the planner
- the resource required by the capability
- whether the human currently occupies the foreground
- whether the operation has an external side effect

---

## Demo Scenarios

### Scenario 1 — Background Execution

The user remains inside Xiaohongshu and asks:

> "What is the current CNY to AUD exchange rate?"

The planner selects:

```text
exchange_rate
```

Because this capability only requires network access and has no external side effect:

```text
Scheduler → EXECUTE
```

The exchange rate is retrieved without leaving Xiaohongshu.

---

### Scenario 2 — Foreground Conflict

While the user is actively using Xiaohongshu, they ask:

> "Find the Xiaohongshu post I viewed today."

The planner selects:

```text
xiaohongshu_gui
```

The runtime detects:

```text
Required resource = FOREGROUND_UI
Human currently occupies foreground = true
```

Therefore:

```text
Scheduler → DEFER
```

The Agent Island displays:

```text
Waiting · Xiaohongshu is in use
```

The agent does not interrupt the user's current interaction.

---

### Scenario 3 — Human Approval

The user asks:

> "Send the local file '何昕芮简历' to my WeChat contact Elysium."

The planner selects:

```text
wechat_send_file
```

The Tool Registry declares:

```text
External side effect = true
```

Therefore:

```text
Scheduler → ASK_APPROVAL
```

The runtime stores the pending action, including its arguments:

```text
PendingAction
├── tool: wechat_send_file
├── file: 何昕芮简历
└── contact: Elysium
```

The Agent Island displays:

```text
Approval required
```

The user can then make an explicit decision:

```text
Volume+ → Approved · Queued
Volume− → Rejected
```

This demonstrates that side-effectful operations require explicit human authorization.

---

## Tech Stack

- Kotlin
- Android AccessibilityService
- Android Foreground Service
- Android system overlay
- MediaRecorder
- DashScope / Qwen Audio ASR
- DeepSeek native tool calling
- JSON-based Tool Registry
- Android broadcast-based runtime events
- Public exchange-rate API

---

## Android Components

### AccessibilityService

Used to:

- observe foreground application activity
- capture global volume-button gestures
- wake the agent without leaving the current app
- handle approval/rejection input

### Foreground Service

Used for microphone recording and the voice-processing pipeline.

### Agent Island

A lightweight system overlay used to expose runtime state such as:

```text
Recording
Understanding
Planning
Executing
Waiting
Approval required
Approved
Rejected
Capability unavailable
```

The overlay provides observability without requiring the agent to occupy the foreground.

---

## Runtime States

The prototype includes explicit runtime state for approval-sensitive actions.

```text
IDLE
 ↓
WAITING_APPROVAL
 ├── Volume+ → APPROVED
 └── Volume− → REJECTED
```

A pending action is stored separately from the UI so that approval refers to a concrete planned operation rather than a generic confirmation screen.

---

## Prototype Limitations

This is a proof-of-concept focused on the **human-aware agent runtime and scheduling model**, rather than broad app automation coverage.

Currently:

- `exchange_rate` has a real network executor.
- `xiaohongshu_gui` demonstrates foreground-resource detection and deferred execution; full Xiaohongshu GUI automation is not implemented.
- `wechat_send_file` demonstrates side-effect detection and human approval. Approved actions are queued, but the final WeChat GUI file-sending executor is not implemented.
- Voice recording currently uses a fixed recording window rather than voice activity detection (VAD).
- Accessibility behavior may vary across Android vendors.
- Only a small initial set of tools is registered in the prototype.

These limitations are surfaced explicitly rather than simulated as successful external actions.

---

## How to Run

1. Clone the repository.
2. Open the project in Android Studio.
3. Configure the required API keys in the local `local.properties` file.
4. Build and install the application on an Android device.
5. Grant microphone permission.
6. Grant system overlay permission.
7. Enable the ParallelAgent Accessibility Service.
8. Keep any normal application in the foreground.
9. Double-press Volume+ and speak a request.

API keys are intentionally excluded from version control.

Example local configuration:

```properties
DASHSCOPE_API_KEY=YOUR_KEY
DEEPSEEK_API_KEY=YOUR_KEY
```

Do not commit real API keys to the repository.

---

## Why ParallelAgent?

The goal of ParallelAgent is not simply to make another agent that can click buttons on a phone.

The prototype explores a different systems question:

> How should a human and an AI agent share the same interactive device?

Traditional GUI agents often model the phone as a resource exclusively controlled by the agent during execution.

ParallelAgent instead treats foreground attention as a scarce human-owned resource.

The agent therefore needs to reason not only about **what to do**, but the runtime must also determine **when the action is appropriate to execute**.

This leads to three first-class scheduling outcomes:

```text
EXECUTE
DEFER
ASK_APPROVAL
```

and a graceful capability fallback:

```text
NO_TOOL
```

---

## Design Principle

**A phone agent shouldn't take over your screen. It should know when to act, when to wait, and when to ask.**

**Foreground belongs to you. The rest belongs to your agent.**
