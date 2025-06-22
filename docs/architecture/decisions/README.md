# Architectural Decision Records (ADRs)

This directory contains the architectural decision records for the Weather Service project.

## ADR Index

| ADR | Title | Status | Date |
|-----|-------|--------|------|
| [0000](0000-adr-template.md) | ADR Template | Template | - |
| [0001](0001-reactive-architecture.md) | Adopt Reactive Architecture with Spring WebFlux | Accepted | 2024-01-15 |
| [0002](0002-r2dbc-database-access.md) | Use R2DBC for Reactive Database Access | Accepted | 2024-01-15 |
| [0003](0003-java-21-adoption.md) | Adopt Java 21 Language Features | Accepted | 2024-01-15 |
| [0004](0004-resilience-patterns.md) | Implement Resilience4j for Fault Tolerance | Accepted | 2024-01-15 |
| [0005](0005-api-design-principles.md) | RESTful API Design with OpenAPI Specification | Accepted | 2024-01-15 |

## About ADRs

Architectural Decision Records (ADRs) are short text documents that capture important architectural decisions made during the project lifecycle. Each ADR describes:

- **Context**: The situation that prompted the decision
- **Decision**: The architectural direction chosen
- **Consequences**: The impact of the decision

## ADR Lifecycle

1. **Proposed**: Initial draft under review
2. **Accepted**: Decision approved and implemented
3. **Rejected**: Decision rejected, with reasoning
4. **Deprecated**: Decision no longer relevant
5. **Superseded**: Decision replaced by newer ADR

## Creating a New ADR

1. Copy the [template](0000-adr-template.md)
2. Use the next sequential number (e.g., 0006-new-decision.md)
3. Fill in all sections thoroughly
4. Submit for team review
5. Update this README index

## References

- [ADR Documentation](https://adr.github.io/)
- [Documenting Architecture Decisions](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions)