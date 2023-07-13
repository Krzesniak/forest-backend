package pl.krzesniak.client;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import pl.krzesniak.dto.AgentResourcesRequest;
import pl.krzesniak.model.ForestPixel;

@HttpExchange
public interface MeasurementClient {

    @PostExchange("/agents/locate")
    ForestPixel[][]  locateAgents(@RequestBody AgentResourcesRequest agentResourcesRequest);
}
