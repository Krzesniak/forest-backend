package pl.krzesniak.dto;


import pl.krzesniak.model.ForestPixel;

public record AgentResourcesRequest(ForestPixel[][] board, long testerAgents, long fireControllerAgents,
                                    long firefighterAgents) {
}
