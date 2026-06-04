package hr.tvz.popovic.deployko.adapter.out.registry;

import java.io.IOException;
import java.util.List;

interface RegistryImageVersionClient {

    List<String> tags(String imageRepository) throws IOException;
}
