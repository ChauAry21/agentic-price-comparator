# Database migrations

This folder holds versioned SQL migrations. Each file is named
`V<version>__<description>.sql` and is applied in order.

## How to apply

The supported path is the Java migration runner, which uses the same
DataSource the app uses and is known to work against Supabase's pooler:

```
mvn -q exec:java -Dexec.mainClass=com.agenticprice.migrate.ApplyMigrations
```

See `../java/com/agenticprice/migrate/README.md` for full instructions
and for the explanation of why Flyway is disabled.

If you don't have Maven set up, the SQL editor in the Supabase dashboard
also works — paste the contents of the file and run it.

## Migrations

| File | What it does |
|------|--------------|
| `V1__add_financed_fields.sql` | Adds `financed boolean` to `price_snapshot`. Idempotent (uses `IF NOT EXISTS`). Lets the UI show a "Financed" badge while the headline numeric price holds the full cost. |

## Production (Railway)

Until Flyway or Liquibase is wired in, every new migration must be
applied **by hand** to the production database once, before the code
that depends on it ships. The migration file is the source of truth —
do not diverge.
