# CloudTrap AI - API Design

**Status:** Proposed MVP contract
**Base path:** `/api/v1`
**Format:** JSON over HTTP
**Time format:** ISO 8601 UTC timestamps
**Pagination:** Zero-based pages
**Contract ownership:** Shared; scan and finding APIs led by Nipul, authentication, assets, graph and AI APIs led by Diksha.

These contracts define intended behavior. Only `GET /api/v1/health` currently exists; the remaining endpoints must be implemented and tested.

## 1. API conventions

- Use JSON request and response bodies unless an endpoint specifies no body.
- Use camelCase for JSON property names.
- Serialize identifiers as strings. Use UUIDs for internal identifiers unless the team explicitly agrees otherwise.
- Use ISO 8601 UTC timestamps, for example `2026-10-14T15:00:00Z`.
- Validate inputs on the server.
- Enforce authentication and authorization on the server for protected endpoints.
- Never return passwords, password hashes, session secrets, cloud credentials, stack traces or internal database details.
- Calculate risk scores on the backend; the frontend must not independently recalculate them.
- Keep deterministic rule findings distinguishable from AI-suggested findings.
- Apply bounded pagination to collection endpoints.

## 2. Endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/health` | Public health check; already implemented |
| POST | `/api/v1/auth/login` | Authenticate a user |
| POST | `/api/v1/auth/logout` | End the authenticated session |
| GET | `/api/v1/auth/me` | Retrieve the current user's safe profile |
| GET | `/api/v1/assets` | List and filter assets |
| GET | `/api/v1/assets/{assetId}` | Retrieve an asset and relevant relationships |
| POST | `/api/v1/scans` | Start a scan against an allowed demo dataset |
| GET | `/api/v1/scans/{scanId}` | Retrieve scan status and summary |
| GET | `/api/v1/findings` | List, filter and paginate findings |
| GET | `/api/v1/findings/{findingId}` | Retrieve complete finding details |
| PATCH | `/api/v1/findings/{findingId}/status` | Change finding workflow status |
| GET | `/api/v1/graph/attack-paths` | Retrieve bounded graph/path candidates |
| POST | `/api/v1/ai/findings/{findingId}/explanation` | Request a validated AI explanation |

## 3. Shared enums

### ScanStatus

- `QUEUED`
- `RUNNING`
- `COMPLETED`
- `FAILED`

### FindingSeverity

- `CRITICAL`
- `HIGH`
- `MEDIUM`
- `LOW`
- `INFO`

### FindingStatus

- `OPEN`
- `ACKNOWLEDGED`
- `RESOLVED`
- `ACCEPTED_RISK`

### FindingSourceType

- `DETERMINISTIC_RULE`
- `AI_SUGGESTED`

### AssetExposure

- `PUBLIC`
- `PRIVATE`
- `UNKNOWN`

### ScanSource

- `DEMO`
- `AWS`

The AWS source is reserved for the later read-only integration milestone. Do not imply that AWS ingestion is already implemented.

## 4. Scan API

### POST `/api/v1/scans`

Starts a scan using an allowed synthetic dataset.

Example request:

    {
      "datasetId": "default-demo"
    }

`datasetId` is required. The server must validate it against configured datasets; clients must not supply arbitrary filesystem paths.

Return `202 Accepted` when the scan has been accepted for processing.

Example response:

    {
      "scanId": "scn-example-001",
      "status": "QUEUED",
      "source": "DEMO",
      "datasetId": "default-demo",
      "timestamps": {
        "createdAt": "2026-10-14T15:00:00Z",
        "startedAt": null,
        "completedAt": null
      },
      "summary": {
        "assetsScanned": 0,
        "findingsCreated": 0,
        "findingsBySeverity": {
          "CRITICAL": 0,
          "HIGH": 0,
          "MEDIUM": 0,
          "LOW": 0,
          "INFO": 0
        }
      },
      "error": null
    }

The example identifier and timestamp are illustrative. Counts must reflect actual processing.

### GET `/api/v1/scans/{scanId}`

Returns the same scan resource with current status, timestamps and counts.

A failed scan uses `status: "FAILED"` and may return a safe error object:

    {
      "code": "SCAN_DATA_INVALID",
      "message": "The selected dataset could not be processed."
    }

