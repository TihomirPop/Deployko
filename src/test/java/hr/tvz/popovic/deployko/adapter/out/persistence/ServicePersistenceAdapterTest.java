package hr.tvz.popovic.deployko.adapter.out.persistence;

import hr.tvz.popovic.deployko.application.domain.model.EnvironmentVariables;
import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.NetworkAttachment;
import hr.tvz.popovic.deployko.application.domain.model.NetworkAttachments;
import hr.tvz.popovic.deployko.application.domain.model.Port;
import hr.tvz.popovic.deployko.application.domain.model.PortMappings;
import hr.tvz.popovic.deployko.application.domain.model.RuntimeConfiguration;
import hr.tvz.popovic.deployko.application.domain.model.Service;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMount;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMounts;
import hr.tvz.popovic.deployko.application.port.out.CreateServicePort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServiceByNamePort;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_ENVIRONMENT_VARIABLES;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_NETWORK_ATTACHMENTS;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_PORT_MAPPINGS;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_VOLUME_MOUNTS;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICES;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class ServicePersistenceAdapterTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("deployko")
            .withUsername("deployko")
            .withPassword("deployko");

    private static DSLContext dsl;
    private static ServicePersistenceAdapter adapter;

    @BeforeAll
    static void migrate_database() {
        Flyway
                .configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .load()
                .migrate();

        dsl = DSL.using(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        adapter = new ServicePersistenceAdapter(dsl, new JooqTransactionHelper(dsl));
    }

    @BeforeEach
    void delete_services() {
        dsl.deleteFrom(SERVICES).execute();
    }

    @Test
    void create_persists_service_with_runtime_configuration() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));

        CreateServicePort.CreateServicePortResult result = adapter.create(service);

        assertThat(result).isInstanceOf(CreateServicePort.CreateServicePortResult.Success.class);
        assertThat(dsl.fetchCount(SERVICES)).isEqualTo(1);
        assertThat(dsl.fetchCount(SERVICE_ENVIRONMENT_VARIABLES)).isEqualTo(2);
        assertThat(dsl.fetchCount(SERVICE_PORT_MAPPINGS)).isEqualTo(2);
        assertThat(dsl.fetchCount(SERVICE_VOLUME_MOUNTS)).isEqualTo(2);
        assertThat(dsl.fetchCount(SERVICE_NETWORK_ATTACHMENTS)).isEqualTo(2);
        assertThat(dsl.fetchExists(
                dsl
                        .selectOne()
                        .from(SERVICES)
                        .where(SERVICES.NAME.eq("billing-api"))
                        .and(SERVICES.IMAGE_REPOSITORY.eq("registry.example.com/team/billing-api"))
        )).isTrue();
        assertThat(dsl.fetchExists(
                dsl
                        .selectOne()
                        .from(SERVICE_ENVIRONMENT_VARIABLES)
                        .where(SERVICE_ENVIRONMENT_VARIABLES.KEY.eq("APP_ENV"))
                        .and(SERVICE_ENVIRONMENT_VARIABLES.VALUE.eq("prod"))
        )).isTrue();
        assertThat(dsl.fetchExists(
                dsl
                        .selectOne()
                        .from(SERVICE_PORT_MAPPINGS)
                        .where(SERVICE_PORT_MAPPINGS.HOST_PORT.eq(8080))
                        .and(SERVICE_PORT_MAPPINGS.HOST_PROTOCOL.eq("TCP"))
                        .and(SERVICE_PORT_MAPPINGS.CONTAINER_PORT.eq(80))
                        .and(SERVICE_PORT_MAPPINGS.CONTAINER_PROTOCOL.eq("TCP"))
        )).isTrue();
        assertThat(dsl.fetchExists(
                dsl
                        .selectOne()
                        .from(SERVICE_VOLUME_MOUNTS)
                        .where(SERVICE_VOLUME_MOUNTS.TARGET_PATH.eq("/app/config"))
                        .and(SERVICE_VOLUME_MOUNTS.MOUNT_TYPE.eq("BIND"))
                        .and(SERVICE_VOLUME_MOUNTS.SOURCE.eq("/opt/deployko/billing-api/config"))
                        .and(SERVICE_VOLUME_MOUNTS.READ_ONLY.isTrue())
        )).isTrue();
        assertThat(dsl.fetchExists(
                dsl
                        .selectOne()
                        .from(SERVICE_NETWORK_ATTACHMENTS)
                        .where(SERVICE_NETWORK_ATTACHMENTS.NETWORK_NAME.eq("deployko_backend"))
        )).isTrue();
    }

    @Test
    void create_returns_already_exists_when_service_name_is_duplicate() {
        Service firstService = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        Service duplicateService = new Service(
                new ServiceName("billing-api"),
                new ImageRepository("registry.example.com/team/other-api"),
                RuntimeConfiguration.empty()
        );

        CreateServicePort.CreateServicePortResult firstResult = adapter.create(firstService);
        CreateServicePort.CreateServicePortResult duplicateResult = adapter.create(duplicateService);

        assertThat(firstResult).isInstanceOf(CreateServicePort.CreateServicePortResult.Success.class);
        assertThat(duplicateResult).isInstanceOf(CreateServicePort.CreateServicePortResult.AlreadyExists.class);
        assertThat(dsl.fetchCount(SERVICES)).isEqualTo(1);
    }

    @Test
    void delete_by_name_deletes_service_and_runtime_configuration() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        adapter.create(service);

        DeleteServiceByNamePort.DeleteServiceByNameResult result = adapter.deleteByName(service.name());

        assertThat(result).isInstanceOf(DeleteServiceByNamePort.DeleteServiceByNameResult.Deleted.class);
        assertThat(dsl.fetchCount(SERVICES)).isZero();
        assertThat(dsl.fetchCount(SERVICE_ENVIRONMENT_VARIABLES)).isZero();
        assertThat(dsl.fetchCount(SERVICE_PORT_MAPPINGS)).isZero();
        assertThat(dsl.fetchCount(SERVICE_VOLUME_MOUNTS)).isZero();
        assertThat(dsl.fetchCount(SERVICE_NETWORK_ATTACHMENTS)).isZero();
    }

    @Test
    void delete_by_name_returns_not_found_when_service_does_not_exist() {
        DeleteServiceByNamePort.DeleteServiceByNameResult result = adapter.deleteByName(new ServiceName("missing-api"));

        assertThat(result).isInstanceOf(DeleteServiceByNamePort.DeleteServiceByNameResult.NotFound.class);
    }

    private static Service serviceWithRuntimeConfiguration(ServiceName serviceName) {
        return new Service(
                serviceName,
                new ImageRepository("registry.example.com/team/billing-api"),
                new RuntimeConfiguration(
                        EnvironmentVariables
                                .empty()
                                .add(new EnvironmentVariables.Key("APP_ENV"), new EnvironmentVariables.Value("prod"))
                                .add(new EnvironmentVariables.Key("JAVA_OPTS"), new EnvironmentVariables.Value("-Xmx512m")),
                        PortMappings
                                .empty()
                                .add(new Port(8080), new Port(80))
                                .add(new Port(8443, Port.Protocol.UDP), new Port(443, Port.Protocol.UDP)),
                        VolumeMounts
                                .empty()
                                .add(new VolumeMount.BindMount(
                                        new VolumeMount.HostPath("/opt/deployko/billing-api/config"),
                                        new VolumeMount.Target("/app/config"),
                                        true
                                ))
                                .add(new VolumeMount.NamedVolumeMount(
                                        new VolumeMount.VolumeName("billing_api_data"),
                                        new VolumeMount.Target("/var/lib/billing-api"),
                                        false
                                )),
                        NetworkAttachments
                                .empty()
                                .add(new NetworkAttachment(new NetworkAttachment.NetworkName("deployko_backend")))
                                .add(new NetworkAttachment(new NetworkAttachment.NetworkName("observability")))
                )
        );
    }
}
