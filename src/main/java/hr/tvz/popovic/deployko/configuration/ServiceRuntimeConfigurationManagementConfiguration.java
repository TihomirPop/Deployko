package hr.tvz.popovic.deployko.configuration;

import hr.tvz.popovic.deployko.application.domain.service.ServiceRuntimeConfigurationDomainService;
import hr.tvz.popovic.deployko.application.port.out.CreateServiceNetworkAttachmentPort;
import hr.tvz.popovic.deployko.application.port.out.CreateServiceEnvironmentVariablePort;
import hr.tvz.popovic.deployko.application.port.out.CreateServicePortMappingPort;
import hr.tvz.popovic.deployko.application.port.out.CreateServiceVolumeMountPort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServiceNetworkAttachmentPort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServiceEnvironmentVariablePort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServicePortMappingPort;
import hr.tvz.popovic.deployko.application.port.out.DeleteServiceVolumeMountPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceNetworkAttachmentsPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceEnvironmentVariablesPort;
import hr.tvz.popovic.deployko.application.port.out.FindServicePortMappingsPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceVolumeMountsPort;
import hr.tvz.popovic.deployko.application.port.out.UpdateServiceEnvironmentVariablePort;
import hr.tvz.popovic.deployko.application.port.out.UpdateServiceVolumeMountPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceRuntimeConfigurationManagementConfiguration {

    @Bean
    ServiceRuntimeConfigurationDomainService serviceRuntimeConfigurationDomainService(
            FindServiceEnvironmentVariablesPort findServiceEnvironmentVariablesPort,
            CreateServiceEnvironmentVariablePort createServiceEnvironmentVariablePort,
            UpdateServiceEnvironmentVariablePort updateServiceEnvironmentVariablePort,
            DeleteServiceEnvironmentVariablePort deleteServiceEnvironmentVariablePort,
            FindServicePortMappingsPort findServicePortMappingsPort,
            CreateServicePortMappingPort createServicePortMappingPort,
            DeleteServicePortMappingPort deleteServicePortMappingPort,
            FindServiceVolumeMountsPort findServiceVolumeMountsPort,
            CreateServiceVolumeMountPort createServiceVolumeMountPort,
            UpdateServiceVolumeMountPort updateServiceVolumeMountPort,
            DeleteServiceVolumeMountPort deleteServiceVolumeMountPort,
            FindServiceNetworkAttachmentsPort findServiceNetworkAttachmentsPort,
            CreateServiceNetworkAttachmentPort createServiceNetworkAttachmentPort,
            DeleteServiceNetworkAttachmentPort deleteServiceNetworkAttachmentPort
    ) {
        return new ServiceRuntimeConfigurationDomainService(
                findServiceEnvironmentVariablesPort,
                createServiceEnvironmentVariablePort,
                updateServiceEnvironmentVariablePort,
                deleteServiceEnvironmentVariablePort,
                findServicePortMappingsPort,
                createServicePortMappingPort,
                deleteServicePortMappingPort,
                findServiceVolumeMountsPort,
                createServiceVolumeMountPort,
                updateServiceVolumeMountPort,
                deleteServiceVolumeMountPort,
                findServiceNetworkAttachmentsPort,
                createServiceNetworkAttachmentPort,
                deleteServiceNetworkAttachmentPort
        );
    }

    @Bean
    @ConditionalOnMissingBean
    FindServiceEnvironmentVariablesPort findServiceEnvironmentVariablesPort() {
        return _ -> new FindServiceEnvironmentVariablesPort.FindServiceEnvironmentVariablesResult.Failure();
    }

    @Bean
    @ConditionalOnMissingBean
    CreateServiceEnvironmentVariablePort createServiceEnvironmentVariablePort() {
        return (_, _, _) -> new CreateServiceEnvironmentVariablePort.CreateServiceEnvironmentVariableResult.Failure();
    }

    @Bean
    @ConditionalOnMissingBean
    UpdateServiceEnvironmentVariablePort updateServiceEnvironmentVariablePort() {
        return (_, _, _) -> new UpdateServiceEnvironmentVariablePort.UpdateServiceEnvironmentVariableResult.Failure();
    }

    @Bean
    @ConditionalOnMissingBean
    DeleteServiceEnvironmentVariablePort deleteServiceEnvironmentVariablePort() {
        return (_, _) -> new DeleteServiceEnvironmentVariablePort.DeleteServiceEnvironmentVariableResult.Failure();
    }

    @Bean
    @ConditionalOnMissingBean
    FindServicePortMappingsPort findServicePortMappingsPort() {
        return _ -> new FindServicePortMappingsPort.FindServicePortMappingsResult.Failure();
    }

    @Bean
    @ConditionalOnMissingBean
    CreateServicePortMappingPort createServicePortMappingPort() {
        return (_, _, _) -> new CreateServicePortMappingPort.CreateServicePortMappingResult.Failure();
    }

    @Bean
    @ConditionalOnMissingBean
    DeleteServicePortMappingPort deleteServicePortMappingPort() {
        return (_, _) -> new DeleteServicePortMappingPort.DeleteServicePortMappingResult.Failure();
    }

    @Bean
    @ConditionalOnMissingBean
    FindServiceVolumeMountsPort findServiceVolumeMountsPort() {
        return _ -> new FindServiceVolumeMountsPort.FindServiceVolumeMountsResult.Failure();
    }

    @Bean
    @ConditionalOnMissingBean
    CreateServiceVolumeMountPort createServiceVolumeMountPort() {
        return (_, _) -> new CreateServiceVolumeMountPort.CreateServiceVolumeMountResult.Failure();
    }

    @Bean
    @ConditionalOnMissingBean
    UpdateServiceVolumeMountPort updateServiceVolumeMountPort() {
        return (_, _) -> new UpdateServiceVolumeMountPort.UpdateServiceVolumeMountResult.Failure();
    }

    @Bean
    @ConditionalOnMissingBean
    DeleteServiceVolumeMountPort deleteServiceVolumeMountPort() {
        return (_, _) -> new DeleteServiceVolumeMountPort.DeleteServiceVolumeMountResult.Failure();
    }

    @Bean
    @ConditionalOnMissingBean
    FindServiceNetworkAttachmentsPort findServiceNetworkAttachmentsPort() {
        return _ -> new FindServiceNetworkAttachmentsPort.FindServiceNetworkAttachmentsResult.Failure();
    }

    @Bean
    @ConditionalOnMissingBean
    CreateServiceNetworkAttachmentPort createServiceNetworkAttachmentPort() {
        return (_, _) -> new CreateServiceNetworkAttachmentPort.CreateServiceNetworkAttachmentResult.Failure();
    }

    @Bean
    @ConditionalOnMissingBean
    DeleteServiceNetworkAttachmentPort deleteServiceNetworkAttachmentPort() {
        return (_, _) -> new DeleteServiceNetworkAttachmentPort.DeleteServiceNetworkAttachmentResult.Failure();
    }
}
