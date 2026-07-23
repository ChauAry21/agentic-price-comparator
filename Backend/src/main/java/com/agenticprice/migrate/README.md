# Manual migration runner

Flyway is currently disabled in `application.properties`. To apply pending
SQL migrations, run this class instead — it uses the same DataSource
Spring Boot uses for the app, so it works against Supabase's pooler.

## Run it

From the Backend folder:

```bash
mvn -q exec:java -Dexec.mainClass=com.agenticprice.migrate.ApplyMigrations
```

Expected output:

```
Applying migrations from: /path/to/Backend/src/main/resources/db/migration
  -> V1__add_financed_fields.sql
     applied
Done.
```

## Why not Flyway?

Supabase's transaction-mode PgBouncer (port 6543) is incompatible with
Flyway 9 on this project's connection setup. The other Supabase endpoints
either reject JDBC connections (session pooler at 5432 due to no SNI) or
the direct host doesn't exist (db.<ref>.supabase.co:5432). The simplest
working path is this runner, which goes through Hikari exactly like the
app does.

## After running

Restart the backend. Hibernate will see the new column. No further action
required for code that reads/writes the column (e.g. `PriceSnapshot.financed`).
