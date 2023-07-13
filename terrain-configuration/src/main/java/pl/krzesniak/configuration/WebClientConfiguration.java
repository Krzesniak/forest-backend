package pl.krzesniak.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import pl.krzesniak.client.MeasurementClient;
import pl.krzesniak.client.SimulationClient;

@RequiredArgsConstructor
@Configuration
public class WebClientConfiguration {

    public static final int MAX_IN_MEMORY_SIZE = 52428800;
    public static final String SIMULATION_SERVICE_URL = "http://SIMULATION-SERVICE";
    public static final String MEASUREMENT_SERVICE_URL = "http://MEASUREMENT-SERVICE";
    private final LoadBalancedExchangeFilterFunction loadBalancedExchangeFilterFunction;
    @Bean
    public SimulationClient simulationClient() {
        WebClient webclient = WebClient.builder()
                .baseUrl(SIMULATION_SERVICE_URL)
                .filter(loadBalancedExchangeFilterFunction)
                .codecs(codecs -> codecs
                        .defaultCodecs()
                        .maxInMemorySize(MAX_IN_MEMORY_SIZE))
                .build();
        HttpServiceProxyFactory httpServiceProxyFactory = HttpServiceProxyFactory
                .builder(WebClientAdapter.forClient(webclient))
                .build();
        return httpServiceProxyFactory.createClient(SimulationClient.class);
    }

    @Bean
    public MeasurementClient measurementClient() {

        WebClient webclient = WebClient.builder()
                .baseUrl(MEASUREMENT_SERVICE_URL)
                .filter(loadBalancedExchangeFilterFunction)
                .codecs(codecs -> codecs
                        .defaultCodecs()
                        .maxInMemorySize(MAX_IN_MEMORY_SIZE))
                .build();
        HttpServiceProxyFactory httpServiceProxyFactory = HttpServiceProxyFactory
                .builder(WebClientAdapter.forClient(webclient))
                .build();
        return httpServiceProxyFactory.createClient(MeasurementClient.class);
    }

}
