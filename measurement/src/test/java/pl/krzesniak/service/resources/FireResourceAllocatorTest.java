package pl.krzesniak.service.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.krzesniak.dto.AgentResourcesRequest;
import pl.krzesniak.model.FireParameter;
import pl.krzesniak.model.ForestPixel;
import pl.krzesniak.model.agents.FireControllerAgent;
import pl.krzesniak.model.agents.FirefighterAgent;
import pl.krzesniak.model.enums.ForestFireState;
import pl.krzesniak.service.AgentCreator;
import pl.krzesniak.service.AgentDashboard;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static pl.krzesniak.model.enums.ForestFireState.*;
import static pl.krzesniak.service.resources.AdditionalResourceNeeded.NO;
import static pl.krzesniak.service.resources.AdditionalResourceNeeded.YES;

class FireResourceAllocatorTest {
    public static final int BOARD_SIZE = 30;

    ForestPixel[][] board = new ForestPixel[BOARD_SIZE][BOARD_SIZE];
    AgentCreator agentCreator = new AgentCreator();
    AgentDashboard agentDashboard = new AgentDashboard(agentCreator);
    FireResourceAllocator fireResourceAllocator = new FireResourceAllocator(agentDashboard);

    @BeforeEach
    void setup() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = ForestPixel.builder().id(i + ":" + j).fireParameter(new FireParameter(false, false, 0, ForestFireState.NONE, "", 0)).build();
            }
        }
        AgentResourcesRequest agentResourcesRequest = new AgentResourcesRequest(board, 3, 5, 60);
        agentDashboard.locateAgents(agentResourcesRequest, new HashSet<>(), new HashSet<>());
    }

    @Test
    void computeFireAllocation_WhenOneFireZoneIsFoundWithEnoughResources() {
        //given
        int iteration = 0;
        double[] forestFireSpeed = {3.2, 0.98, 5.2, 1.6, 4.2, 2.3, 1.1, 2.3, 5.5};

        Set<ForestPixel> burnedPixels = new HashSet<>();
        for (int i = 4; i < 7; i++) {
            for (int j = 4; j < 7; j++) {
                board[i][j].setFireParameter(new FireParameter(true, false, 1, convertToForestFireState(forestFireSpeed[iteration]), "", forestFireSpeed[iteration]));
                burnedPixels.add(board[i][j]);
                iteration++;
            }
        }
        var map = Map.of("4:4", burnedPixels);
        //when
        Map<String, FireResourceMetadata> result = fireResourceAllocator.computeFireAllocation(map);
        assertEquals(22, result.get("4:4").getOptimalFireFighterCount());
        assertEquals(22, result.get("4:4").getFirefightersCount());
        assertEquals(22, getFireFighterAssignedToFireZoneId("4:4"));
        assertEquals(NO, result.get("4:4").getAdditionalResourceNeeded());
        assertEquals(getNonBusyFirefighters(), 38);
        assertEquals(getNonBusyFireControllerAgents(), 4);

    }

    @Test
    void computeFireAllocation_WhenTwoFireZoneIsFoundWithEnoughResources() {
        //given
        int iteration = 0;
        double[] forestFireSpeed = {3.2, 0.98, 5.2, 1.6, 4.2, 2.3, 1.1, 2.3, 5.5};

        Set<ForestPixel> burnedPixels = new HashSet<>();
        for (int i = 4; i < 7; i++) {
            for (int j = 4; j < 7; j++) {
                board[i][j].setFireParameter(new FireParameter(true, false, 1, convertToForestFireState(forestFireSpeed[iteration]), "", forestFireSpeed[iteration]));
                burnedPixels.add(board[i][j]);
                iteration++;
            }
        }

        iteration = 0;
        forestFireSpeed = new double[]{1.2, 0.98, 3.2, 2.6};

        Set<ForestPixel> burnedPixels2 = new HashSet<>();
        for (int i = 15; i < 17; i++) {
            for (int j = 15; j < 17; j++) {
                board[i][j].setFireParameter(new FireParameter(true, false, 1, convertToForestFireState(forestFireSpeed[iteration]), "", forestFireSpeed[iteration]));
                burnedPixels2.add(board[i][j]);
                iteration++;
            }
        }
        String firstId = "4:4";
        String secondId = "15:17";
        var map = Map.of(firstId, burnedPixels, secondId, burnedPixels2);
        //when
        Map<String, FireResourceMetadata> result = fireResourceAllocator.computeFireAllocation(map);
        assertEquals(22, result.get(firstId).getOptimalFireFighterCount());
        assertEquals(22, result.get(firstId).getFirefightersCount());
        assertEquals(22, getFireFighterAssignedToFireZoneId(firstId));
        assertEquals(NO, result.get(firstId).getAdditionalResourceNeeded());
        assertEquals(8, result.get(secondId).getOptimalFireFighterCount());
        assertEquals(8, result.get(secondId).getFirefightersCount());
        assertEquals(8, getFireFighterAssignedToFireZoneId(secondId));
        assertEquals(NO, result.get(secondId).getAdditionalResourceNeeded());
        assertEquals(30, getNonBusyFirefighters());
        assertEquals(3, getNonBusyFireControllerAgents());
    }

    @Test
    void computeFireAllocation_WhenOneFireZoneIsFoundWithNotEnoughResources() {
        //given
        int iteration = 0;
        double[] forestFireSpeed = {3.2, 5, 5.2, 5, 4.2, 2.3, 1.1, 5, 5.5, 5.2, 5.1, 5.3, 2.5, 5.1, 5.7, 3, 2, 5, 5, 5, 5, 5, 5, 5};

        Set<ForestPixel> burnedPixels = new HashSet<>();
        for (int i = 4; i < 9; i++) {
            for (int j = 4; j < 8; j++) {
                board[i][j].setFireParameter(new FireParameter(true, false, 1, convertToForestFireState(forestFireSpeed[iteration]), "", forestFireSpeed[iteration]));
                burnedPixels.add(board[i][j]);
                iteration++;
            }
        }
        var map = Map.of("4:4", burnedPixels);
        //when
        Map<String, FireResourceMetadata> result = fireResourceAllocator.computeFireAllocation(map);
        assertEquals(62, result.get("4:4").getOptimalFireFighterCount());
        assertEquals(60, result.get("4:4").getFirefightersCount());
        assertEquals(60, getFireFighterAssignedToFireZoneId("4:4"));
        assertEquals(AdditionalResourceNeeded.YES, result.get("4:4").getAdditionalResourceNeeded());
        assertEquals(0, getNonBusyFirefighters());
        assertEquals(4, getNonBusyFireControllerAgents());

    }

    @Test
    void computeFireAllocation_WhenDoubleFireZoneIsFoundWithNotEnoughResources() {
        //given
        int iteration = 0;
        double[] forestFireSpeed = {3.2, 5, 5.2, 5, 4.2, 2.3, 1.1, 5, 5.5, 5.2, 5.1, 5.3, 2.5, 5.1, 5.7, 3, 2, 5, 5, 5, 5, 5, 5, 5};

        Set<ForestPixel> burnedPixels = new HashSet<>();
        for (int i = 4; i < 9; i++) {
            for (int j = 4; j < 8; j++) {
                board[i][j].setFireParameter(new FireParameter(true, false, 1, convertToForestFireState(forestFireSpeed[iteration]), "", forestFireSpeed[iteration]));
                burnedPixels.add(board[i][j]);
                iteration++;
            }
        }

        iteration = 0;
        forestFireSpeed = new double[]{1.2, 0.98, 3.2, 2.6};

        Set<ForestPixel> burnedPixels2 = new HashSet<>();
        for (int i = 15; i < 17; i++) {
            for (int j = 15; j < 17; j++) {
                board[i][j].setFireParameter(new FireParameter(true, false, 1, convertToForestFireState(forestFireSpeed[iteration]), "", forestFireSpeed[iteration]));
                burnedPixels2.add(board[i][j]);
                iteration++;
            }
        }
        String firstId = "4:4";
        String secondId = "15:17";
        var map = Map.of(firstId, burnedPixels, secondId, burnedPixels2);
        //when
        Map<String, FireResourceMetadata> result = fireResourceAllocator.computeFireAllocation(map);
        assertEquals(result.get(firstId).getOptimalFireFighterCount(), 62);
        assertEquals(result.get(firstId).getFirefightersCount(), 55);
        assertEquals(result.get(firstId).getAdditionalResourceNeeded(), AdditionalResourceNeeded.YES);
        assertEquals(55, getFireFighterAssignedToFireZoneId(firstId));
        assertEquals(result.get(secondId).getOptimalFireFighterCount(), 8);
        assertEquals(result.get(secondId).getFirefightersCount(), 5);
        assertEquals(5, getFireFighterAssignedToFireZoneId(secondId));
        assertEquals(result.get(secondId).getAdditionalResourceNeeded(), AdditionalResourceNeeded.YES);
        assertEquals(0, getNonBusyFirefighters());
        assertEquals(3, getNonBusyFireControllerAgents());

    }


    @Test
    void computeFireAllocation_WhenThreeFireZoneIsFoundWithEnoughResources() {
        //given

        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.43), "", 2.43));
        board[4][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.6), "", 5.6));
        var burnedPixels1 = new HashSet<>(Arrays.asList(board[4][4], board[4][5]));

        board[7][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.4), "", 2.4));
        board[7][8].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.6), "", 5.6));
        var burnedPixels2 = new HashSet<>(Arrays.asList(board[7][7], board[7][8]));

        board[15][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.4), "", 2.4));
        board[15][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.6), "", 5.6));
        var burnedPixels3 = new HashSet<>(Arrays.asList(board[15][4], board[15][5]));

        String firstId = "4:4";
        String secondId = "15:17";
        String thirdId = "15:4";
        Map<String, Set<ForestPixel>> map = Map.of(firstId, burnedPixels1, secondId, burnedPixels2, thirdId, burnedPixels3);

        //when
        Map<String, FireResourceMetadata> result = fireResourceAllocator.computeFireAllocation(map);

        assertEquals(6, result.get(firstId).getOptimalFireFighterCount());
        assertEquals(6, result.get(firstId).getFirefightersCount());
        assertEquals(6, getFireFighterAssignedToFireZoneId(firstId));
        assertEquals(NO, result.get(firstId).getAdditionalResourceNeeded());
        assertEquals(6, result.get(secondId).getOptimalFireFighterCount());
        assertEquals(6, result.get(secondId).getFirefightersCount());
        assertEquals(6, getFireFighterAssignedToFireZoneId(secondId));
        assertEquals(NO, result.get(secondId).getAdditionalResourceNeeded());
        assertEquals(NO, result.get(thirdId).getAdditionalResourceNeeded());
        assertEquals(6, result.get(thirdId).getOptimalFireFighterCount());
        assertEquals(6, result.get(thirdId).getFirefightersCount());
        assertEquals(6, getFireFighterAssignedToFireZoneId(thirdId));
        assertEquals(42, getNonBusyFirefighters());
        assertEquals(2, getNonBusyFireControllerAgents());

    }

    @Test
    void computeFireAllocation_WhenThreeFireZoneIsFoundWithNotEnoughResources() {
        //given
        agentDashboard.setFirefighterAgents(limitFirefightersCount(20));

        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.51), "", 5.51));
        board[4][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.6), "", 5.6));
        var burnedPixels1 = new HashSet<>(Arrays.asList(board[4][4], board[4][5]));

        board[7][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.5), "", 5.5));
        board[7][8].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.6), "", 5.6));
        var burnedPixels2 = new HashSet<>(Arrays.asList(board[7][7], board[7][8]));

        board[15][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.5), "", 5.5));
        board[15][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.6), "", 5.6));
        var burnedPixels3 = new HashSet<>(Arrays.asList(board[15][4], board[15][5]));

        String firstId = "4:4";
        String secondId = "15:17";
        String thirdId = "15:4";
        Map<String, Set<ForestPixel>> map = Map.of(firstId, burnedPixels1, secondId, burnedPixels2, thirdId, burnedPixels3);

        //when
        Map<String, FireResourceMetadata> result = fireResourceAllocator.computeFireAllocation(map);

        assertEquals(8, result.get(firstId).getOptimalFireFighterCount());
        assertEquals(8, result.get(firstId).getFirefightersCount());
        assertEquals(8, getFireFighterAssignedToFireZoneId(firstId));
        assertEquals(NO, result.get(firstId).getAdditionalResourceNeeded());
        assertEquals(8, result.get(secondId).getOptimalFireFighterCount());
        assertEquals(6, result.get(secondId).getFirefightersCount());
        assertEquals(6, getFireFighterAssignedToFireZoneId(secondId));
        assertEquals(YES, result.get(secondId).getAdditionalResourceNeeded());
        assertEquals(YES, result.get(thirdId).getAdditionalResourceNeeded());
        assertEquals(8, result.get(thirdId).getOptimalFireFighterCount());
        assertEquals(6, result.get(thirdId).getFirefightersCount());
        assertEquals(6, getFireFighterAssignedToFireZoneId(thirdId));
        assertEquals(0, getNonBusyFirefighters());
        assertEquals(2, getNonBusyFireControllerAgents());

    }

    @Test
    void computeFireAllocation_WhenOneFireZoneIsFoundWithNotEnoughFireControllerAgents() {
        //given
        agentDashboard.setFireControllerAgents(limitFireControllerAgentsCount(0));

        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.51), "", 5.51));
        board[4][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.6), "", 5.6));
        var burnedPixels1 = new HashSet<>(Arrays.asList(board[4][4], board[4][5]));

        String firstId = "4:4";

        Map<String, Set<ForestPixel>> map = Map.of(firstId, burnedPixels1);

        //when
        Map<String, FireResourceMetadata> result = fireResourceAllocator.computeFireAllocation(map);

        assertNull(result.get(firstId));
        assertEquals(60, getNonBusyFirefighters());
        assertEquals(0, getFireFighterAssignedToFireZoneId(firstId));
        assertEquals(0, getNonBusyFireControllerAgents());

    }

    @Test
    void computeFireAllocation_WhenTWoFireZoneIsFoundWithOnlyOneFireControllerAgent_ThenGetResourceForTheMostDangerousZone() {
        //given
        agentDashboard.setFireControllerAgents(limitFireControllerAgentsCount(1));

        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 1.51));
        board[4][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.6), "", 1.6));
        var burnedPixels1 = new HashSet<>(Arrays.asList(board[4][4], board[4][5]));

        board[7][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.51), "", 5.51));
        board[7][8].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.6), "", 5.6));
        var burnedPixels2 = new HashSet<>(Arrays.asList(board[7][7], board[7][8]));

        String firstId = "4:4";
        String secondId = "7:7";

        Map<String, Set<ForestPixel>> map = Map.of(firstId, burnedPixels1, secondId, burnedPixels2);

        //when
        Map<String, FireResourceMetadata> result = fireResourceAllocator.computeFireAllocation(map);

        assertNull(result.get(firstId));
        assertEquals(8, result.get(secondId).getOptimalFireFighterCount());
        assertEquals(8, result.get(secondId).getFirefightersCount());
        assertEquals(8, getFireFighterAssignedToFireZoneId(secondId));
        assertEquals(NO, result.get(secondId).getAdditionalResourceNeeded());
        assertEquals(52, getNonBusyFirefighters());
        assertEquals(0, getNonBusyFireControllerAgents());

    }

    @Test
    void computeFireAllocation_WhenOneFireZoneIsFoundMultipleTimes() {
        //given
        agentDashboard.setFireControllerAgents(limitFireControllerAgentsCount(1));

        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(0.51), "", 0.51));
        board[4][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.6), "", 1.6));
        var burnedPixels1 = new HashSet<>(Arrays.asList(board[4][4], board[4][5]));

        String firstId = "4:4";


        Map<String, Set<ForestPixel>> map = Map.of(firstId, burnedPixels1);

        //when
        fireResourceAllocator.computeFireAllocation(map);
        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 1.51));
        fireResourceAllocator.computeFireAllocation(map);
        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.51), "", 2.51));
        fireResourceAllocator.computeFireAllocation(map);
        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.51), "", 5.51));
        fireResourceAllocator.computeFireAllocation(map);
        board[4][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.51), "", 2.51));

        var result = fireResourceAllocator.computeFireAllocation(map);

        assertEquals(7, result.get(firstId).getOptimalFireFighterCount());
        assertEquals(7, result.get(firstId).getFirefightersCount());
        assertEquals(7, getFireFighterAssignedToFireZoneId(firstId));
        assertEquals(NO, result.get(firstId).getAdditionalResourceNeeded());
        assertEquals(0, getNonBusyFireControllerAgents());
        assertEquals(53, getNonBusyFirefighters());


    }

    @Test
    void computeFireAllocation_WhenTwoFireZoneIsFoundMultipleTimes() {
        //given

        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(0.51), "", 0.51));
        board[4][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.6), "", 1.6));
        var burnedPixels1 = new HashSet<>(Arrays.asList(board[4][4], board[4][5]));

        board[7][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(0.51), "", 0.51));
        board[7][8].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(0.6), "", 0.6));
        var burnedPixels2 = new HashSet<>(Arrays.asList(board[7][7], board[7][8]));

        String firstId = "4:4";
        String secondId = "7:7";

        Map<String, Set<ForestPixel>> map = Map.of(firstId, burnedPixels1, secondId, burnedPixels2);

        //when
        fireResourceAllocator.computeFireAllocation(map);
        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 1.51));
        board[7][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 5.51));
        fireResourceAllocator.computeFireAllocation(map);
        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.51), "", 2.51));
        board[7][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.51), "", 5.51));

        fireResourceAllocator.computeFireAllocation(map);
        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.51), "", 5.51));
        board[7][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.51), "", 5.51));

        fireResourceAllocator.computeFireAllocation(map);
        board[4][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.51), "", 2.51));
        board[7][8].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 1.51));

        var result = fireResourceAllocator.computeFireAllocation(map);

        assertEquals(7, result.get(firstId).getOptimalFireFighterCount());
        assertEquals(7, result.get(firstId).getFirefightersCount());
        assertEquals(7, getFireFighterAssignedToFireZoneId(firstId));
        assertEquals(NO, result.get(firstId).getAdditionalResourceNeeded());
        assertEquals(6, result.get(secondId).getOptimalFireFighterCount());
        assertEquals(6, result.get(secondId).getFirefightersCount());
        assertEquals(6, getFireFighterAssignedToFireZoneId(secondId));
        assertEquals(NO, result.get(secondId).getAdditionalResourceNeeded());
        assertEquals(3, getNonBusyFireControllerAgents());
        assertEquals(47, getNonBusyFirefighters());


    }


    @Test
    void computeFireAllocation_WhenTwoFireZoneIsFoundMultipleTimes_NotEnoughFirefightersCount() {
        //given

        agentDashboard.setFirefighterAgents(limitFirefightersCount(10));

        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.51), "", 5.51));
        board[4][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.6), "", 5.6));
        var burnedPixels1 = new HashSet<>(Arrays.asList(board[4][4], board[4][5]));

        board[7][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.51), "", 2.51));
        board[7][8].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.6), "", 5.6));
        var burnedPixels2 = new HashSet<>(Arrays.asList(board[7][7], board[7][8]));

        String firstId = "4:4";
        String secondId = "7:7";

        Map<String, Set<ForestPixel>> map = Map.of(firstId, burnedPixels1, secondId, burnedPixels2);

        //when
        fireResourceAllocator.computeFireAllocation(map);
        fireResourceAllocator.computeFireAllocation(map);
        fireResourceAllocator.computeFireAllocation(map);
        fireResourceAllocator.computeFireAllocation(map);

        var result = fireResourceAllocator.computeFireAllocation(map);

        assertEquals(8, result.get(firstId).getOptimalFireFighterCount());
        assertEquals(6, result.get(firstId).getFirefightersCount());
        assertEquals(6, getFireFighterAssignedToFireZoneId(firstId));
        assertEquals(YES, result.get(firstId).getAdditionalResourceNeeded());
        assertEquals(7, result.get(secondId).getOptimalFireFighterCount());
        assertEquals(4, result.get(secondId).getFirefightersCount());
        assertEquals(4, getFireFighterAssignedToFireZoneId(secondId));
        assertEquals(YES, result.get(secondId).getAdditionalResourceNeeded());
        assertEquals(3, getNonBusyFireControllerAgents());
        assertEquals(0, getNonBusyFirefighters());


    }

    @Test
    void computeFireAllocation_WhenThreeFireZoneIsFoundMultipleTimes_NotEnoughFirefightersCount() {
        //given

        agentDashboard.setFirefighterAgents(limitFirefightersCount(20));

        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(0.51), "", 0.51));
        board[4][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(4.61), "", 4.61));
        var burnedPixels1 = new HashSet<>(Arrays.asList(board[4][4], board[4][5]));

        board[7][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(0.51), "", 0.51));
        board[7][8].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(4.6), "", 4.6));
        var burnedPixels2 = new HashSet<>(Arrays.asList(board[7][7], board[7][8]));

        board[15][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(0.5), "", 0.5));
        board[15][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(4.6), "", 4.6));
        var burnedPixels3 = new HashSet<>(Arrays.asList(board[15][4], board[15][5]));


        String firstId = "4:4";
        String secondId = "7:7";
        String thirdId = "15:4";

        Map<String, Set<ForestPixel>> map = Map.of(firstId, burnedPixels1, secondId, burnedPixels2, thirdId, burnedPixels3);

        //when
        fireResourceAllocator.computeFireAllocation(map);
        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.223), "", 2.223));
        board[7][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.22), "", 2.22));
        board[15][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.21), "", 2.21));
        fireResourceAllocator.computeFireAllocation(map);

        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.613), "", 2.613));
        board[7][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.61), "", 2.61));
        board[15][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.6), "", 2.6));
        fireResourceAllocator.computeFireAllocation(map);

        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.512), "", 5.512));
        board[7][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.51), "", 5.51));
        board[15][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.5), "", 5.5));
        fireResourceAllocator.computeFireAllocation(map);

        board[4][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.51), "", 5.51));
        board[7][8].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.51), "", 5.51));
        board[15][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.5), "", 5.5));
        var result = fireResourceAllocator.computeFireAllocation(map);

        assertEquals(8, result.get(firstId).getOptimalFireFighterCount());
        assertEquals(8, result.get(firstId).getFirefightersCount());
        assertEquals(8, getFireFighterAssignedToFireZoneId(firstId));
        assertEquals(NO, result.get(firstId).getAdditionalResourceNeeded());
        assertEquals(8, result.get(secondId).getOptimalFireFighterCount());
        assertEquals(6, result.get(secondId).getFirefightersCount());
        assertEquals(6, getFireFighterAssignedToFireZoneId(secondId));
        assertEquals(YES, result.get(secondId).getAdditionalResourceNeeded());
        assertEquals(8, result.get(thirdId).getOptimalFireFighterCount());
        assertEquals(6, result.get(thirdId).getFirefightersCount());
        assertEquals(6, getFireFighterAssignedToFireZoneId(thirdId));
        assertEquals(YES, result.get(thirdId).getAdditionalResourceNeeded());
        assertEquals(2, getNonBusyFireControllerAgents());
        assertEquals(0, getNonBusyFirefighters());


    }


    @Test
    void computeFireAllocation_WhenTwoFireZoneIsFoundMultipleTimes_NotFireControllerCountEnough() {
        //given
        agentDashboard.setFireControllerAgents(limitFireControllerAgentsCount(1));

        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(0.51), "", 0.51));
        board[4][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.6), "", 1.6));
        var burnedPixels1 = new HashSet<>(Arrays.asList(board[4][4], board[4][5]));

        board[7][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(0.51), "", 0.51));
        board[7][8].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(0.6), "", 0.6));
        var burnedPixels2 = new HashSet<>(Arrays.asList(board[7][7], board[7][8]));

        String firstId = "4:4";
        String secondId = "7:7";

        Map<String, Set<ForestPixel>> map = Map.of(firstId, burnedPixels1, secondId, burnedPixels2);

        //when
        fireResourceAllocator.computeFireAllocation(map);
        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 1.51));
        board[7][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 5.51));
        fireResourceAllocator.computeFireAllocation(map);
        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.51), "", 2.51));
        board[7][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.51), "", 5.51));

        fireResourceAllocator.computeFireAllocation(map);
        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.51), "", 5.51));
        board[7][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.51), "", 5.51));

        fireResourceAllocator.computeFireAllocation(map);
        board[4][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.51), "", 2.51));
        board[7][8].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.51), "", 5.51));

        var result = fireResourceAllocator.computeFireAllocation(map);

        assertEquals(7, result.get(firstId).getOptimalFireFighterCount());
        assertEquals(7, result.get(firstId).getFirefightersCount());
        assertEquals(7, getFireFighterAssignedToFireZoneId(firstId));
        assertEquals(NO, result.get(firstId).getAdditionalResourceNeeded());
        assertEquals(0, getNonBusyFireControllerAgents());
        assertEquals(53, getNonBusyFirefighters());


    }

    @Test
    void computeFireAllocation_WhenTwoFireZoneIsFoundMultipleTimes_NotFireControllerCountEnoughThenAllocateCauseOneHasFinishedJob() {
        //given
        agentDashboard.setFirefighterAgents(limitFirefightersCount(15));

        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(0.51), "", 0.51));
        board[4][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.6), "", 1.6));
        board[4][6].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.6), "", 5.6));
        var burnedPixels1 = new HashSet<>(Arrays.asList(board[4][4], board[4][5], board[4][6]));

        board[7][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(0.51), "", 0.51));
        board[7][8].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(0.6), "", 0.6));
        board[7][9].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.6), "", 5.6));
        var burnedPixels2 = new HashSet<>(Arrays.asList(board[7][7], board[7][8], board[7][9]));

        String firstId = "4:4";
        String secondId = "7:7";

        Map<String, Set<ForestPixel>> map = new HashMap<>(Map.of(firstId, burnedPixels1, secondId, burnedPixels2));

        //when
        fireResourceAllocator.computeFireAllocation(map);
        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 1.51));
        board[7][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 5.51));
        fireResourceAllocator.computeFireAllocation(map);
        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.51), "", 2.51));
        board[7][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.51), "", 5.51));

        fireResourceAllocator.computeFireAllocation(map);
        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.512), "", 5.512));
        board[7][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.51), "", 5.51));

        fireResourceAllocator.computeFireAllocation(map);
        board[4][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.51), "", 2.51));
        board[7][8].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.51), "", 5.51));

        var result = fireResourceAllocator.computeFireAllocation(map);

        assertEquals(11, result.get(firstId).getOptimalFireFighterCount());
        assertEquals(7, result.get(firstId).getFirefightersCount());
        assertEquals(7, getFireFighterAssignedToFireZoneId(firstId));
        assertEquals(YES, result.get(firstId).getAdditionalResourceNeeded());
        assertEquals(11, result.get(firstId).getOptimalFireFighterCount());
        assertEquals(8, result.get(secondId).getFirefightersCount());
        assertEquals(8, getFireFighterAssignedToFireZoneId(secondId));
        assertEquals(YES, result.get(firstId).getAdditionalResourceNeeded());
        assertEquals(3, getNonBusyFireControllerAgents());
        assertEquals(0, getNonBusyFirefighters());

        agentDashboard.getFirefighterAgents().stream().filter(FirefighterAgent::isBusy)
                .filter(firefighterAgent -> firefighterAgent.getFireZoneId().equals("7:7")).limit(7).forEach(firefighterAgent -> firefighterAgent.setBusy(false));

        map.remove("7:7");
        result = fireResourceAllocator.computeFireAllocation(map);
        assertEquals(11, result.get(firstId).getOptimalFireFighterCount());
        assertEquals(11, result.get(firstId).getFirefightersCount());
        assertEquals(11, getFireFighterAssignedToFireZoneId(firstId));

        assertEquals(NO, result.get(firstId).getAdditionalResourceNeeded());


    }

    private List<FirefighterAgent> limitFirefightersCount(int i) {
        return agentDashboard.getFirefighterAgents().stream().limit(i).collect(Collectors.toList());
    }

    private List<FireControllerAgent> limitFireControllerAgentsCount(int i) {
        return agentDashboard.getFireControllerAgents().stream().limit(i).collect(Collectors.toList());
    }


    private long getNonBusyFirefighters() {
        return agentDashboard.getFirefighterAgents().stream().filter(not(FirefighterAgent::isBusy)).count();
    }

    private long getNonBusyFireControllerAgents() {
        return agentDashboard.getFireControllerAgents().stream().filter(not(FireControllerAgent::isBusy)).count();
    }

    private long getFireFighterAssignedToFireZoneId(String zoneId) {
        return agentDashboard.getFirefighterAgents()
                .stream()
                .filter(firefighterAgent -> firefighterAgent.getFireZoneId().equals(zoneId))
                .count();
    }

    public ForestFireState convertToForestFireState(double fireSpeed) {
        if (fireSpeed == 0) return NONE;
        else if (fireSpeed <= 1.25) return LOW;
        else if (fireSpeed <= 2.5) return MEDIUM;
        else if (fireSpeed <= 5.0) return HIGH;
        else return EXTREME;
    }


}
