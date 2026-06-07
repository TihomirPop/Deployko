package hr.tvz.popovic.deployko.adapter.out.persistence;

import hr.tvz.popovic.deployko.application.domain.model.DesiredDeployment;
import hr.tvz.popovic.deployko.application.domain.model.DesiredDeploymentState;
import hr.tvz.popovic.deployko.application.domain.model.EnvironmentVariables;
import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.ImageVersion;
import hr.tvz.popovic.deployko.application.domain.model.NetworkAttachment;
import hr.tvz.popovic.deployko.application.domain.model.NetworkAttachments;
import hr.tvz.popovic.deployko.application.domain.model.Port;
import hr.tvz.popovic.deployko.application.domain.model.PortMappings;
import hr.tvz.popovic.deployko.application.domain.model.RuntimeConfiguration;
import hr.tvz.popovic.deployko.application.domain.model.Service;
import hr.tvz.popovic.deployko.application.domain.model.ServiceName;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMount;
import hr.tvz.popovic.deployko.application.domain.model.VolumeMounts;
import hr.tvz.popovic.deployko.application.port.out.CreateServicePortMappingPort;
import hr.tvz.popovic.deployko.application.port.out.CreateServicePort;
import hr.tvz.popovic.deployko.application.port.out.CreateServiceVolumeMountPort;
import hr.tvz.popovic.deployko.application.port.out.CreateServiceNetworkAttachmentPort;
import hr.tvz.popovic.deployko.application.port.out.DeleteDesiredDeploymentPort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServiceByNamePort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServiceNetworkAttachmentPort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServicePortMappingPort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServiceVolumeMountPort;
import hr.tvz.popovic.deployko.application.port.out.CreateServiceEnvironmentVariablePort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServiceEnvironmentVariablePort;
import hr.tvz.popovic.deployko.application.port.out.FindDesiredDeploymentStatePort;
import hr.tvz.popovic.deployko.application.port.out.FindLastCiDeploymentPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceDefinitionPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceEnvironmentVariablesPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceNamesByImageRepositoryPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceNetworkAttachmentsPort;
import hr.tvz.popovic.deployko.application.port.out.FindServicePortMappingsPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceSummaryCandidatesPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceVolumeMountsPort;
import hr.tvz.popovic.deployko.application.port.out.RecordCiDeploymentPort;
import hr.tvz.popovic.deployko.application.port.out.RecordDeploymentHistoryPort;
import hr.tvz.popovic.deployko.application.port.out.UpdateServiceEnvironmentVariablePort;
import hr.tvz.popovic.deployko.application.port.out.UpdateDesiredDeploymentStatePort;
import hr.tvz.popovic.deployko.application.port.out.UpdateServiceVolumeMountPort;
import hr.tvz.popovic.deployko.application.port.out.UpsertDesiredDeploymentPort;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.OffsetDateTime;
import java.util.UUID;

