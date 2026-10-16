# CloudTrap AI — Scanning Engine Architecture

**Status:** Implementation plan for the first deterministic scanning release  
**Runtime:** Java 21, Spring Boot 4.1.1, PostgreSQL 17, Redis 7  
**Execution model:** Durable database-backed worker; Redis is not the source of truth  
**Initial source:** Explicitly configured synthetic datasets  
**Real cloud providers:** Read-only adapters in a later milestone

## Goals

The scanner must be reproducible, tenant-isolated, auditable, resilient to process restarts, safe with untrusted inventory data, and extensible without making provider-specific assumptions part of the rule engine.

## Pipeline

1. **Authorize and accept** — resolve the authenticated user and workspace membership on the server; verify the workspace and cloud account are active; validate the configured dataset ID; accept an idempotency key.
2. **Persist a scan job** — insert a QUEUED scan with immutable source/dataset/rule-set metadata. Return 202 only after commit.
3. **Claim durably** — workers claim QUEUED jobs (and expired RUNNING leases) with PostgreSQL row locks using FOR UPDATE SKIP LOCKED. A process restart must not lose accepted work.
4. **Load bounded input** — load only server-configured dataset resources, enforce byte/asset/depth/string limits, validate schema and reject unknown or malformed resource types safely. Never accept filesystem paths from clients.
5. **Normalize inventory** — map provider records to a provider-neutral asset model with stable external identity, normalized attributes, exposure and explicit relationships. Keep raw provider payloads out of public API responses and avoid persisting secrets.
6. **Evaluate versioned rules** — execute registered deterministic rules against normalized assets and relationships. Rules return structured evidence; they do not perform network calls, mutate assets, or call an LLM.
7. **Calculate risk** — use a documented, versioned formula. Store score, factors and formula version with the finding. Do not use random or time-dependent inputs.
8. **Persist atomically** — upsert inventory, create scan-asset membership, findings, evidence, risk assessments and remediation in a transaction. Mark COMPLETED only after all results commit.
9. **Reconcile and audit** — persist safe failure codes, durations, counters, retry attempts, request correlation, and important state transitions. Never persist stack traces or secrets in API-visible fields.
10. **Serve bounded APIs** — enforce workspace scope in every query, allowlisted sorting, bounded pagination, stable ordering, and consistent error contracts.

## Reliability model

- PostgreSQL is the source of truth for jobs and results. Redis is reserved for rate limiting/cache, not job durability.
- A scan is idempotent by workspace ID and idempotency key when the caller supplies a key. Reusing a key with a different normalized request is a conflict.
- Workers claim jobs transactionally, use leases and heartbeats, and increment a bounded attempt counter. A crashed worker's lease expires and the scan can be reclaimed.
- A terminal failure has a stable public error code and safe message. Detailed diagnostics belong in structured server logs correlated by request ID and scan ID.
- Do not blindly retry invalid input. Retry only transient infrastructure failures; cap attempts and transition to FAILED after exhaustion.
- Duplicate rule results are prevented by database uniqueness constraints.
- The scan completion transaction must include result persistence and the COMPLETED transition. Partial result persistence must roll back.
- A periodic reconciler detects stale leases and impossible states; metrics track queue age, processing duration, failure rate, and retry count.

## Security boundaries

- Every scan, asset, finding and related record belongs to exactly one workspace.
- The authenticated principal, not a client-provided user ID, determines actor identity.
- Client input cannot choose an arbitrary local path, URL, cloud role, executable, SQL fragment, rule class, or worker command.
- Dataset IDs, providers, rule IDs, sort fields, filters and state transitions are allowlisted.
- Synthetic fixtures contain no real credentials. Cloud credentials are not part of this milestone.
- API responses contain normalized attributes and evidence only; never return raw credential-bearing payloads.
- Limit dataset bytes, asset count, relationship count, nesting depth, evidence size, page size, and execution time.
- Rule evaluation is pure and deterministic. AI explanations are optional post-processing and cannot alter rule outcomes or risk.
- Findings are workflow records; marking one resolved does not remediate a cloud resource.

## Domain invariants

- Scan states: QUEUED, RUNNING, COMPLETED, FAILED.
- Only QUEUED may normally be claimed; expired RUNNING jobs may be reclaimed under lease rules.
- COMPLETED requires persisted result counts and a completion timestamp.
- FAILED requires a safe error code and completion timestamp.
- A finding has deterministic provenance: rule ID and version, source type, affected asset, scan, structured evidence, severity, and remediation.
- Risk scores are integers from 0 through 100. Severity mapping is versioned and documented.
- Assets have a stable identity scoped by workspace/provider/external ID. Identity changes must not silently create duplicate assets.
- Scan history and findings are retained; user workflows do not hard-delete historical scan evidence.

## Initial risk formula

Use a deterministic base score per rule severity, then add bounded contextual modifiers. Clamp the result to [0,100]. Persist every factor and formulaVersion so a score can be explained and recalculated reproducibly. A rule's severity and risk base are versioned inputs. Exposure and asset criticality are explicit normalized fields, not inferred from resource names.

The initial scoring formula must be implemented as a pure Java component with unit tests for boundaries, monotonicity, and repeatability. Do not claim CVSS compatibility unless the full CVSS specification is implemented.

## Rollout milestones

### Milestone A — persistence and contracts
- Additive Flyway migration for assets, relationships, scans, scan assets, rule metadata, findings, risk assessments and remediation.
- Domain enums/DTOs and workspace-scoped repositories.
- No provider credentials and no production cloud mutations.

### Milestone B — deterministic demo scanner
- Versioned, allowlisted demo dataset registry.
- Input validation and normalization.
- Small initial rule pack covering public exposure, overly permissive network access, encryption disabled, and missing audit logging where the normalized dataset supports those facts.
- Evidence, remediation and risk score persisted with every finding.

### Milestone C — durable worker and API
- Idempotent scan creation, DB-backed claim/lease, retries, status and summary endpoints.
- Transactional completion and safe failure handling.
- Authorization, pagination, integration tests, and observability.

### Milestone D — provider adapters
- Read-only least-privilege cloud collection, pagination, rate limits, cancellation, credential isolation and provider API error handling.
- Provider-specific API contracts and security review before enabling any live account.
