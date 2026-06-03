package hr.tvz.popovic.deployko.configuration;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import hr.tvz.popovic.deployko.adapter.out.docker.DockerDeployContainerAdapter;
import hr.tvz.popovic.deployko.adapter.out.docker.DockerStartContainerAdapter;
import hr.tvz.popovic.deployko.adapter.out.docker.DockerStopContainerAdapter;
import hr.tvz.popovic.deployko.application.port.out.DeployContainerPort;
import hr.tvz.popovic.deployko.application.port.out.StartContainerPort;
import hr.tvz.popovic.deployko.application.port.out.StopContainerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DockerClientConfiguration {

    @Bean
    DockerClientConfig dockerClientConfig() {
        return DefaultDockerClientConfig.createDefaultConfigBuilder().build();
    }

    @Bean
    DockerHttpClient dockerHttpClient(DockerClientConfig dockerClientConfig) {
        return new ApacheDockerHttpClient.Builder()
                .dockerHost(dockerClientConfig.getDockerHost())
                .sslConfig(dockerClientConfig.getSSLConfig())
                .build();
    }

    @Bean
    DockerClient dockerClient(DockerClientConfig dockerClientConfig, DockerHttpClient dockerHttpClient) {
        return DockerClientImpl.getInstance(dockerClientConfig, dockerHttpClient);
    }

    @Bean
    DeployContainerPort deployContainerPort(DockerClient dockerClient) {
        return new DockerDeployContainerAdapter(dockerClient);
    }

    @Bean
    StartContainerPort startContainerPort(DockerClient dockerClient) {
        return new DockerStartContainerAdapter(dockerClient);
    }

    @Bean
    StopContainerPort stopContainerPort(DockerClient dockerClient) {
        return new DockerStopContainerAdapter(dockerClient);
    }
}
