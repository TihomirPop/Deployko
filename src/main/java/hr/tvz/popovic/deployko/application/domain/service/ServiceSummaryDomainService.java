package hr.tvz.popovic.deployko.application.domain.service;

import hr.tvz.popovic.deployko.application.domain.model.ServiceSummary;
import hr.tvz.popovic.deployko.application.port.in.GetServiceSummariesUseCase.GetServiceSummariesResult;
import hr.tvz.popovic.deployko.application.port.out.FindServiceSummaryCandidatesPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ServiceSummaryDomainService {

    private final FindServiceSummaryCandidatesPort findServiceSummaryCandidatesPort;
    private final RuntimeStatusDomainService runtimeStatusDomainService;

    public ServiceSummaryDomainService(
            FindServiceSummaryCandidatesPort findServiceSummaryCandidatesPort,
            RuntimeStatusDomainService runtimeStatusDomainService
    ) {
        this.findServiceSummaryCandidatesPort = Objects.requireNonNull(
                findServiceSummaryCandidatesPort,
                "findServiceSummaryCandidatesPort must not be null"
        );
        this.runtimeStatusDomainService = Objects.requireNonNull(
                runtimeStatusDomainService,
                "runtimeStatusDomainService must not be null"
        );
    }

    public GetServiceSummariesResult getServiceSummaries() {
        return switch (findServiceSummaryCandidatesPort.findServiceSummaryCandidates()) {
            case FindServiceSummaryCandidatesPort.FindServiceSummaryCandidatesResult.Found found ->
                    serviceSummariesFrom(found.services());
            case FindServiceSummaryCandidatesPort.FindServiceSummaryCandidatesResult.Failure _ ->
                    new GetServiceSummariesResult.Failure();
        };
    }

    private GetServiceSummariesResult serviceSummariesFrom(
            List<FindServiceSummaryCandidatesPort.ServiceSummaryCandidate> candidates
    ) {
        List<ServiceSummary> serviceSummaries = new ArrayList<>();
        for (FindServiceSummaryCandidatesPort.ServiceSummaryCandidate candidate : candidates) {
            switch (runtimeStatusDomainService.findStatus(candidate.name(), candidate.desiredState())) {
                case RuntimeStatusDomainService.RuntimeStatusResult.Success success ->
                        serviceSummaries.add(new ServiceSummary(
                                candidate.name(),
                                candidate.imageRepository(),
                                candidate.deployedVersion(),
                                success.status()
                        ));
                case RuntimeStatusDomainService.RuntimeStatusResult.DockerFailure _ -> {
                    return new GetServiceSummariesResult.Failure();
                }
            }
        }
        return new GetServiceSummariesResult.Success(List.copyOf(serviceSummaries));
    }
}
