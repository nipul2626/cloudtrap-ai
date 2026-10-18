# CloudTrap AI

### Explainable Cloud Risk Intelligence & Remediation

```{=html}
<p align="center">
```
`<strong>`{=html}Understand cloud risk. Connect the evidence. Make safer
remediation decisions.`</strong>`{=html}
```{=html}
</p>
```
```{=html}
<p align="center">
```
`<a href="https://github.com/nipul2626/cloudtrap-ai">`{=html}GitHub
Repository`</a>`{=html}
```{=html}
</p>
```

------------------------------------------------------------------------

## Table of Contents

-   [Overview](#overview)
-   [The Problem](#the-problem)
-   [Our Approach](#our-approach)
-   [Current Prototype Status](#current-prototype-status)
-   [Key Capabilities](#key-capabilities)
-   [Architecture](#architecture)
-   [Technology Stack](#technology-stack)
-   [Repository Structure](#repository-structure)
-   [Prerequisites](#prerequisites)
-   [Getting Started](#getting-started)
    -   [1. Clone the Repository](#1-clone-the-repository)
    -   [2. Configure Environment
        Variables](#2-configure-environment-variables)
    -   [3. Start PostgreSQL and Redis](#3-start-postgresql-and-redis)
    -   [4. Run the Backend API](#4-run-the-backend-api)
    -   [5. Run the Frontend](#5-run-the-frontend)
-   [Configuration Reference](#configuration-reference)
-   [Testing and Quality Checks](#testing-and-quality-checks)
-   [API and Health Checks](#api-and-health-checks)
-   [Security and Responsible Use](#security-and-responsible-use)
-   [Roadmap](#roadmap)
-   [Known Limitations](#known-limitations)
-   [Contributing](#contributing)
-   [Team](#team)
-   [License](#license)
-   [Contact](#contact)

------------------------------------------------------------------------

## Overview

**CloudTrap AI** is a cloud-security project focused on making cloud
risks easier to understand, prioritize, and address. Cloud environments
can contain many separate configuration issues---such as exposed
resources, weak access controls, missing audit logging, or missing
encryption. Looking at each alert in isolation can make it difficult to
understand the wider security implications.

CloudTrap AI is being developed to bring configuration evidence and
cloud-resource context together. Its longer-term direction is to help
security teams move from a list of disconnected findings toward
evidence-backed explanations of potential risk paths and more informed
remediation decisions.

The project combines a web application with a Java/Spring Boot API and a
foundation for cloud-resource collection. The current prototype includes
workspace and scan workflows, a demo dataset, scan-scoped findings, and
AWS S3/EC2 collector components.

> **Project status:** This is an actively developed prototype. Some of
> the product capabilities described in the vision and
> roadmap---especially full graph-based attack-path analysis,
> AI-generated explanations, remediation simulation, and live AWS
> integration workflows---are planned or evolving. They should not be
> assumed to be production-ready unless they are explicitly implemented
> and tested in the current code.

## The Problem

Cloud-security teams may face several related challenges:

1.  **Fragmented findings:** Security alerts often describe individual
    misconfigurations without showing how they relate to other
    resources.
2.  **Unclear prioritization:** A long list of findings can make it
    difficult to decide what should be investigated first.
3.  **Insufficient context:** A finding is more useful when analysts can
    inspect the underlying resource and evidence that produced it.
4.  **Remediation uncertainty:** A proposed configuration change should
    be evaluated carefully to understand its likely effect before it is
    applied.
5.  **Alert fatigue:** Teams need understandable, actionable
    information---not only a list of warnings.

CloudTrap AI explores a workflow that connects findings to resource
context, communicates risk in plain language, and ultimately helps teams
evaluate possible fixes.

## Our Approach

The intended product workflow is:

1.  **Collect or import resource data** from a supported source or
    structured dataset.
2.  **Normalize resources** into a consistent internal representation.
3.  **Connect related resources and permissions** to support contextual
    risk analysis.
4.  **Identify and prioritize candidate risks** using deterministic,
    inspectable analysis.
5.  **Explain findings** with references to the evidence that supports
    them.
6.  **Evaluate proposed remediation** against a versioned representation
    of the environment before any real change is made.

The analysis engine---not a language model---should remain responsible
for authoritative graph relationships and risk calculations. AI can help
explain verified evidence, but it must not invent resources,
relationships, or scores.

## Current Prototype Status

The repository has been developed as a working foundation, with the
following capabilities verified during development:

-   User/session authentication flow, including session handling, CSRF
    protection, and logout.
-   Workspace access and workspace-related workflows.
-   Cloud-account CRUD workflows.
-   Asset data handling.
-   Scan queue/worker flow and scan status updates.
-   Findings associated with a scan.
-   AWS S3 and EC2 collector components in the backend.
-   A synthetic demo dataset used to exercise the scan workflow.
-   PostgreSQL persistence, Redis service configuration, and database
    migrations.
-   A Spring Boot health endpoint and a basic automated backend test.

### Example demo result

A previously tested run against the **synthetic demo dataset** reported:

  Metric                Result
  ------------------- --------
  Assets discovered          4
  Assets scanned             4
  Findings created           5
  Critical findings          1
  High findings              4

Example demo findings included unrestricted administrative network
access, disabled audit logging, disabled storage encryption, and
publicly exposed assets.

**Important:** These numbers describe one demo run only. They are not
results from a real customer cloud account, and they should not be
interpreted as evidence of an actual compromise or exploit.

## Key Capabilities

### Available in the current project foundation

-   **Workspace-oriented workflow:** Organize cloud-security activity
    around workspaces.
-   **Cloud-account management:** Backend workflows for creating and
    managing cloud-account records.
-   **Asset collection foundation:** Asset handling and AWS S3/EC2
    collector components.
-   **Scan lifecycle:** Queue/worker processing and scan status updates.
-   **Scan-scoped findings:** Findings are associated with the scan that
    produced them.
-   **Demo workflow:** Exercise the scan and findings flow without
    connecting a production cloud environment.

### Planned or still evolving

-   **Resource relationship graph:** Build a typed graph of resources,
    identities, permissions, network exposure, and sensitive assets.
-   **Candidate attack-path analysis:** Identify possible risk chains
    based on explicit graph relationships and configuration evidence.
-   **Evidence-grounded AI analyst:** Explain findings and recommend
    next steps using retrieved, structured evidence.
-   **Remediation simulation:** Compare a proposed change against a
    versioned copy of the resource graph and show before/after analysis.
-   **Read-only AWS integration:** Provide a documented, least-privilege
    collection path for authorized AWS accounts.
-   **Reporting and export:** Generate shareable summaries for
    investigation and remediation tracking.

The exact availability of these items may change as development
progresses. Consult the current code and project issues before relying
on a roadmap item.

## Architecture

At a high level, CloudTrap AI is organized as a web client, a backend
API, and supporting data services.

``` text
┌─────────────────────────────────────────────┐
│           Web Application                   │
│       Next.js · React · TypeScript          │
└──────────────────────┬──────────────────────┘
                       │ HTTP
                       ▼
┌─────────────────────────────────────────────┐
│              Spring Boot API                │
│ Authentication · Workspaces · Cloud Accounts│
│ Assets · Scan Queue/Worker · Findings       │
│ AWS S3/EC2 Collector Components             │
└───────────────┬─────────────────┬───────────┘
                │                 │
                ▼                 ▼
       ┌────────────────┐  ┌────────────────┐
       │   PostgreSQL   │  │     Redis      │
       │ Persistent data│  │ Supporting data│
       └────────────────┘  └────────────────┘
```

This is a logical overview, not a guarantee that every future
graph-analysis or AI component is already implemented.

### Component responsibilities

-   **Frontend (`apps/web`)**: Next.js application for the user-facing
    experience.
-   **Backend (`apps/api`)**: Spring Boot REST API,
    authentication/session handling, persistence, scan workflows, and
    cloud collector components.
-   **PostgreSQL**: Relational persistence for application data.
-   **Redis**: Supporting in-memory data service configured for local
    development.
-   **AWS SDK components**: Dependencies used by backend
    collector/integration work for AWS services.

### Design principles

-   **Evidence first:** Findings should be traceable to the data that
    supports them.
-   **Deterministic analysis:** Risk calculations and graph
    relationships should be inspectable and testable.
-   **Least privilege:** Cloud access should be limited to the
    permissions required for collection.
-   **Human oversight:** Recommendations should support analysts rather
    than silently make production changes.
-   **Clear uncertainty:** A candidate risk path is an analytical
    possibility, not proof that an attack occurred or will succeed.

## Technology Stack

  -----------------------------------------------------------------------
  Area                                Technology
  ----------------------------------- -----------------------------------
  Frontend framework                  Next.js 16

  UI library                          React 19

  Frontend language                   TypeScript

  Styling                             Tailwind CSS 4

  Backend language                    Java 21

  Backend framework                   Spring Boot 4.1.1

  API layer                           Spring MVC

  Security                            Spring Security, session-based
                                      authentication, CSRF protection

  Persistence                         Spring Data JPA / Hibernate

  Database                            PostgreSQL

  Database migrations                 Flyway

  Supporting data service             Redis

  Cloud SDK                           AWS SDK for Java 2.x (including S3
                                      and EC2 modules)

  Backend tests                       Spring Boot test dependencies and
                                      H2 for tests

  Local infrastructure                Docker Compose

  Build tool                          Maven Wrapper for the backend;
                                      npm-compatible scripts for the
                                      frontend
  -----------------------------------------------------------------------

Version details can change as dependencies are updated. Refer to
`apps/api/pom.xml` and `apps/web/package.json` for the versions declared
in the repository.

## Repository Structure

``` text
cloudtrap-ai/
├── apps/
│   ├── api/                  # Spring Boot backend
│   │   ├── src/
│   │   │   ├── main/         # Application source and resources
│   │   │   └── test/         # Backend tests
│   │   ├── mvnw              # Maven Wrapper (Unix-like systems)
│   │   ├── mvnw.cmd          # Maven Wrapper (Windows)
│   │   └── pom.xml            # Backend dependencies and build config
│   └── web/                  # Next.js frontend
│       ├── app/               # App Router pages/components (if present)
│       ├── public/            # Static assets (if present)
│       └── package.json       # Frontend scripts and dependencies
├── docs/                      # Project documentation
├── docker-compose.yml         # Local PostgreSQL and Redis services
├── .env.example               # Environment-variable template
├── .gitignore
└── README.md
```

The tree above highlights the main project areas; individual source
folders may evolve as the project develops.

## Prerequisites

Install the following before running the project locally:

-   **Git**
-   **Java Development Kit (JDK) 21**
-   **Docker Desktop with Docker Compose** (recommended for local
    PostgreSQL and Redis)
-   **Node.js** compatible with the installed Next.js version
-   **npm** (bundled with Node.js)
-   A terminal such as PowerShell, Command Prompt, Bash, or Zsh

Check your tools:

``` bash
git --version
java -version
node --version
npm --version
docker --version
docker compose version
```

The backend includes Maven Wrapper scripts, so a separate Maven
installation is normally unnecessary.

## Getting Started

The commands below describe a local development setup. Run the frontend
and backend in separate terminals.

### 1. Clone the Repository

``` bash
git clone https://github.com/nipul2626/cloudtrap-ai.git
cd cloudtrap-ai
```

### 2. Configure Environment Variables

Create a local `.env` file from the template.

**PowerShell:**

``` powershell
Copy-Item .env.example .env
```

**Bash / macOS / Linux:**

``` bash
cp .env.example .env
```

Open `.env` and fill in local development values. For example, if you
want PostgreSQL available on host port `5433`:

``` dotenv
# Local development only — use your own values.
POSTGRES_DB=cloudtrap
POSTGRES_USER=cloudtrap
POSTGRES_PASSWORD=change-this-local-password
POSTGRES_PORT=5433

REDIS_PORT=6379

# Spring Boot API connection (API runs on your host)
DB_URL=jdbc:postgresql://localhost:5433/cloudtrap
DB_USERNAME=cloudtrap
DB_PASSWORD=change-this-local-password
```

Use the **same database name, username, and password** in the PostgreSQL
container and backend connection settings. If you choose a different
host port, update `POSTGRES_PORT` and the port in `DB_URL` to match.

These are examples for local development, not production credentials.
Never commit `.env` or real secrets to Git.

### 3. Start PostgreSQL and Redis

From the repository root:

``` bash
docker compose up -d postgres redis
```

Check service status:

``` bash
docker compose ps
```

Check PostgreSQL readiness:

``` bash
docker compose exec postgres pg_isready -U cloudtrap -d cloudtrap
```

Check Redis:

``` bash
docker compose exec redis redis-cli ping
```

Redis should respond with:

``` text
PONG
```

If you used different database credentials, replace `cloudtrap` in the
PostgreSQL command with the value from your `.env`.

To view service logs:

``` bash
docker compose logs -f postgres redis
```

To stop the services without deleting stored data:

``` bash
docker compose down
```

To stop services **and permanently remove their named local
volumes/data**, use the following only if you are certain you no longer
need the data:

``` bash
docker compose down -v
```

### 4. Run the Backend API

Open a terminal and move into the API directory:

``` bash
cd apps/api
```

**Windows PowerShell:**

``` powershell
.\mvnw.cmd spring-boot:run
```

**macOS / Linux:**

``` bash
./mvnw spring-boot:run
```

If the wrapper is not executable on macOS/Linux, run:

``` bash
chmod +x mvnw
./mvnw spring-boot:run
```

The API has been run locally on port `8080` during development. If the
application does not start, inspect the startup logs for database
connection, migration, Redis, and environment-configuration errors.

#### Run backend tests

From `apps/api`:

**Windows PowerShell:**

``` powershell
.\mvnw.cmd test
```

**macOS / Linux:**

``` bash
./mvnw test
```

### 5. Run the Frontend

Open another terminal from the repository root:

``` bash
cd apps/web
npm install
npm run dev
```

Open the local address printed by Next.js, typically:

-   Frontend: `http://localhost:3000`
-   Backend health endpoint: `http://localhost:8080/actuator/health` (if
    enabled by the current application configuration)

If the frontend cannot communicate with the backend, inspect the current
frontend API configuration and backend CORS/security settings. Do not
assume that a particular API base-URL environment variable exists unless
it is defined in the current source.

#### Frontend scripts

Run these commands from `apps/web`:

``` bash
npm run dev
npm run lint
npm run build
npm run start
```

-   `dev` starts the local development server.
-   `lint` runs ESLint.
-   `build` creates a production build.
-   `start` serves the production build after a successful build.

## Configuration Reference

The root `.env.example` currently defines the following variables:

  Variable              Purpose
  --------------------- ---------------------------------------------------
  `POSTGRES_DB`         Database name created by the PostgreSQL container
  `POSTGRES_USER`       Local PostgreSQL user
  `POSTGRES_PASSWORD`   Local PostgreSQL password
  `POSTGRES_PORT`       Host port mapped to PostgreSQL's container port
  `REDIS_PORT`          Host port mapped to Redis
  `DB_URL`              JDBC connection URL used by the backend
  `DB_USERNAME`         Backend database username
  `DB_PASSWORD`         Backend database password

The Compose file binds PostgreSQL and Redis to `127.0.0.1`, limiting
their published ports to the local machine. Preserve that local-only
binding for development. Production deployments require a separate,
security-reviewed configuration.

## Testing and Quality Checks

Before opening a pull request, run the relevant checks.

### Backend

From `apps/api`:

``` bash
# Windows
.\mvnw.cmd test

# macOS / Linux
./mvnw test
```

The backend test setup includes H2 for test-time database use. Test
coverage is evolving; a passing test suite does not imply that every
integration or security scenario has been tested.

### Frontend

From `apps/web`:

``` bash
npm run lint
npm run build
```

### Manual smoke-test checklist

-   [ ] PostgreSQL and Redis start successfully.
-   [ ] Backend starts without migration or connection errors.
-   [ ] Health endpoint returns a healthy response, where configured.
-   [ ] Authentication and logout work as expected.
-   [ ] Workspace access works.
-   [ ] A demo scan can be initiated and its status can be observed.
-   [ ] Findings are associated with the correct scan.
-   [ ] Frontend lint and production build pass.

## API and Health Checks

The backend uses Spring Boot and Spring MVC. The precise route list and
request/response schemas should be treated as source-of-truth in the
current API implementation; endpoint availability may evolve during
development.

If Spring Boot Actuator health is enabled, test it locally with:

``` bash
curl http://localhost:8080/actuator/health
```

A healthy response generally includes an `UP` status. If the endpoint is
protected or configured under a different path, follow the security and
management configuration in `apps/api`.

For API documentation, inspect the controllers, request DTOs, security
configuration, and application properties under `apps/api/src/main`.

## Security and Responsible Use

CloudTrap AI is a security-analysis prototype. Use it responsibly and
only with systems and cloud accounts for which you have explicit
authorization.

-   **Do not commit secrets.** Keep `.env`, access keys, tokens,
    passwords, and private configuration out of version control.
-   **Use least privilege.** Any future cloud integration should use
    narrowly scoped, read-only permissions wherever possible.
-   **Do not test against unauthorized infrastructure.** Only scan
    assets you own or have permission to assess.
-   **Treat demo data as synthetic.** Demo findings are illustrative and
    do not establish that a real environment is vulnerable.
-   **Review recommendations.** Validate findings and remediation
    suggestions before acting on them.
-   **Do not equate a candidate path with a successful exploit.** A
    graph-derived path indicates a possible relationship under stated
    assumptions, not proof of exploitability, access, or compromise.
-   **Keep humans in control.** Production changes should require
    explicit authorization and review; the prototype should not be
    treated as an autonomous remediation system.
-   **Secure deployment separately.** Local Compose settings and
    development credentials are not production-ready defaults.

If you discover a security issue in the project, avoid posting sensitive
credentials, exploitable details, or private infrastructure data in a
public issue.

## Roadmap

The following items describe the intended direction and are not a
promise that every item is already implemented.

### Near term

-   Improve frontend and backend integration and error states.
-   Expand automated tests for authentication, scan lifecycle, and
    findings.
-   Document API routes and example request/response payloads.
-   Improve demo data and repeatable end-to-end smoke tests.

### Analysis and explainability

-   Define a typed cloud-resource graph and supported relationship
    schema.
-   Add deterministic candidate attack-path analysis.
-   Link findings and path steps to the evidence used in analysis.
-   Prioritize risk using transparent, testable scoring rules.
-   Add AI-assisted explanations grounded in retrieved evidence, with
    clear uncertainty.

### Remediation workflow

-   Model remediation proposals as explicit changes.
-   Run proposed changes against a versioned graph or snapshot.
-   Compare the resulting risk view before and after the proposed
    change.
-   Require human review before any real-world action.

### Cloud integrations and reporting

-   Strengthen authorized, read-only AWS collection.
-   Improve integration testing with isolated test accounts or fixtures.
-   Add useful reporting and export workflows.
-   Add deployment, operational monitoring, and security hardening
    documentation.

## Known Limitations

-   The application is under active development; interfaces and data
    models may change.
-   The demo dataset is synthetic and should not be used to make claims
    about a real cloud environment.
-   Full resource-graph analysis, attack-path analysis, AI explanations,
    and remediation simulation should be considered planned/evolving
    unless verified in the current implementation.
-   AWS SDK dependencies and collector components do not, by themselves,
    mean a complete production-ready AWS integration is available.
-   The repository's current tests are a starting point and do not
    establish comprehensive security or integration coverage.
-   Production deployment requires separate configuration, threat
    modeling, secret management, observability, and security review.

## Contributing

Contributions, issue reports, and suggestions are welcome.

1.  Fork the repository.
2.  Create a focused branch for your change.
3.  Keep changes small and describe the problem they solve.
4.  Run backend tests and frontend lint/build checks where relevant.
5.  Update documentation when behavior, configuration, or API contracts
    change.
6.  Open a pull request with a clear summary and test evidence.

Please do not include credentials, private cloud-account data, or other
sensitive information in commits, logs, screenshots, or issue reports.

## Team

**Nipul Rathod**\
3rd Year B.Tech CSE\
G H Raisoni International Skill Tech University, Pune

**Diksha Shah**\
3rd Year B.Tech CSE\
G H Raisoni International Skill Tech University, Pune

## License

No license has been specified in this repository README. Unless a
license file is added, do not assume that the project is available for
unrestricted reuse, redistribution, or modification.

If you intend to publish the project as open source, add an appropriate
`LICENSE` file and update this section accordingly.

## Contact

-   **Repository:** https://github.com/nipul2626/cloudtrap-ai
-   **Issues and feature requests:**
    https://github.com/nipul2626/cloudtrap-ai/issues

For bug reports, include reproducible steps, expected behavior, actual
behavior, and relevant sanitized logs. Never attach secrets or sensitive
cloud data.
