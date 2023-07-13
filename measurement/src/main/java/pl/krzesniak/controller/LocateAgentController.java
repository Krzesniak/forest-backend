package pl.krzesniak.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pl.krzesniak.dto.AgentResourcesRequest;
import pl.krzesniak.model.ForestPixel;
import pl.krzesniak.service.AgentDashboard;
import pl.krzesniak.service.AgentLocator;

import static org.springframework.http.HttpStatus.OK;

@RestController
@RequiredArgsConstructor
public class LocateAgentController {

    private final AgentLocator agentLocator;
    @PostMapping("/agents/locate")
    ResponseEntity<ForestPixel[][]> locateAgents(@RequestBody AgentResourcesRequest agentResourcesRequest) {
        return new ResponseEntity<>(agentLocator.locateAgents(agentResourcesRequest), OK);
    }
}
