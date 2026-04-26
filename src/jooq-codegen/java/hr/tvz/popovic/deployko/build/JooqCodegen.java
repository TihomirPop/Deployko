package hr.tvz.popovic.deployko.build;

import org.flywaydb.core.Flyway;
import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.Configuration;
import org.jooq.meta.jaxb.Database;
import org.jooq.meta.jaxb.Generate;
import org.jooq.meta.jaxb.Generator;
import org.jooq.meta.jaxb.Jdbc;
import org.jooq.meta.jaxb.Target;
import org.testcontainers.postgresql.PostgreSQLContainer;

public final class JooqCodegen {

    private static final String DATABASE_NAME = "deployko";
    private static final String DATABASE_USERNAME = "deployko";
    private static final String DATABASE_PASSWORD = "deployko";
    private static final String POSTGRES_IMAGE = "postgres:18-alpine";
    private static final String GENERATED_PACKAGE =
            "hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated";
    private static final String GENERATED_DIRECTORY = "src/main/java";

    private JooqCodegen() {
    }

    public static void main(String[] args) throws Exception {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer(POSTGRES_IMAGE)
                .withDatabaseName(DATABASE_NAME)
                .withUsername(DATABASE_USERNAME)
                .withPassword(DATABASE_PASSWORD)) {
            postgres.start();

            Flyway
                    .configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .load()
                    .migrate();

            GenerationTool.generate(jooqConfiguration(postgres));
        }
    }

    private static Configuration jooqConfiguration(PostgreSQLContainer postgres) {
        return new Configuration()
                .withJdbc(new Jdbc()
                        .withDriver("org.postgresql.Driver")
                        .withUrl(postgres.getJdbcUrl())
                        .withUser(postgres.getUsername())
                        .withPassword(postgres.getPassword()))
                .withGenerator(new Generator()
                        .withDatabase(new Database()
                                .withName("org.jooq.meta.postgres.PostgresDatabase")
                                .withInputSchema("public")
                                .withIncludes("services|service_.*")
                                .withExcludes("flyway_schema_history"))
                        .withGenerate(new Generate()
                                .withRoutines(false))
                        .withTarget(new Target()
                                .withPackageName(GENERATED_PACKAGE)
                                .withDirectory(GENERATED_DIRECTORY)));
    }
}
