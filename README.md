# PriceHawk — Agentic Price Comparator

PriceHawk is a price comparison tool that searches multiple online retailers, extracts structured pricing from their product pages with the help of an LLM, and surfaces the results in a side-by-side view. Instead of relying on brittle per-retailer scrapers, the project uses a small fleet of HTML fetchers plus a shared LLM extraction step to interpret whatever the retailers return.

## Repository layout

The project is split into three pieces:

```
agentic-price-comparator/
├── Backend/      # Spring Boot 3.2.5 service (Java 17, Maven)
├── Frontend/     # React + TypeScript + Vite dashboard
└── proxy-rotator/  # Python service that cycles outbound proxy IPs
```

The `Backend` is the main application. The `Frontend` is a single-page dashboard for searching and comparing products. The `proxy-rotator` is a small companion service that helps the Backend avoid rate limits when scraping retailer sites.

## How it works end-to-end

1. **Search.** The frontend posts a product query to the Backend's `/api/prices/search` endpoint.
2. **Parse query.** The Backend uses an LLM to clean the query into a canonical product name (e.g. "iphone 16 128gb black").
3. **Scrape retailers.** In parallel, each registered `ScraperAgent` fetches a search results page (Amazon, Walmart, eBay, Etsy, Target, Temu, Newegg). Most use Jsoup; the JavaScript-heavy ones use Playwright.
4. **Extract prices with LLM.** For each result, the Backend sends the item HTML to the LLM with the `EXTRACT_PRICE` prompt. The prompt asks for a structured `{price, currency, financed}` JSON response, where `price` is the **total** cost — for financed listings like "$10.75/month with $791 down for 24 months", the LLM is required to compute `(monthly * term_months) + down_payment` and return that as `price`.
5. **Rank results.** Another LLM call uses the `RANK_PRODUCTS` prompt to order the cleaned listings by semantic fit to the original query (not just keyword overlap — a query about "long battery life" should favor "60H battery" over "long battery life" even if the wording is less direct).
6. **Cache and respond.** The full `PriceComparisonResponse` (results, summary numbers like lowest/highest/average/savings, ranking) is cached per query string in Postgres, and returned to the frontend.
7. **Compare.** When the user selects two or more products, the frontend calls `/api/prices/extract-features`. A third LLM call (`EXTRACT_FEATURES`) builds a side-by-side table with a consistent vocabulary across products. Price and Condition rows are computed locally (not by the LLM) and merged in.
8. **Display.** The frontend renders the comparison table with badges — including a "Financed" badge next to the price for any listing backed by a carrier installment plan.

## Tech stack

**Backend**
- Spring Boot 3.2.5, Java 17, Maven
- Spring Data JPA + Hibernate (Postgres)
- OpenAI Java SDK (`com.openai:openai-java`) for LLM calls
- Jsoup for HTML parsing, Playwright for JS-heavy retailers
- Lombok for boilerplate reduction
- exec-maven-plugin for running the standalone migration tool

**Frontend**
- React 19, TypeScript, Vite
- React Router for navigation
- Recharts for any price-history visualizations

**proxy-rotator**
- Python (FastAPI or similar) — rotates outbound IPs to keep retailer scrapers under rate limits

## Prerequisites

- **JDK 17** (Temurin 17 recommended). Newer JDKs (21, 26) work for most things but Lombok and a few other build tools flicker; staying on 17 keeps things stable.
- **Node.js 20+** and npm.
- **PostgreSQL** (or a Supabase project, which the project is configured for).
- **Python 3.11+** if you're running the proxy-rotator locally.

## Getting started

### 1. Clone and configure

```bash
git clone <your-fork-url> agentic-price-comparator
cd agentic-price-comparator
```

Copy `Backend/src/main/resources/application.properties` and edit the values for your local environment. The minimum you need is:

```
OPENAI_API_KEY=sk-...
spring.datasource.url=jdbc:postgresql://<host>:<port>/postgres
spring.datasource.username=postgres.<ref>
spring.datasource.password=<password>
```

### 2. Run database migrations

Flyway is disabled in this project (Supabase's transaction-mode pooler is incompatible with Flyway's prepared statements). Instead, run migrations with the bundled runner:

```bash
cd Backend
mvn -q exec:java -Dexec.mainClass=com.agenticprice.migrate.ApplyMigrations
```

The runner uses the same DataSource the app uses, so it works against Supabase's pooler. It applies every `V*.sql` file in `src/main/resources/db/migration/` in alphabetical order. Each migration must be a single SQL statement and use idempotent clauses (`ADD COLUMN IF NOT EXISTS`, etc.) so re-running is safe.

### 3. Start the backend

