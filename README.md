# NUCOSMOS POS

NUCOSMOS POS currently includes:

- `Flutter POS APK` for Android tablets
- `Vue 3` admin web
- `Java 17 Spring Boot` backend API
- `PostgreSQL + Flyway`

## Local development

Admin web:

```bash
npm install
npm run dev
```

- Admin web: `http://localhost:5173`
- API base env example: [/.env.example](/c:/NUCOSMOS_POS/.env.example)

Backend:

```bash
cd backend
./mvnw spring-boot:run
```

PostgreSQL:

```bash
cd backend
docker compose --env-file .env.example up -d
```

Current local baseline:

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8081`
- PostgreSQL: `localhost:5433`

## Verification

Frontend build:

```bash
npm run build
```

Backend tests:

```bash
cd backend
./mvnw test
```

## Docs

- [Admin System Roadmap](/c:/NUCOSMOS_POS/docs/admin-system-roadmap.md)
- [POS PRD](/c:/NUCOSMOS_POS/docs/pos-system-prd.md)
- [Production Deployment](/c:/NUCOSMOS_POS/docs/production-deployment.md)
- [SFTP Deployment Package](/c:/NUCOSMOS_POS/docs/sftp-deployment-package.md)
- [Vue Admin Overview](/c:/NUCOSMOS_POS/docs/vue-admin-web.md)
- [Vue Admin API Spec](/c:/NUCOSMOS_POS/docs/vue-admin-api-spec.md)
- [Vue Admin Page Field Spec](/c:/NUCOSMOS_POS/docs/vue-admin-page-field-spec.md)
- [Vue Admin Frontend Guidelines](/c:/NUCOSMOS_POS/docs/vue-admin-frontend-guidelines.md)
- [Backend Foundation](/c:/NUCOSMOS_POS/docs/backend-foundation.md)
- [PIN + JWT Auth Design](/c:/NUCOSMOS_POS/docs/pin-jwt-auth-design.md)
- [MacBook Handoff](/c:/NUCOSMOS_POS/docs/macbook-handoff.md)
- [Mac Docker Desktop Flow](/c:/NUCOSMOS_POS/docs/mac-docker-desktop-flow.md)
- [Service Restart And Env Validation](/c:/NUCOSMOS_POS/docs/service-restart-and-env-validation.md)