import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_CI_DEPLOYMENTS;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_DEPLOYMENT_HISTORY;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_DESIRED_DEPLOYMENTS;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_DESIRED_DEPLOYMENT_ENVIRONMENT_VARIABLES;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_DESIRED_DEPLOYMENT_NETWORK_ATTACHMENTS;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_DESIRED_DEPLOYMENT_PORT_MAPPINGS;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_DESIRED_DEPLOYMENT_VOLUME_MOUNTS;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_ENVIRONMENT_VARIABLES;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_NETWORK_ATTACHMENTS;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_PORT_MAPPINGS;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICE_VOLUME_MOUNTS;
import static hr.tvz.popovic.deployko.adapter.out.persistence.jooq.generated.Tables.SERVICES;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class PersistenceAdaptersTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("deployko")
            .withUsername("deployko")
            .withPassword("deployko");

    private static DSLContext dsl;
    private static ServiceDefinitionPersistenceAdapter serviceDefinitions;
    private static DesiredDeploymentPersistenceAdapter desiredDeployments;
    private static CiDeploymentPersistenceAdapter ciDeployments;
    private static DeploymentHistoryPersistenceAdapter deploymentHistory;
    private static ServiceEnvironmentVariablePersistenceAdapter environmentVariables;
    private static ServicePortMappingPersistenceAdapter portMappings;
    private static ServiceVolumeMountPersistenceAdapter volumeMounts;
    private static ServiceNetworkAttachmentPersistenceAdapter networkAttachments;

    @BeforeAll
    static void migrate_database() {
        Flyway
                .configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .load()
                .migrate();

        dsl = DSL.using(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        JooqTransactionHelper transactions = new JooqTransactionHelper(dsl);
        serviceDefinitions = new ServiceDefinitionPersistenceAdapter(dsl, transactions);
        desiredDeployments = new DesiredDeploymentPersistenceAdapter(dsl, transactions);
        ciDeployments = new CiDeploymentPersistenceAdapter(dsl);
        deploymentHistory = new DeploymentHistoryPersistenceAdapter(dsl);
        environmentVariables = new ServiceEnvironmentVariablePersistenceAdapter(dsl, transactions);
        portMappings = new ServicePortMappingPersistenceAdapter(dsl, transactions);
        volumeMounts = new ServiceVolumeMountPersistenceAdapter(dsl, transactions);
        networkAttachments = new ServiceNetworkAttachmentPersistenceAdapter(dsl, transactions);
    }

    @BeforeEach
    void delete_services() {
        dsl.deleteFrom(SERVICES).execute();
    }

    @Test
    void create_persists_service_with_runtime_configuration() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));

        CreateServicePort.CreateServicePortResult result = serviceDefinitions.create(service);

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

        CreateServicePort.CreateServicePortResult firstResult = serviceDefinitions.create(firstService);
        CreateServicePort.CreateServicePortResult duplicateResult = serviceDefinitions.create(duplicateService);

        assertThat(firstResult).isInstanceOf(CreateServicePort.CreateServicePortResult.Success.class);
        assertThat(duplicateResult).isInstanceOf(CreateServicePort.CreateServicePortResult.AlreadyExists.class);
        assertThat(dsl.fetchCount(SERVICES)).isEqualTo(1);
    }

    @Test
    void find_by_name_returns_service_definition_when_service_exists() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);

        FindServiceDefinitionPort.FindServiceDefinitionResult result = serviceDefinitions.findByName(service.name());

        assertThat(result).isInstanceOf(FindServiceDefinitionPort.FindServiceDefinitionResult.Found.class);
        FindServiceDefinitionPort.FindServiceDefinitionResult.Found found =
                (FindServiceDefinitionPort.FindServiceDefinitionResult.Found) result;
        assertThat(found.service()).isEqualTo(service);
    }

    @Test
    void find_by_name_returns_not_found_when_service_does_not_exist() {
        FindServiceDefinitionPort.FindServiceDefinitionResult result =
                serviceDefinitions.findByName(new ServiceName("missing-api"));

        assertThat(result).isInstanceOf(FindServiceDefinitionPort.FindServiceDefinitionResult.NotFound.class);
    }

    @Test
    void find_service_names_by_image_repository_returns_matching_services_in_name_order() {
        ImageRepository sharedRepository = new ImageRepository("registry.example.com/team/billing-api");
        Service workerService = serviceWithRuntimeConfiguration(new ServiceName("billing-worker"));
        Service apiService = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        Service otherService = new Service(
                new ServiceName("other-api"),
                new ImageRepository("registry.example.com/team/other-api"),
                RuntimeConfiguration.empty()
        );
        serviceDefinitions.create(workerService);
        serviceDefinitions.create(otherService);
        serviceDefinitions.create(apiService);

        FindServiceNamesByImageRepositoryPort.FindServiceNamesByImageRepositoryResult result =
                serviceDefinitions.findServiceNamesByImageRepository(sharedRepository);

        assertThat(result)
                .isInstanceOf(FindServiceNamesByImageRepositoryPort.FindServiceNamesByImageRepositoryResult.Found.class);
        FindServiceNamesByImageRepositoryPort.FindServiceNamesByImageRepositoryResult.Found found =
                (FindServiceNamesByImageRepositoryPort.FindServiceNamesByImageRepositoryResult.Found) result;
        assertThat(found.serviceNames()).containsExactly(
                new ServiceName("billing-api"),
                new ServiceName("billing-worker")
        );
    }

    @Test
    void find_service_names_by_image_repository_returns_empty_list_when_repository_has_no_services() {
        FindServiceNamesByImageRepositoryPort.FindServiceNamesByImageRepositoryResult result =
                serviceDefinitions.findServiceNamesByImageRepository(
                        new ImageRepository("registry.example.com/team/missing-api")
                );

        assertThat(result)
                .isInstanceOf(FindServiceNamesByImageRepositoryPort.FindServiceNamesByImageRepositoryResult.Found.class);
        FindServiceNamesByImageRepositoryPort.FindServiceNamesByImageRepositoryResult.Found found =
                (FindServiceNamesByImageRepositoryPort.FindServiceNamesByImageRepositoryResult.Found) result;
        assertThat(found.serviceNames()).isEmpty();
    }

    @Test
    void find_service_summary_candidates_returns_services_with_optional_desired_deployment() {
        Service billingService = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        Service deploykoService = serviceWithRuntimeConfiguration(new ServiceName("deployko-api"));
        serviceDefinitions.create(deploykoService);
        serviceDefinitions.create(billingService);
        desiredDeployments.upsert(desiredDeployment(deploykoService));

        FindServiceSummaryCandidatesPort.FindServiceSummaryCandidatesResult result =
                serviceDefinitions.findServiceSummaryCandidates();

        assertThat(result)
                .isInstanceOf(FindServiceSummaryCandidatesPort.FindServiceSummaryCandidatesResult.Found.class);
        FindServiceSummaryCandidatesPort.FindServiceSummaryCandidatesResult.Found found =
                (FindServiceSummaryCandidatesPort.FindServiceSummaryCandidatesResult.Found) result;
        assertThat(found.services()).containsExactly(
                new FindServiceSummaryCandidatesPort.ServiceSummaryCandidate(
                        billingService.name(),
                        billingService.imageRepository(),
                        java.util.Optional.empty(),
                        java.util.Optional.empty()
                ),
                new FindServiceSummaryCandidatesPort.ServiceSummaryCandidate(
                        deploykoService.name(),
                        deploykoService.imageRepository(),
                        java.util.Optional.of(new ImageVersion("1.0.0")),
                        java.util.Optional.of(DesiredDeploymentState.RUNNING)
                )
        );
    }

    @Test
    void find_port_mappings_returns_mappings_when_service_exists() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);

        FindServicePortMappingsPort.FindServicePortMappingsResult result =
                portMappings.findPortMappings(service.name());

        assertThat(result).isInstanceOf(FindServicePortMappingsPort.FindServicePortMappingsResult.Found.class);
        FindServicePortMappingsPort.FindServicePortMappingsResult.Found found =
                (FindServicePortMappingsPort.FindServicePortMappingsResult.Found) result;
        assertThat(found.portMappings()).isEqualTo(service.runtimeConfiguration().portMappings());
    }

    @Test
    void find_port_mappings_returns_service_not_found_when_service_does_not_exist() {
        FindServicePortMappingsPort.FindServicePortMappingsResult result =
                portMappings.findPortMappings(new ServiceName("missing-api"));

        assertThat(result)
                .isInstanceOf(FindServicePortMappingsPort.FindServicePortMappingsResult.ServiceNotFound.class);
    }

    @Test
    void find_environment_variables_returns_variables_when_service_exists() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);

        FindServiceEnvironmentVariablesPort.FindServiceEnvironmentVariablesResult result =
                environmentVariables.findEnvironmentVariables(service.name());

        assertThat(result)
                .isInstanceOf(FindServiceEnvironmentVariablesPort.FindServiceEnvironmentVariablesResult.Found.class);
        FindServiceEnvironmentVariablesPort.FindServiceEnvironmentVariablesResult.Found found =
                (FindServiceEnvironmentVariablesPort.FindServiceEnvironmentVariablesResult.Found) result;
        assertThat(found.environmentVariables()).isEqualTo(service.runtimeConfiguration().environmentVariables());
    }

    @Test
    void find_environment_variables_returns_service_not_found_when_service_does_not_exist() {
        FindServiceEnvironmentVariablesPort.FindServiceEnvironmentVariablesResult result =
                environmentVariables.findEnvironmentVariables(new ServiceName("missing-api"));

        assertThat(result)
                .isInstanceOf(FindServiceEnvironmentVariablesPort.FindServiceEnvironmentVariablesResult.ServiceNotFound.class);
    }

    @Test
    void create_environment_variable_inserts_variable_when_service_exists() {
        Service service = new Service(
                new ServiceName("billing-api"),
                new ImageRepository("registry.example.com/team/billing-api"),
                RuntimeConfiguration.empty()
        );
        serviceDefinitions.create(service);

        CreateServiceEnvironmentVariablePort.CreateServiceEnvironmentVariableResult result =
                environmentVariables.createEnvironmentVariable(
                        service.name(),
                        new EnvironmentVariables.Key("APP_ENV"),
                        new EnvironmentVariables.Value("prod")
                );

        assertThat(result)
                .isInstanceOf(CreateServiceEnvironmentVariablePort.CreateServiceEnvironmentVariableResult.Created.class);
        assertThat(dsl.fetchExists(
                dsl
                        .selectOne()
                        .from(SERVICE_ENVIRONMENT_VARIABLES)
                        .where(SERVICE_ENVIRONMENT_VARIABLES.KEY.eq("APP_ENV"))
                        .and(SERVICE_ENVIRONMENT_VARIABLES.VALUE.eq("prod"))
        )).isTrue();
    }

    @Test
    void create_environment_variable_returns_service_not_found_when_service_does_not_exist() {
        CreateServiceEnvironmentVariablePort.CreateServiceEnvironmentVariableResult result =
                environmentVariables.createEnvironmentVariable(
                        new ServiceName("missing-api"),
                        new EnvironmentVariables.Key("APP_ENV"),
                        new EnvironmentVariables.Value("prod")
                );

        assertThat(result)
                .isInstanceOf(CreateServiceEnvironmentVariablePort.CreateServiceEnvironmentVariableResult.ServiceNotFound.class);
        assertThat(dsl.fetchCount(SERVICE_ENVIRONMENT_VARIABLES)).isZero();
    }

    @Test
    void create_environment_variable_returns_already_exists_when_key_conflicts() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);

        CreateServiceEnvironmentVariablePort.CreateServiceEnvironmentVariableResult result =
                environmentVariables.createEnvironmentVariable(
                        service.name(),
                        new EnvironmentVariables.Key("APP_ENV"),
                        new EnvironmentVariables.Value("staging")
                );

        assertThat(result)
                .isInstanceOf(CreateServiceEnvironmentVariablePort.CreateServiceEnvironmentVariableResult.AlreadyExists.class);
        assertThat(dsl.fetchCount(SERVICE_ENVIRONMENT_VARIABLES)).isEqualTo(2);
    }

    @Test
    void update_environment_variable_updates_variable_when_service_and_key_exist() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);

        UpdateServiceEnvironmentVariablePort.UpdateServiceEnvironmentVariableResult result =
                environmentVariables.updateEnvironmentVariable(
                        service.name(),
                        new EnvironmentVariables.Key("APP_ENV"),
                        new EnvironmentVariables.Value("staging")
                );

        assertThat(result)
                .isInstanceOf(UpdateServiceEnvironmentVariablePort.UpdateServiceEnvironmentVariableResult.Updated.class);
        assertThat(dsl.fetchExists(
                dsl
                        .selectOne()
                        .from(SERVICE_ENVIRONMENT_VARIABLES)
                        .where(SERVICE_ENVIRONMENT_VARIABLES.KEY.eq("APP_ENV"))
                        .and(SERVICE_ENVIRONMENT_VARIABLES.VALUE.eq("staging"))
        )).isTrue();
        assertThat(dsl.fetchCount(SERVICE_ENVIRONMENT_VARIABLES)).isEqualTo(2);
    }

    @Test
    void update_environment_variable_returns_service_not_found_when_service_does_not_exist() {
        UpdateServiceEnvironmentVariablePort.UpdateServiceEnvironmentVariableResult result =
                environmentVariables.updateEnvironmentVariable(
                        new ServiceName("missing-api"),
                        new EnvironmentVariables.Key("APP_ENV"),
                        new EnvironmentVariables.Value("staging")
                );

        assertThat(result)
                .isInstanceOf(UpdateServiceEnvironmentVariablePort.UpdateServiceEnvironmentVariableResult.ServiceNotFound.class);
    }

    @Test
    void update_environment_variable_returns_variable_not_found_when_key_does_not_exist() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);

        UpdateServiceEnvironmentVariablePort.UpdateServiceEnvironmentVariableResult result =
                environmentVariables.updateEnvironmentVariable(
                        service.name(),
                        new EnvironmentVariables.Key("MISSING_ENV"),
                        new EnvironmentVariables.Value("staging")
                );

        assertThat(result)
                .isInstanceOf(UpdateServiceEnvironmentVariablePort.UpdateServiceEnvironmentVariableResult.EnvironmentVariableNotFound.class);
        assertThat(dsl.fetchCount(SERVICE_ENVIRONMENT_VARIABLES)).isEqualTo(2);
    }

    @Test
    void delete_environment_variable_deletes_variable_when_service_and_key_exist() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);

        DeleteServiceEnvironmentVariablePort.DeleteServiceEnvironmentVariableResult result =
                environmentVariables.deleteEnvironmentVariable(
                        service.name(),
                        new EnvironmentVariables.Key("APP_ENV")
                );

        assertThat(result)
                .isInstanceOf(DeleteServiceEnvironmentVariablePort.DeleteServiceEnvironmentVariableResult.Deleted.class);
        assertThat(dsl.fetchCount(SERVICE_ENVIRONMENT_VARIABLES)).isEqualTo(1);
        assertThat(dsl.fetchExists(
                dsl
                        .selectOne()
                        .from(SERVICE_ENVIRONMENT_VARIABLES)
                        .where(SERVICE_ENVIRONMENT_VARIABLES.KEY.eq("APP_ENV"))
        )).isFalse();
    }

    @Test
    void delete_environment_variable_returns_service_not_found_when_service_does_not_exist() {
        DeleteServiceEnvironmentVariablePort.DeleteServiceEnvironmentVariableResult result =
                environmentVariables.deleteEnvironmentVariable(
                        new ServiceName("missing-api"),
                        new EnvironmentVariables.Key("APP_ENV")
                );

        assertThat(result)
                .isInstanceOf(DeleteServiceEnvironmentVariablePort.DeleteServiceEnvironmentVariableResult.ServiceNotFound.class);
    }

    @Test
    void delete_environment_variable_returns_variable_not_found_when_key_does_not_exist() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);

        DeleteServiceEnvironmentVariablePort.DeleteServiceEnvironmentVariableResult result =
                environmentVariables.deleteEnvironmentVariable(
                        service.name(),
                        new EnvironmentVariables.Key("MISSING_ENV")
                );

        assertThat(result)
                .isInstanceOf(DeleteServiceEnvironmentVariablePort.DeleteServiceEnvironmentVariableResult.EnvironmentVariableNotFound.class);
        assertThat(dsl.fetchCount(SERVICE_ENVIRONMENT_VARIABLES)).isEqualTo(2);
    }

    @Test
    void find_volume_mounts_returns_mounts_when_service_exists() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);

        FindServiceVolumeMountsPort.FindServiceVolumeMountsResult result =
                volumeMounts.findVolumeMounts(service.name());

        assertThat(result).isInstanceOf(FindServiceVolumeMountsPort.FindServiceVolumeMountsResult.Found.class);
        FindServiceVolumeMountsPort.FindServiceVolumeMountsResult.Found found =
                (FindServiceVolumeMountsPort.FindServiceVolumeMountsResult.Found) result;
        assertThat(found.volumeMounts()).isEqualTo(service.runtimeConfiguration().volumeMounts());
    }

    @Test
    void find_volume_mounts_returns_service_not_found_when_service_does_not_exist() {
        FindServiceVolumeMountsPort.FindServiceVolumeMountsResult result =
                volumeMounts.findVolumeMounts(new ServiceName("missing-api"));

        assertThat(result)
                .isInstanceOf(FindServiceVolumeMountsPort.FindServiceVolumeMountsResult.ServiceNotFound.class);
    }

    @Test
    void create_volume_mount_inserts_mount_when_service_exists() {
        Service service = new Service(
                new ServiceName("billing-api"),
                new ImageRepository("registry.example.com/team/billing-api"),
                RuntimeConfiguration.empty()
        );
        serviceDefinitions.create(service);

        CreateServiceVolumeMountPort.CreateServiceVolumeMountResult result = volumeMounts.createVolumeMount(
                service.name(),
                new VolumeMount.BindMount(
                        new VolumeMount.HostPath("/opt/deployko/config"),
                        new VolumeMount.Target("/app/config"),
                        true
                )
        );

        assertThat(result).isInstanceOf(CreateServiceVolumeMountPort.CreateServiceVolumeMountResult.Created.class);
        assertThat(dsl.fetchExists(
                dsl
                        .selectOne()
                        .from(SERVICE_VOLUME_MOUNTS)
                        .where(SERVICE_VOLUME_MOUNTS.TARGET_PATH.eq("/app/config"))
                        .and(SERVICE_VOLUME_MOUNTS.MOUNT_TYPE.eq("BIND"))
                        .and(SERVICE_VOLUME_MOUNTS.SOURCE.eq("/opt/deployko/config"))
                        .and(SERVICE_VOLUME_MOUNTS.READ_ONLY.eq(true))
        )).isTrue();
    }

    @Test
    void create_volume_mount_returns_service_not_found_when_service_does_not_exist() {
        CreateServiceVolumeMountPort.CreateServiceVolumeMountResult result = volumeMounts.createVolumeMount(
                new ServiceName("missing-api"),
                new VolumeMount.BindMount(
                        new VolumeMount.HostPath("/opt/deployko/config"),
                        new VolumeMount.Target("/app/config"),
                        true
                )
        );

        assertThat(result)
                .isInstanceOf(CreateServiceVolumeMountPort.CreateServiceVolumeMountResult.ServiceNotFound.class);
        assertThat(dsl.fetchCount(SERVICE_VOLUME_MOUNTS)).isZero();
    }

    @Test
    void create_volume_mount_returns_already_exists_when_target_conflicts() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);

        CreateServiceVolumeMountPort.CreateServiceVolumeMountResult result = volumeMounts.createVolumeMount(
                service.name(),
                new VolumeMount.NamedVolumeMount(
                        new VolumeMount.VolumeName("other_config"),
                        new VolumeMount.Target("/app/config"),
                        false
                )
        );

        assertThat(result)
                .isInstanceOf(CreateServiceVolumeMountPort.CreateServiceVolumeMountResult.AlreadyExists.class);
        assertThat(dsl.fetchCount(SERVICE_VOLUME_MOUNTS)).isEqualTo(2);
    }

    @Test
    void update_volume_mount_updates_mount_when_service_and_mount_exist() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);

        UpdateServiceVolumeMountPort.UpdateServiceVolumeMountResult result = volumeMounts.updateVolumeMount(
                service.name(),
                new VolumeMount.NamedVolumeMount(
                        new VolumeMount.VolumeName("deployko_config"),
                        new VolumeMount.Target("/app/config"),
                        false
                )
        );

        assertThat(result).isInstanceOf(UpdateServiceVolumeMountPort.UpdateServiceVolumeMountResult.Updated.class);
        assertThat(dsl.fetchExists(
                dsl
                        .selectOne()
                        .from(SERVICE_VOLUME_MOUNTS)
                        .where(SERVICE_VOLUME_MOUNTS.TARGET_PATH.eq("/app/config"))
                        .and(SERVICE_VOLUME_MOUNTS.MOUNT_TYPE.eq("VOLUME"))
                        .and(SERVICE_VOLUME_MOUNTS.SOURCE.eq("deployko_config"))
                        .and(SERVICE_VOLUME_MOUNTS.READ_ONLY.eq(false))
        )).isTrue();
        assertThat(dsl.fetchCount(SERVICE_VOLUME_MOUNTS)).isEqualTo(2);
    }

    @Test
    void update_volume_mount_returns_service_not_found_when_service_does_not_exist() {
        UpdateServiceVolumeMountPort.UpdateServiceVolumeMountResult result = volumeMounts.updateVolumeMount(
                new ServiceName("missing-api"),
                new VolumeMount.NamedVolumeMount(
                        new VolumeMount.VolumeName("deployko_config"),
                        new VolumeMount.Target("/app/config"),
                        false
                )
        );

        assertThat(result)
                .isInstanceOf(UpdateServiceVolumeMountPort.UpdateServiceVolumeMountResult.ServiceNotFound.class);
    }

    @Test
    void update_volume_mount_returns_volume_mount_not_found_when_mount_does_not_exist() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);

        UpdateServiceVolumeMountPort.UpdateServiceVolumeMountResult result = volumeMounts.updateVolumeMount(
                service.name(),
                new VolumeMount.NamedVolumeMount(
                        new VolumeMount.VolumeName("deployko_logs"),
                        new VolumeMount.Target("/var/log/deployko"),
                        false
                )
        );

        assertThat(result)
                .isInstanceOf(UpdateServiceVolumeMountPort.UpdateServiceVolumeMountResult.VolumeMountNotFound.class);
        assertThat(dsl.fetchCount(SERVICE_VOLUME_MOUNTS)).isEqualTo(2);
    }

    @Test
    void delete_volume_mount_deletes_mount_when_service_and_mount_exist() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);

        DeleteServiceVolumeMountPort.DeleteServiceVolumeMountResult result = volumeMounts.deleteVolumeMount(
                service.name(),
                new VolumeMount.Target("/app/config")
        );

        assertThat(result).isInstanceOf(DeleteServiceVolumeMountPort.DeleteServiceVolumeMountResult.Deleted.class);
        assertThat(dsl.fetchCount(SERVICE_VOLUME_MOUNTS)).isEqualTo(1);
        assertThat(dsl.fetchExists(
                dsl
                        .selectOne()
                        .from(SERVICE_VOLUME_MOUNTS)
                        .where(SERVICE_VOLUME_MOUNTS.TARGET_PATH.eq("/app/config"))
        )).isFalse();
    }

    @Test
    void delete_volume_mount_returns_service_not_found_when_service_does_not_exist() {
        DeleteServiceVolumeMountPort.DeleteServiceVolumeMountResult result = volumeMounts.deleteVolumeMount(
                new ServiceName("missing-api"),
                new VolumeMount.Target("/app/config")
        );

        assertThat(result)
                .isInstanceOf(DeleteServiceVolumeMountPort.DeleteServiceVolumeMountResult.ServiceNotFound.class);
    }

    @Test
    void delete_volume_mount_returns_volume_mount_not_found_when_mount_does_not_exist() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);

        DeleteServiceVolumeMountPort.DeleteServiceVolumeMountResult result = volumeMounts.deleteVolumeMount(
                service.name(),
                new VolumeMount.Target("/var/log/deployko")
        );

        assertThat(result)
                .isInstanceOf(DeleteServiceVolumeMountPort.DeleteServiceVolumeMountResult.VolumeMountNotFound.class);
        assertThat(dsl.fetchCount(SERVICE_VOLUME_MOUNTS)).isEqualTo(2);
    }

    @Test
    void create_port_mapping_inserts_mapping_when_service_exists() {
        Service service = new Service(
                new ServiceName("billing-api"),
                new ImageRepository("registry.example.com/team/billing-api"),
                RuntimeConfiguration.empty()
        );
        serviceDefinitions.create(service);

        CreateServicePortMappingPort.CreateServicePortMappingResult result = portMappings.createPortMapping(
                service.name(),
                new Port(8080),
                new Port(80)
        );

        assertThat(result).isInstanceOf(CreateServicePortMappingPort.CreateServicePortMappingResult.Created.class);
        assertThat(dsl.fetchExists(
                dsl
                        .selectOne()
                        .from(SERVICE_PORT_MAPPINGS)
                        .where(SERVICE_PORT_MAPPINGS.HOST_PORT.eq(8080))
                        .and(SERVICE_PORT_MAPPINGS.HOST_PROTOCOL.eq("TCP"))
                        .and(SERVICE_PORT_MAPPINGS.CONTAINER_PORT.eq(80))
                        .and(SERVICE_PORT_MAPPINGS.CONTAINER_PROTOCOL.eq("TCP"))
        )).isTrue();
    }

    @Test
    void create_port_mapping_returns_service_not_found_when_service_does_not_exist() {
        CreateServicePortMappingPort.CreateServicePortMappingResult result = portMappings.createPortMapping(
                new ServiceName("missing-api"),
                new Port(8080),
                new Port(80)
        );

        assertThat(result)
                .isInstanceOf(CreateServicePortMappingPort.CreateServicePortMappingResult.ServiceNotFound.class);
        assertThat(dsl.fetchCount(SERVICE_PORT_MAPPINGS)).isZero();
    }

    @Test
    void create_port_mapping_returns_already_exists_when_host_port_conflicts() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);

        CreateServicePortMappingPort.CreateServicePortMappingResult result = portMappings.createPortMapping(
                service.name(),
                new Port(8080),
                new Port(81)
        );

        assertThat(result)
                .isInstanceOf(CreateServicePortMappingPort.CreateServicePortMappingResult.AlreadyExists.class);
        assertThat(dsl.fetchCount(SERVICE_PORT_MAPPINGS)).isEqualTo(2);
    }

    @Test
    void create_port_mapping_returns_already_exists_when_container_port_conflicts() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);

        CreateServicePortMappingPort.CreateServicePortMappingResult result = portMappings.createPortMapping(
                service.name(),
                new Port(8081),
                new Port(80)
        );

        assertThat(result)
                .isInstanceOf(CreateServicePortMappingPort.CreateServicePortMappingResult.AlreadyExists.class);
        assertThat(dsl.fetchCount(SERVICE_PORT_MAPPINGS)).isEqualTo(2);
    }

    @Test
    void delete_port_mapping_deletes_mapping_when_service_and_mapping_exist() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);

        DeleteServicePortMappingPort.DeleteServicePortMappingResult result = portMappings.deletePortMapping(
                service.name(),
                new Port(8080)
        );

        assertThat(result).isInstanceOf(DeleteServicePortMappingPort.DeleteServicePortMappingResult.Deleted.class);
        assertThat(dsl.fetchCount(SERVICE_PORT_MAPPINGS)).isEqualTo(1);
        assertThat(dsl.fetchExists(
                dsl
                        .selectOne()
                        .from(SERVICE_PORT_MAPPINGS)
                        .where(SERVICE_PORT_MAPPINGS.HOST_PORT.eq(8080))
                        .and(SERVICE_PORT_MAPPINGS.HOST_PROTOCOL.eq("TCP"))
        )).isFalse();
    }

    @Test
    void delete_port_mapping_returns_service_not_found_when_service_does_not_exist() {
        DeleteServicePortMappingPort.DeleteServicePortMappingResult result = portMappings.deletePortMapping(
                new ServiceName("missing-api"),
                new Port(8080)
        );

        assertThat(result)
                .isInstanceOf(DeleteServicePortMappingPort.DeleteServicePortMappingResult.ServiceNotFound.class);
    }

    @Test
    void delete_port_mapping_returns_port_mapping_not_found_when_mapping_does_not_exist() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);

        DeleteServicePortMappingPort.DeleteServicePortMappingResult result = portMappings.deletePortMapping(
                service.name(),
                new Port(8081)
        );

        assertThat(result)
                .isInstanceOf(DeleteServicePortMappingPort.DeleteServicePortMappingResult.PortMappingNotFound.class);
        assertThat(dsl.fetchCount(SERVICE_PORT_MAPPINGS)).isEqualTo(2);
    }

    @Test
    void find_network_attachments_returns_attachments_when_service_exists() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);

        FindServiceNetworkAttachmentsPort.FindServiceNetworkAttachmentsResult result =
                networkAttachments.findNetworkAttachments(service.name());

        assertThat(result)
                .isInstanceOf(FindServiceNetworkAttachmentsPort.FindServiceNetworkAttachmentsResult.Found.class);
        FindServiceNetworkAttachmentsPort.FindServiceNetworkAttachmentsResult.Found found =
                (FindServiceNetworkAttachmentsPort.FindServiceNetworkAttachmentsResult.Found) result;
        assertThat(found.networkAttachments()).isEqualTo(service.runtimeConfiguration().networkAttachments());
    }

    @Test
    void find_network_attachments_returns_service_not_found_when_service_does_not_exist() {
        FindServiceNetworkAttachmentsPort.FindServiceNetworkAttachmentsResult result =
                networkAttachments.findNetworkAttachments(new ServiceName("missing-api"));

        assertThat(result)
                .isInstanceOf(FindServiceNetworkAttachmentsPort.FindServiceNetworkAttachmentsResult.ServiceNotFound.class);
    }

    @Test
    void create_network_attachment_inserts_attachment_when_service_exists() {
        Service service = new Service(
                new ServiceName("billing-api"),
                new ImageRepository("registry.example.com/team/billing-api"),
                RuntimeConfiguration.empty()
        );
        serviceDefinitions.create(service);

        CreateServiceNetworkAttachmentPort.CreateServiceNetworkAttachmentResult result =
                networkAttachments.createNetworkAttachment(
                        service.name(),
                        new NetworkAttachment(new NetworkAttachment.NetworkName("deployko_backend"))
                );

        assertThat(result)
                .isInstanceOf(CreateServiceNetworkAttachmentPort.CreateServiceNetworkAttachmentResult.Created.class);
        assertThat(dsl.fetchExists(
                dsl
                        .selectOne()
                        .from(SERVICE_NETWORK_ATTACHMENTS)
                        .where(SERVICE_NETWORK_ATTACHMENTS.NETWORK_NAME.eq("deployko_backend"))
        )).isTrue();
    }

    @Test
    void create_network_attachment_returns_service_not_found_when_service_does_not_exist() {
        CreateServiceNetworkAttachmentPort.CreateServiceNetworkAttachmentResult result =
                networkAttachments.createNetworkAttachment(
                        new ServiceName("missing-api"),
                        new NetworkAttachment(new NetworkAttachment.NetworkName("deployko_backend"))
                );

        assertThat(result)
                .isInstanceOf(CreateServiceNetworkAttachmentPort.CreateServiceNetworkAttachmentResult.ServiceNotFound.class);
        assertThat(dsl.fetchCount(SERVICE_NETWORK_ATTACHMENTS)).isZero();
    }

    @Test
    void create_network_attachment_returns_already_exists_when_network_name_conflicts() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);

        CreateServiceNetworkAttachmentPort.CreateServiceNetworkAttachmentResult result =
                networkAttachments.createNetworkAttachment(
                        service.name(),
                        new NetworkAttachment(new NetworkAttachment.NetworkName("deployko_backend"))
                );

        assertThat(result)
                .isInstanceOf(CreateServiceNetworkAttachmentPort.CreateServiceNetworkAttachmentResult.AlreadyExists.class);
        assertThat(dsl.fetchCount(SERVICE_NETWORK_ATTACHMENTS)).isEqualTo(2);
    }

    @Test
    void delete_network_attachment_deletes_attachment_when_service_and_attachment_exist() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);

        DeleteServiceNetworkAttachmentPort.DeleteServiceNetworkAttachmentResult result =
                networkAttachments.deleteNetworkAttachment(
                        service.name(),
                        new NetworkAttachment.NetworkName("deployko_backend")
                );

        assertThat(result)
                .isInstanceOf(DeleteServiceNetworkAttachmentPort.DeleteServiceNetworkAttachmentResult.Deleted.class);
        assertThat(dsl.fetchCount(SERVICE_NETWORK_ATTACHMENTS)).isEqualTo(1);
        assertThat(dsl.fetchExists(
                dsl
                        .selectOne()
                        .from(SERVICE_NETWORK_ATTACHMENTS)
                        .where(SERVICE_NETWORK_ATTACHMENTS.NETWORK_NAME.eq("deployko_backend"))
        )).isFalse();
    }

    @Test
    void delete_network_attachment_returns_service_not_found_when_service_does_not_exist() {
        DeleteServiceNetworkAttachmentPort.DeleteServiceNetworkAttachmentResult result =
                networkAttachments.deleteNetworkAttachment(
                        new ServiceName("missing-api"),
                        new NetworkAttachment.NetworkName("deployko_backend")
                );

        assertThat(result)
                .isInstanceOf(DeleteServiceNetworkAttachmentPort.DeleteServiceNetworkAttachmentResult.ServiceNotFound.class);
    }

    @Test
    void delete_network_attachment_returns_attachment_not_found_when_attachment_does_not_exist() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);

        DeleteServiceNetworkAttachmentPort.DeleteServiceNetworkAttachmentResult result =
                networkAttachments.deleteNetworkAttachment(
                        service.name(),
                        new NetworkAttachment.NetworkName("missing_network")
                );

        assertThat(result)
                .isInstanceOf(DeleteServiceNetworkAttachmentPort.DeleteServiceNetworkAttachmentResult.NetworkAttachmentNotFound.class);
        assertThat(dsl.fetchCount(SERVICE_NETWORK_ATTACHMENTS)).isEqualTo(2);
    }

    @Test
    void upsert_persists_desired_deployment_with_runtime_configuration_snapshot() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);

        UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult result =
                desiredDeployments.upsert(desiredDeployment(service));

        assertThat(result).isInstanceOf(UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success.class);
        assertThat(dsl.fetchCount(SERVICE_DESIRED_DEPLOYMENTS)).isEqualTo(1);
        assertThat(dsl.fetchCount(SERVICE_DESIRED_DEPLOYMENT_ENVIRONMENT_VARIABLES)).isEqualTo(2);
        assertThat(dsl.fetchCount(SERVICE_DESIRED_DEPLOYMENT_PORT_MAPPINGS)).isEqualTo(2);
        assertThat(dsl.fetchCount(SERVICE_DESIRED_DEPLOYMENT_VOLUME_MOUNTS)).isEqualTo(2);
        assertThat(dsl.fetchCount(SERVICE_DESIRED_DEPLOYMENT_NETWORK_ATTACHMENTS)).isEqualTo(2);
        assertThat(dsl.fetchExists(
                dsl
                        .selectOne()
                        .from(SERVICE_DESIRED_DEPLOYMENTS)
                        .where(SERVICE_DESIRED_DEPLOYMENTS.IMAGE_VERSION.eq("1.0.0"))
                        .and(SERVICE_DESIRED_DEPLOYMENTS.DESIRED_STATE.eq("RUNNING"))
        )).isTrue();
        assertThat(dsl.fetchExists(
                dsl
                        .selectOne()
                        .from(SERVICE_DESIRED_DEPLOYMENT_ENVIRONMENT_VARIABLES)
                        .where(SERVICE_DESIRED_DEPLOYMENT_ENVIRONMENT_VARIABLES.KEY.eq("APP_ENV"))
                        .and(SERVICE_DESIRED_DEPLOYMENT_ENVIRONMENT_VARIABLES.VALUE.eq("prod"))
        )).isTrue();
    }

    @Test
    void upsert_replaces_existing_desired_deployment_snapshot() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);
        desiredDeployments.upsert(desiredDeployment(service));
        DesiredDeployment replacement = new DesiredDeployment(
                service.name(),
                service.imageRepository(),
                new ImageVersion("2.0.0"),
                RuntimeConfiguration.empty(),
                DesiredDeploymentState.RUNNING
        );

        UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult result = desiredDeployments.upsert(replacement);

        assertThat(result).isInstanceOf(UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.Success.class);
        assertThat(dsl.fetchCount(SERVICE_DESIRED_DEPLOYMENTS)).isEqualTo(1);
        assertThat(dsl.fetchValue(
                dsl
                        .select(SERVICE_DESIRED_DEPLOYMENTS.IMAGE_VERSION)
                        .from(SERVICE_DESIRED_DEPLOYMENTS)
        )).isEqualTo("2.0.0");
        assertThat(dsl.fetchCount(SERVICE_DESIRED_DEPLOYMENT_ENVIRONMENT_VARIABLES)).isZero();
        assertThat(dsl.fetchCount(SERVICE_DESIRED_DEPLOYMENT_PORT_MAPPINGS)).isZero();
        assertThat(dsl.fetchCount(SERVICE_DESIRED_DEPLOYMENT_VOLUME_MOUNTS)).isZero();
        assertThat(dsl.fetchCount(SERVICE_DESIRED_DEPLOYMENT_NETWORK_ATTACHMENTS)).isZero();
    }

    @Test
    void upsert_returns_service_not_found_when_service_does_not_exist() {
        DesiredDeployment desiredDeployment = new DesiredDeployment(
                new ServiceName("missing-api"),
                new ImageRepository("registry.example.com/team/missing-api"),
                new ImageVersion("1.0.0"),
                RuntimeConfiguration.empty(),
                DesiredDeploymentState.RUNNING
        );

        UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult result = desiredDeployments.upsert(desiredDeployment);

        assertThat(result)
                .isInstanceOf(UpsertDesiredDeploymentPort.UpsertDesiredDeploymentResult.ServiceNotFound.class);
        assertThat(dsl.fetchCount(SERVICE_DESIRED_DEPLOYMENTS)).isZero();
    }

    @Test
    void update_state_updates_existing_desired_deployment_state() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);
        desiredDeployments.upsert(desiredDeployment(service));

        UpdateDesiredDeploymentStatePort.UpdateDesiredDeploymentStateResult result =
                desiredDeployments.updateState(service.name(), DesiredDeploymentState.STOPPED);

        assertThat(result)
                .isInstanceOf(UpdateDesiredDeploymentStatePort.UpdateDesiredDeploymentStateResult.Success.class);
        assertThat(dsl.fetchValue(
                dsl
                        .select(SERVICE_DESIRED_DEPLOYMENTS.DESIRED_STATE)
                        .from(SERVICE_DESIRED_DEPLOYMENTS)
        )).isEqualTo("STOPPED");
    }

    @Test
    void update_state_returns_not_deployed_when_service_has_no_desired_deployment() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);

        UpdateDesiredDeploymentStatePort.UpdateDesiredDeploymentStateResult result =
                desiredDeployments.updateState(service.name(), DesiredDeploymentState.RUNNING);

        assertThat(result)
                .isInstanceOf(UpdateDesiredDeploymentStatePort.UpdateDesiredDeploymentStateResult.NotDeployed.class);
    }

    @Test
    void update_state_returns_service_not_found_when_service_does_not_exist() {
        UpdateDesiredDeploymentStatePort.UpdateDesiredDeploymentStateResult result =
                desiredDeployments.updateState(new ServiceName("missing-api"), DesiredDeploymentState.RUNNING);

        assertThat(result)
                .isInstanceOf(
                        UpdateDesiredDeploymentStatePort.UpdateDesiredDeploymentStateResult.ServiceNotFound.class
                );
    }

    @Test
    void find_desired_state_returns_existing_desired_deployment_state() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);
        desiredDeployments.upsert(desiredDeployment(service));

        FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult result =
                desiredDeployments.findDesiredState(service.name());

        assertThat(result).isEqualTo(new FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.Found(
                DesiredDeploymentState.RUNNING
        ));
    }

    @Test
    void find_desired_state_returns_not_deployed_when_service_has_no_desired_deployment() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);

        FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult result =
                desiredDeployments.findDesiredState(service.name());

        assertThat(result)
                .isInstanceOf(FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.NotDeployed.class);
    }

    @Test
    void find_desired_state_returns_service_not_found_when_service_does_not_exist() {
        FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult result =
                desiredDeployments.findDesiredState(new ServiceName("missing-api"));

        assertThat(result)
                .isInstanceOf(FindDesiredDeploymentStatePort.FindDesiredDeploymentStateResult.ServiceNotFound.class);
    }

    @Test
    void delete_desired_deployment_removes_snapshot() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);
        desiredDeployments.upsert(desiredDeployment(service));

        DeleteDesiredDeploymentPort.DeleteDesiredDeploymentResult result = desiredDeployments.delete(service.name());

        assertThat(result).isInstanceOf(DeleteDesiredDeploymentPort.DeleteDesiredDeploymentResult.Deleted.class);
        assertThat(dsl.fetchCount(SERVICE_DESIRED_DEPLOYMENTS)).isZero();
        assertThat(dsl.fetchCount(SERVICE_DESIRED_DEPLOYMENT_ENVIRONMENT_VARIABLES)).isZero();
        assertThat(dsl.fetchCount(SERVICE_DESIRED_DEPLOYMENT_PORT_MAPPINGS)).isZero();
        assertThat(dsl.fetchCount(SERVICE_DESIRED_DEPLOYMENT_VOLUME_MOUNTS)).isZero();
        assertThat(dsl.fetchCount(SERVICE_DESIRED_DEPLOYMENT_NETWORK_ATTACHMENTS)).isZero();
    }

    @Test
    void delete_desired_deployment_returns_not_deployed_when_service_has_no_desired_deployment() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);

        DeleteDesiredDeploymentPort.DeleteDesiredDeploymentResult result = desiredDeployments.delete(service.name());

        assertThat(result).isInstanceOf(DeleteDesiredDeploymentPort.DeleteDesiredDeploymentResult.NotDeployed.class);
    }

    @Test
    void delete_desired_deployment_returns_service_not_found_when_service_does_not_exist() {
        DeleteDesiredDeploymentPort.DeleteDesiredDeploymentResult result =
                desiredDeployments.delete(new ServiceName("missing-api"));

        assertThat(result).isInstanceOf(DeleteDesiredDeploymentPort.DeleteDesiredDeploymentResult.ServiceNotFound.class);
    }

    @Test
    void record_ci_deployment_upserts_last_deployed_timestamp() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);
        OffsetDateTime firstDeployment = OffsetDateTime.parse("2026-06-05T10:00:00Z");
        OffsetDateTime secondDeployment = OffsetDateTime.parse("2026-06-05T10:05:00Z");

        RecordCiDeploymentPort.RecordCiDeploymentResult firstResult =
                ciDeployments.recordCiDeployment(service.name(), firstDeployment);
        RecordCiDeploymentPort.RecordCiDeploymentResult secondResult =
                ciDeployments.recordCiDeployment(service.name(), secondDeployment);
        FindLastCiDeploymentPort.FindLastCiDeploymentResult findResult =
                ciDeployments.findLastCiDeployment(service.name());

        assertThat(firstResult).isInstanceOf(RecordCiDeploymentPort.RecordCiDeploymentResult.Recorded.class);
        assertThat(secondResult).isInstanceOf(RecordCiDeploymentPort.RecordCiDeploymentResult.Recorded.class);
        assertThat(findResult).isInstanceOf(FindLastCiDeploymentPort.FindLastCiDeploymentResult.Found.class);
        FindLastCiDeploymentPort.FindLastCiDeploymentResult.Found found =
                (FindLastCiDeploymentPort.FindLastCiDeploymentResult.Found) findResult;
        assertThat(found.deployedAt().toInstant()).isEqualTo(secondDeployment.toInstant());
        assertThat(dsl.fetchCount(SERVICE_CI_DEPLOYMENTS)).isEqualTo(1);
    }

    @Test
    void find_last_ci_deployment_returns_not_deployed_when_timestamp_does_not_exist() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);

        FindLastCiDeploymentPort.FindLastCiDeploymentResult result =
                ciDeployments.findLastCiDeployment(service.name());

        assertThat(result).isInstanceOf(FindLastCiDeploymentPort.FindLastCiDeploymentResult.NotDeployed.class);
    }

    @Test
    void ci_deployment_adapter_returns_service_not_found_when_service_does_not_exist() {
        ServiceName missingService = new ServiceName("missing-api");

        FindLastCiDeploymentPort.FindLastCiDeploymentResult findResult =
                ciDeployments.findLastCiDeployment(missingService);
        RecordCiDeploymentPort.RecordCiDeploymentResult recordResult =
                ciDeployments.recordCiDeployment(missingService, OffsetDateTime.parse("2026-06-05T10:00:00Z"));

        assertThat(findResult).isInstanceOf(FindLastCiDeploymentPort.FindLastCiDeploymentResult.ServiceNotFound.class);
        assertThat(recordResult).isInstanceOf(RecordCiDeploymentPort.RecordCiDeploymentResult.ServiceNotFound.class);
    }

    @Test
    void record_deployment_history_inserts_uuid_v7_attempt_for_service_and_version() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);

        RecordDeploymentHistoryPort.RecordDeploymentHistoryResult result =
                deploymentHistory.recordDeployment(service.name(), new ImageVersion("2.0.0"));

        assertThat(result).isInstanceOf(RecordDeploymentHistoryPort.RecordDeploymentHistoryResult.Recorded.class);
        assertThat(dsl.fetchCount(SERVICE_DEPLOYMENT_HISTORY)).isEqualTo(1);
        UUID id = dsl
                .select(SERVICE_DEPLOYMENT_HISTORY.ID)
                .from(SERVICE_DEPLOYMENT_HISTORY)
                .fetchSingle(SERVICE_DEPLOYMENT_HISTORY.ID);
        assertThat(id.version()).isEqualTo(7);
        assertThat(dsl.fetchExists(
                dsl
                        .selectOne()
                        .from(SERVICE_DEPLOYMENT_HISTORY)
                        .join(SERVICES)
                        .on(SERVICE_DEPLOYMENT_HISTORY.SERVICE_ID.eq(SERVICES.ID))
                        .where(SERVICES.NAME.eq("billing-api"))
                        .and(SERVICE_DEPLOYMENT_HISTORY.IMAGE_VERSION.eq("2.0.0"))
                        .and(SERVICE_DEPLOYMENT_HISTORY.RECORDED_AT.isNotNull())
        )).isTrue();
    }

    @Test
    void record_deployment_history_returns_service_not_found_when_service_does_not_exist() {
        RecordDeploymentHistoryPort.RecordDeploymentHistoryResult result =
                deploymentHistory.recordDeployment(new ServiceName("missing-api"), new ImageVersion("2.0.0"));

        assertThat(result)
                .isInstanceOf(RecordDeploymentHistoryPort.RecordDeploymentHistoryResult.ServiceNotFound.class);
        assertThat(dsl.fetchCount(SERVICE_DEPLOYMENT_HISTORY)).isZero();
    }

    @Test
    void delete_by_name_cascades_deployment_history() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);
        deploymentHistory.recordDeployment(service.name(), new ImageVersion("2.0.0"));

        DeleteServiceByNamePort.DeleteServiceByNameResult result = serviceDefinitions.deleteByName(service.name());

        assertThat(result).isInstanceOf(DeleteServiceByNamePort.DeleteServiceByNameResult.Deleted.class);
        assertThat(dsl.fetchCount(SERVICE_DEPLOYMENT_HISTORY)).isZero();
    }

    @Test
    void delete_by_name_deletes_service_and_runtime_configuration() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);

        DeleteServiceByNamePort.DeleteServiceByNameResult result = serviceDefinitions.deleteByName(service.name());

        assertThat(result).isInstanceOf(DeleteServiceByNamePort.DeleteServiceByNameResult.Deleted.class);
        assertThat(dsl.fetchCount(SERVICES)).isZero();
        assertThat(dsl.fetchCount(SERVICE_ENVIRONMENT_VARIABLES)).isZero();
        assertThat(dsl.fetchCount(SERVICE_PORT_MAPPINGS)).isZero();
        assertThat(dsl.fetchCount(SERVICE_VOLUME_MOUNTS)).isZero();
        assertThat(dsl.fetchCount(SERVICE_NETWORK_ATTACHMENTS)).isZero();
    }

    @Test
    void delete_by_name_returns_deployment_exists_when_service_has_desired_deployment() {
        Service service = serviceWithRuntimeConfiguration(new ServiceName("billing-api"));
        serviceDefinitions.create(service);
        desiredDeployments.upsert(desiredDeployment(service));

        DeleteServiceByNamePort.DeleteServiceByNameResult result = serviceDefinitions.deleteByName(service.name());

        assertThat(result).isInstanceOf(DeleteServiceByNamePort.DeleteServiceByNameResult.DeploymentExists.class);
        assertThat(dsl.fetchCount(SERVICES)).isEqualTo(1);
        assertThat(dsl.fetchCount(SERVICE_DESIRED_DEPLOYMENTS)).isEqualTo(1);
    }

    @Test
    void delete_by_name_returns_not_found_when_service_does_not_exist() {
        DeleteServiceByNamePort.DeleteServiceByNameResult result =
                serviceDefinitions.deleteByName(new ServiceName("missing-api"));

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
                                .add(
                                        new EnvironmentVariables.Key("JAVA_OPTS"),
                                        new EnvironmentVariables.Value("-Xmx512m")
                                ),
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

    private static DesiredDeployment desiredDeployment(Service service) {
        return new DesiredDeployment(
                service.name(),
                service.imageRepository(),
                new ImageVersion("1.0.0"),
                service.runtimeConfiguration(),
                DesiredDeploymentState.RUNNING
        );
    }
}
