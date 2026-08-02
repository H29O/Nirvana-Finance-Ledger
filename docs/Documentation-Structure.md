# Documentation Structure

This folder contains all design documents for the Finance Ledger
project. The goal is to document important engineering decisions before
and during implementation.

    finance-ledger/
    │
    ├── docs/
    │   ├── 01-Project-Vision.md
    │   ├── 02-Software-Requirements-Specification.md
    │   ├── 03-System-Architecture.md
    │   ├── 04-Domain-Model.md
    │   ├── 05-Database-Design.md
    │   ├── 06-API-Specification.md
    │   ├── 07-Development-Roadmap.md
    │   ├── 08-Interview-Notes.md
    │   ├── diagrams/
    │   │   ├── architecture.drawio
    │   │   ├── er-diagram.drawio
    │   │   ├── class-diagram.drawio
    │   │   └── sequence-diagrams.drawio
    │   └── decisions/
    │       ├── ADR-001-Transaction-Model.md
    │       ├── ADR-002-Debt-System.md
    │       ├── ADR-003-OCR-Flow.md
    │       └── ADR-004-File-Storage.md
    │
    ├── backend/
    ├── frontend/
    └── README.md

------------------------------------------------------------------------

# Purpose of Each Document

## 01 - Project Vision

Defines the product idea, target users, problems solved, and project
goals.

## 02 - Software Requirements Specification (SRS)

Contains: - Functional requirements - Non-functional requirements - Use
cases - Constraints - Future scope

## 03 - System Architecture

Explains the overall architecture, modules, technology stack, component
interactions, authentication flow, file storage, OCR integration, and
deployment strategy.

## 04 - Domain Model

Defines the business concepts: - User - Transaction - Debt - Person -
Category - Attachment - ExtractionResult - MonthlySummary - Insight

No database-specific details should appear here.

## 05 - Database Design

Documents: - ER Diagram - Entity relationships - Constraints - Cascade
rules - Indexes - Migration strategy

## 06 - API Specification

Documents every REST endpoint with: - Request - Response - Validation
rules - Error responses - Authentication requirements

## 07 - Development Roadmap

Tracks implementation progress by milestone and sprint.

## 08 - Interview Notes

Maintains concise explanations for important technical decisions such
as: - Why PostgreSQL? - Why Spring Boot? - Why DTOs? - Why JWT? - Why
Flyway? - Why transactions are the source of truth?

------------------------------------------------------------------------

# Architecture Decision Records (ADRs)

Each ADR captures one important architectural decision.

Recommended format:

-   Context
-   Decision
-   Reasoning
-   Alternatives considered
-   Consequences

Example: ADR-001: Transactions are the single source of truth instead of
storing derived financial state.

------------------------------------------------------------------------

# Documentation Workflow

For every major feature:

1.  Design the feature.
2.  Update the relevant documentation.
3.  Implement the feature.
4.  Test the feature.
5.  Commit the changes.

Documentation should evolve alongside the code and remain the single
source of truth for project architecture.
