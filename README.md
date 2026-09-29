# Seoul Satellite Archive

| 구성 | 스택 | 기본 포트 |
| --- | --- | --- |
| `frontend/` | Next.js (TypeScript, Tailwind) | 3000 |
| `backend/` | Spring Boot 4 (Java 21, Gradle) | 8080 |
| postgis | PostgreSQL 18 + PostGIS 3.6 | 5432 |

## 실행

```bash
cp .env.example .env
docker compose up -d --build
```

- 프론트엔드: http://localhost:3000
- 백엔드: http://localhost:8080 (Swagger: `/swagger-ui.html`, 헬스체크: `/health`)
- 종료: `docker compose down` (DB 데이터 삭제: `-v`)

## 로컬 개발

DB만 compose로 띄우고 나머지는 직접 실행한다.

```bash
docker compose up -d postgis
cd backend && ./gradlew bootRun     # Windows: gradlew.bat bootRun
cd frontend && npm ci && npm run dev
```

## 환경변수

`.env.example` 참고. `NEXT_PUBLIC_API_URL`은 프론트 **빌드 시점**에 번들에 박히므로 값을 바꾸면 이미지를 다시 빌드해야 한다.

## 운영 배포

필수 값이 없으면 compose 단계에서 실패한다. postgis 포트는 외부에 열지 않는다.

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

필수: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `CORS_ALLOWED_ORIGINS`, `NEXT_PUBLIC_API_URL`

## DB 마이그레이션

Flyway. `backend/src/main/resources/db/migration`(스키마), `db/seed`(초기 데이터)에 `V{n}__설명.sql`로 추가한다.

## CI

- `backend-ci`: Spotless, Flyway 마이그레이션 검증, 빌드/테스트
- `frontend-ci`: ESLint, Prettier, 타입 체크, 빌드, Docker 이미지 빌드
