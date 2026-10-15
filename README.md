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

cd frontend
echo "NEXT_PUBLIC_API_URL=http://localhost:8080" > .env.local   # 없으면 dev/build가 시작하자마자 실패 (Bash 기준)
npm ci && npm run dev
```

## 환경변수

`.env.example` 참고. `NEXT_PUBLIC_API_URL`은 프론트 **빌드 시점**에 번들에 박히므로 값을 바꾸면 이미지를 다시 빌드해야 한다.

- 값이 없거나 URL 형식이 아니면 `next.config.ts`에서 빌드(`dev` 포함)가 실패한다.
- 프론트 Docker 빌드는 `--build-arg NEXT_PUBLIC_API_URL=...`이 있으면 그 값을, 없으면 `frontend/`의 env 파일(`.env.production`, `.env.local` 등) 값을 쓴다. compose는 항상 빌드 인자로 넘긴다.
- 이 값의 출처(origin)가 CSP `connect-src`에 들어간다.

## 운영 배포

필수 값이 없으면 compose 단계에서 실패한다. postgis 포트는 외부에 열지 않는다.

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

필수: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `CORS_ALLOWED_ORIGINS`, `NEXT_PUBLIC_API_URL`

## 프론트엔드

Next.js 16 (App Router), React 19, TanStack Query, nuqs(URL 상태), zod, Tailwind CSS 4, shadcn/ui(Base UI).

### 폴더 구조

```
frontend/src/
├── app/                  # 라우트, 레이아웃, 전역 CSS, 에러 화면, Provider
├── features/image/       # 영상 검색 기능
│   ├── components/       #   화면 컴포넌트
│   ├── api.ts            #   API 호출
│   ├── queries.ts        #   TanStack Query 훅
│   ├── schemas.ts        #   zod 스키마 (응답 검증 + 타입의 원본)
│   ├── searchParams.ts   #   검색 조건 ↔ URL
│   └── imageText.ts      #   화면 표시 문자열
└── shared/               # 기능과 무관한 공용 코드
    ├── api/              #   API 클라이언트(apiGet, ApiError)
    ├── components/       #   공용 컴포넌트
    │   └── ui/           #   shadcn이 생성한 컴포넌트
    └── utils/
```

### 명령

| 명령 | 설명 |
| --- | --- |
| `npm run dev` | 개발 서버 (http://localhost:3000) |
| `npm run build` | 운영 빌드 (`output: "standalone"`) |
| `npm test` | Vitest 단위 테스트 (`npx vitest`: 저장 시 재실행) |
| `npm run lint` / `lint:fix` | ESLint 검사 / 자동 수정 |
| `npm run format` / `format:check` | Prettier 적용 / 검사 |

커밋 전: `format` → `npx tsc --noEmit` → `lint:fix` → `format` → `build` → `test`

### 규칙

- 테스트는 대상 파일 옆에 `*.test.ts`로 둔다.
- 의존성은 정확한 버전으로 추가한다: `npm install -E <패키지>` (개발용은 `-D -E`).
- shadcn 컴포넌트: `npx shadcn@4.21.5 add <이름>` → `src/shared/components/ui`에 생성된다. 생성된 파일의 `cn` import는 `import { cn } from "cn";`으로 바꾼다.
- 색은 `globals.css`의 토큰(`bg-background`, `text-muted-foreground` 등)만 쓰고 고정색·`dark:` 클래스는 쓰지 않는다. 다크 모드는 OS 설정을 따른다.
- 보안 헤더와 CSP는 `next.config.ts`에 있다. 외부 출처(지도 타일 등)를 불러오려면 CSP에 추가해야 한다.

## DB 마이그레이션

Flyway. `backend/src/main/resources/db/migration`(스키마), `db/seed`(초기 데이터)에 `V{n}__설명.sql`로 추가한다.

## CI

- `backend-ci`: Spotless, Flyway 마이그레이션 검증, 빌드/테스트
- `frontend-ci`: ESLint, Prettier, 타입 체크, Vitest, 빌드, Docker 이미지 빌드