Never expose internal exception messages or stack traces. A scan must not be reported as `COMPLETED` until its results have been successfully persisted.

## 5. Finding API

### GET `/api/v1/findings`

Supported query parameters:

- `page`: zero-based page index; default `0`.
- `size`: page size; default `20`, maximum `100`.
- `sort`: allowlisted property and direction; default `lastDetectedAt,desc`.
- `severity`: one or more supported severity values.
- `status`: one or more supported finding statuses.
- `assetId`: filter by affected asset.
- `scanId`: filter by scan.
- `sourceType`: filter by finding source.
- `search`: optional bounded search over supported text fields.

Invalid enum values, invalid pagination values and unsupported sort properties must return a validation error. Do not concatenate user-supplied sort expressions into SQL.

Example page response:

    {
      "items": [],
      "page": {
        "number": 0,
        "size": 20,
        "totalElements": 0,
        "totalPages": 0,
        "hasNext": false,
        "hasPrevious": false
      }
    }

The `items` array contains compact finding representations. The detail endpoint returns complete evidence, risk breakdown and remediation.

### GET `/api/v1/findings/{findingId}`

Returns the complete finding representation defined in the finding contract. Return `404 Not Found` when the resource does not exist or is not accessible under the chosen authorization policy.

### PATCH `/api/v1/findings/{findingId}/status`

Request:

    {
      "status": "ACKNOWLEDGED"
    }

Return `200 OK` with the updated finding ID, status and `updatedAt`.

The server must validate allowed state transitions and user permissions. Marking a finding `RESOLVED` updates CloudTrap's workflow state; it does not automatically remediate the cloud resource.

## 6. Canonical finding representation

The full finding response contains:

- `findingId`, `scanId`, `ruleId` and `ruleVersion`
- `title`, `description`, `category`, `severity` and `status`
- `source`: provenance and source type
- `asset`: affected asset reference and safe summary
- `evidence`: structured observations supporting the finding
- `risk`: score, severity, factors, formula version and calculation timestamp
- `remediation`: summary, steps, priority and human-review requirement
- `timestamps`: first detection, last detection, creation, update and optional resolution time

Example:

    {
      "findingId": "fnd-example-001",
      "scanId": "scn-example-001",
      "ruleId": "STORAGE_PUBLIC_ACCESS",
      "ruleVersion": "1.0.0",
      "title": "Storage resource is publicly accessible",
      "description": "The normalized configuration indicates public access.",
      "category": "MISCONFIGURATION",
      "severity": "CRITICAL",
      "status": "OPEN",
      "source": {
        "type": "DETERMINISTIC_RULE",
        "ruleId": "STORAGE_PUBLIC_ACCESS",
        "ruleVersion": "1.0.0"
      },
      "asset": {
        "assetId": "ast-example-001",
        "name": "customer-data-bucket",
        "provider": "AWS",
        "type": "STORAGE_BUCKET",
        "region": "ap-south-1",
        "exposure": "PUBLIC"
      },
      "evidence": [
        {
          "evidenceId": "ev-example-001",
          "key": "publicAccess",
          "label": "Public access enabled",
          "observedValue": true,
          "expectedValue": false,
          "description": "The normalized asset configuration indicates public access.",
          "source": "SYNTHETIC_ASSET_DATA"
        }
      ],
      "risk": {
        "score": 92,
        "severity": "CRITICAL",
        "factors": [
          {
            "code": "PUBLIC_EXPOSURE",
            "label": "Public exposure",
            "contribution": 35,
            "description": "The resource is publicly accessible."
          }
        ],
        "formulaVersion": "1.0.0",
        "calculatedAt": "2026-10-14T15:00:00Z"
      },
      "remediation": {
        "summary": "Restrict unintended public access.",
        "steps": [
          "Disable unintended public access.",
          "Review the resource access policy.",
          "Verify that only authorized principals can access the resource."
        ],
        "priority": "IMMEDIATE",
        "requiresHumanReview": true
      },
      "timestamps": {
        "firstDetectedAt": "2026-10-14T15:00:00Z",
        "lastDetectedAt": "2026-10-14T15:00:00Z",
        "createdAt": "2026-10-14T15:00:00Z",
        "updatedAt": "2026-10-14T15:00:00Z",
        "resolvedAt": null
      }
    }

