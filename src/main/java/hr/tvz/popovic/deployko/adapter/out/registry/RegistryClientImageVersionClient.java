package hr.tvz.popovic.deployko.adapter.out.registry;

import io.github.ya_b.registry.client.RegistryClient;

import java.io.IOException;
import java.util.List;

final class RegistryClientImageVersionClient implements RegistryImageVersionClient {

    @Override
    public List<String> tags(String imageRepository) throws IOException {
        return RegistryClient.tags(imageRepository);
    }
}
