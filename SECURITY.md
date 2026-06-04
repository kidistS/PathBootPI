# PathBoot PI Security Guide

## Overview
PathBoot PI uses stateless API-key authentication and role-based authorization to protect all non-public endpoints.

## Authentication
- Requests to protected endpoints must include `X-API-Key`.
- API keys are compared in a timing-safe way.
- The login endpoint (`/api/v1/auth/login`) is public and returns an API key for valid credentials.

## Authorization
- Authenticated requests are assigned `ROLE_API_USER`.
- Public paths remain limited to:
  - `/api/v1/auth/**`
  - `/swagger-ui/**`
  - `/api-docs/**`
  - `/v3/api-docs/**`
  - `/actuator/health`
  - `/actuator/info`
  - `/error`

## Profile Rules

### local
- API key enforcement: enabled
- Database: SQLite
- CORS: permissive for development
- Swagger: enabled

### test
- API key enforcement: enabled
- Database: PostgreSQL
- CORS: localhost-only
- Swagger: enabled

### prod
- API key enforcement: enabled
- Database: PostgreSQL
- CORS: restricted to `FRONTEND_URL`
- Swagger/OpenAPI: disabled
- Actuator: minimal exposure only

## Secrets
Production secrets must come from environment variables or a secret manager.
Never commit real values into the repository.

Required production variables include:
- `FRONTEND_URL`
- `PATHBOOT_API_KEY_1`
- `PATHBOOT_API_KEY_2`
- `PATHBOOT_ADMIN_PASSWORD`
- `PATHBOOT_USER_PASSWORD`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

## Vulnerability Controls
- Dependency scanning is enabled via Maven OWASP dependency-check.
- Production uses `ddl-auto: validate` to avoid accidental schema mutation.
- Logging is reduced in production to avoid information leakage.

## Notes
- HTTPS should be terminated at a reverse proxy or load balancer in production.
- Keep secrets out of Dockerfiles and source control.