```bash
cd Backend
mvn spring-boot:run
```

The service comes up on port 8080 (override with `PORT=... mvn spring-boot:run`).

### 4. Start the frontend

```bash
cd Frontend
npm install
npm run dev
```

The dev server runs on port 5173 by default. The Backend's `allowed.origins` already permits `http://localhost:5173`.

### 5. (Optional) Start the proxy-rotator

```bash
cd proxy-rotator
pip install -r requirements.txt
# See proxy-rotator/README or app/ entrypoint for the run command
```

The Backend defaults to `proxy.rotator.url=http://localhost:8001`; adjust if you run it elsewhere.

## Project layout (Backend)

```
src/main/java/com/agenticprice/
├── App.java                          # Spring Boot entry point
├── agent/                            # Orchestration
│   └── PriceComparisonAgent.java     # The main compare() flow + cache
├── api/                              # Request/response DTOs
├── config/                           # Spring configuration
├── controller/                       # REST endpoints
├── migrate/                          # Standalone migration runner
├── model/                            # JPA entities
├── prompt/                           # PriceHawkPrompt enum — all LLM prompts
├── repository/                       # Spring Data JPA repositories
├── scheduler/                        # @Scheduled jobs (price alerts, etc.)
├── scraper/                          # One ScraperAgent per retailer
├── service/                          # Business logic
├── tracking/                         # Tracking pixels / analytics helpers
└── util/                             # Parsers, helpers, RawLlmLogger
```

## Project layout (Frontend)

```
Frontend/src/
├── api/             # Typed wrappers around Backend endpoints
├── components/      # Reusable UI (CompareTray, FeatureTable, etc.)
├── assets/          # Static images
├── App.tsx
├── Dashboard.tsx    # Main search + results view
├── Settings.tsx
└── dashboard.css
```

## How caching works

There is currently one cache: `SearchCache` (a JPA entity backed by Postgres).

- **Key:** the normalized query string (trimmed + lowercased).
- **Value:** the full serialized `PriceComparisonResponse` for that query.
- **TTL:** `cache.ttl.hours` in `application.properties` (default `2`; we run `0` in some environments for debugging).
- **Invalidation:** the `PriceController.clearCache(query)` endpoint deletes a single query's row, or run `DELETE FROM search_cache;` in the database for a full reset.

The LLM calls themselves (`extractPrice`, `rankProducts`, `extractFeatures`) are **not** cached individually. If two users search for the same product within the TTL, only one set of LLM calls fires. If the cache is expired or the queries differ slightly, every call runs fresh.

## Database migrations

- Migrations live in `Backend/src/main/resources/db/migration/` named `V<version>__<description>.sql`.
- They are applied in alphabetical order by `ApplyMigrations.java`.
- Each file must contain a single SQL statement and use idempotent clauses (`IF NOT EXISTS`).
- A migration can be re-run safely; the runner does not maintain a "applied migrations" table.

## Common tasks

**Clear the search cache for a single query:**
```bash
curl -X POST 'http://localhost:8080/api/prices/cache/clear?query=iphone%2016'
```

**Clear the entire cache:**
```sql
DELETE FROM search_cache;
```

**Inspect what the LLM saw (for debugging prompt issues):**
The Backend writes every raw input sent to the LLM into `Backend/logs/raw-llm-<runId>/`. Open the file with the matching `<seq>-<label>-<runId>.html` to see exactly what HTML the model was given for a particular `extractPrice` call.

**Reset the local DB to a known state:**
```bash
# drop and recreate the schema, then re-run migrations
psql "$SUPABASE_DB_URL" -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
mvn -q exec:java -Dexec.mainClass=com.agenticprice.migrate.ApplyMigrations
```

## Deployment

- **Backend:** hosted on Railway. Build with the standard Spring Boot Maven plugin (`mvn package`), then run the produced jar. Railway sets `PORT` automatically.
- **Frontend:** built with `npm run build`, output goes to `Frontend/dist/`. Host on any static host (Vercel, Netlify, Railway static).
- **Database:** Supabase Postgres, accessed through the transaction-mode pooler (port 6543). The Backend is configured for that endpoint.

## Contributing

1. Branch off `main` with a descriptive name (`feature/<thing>`, `fix/<thing>`).
2. Make focused commits. We tend to split backend / frontend / migrations into separate commits for reviewability.
3. Run the Backend locally and exercise the change end-to-end. Prompt changes especially need eyeball verification — there are no unit tests for "does this ranking look right."
4. If you're touching a scraper, run a few real queries and check `logs/raw-llm-*/` to see what the LLM is actually receiving.
5. If you're adding a schema change, add a new `V<n>__<description>.sql` and re-run the migration locally before pushing.