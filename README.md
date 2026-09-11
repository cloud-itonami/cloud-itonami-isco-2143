# cloud-itonami-isco-2143

ISCO-08 unit group 2143: Environmental Engineers

An itonami actor — an autonomous LLM/advisor behind an independent Governor,
langgraph-clj StateGraph, and append-only audit ledger — that drafts and
prepares environmental engineering analysis material for licensed engineer
review and sign-off.

## Scope

This actor assists with:
- **`:draft-remediation-plan`** — remediation/pollution-control plan drafts
- **`:log-site-data`** — site assessment data logging and organization
- **`:flag-regulatory-risk`** — surface regulatory compliance risks (always escalates to human)
- **`:request-client-review`** — propose scheduling client/regulator review sessions

## Hard Scope Exclusions

This actor NEVER:
- Issues a final certified engineering design (exclusive to licensed environmental engineer)
- Certifies compliance or regulatory sign-off (exclusive to licensed environmental engineer)
- Executes actuation directly (all proposals are `:propose` only; commitment gated by Governor)

Any proposal attempting certification triggers a permanent hard block (`:hard? true`, `:hold`).

## Governor Rules

The `EnvengGovernor` is an independent system that gates all proposals:

### Hard Violations (permanent block, `:hold`)
1. **Project provenance**: request's project must be registered
2. **No actuation**: effect must be `:propose` (never `:direct-write` or similar)
3. **No certification**: any attempt to issue certified design or certify compliance

### Escalation Rules (always human sign-off, `:request-approval`)
1. **`:flag-regulatory-risk` operation**: always escalates
2. **Hazardous material handling**: proposals with tags like `:hazardous`, `:contaminated`, `:heavy-metals`, `:pcb`, `:asbestos`, `:petroleum`, `:pah`
3. **Low confidence**: < 0.6 on a 0.0–1.0 scale

### Clean Flow
Proposals that pass all checks proceed to `:commit` without interruption.

## Architecture

```
:intake → :advise → :govern → :decide ─┬─→ :commit           (ok)
                                        ├─→ :request-approval  (escalate)
                                        └─→ :hold              (hard)
```

- **`:advise`**: `Advisor` protocol generates a proposal. Default is `mock-advisor`; swap for `llm-advisor` wrapping a `langchain.model/ChatModel`.
- **`:govern`**: `Governor/check` evaluates the proposal against hard/escalation rules.
- **`:decide`**: Routes on the verdict.
- **`:commit`** (or resume after approval): Writes record to store, appends ledger.
- **`:hold`**: Logs a hard block; no write.

## Store Protocol

`Store` is injected into the actor graph:
- `(project store project-id)` — lookup registered project
- `(records-of store project-id)` — all engineering records under a project
- `(ledger store)` — append-only audit trail
- `(register-project! store project)` — add a project
- `(commit-record! store record)` — write an engineering record (gated by Governor)
- `(append-ledger! store fact)` — audit log entry

Default: `MemStore` (deterministic, zero-dep). Swap for Datomic/kotoba-server
backend without touching actor or governor.

## Tests

```bash
kbb -M:test
```

- `governor_test.clj`: hard violations, escalation rules, store ops
- `actor_test.clj`: graph flow, interrupt/resume, ledger behavior

## Dependencies

- `io.github.kotoba-lang/langgraph` — StateGraph, checkpoint protocol
- `io.github.cognitect-labs/test-runner` — test runner (`:test` alias)

## License

AGPL-3.0-or-later
