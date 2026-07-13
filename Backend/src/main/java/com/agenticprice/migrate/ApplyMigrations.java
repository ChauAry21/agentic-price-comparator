package com.agenticprice.migrate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Standalone migration runner. Uses the same DataSource the application uses
 * (which we know works against Supabase's pooler) so we don't need psql,
 * the Supabase CLI, or any extra tooling installed.
 *
 * Why this exists: Flyway is incompatible with Supabase's transaction-mode
 * PgBouncer pooling on this project's connection setup. Until that's fixed,
 * migrations are applied manually by running this class.
 *
 * Usage:
 *   mvn -q exec:java -Dexec.mainClass=com.agenticprice.migrate.ApplyMigrations
 *
 * After running successfully, optionally re-enable Flyway for future code
 * changes that don't touch the schema (or leave it disabled and just
 * re-run this script whenever a new V*.sql file is added).
 *
 * Spring Boot web auto-configuration is excluded so this doesn't try to
 * bind a port — it just boots the DataSource, runs SQL, exits.
 */
@SpringBootApplication(
        scanBasePackages = "com.agenticprice",
        exclude = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class
        }
)
public class ApplyMigrations {

    public static void main(String[] args) throws Exception {
        // SpringApplication with web=none so no embedded Tomcat is started.
        var app = new SpringApplication(ApplyMigrations.class);
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        try (ConfigurableApplicationContext ctx = app.run(args)) {
            DataSource ds = ctx.getBean(DataSource.class);
            Path migrationsDir = locateMigrationsDir();
            System.out.println("Applying migrations from: " + migrationsDir);
            try (Stream<Path> files = Files.list(migrationsDir);
                 Connection conn = ds.getConnection()) {
                files.filter(p -> p.getFileName().toString().endsWith(".sql"))
                        .sorted(Comparator.comparing(Path::getFileName))
                        .forEach(p -> runOne(conn, p));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            System.out.println("Done.");
        }
    }

    private static Path locateMigrationsDir() {
        // CWD is the Backend/ project root when run via `mvn exec:java`.
        Path direct = Paths.get("src/main/resources/db/migration");
        if (Files.isDirectory(direct)) return direct;
        // Fallback: locate via classpath.
        URL u = ApplyMigrations.class.getResource("/db/migration");
        if (u != null && "file".equals(u.getProtocol())) {
            return Path.of(u.getPath());
        }
        throw new IllegalStateException(
                "Could not find src/main/resources/db/migration (cwd=" + direct.toAbsolutePath() + ")");
    }

    private static void runOne(Connection conn, Path file) {
        System.out.println("  -> " + file.getFileName());
        String sql;
        try {
            sql = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        try (Statement st = conn.createStatement()) {
            // Each file is the unit of work; commit-per-file when the
            // connection supports manual commits. Hikari defaults to
            // autoCommit=true, in which case each statement is its own
            // transaction and an explicit commit() throws.
            st.execute(sql);
            if (!conn.getAutoCommit()) {
                conn.commit();
            }
            System.out.println("     applied");
        } catch (SQLException e) {
            throw new RuntimeException("Failed applying " + file.getFileName() + ": " + e.getMessage(), e);
        }
    }
}