All example IDs, scores and timestamps are illustrative. Evidence, severity, risk and remediation must be grounded in actual analysis results.

For list responses, evidence and remediation steps may be omitted to keep payloads compact. Keep field names and types consistent wherever a field is present.

Risk scores are integers from 0 to 100. `factors` must explain the actual scoring algorithm; do not assume their contributions sum to the final score unless the implemented formula guarantees it. The score and formula version are produced by the backend.

For AI-suggested findings, `source.type` is `AI_SUGGESTED`. Do not represent an unverified AI suggestion as a confirmed deterministic rule finding.

## 7. Asset API

### GET `/api/v1/assets`

Supported filters:

- `page`, `size`, `sort`
- `provider`
- `type`
- `region`
- `exposure`
- `search`

Return a paginated response using the shared page envelope. Each item contains a stable `assetId`, provider, external ID where appropriate, type, name, region, exposure, tags, last-seen timestamp and safe normalized attributes.

### GET `/api/v1/assets/{assetId}`

Return the asset's normalized metadata, relevant relationships and linked findings where useful. Never return cloud credentials or secret-bearing raw provider data.

## 8. Authentication API

### POST `/api/v1/auth/login`

Accept validated credentials and return safe user/session metadata. The team must explicitly select cookie/session or token-based authentication before implementation.

For cookie-based sessions, use appropriate secure cookie flags and CSRF protection. Never return a password or password hash.

### POST `/api/v1/auth/logout`

Return `204 No Content` after ending the session. Repeated logout requests should be handled safely.

### GET `/api/v1/auth/me`

Return the authenticated user's safe profile, identifier and role. Return `401 Unauthorized` if unauthenticated.

Registration is not part of the initial endpoint contract; add it only if the team agrees it is needed.

## 9. Attack-path API

### GET `/api/v1/graph/attack-paths`

Return bounded nodes, edges and plausible paths built from normalized asset relationships and findings.

The response should identify node and edge IDs, source and target references, relationship types, relevant risk context and path rationale. Include enough metadata for the frontend to display a selected path and explain why it was identified.

Paths are analytical candidates, not proof of exploitability. The API must handle empty graphs, cycles and configured size limits.

The exact graph DTO is to be documented before its implementation.

## 10. AI explanation API

### POST `/api/v1/ai/findings/{findingId}/explanation`

The server loads finding context using the ID; clients must not be trusted to provide authoritative evidence or risk scores.

Return a structured explanation containing a summary, potential impact, limitations, remediation suggestions and an explicit AI-generated label. Validate output fields, lengths and referenced evidence.

Provider failures and timeouts must not invalidate deterministic findings or scan results. AI output must never be executed as a command or automatically applied to cloud resources.

The exact AI DTO is to be documented before its implementation.

## 11. Standard error response

Use one error format for API failures:

    {
      "timestamp": "2026-10-14T15:05:00Z",
      "status": 400,
      "code": "VALIDATION_ERROR",
      "message": "The request contains invalid fields.",
      "path": "/api/v1/scans",
      "requestId": "req-example-001",
      "fieldErrors": [
        {
          "field": "datasetId",
          "code": "NotBlank",
          "message": "Dataset ID is required."
        }
      ]
    }

`fieldErrors` may be omitted when not applicable. Do not expose stack traces, SQL details, secrets or raw internal/provider errors.

Expected status conventions:

- `400`: malformed request or invalid parameters
- `401`: missing or invalid authentication
- `403`: insufficient permissions
- `404`: resource not found or not accessible
- `409`: conflicting operation or invalid state transition
- `429`: rate limit exceeded
- `500`: unexpected internal error

## 12. Contract testing and acceptance

Before merging a feature:

1. Test request validation and response serialization.
2. Verify pagination and filtering, including empty results.
3. Verify authorization and safe error responses.
4. Confirm timestamps and enum values match the documented contract.
5. Ensure risk is calculated by the backend and remains reproducible for identical inputs and formula versions.
6. Ensure AI provenance is explicit and AI failure does not break deterministic analysis.
7. Update this document, DTOs, frontend types and tests together when a contract changes.

The primary MVP acceptance workflow is: authenticate â†’ view synthetic assets â†’ start a demo scan â†’ persist findings â†’ view findings and risk breakdown â†’ inspect a plausible attack path â†’ request an optional AI explanation.
