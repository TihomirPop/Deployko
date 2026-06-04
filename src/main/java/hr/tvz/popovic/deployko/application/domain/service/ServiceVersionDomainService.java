package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.domain.model.ImageRepository;
import hr.tvz.popovic.deployko.application.domain.model.Service;
import hr.tvz.popovic.deployko.application.port.in.GetServiceVersionsUseCase;
import hr.tvz.popovic.deployko.application.port.out.FindImageVersionsPort;
import hr.tvz.popovic.deployko.application.port.out.FindServiceDefinitionPort;

import java.util.Objects;

public final class ServiceVersionDomainService implements GetServiceVersionsUseCase {

    private final FindServiceDefinitionPort findServiceDefinitionPort;
    private final FindImageVersionsPort findImageVersionsPort;

    public ServiceVersionDomainService(
            FindServiceDefinitionPort findServiceDefinitionPort,
            FindImageVersionsPort findImageVersionsPort
    ) {
        this.findServiceDefinitionPort = Objects.requireNonNull(
                findServiceDefinitionPort,
                "findServiceDefinitionPort must not be null"
        );
        this.findImageVersionsPort = Objects.requireNonNull(
                findImageVersionsPort,
                "findImageVersionsPort must not be null"
        );
    }

    @Override
    public GetServiceVersionsResult getServiceVersions(GetServiceVersionsCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return switch (findServiceDefinitionPort.findByName(command.serviceName())) {
            case FindServiceDefinitionPort.FindServiceDefinitionResult.Found(
                    Service(_, ImageRepository imageRepository, _)
            ) -> findImageVersions(imageRepository);
            case FindServiceDefinitionPort.FindServiceDefinitionResult.NotFound _ ->
                    new GetServiceVersionsResult.NotFound();
            case FindServiceDefinitionPort.FindServiceDefinitionResult.Failure _ ->
                    new GetServiceVersionsResult.Failure();
        };
    }

    private GetServiceVersionsResult findImageVersions(ImageRepository imageRepository) {
        return switch (findImageVersionsPort.findImageVersions(imageRepository)) {
            case FindImageVersionsPort.FindImageVersionsResult.Found found ->
                    new GetServiceVersionsResult.Success(found.imageVersions());
            case FindImageVersionsPort.FindImageVersionsResult.Failure _ -> new GetServiceVersionsResult.Failure();
        };
    }
}
