package pl.krzesniak.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.krzesniak.dto.AgentResourcesRequest;
import pl.krzesniak.model.FireParameter;
import pl.krzesniak.model.ForestPixel;
import pl.krzesniak.model.agents.FireControllerAgent;
import pl.krzesniak.model.agents.FirefighterAgent;
import pl.krzesniak.model.enums.ForestFireState;
import pl.krzesniak.service.resources.FireResourceAllocator;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;
import static org.junit.jupiter.api.Assertions.*;
import static pl.krzesniak.model.enums.ForestFireState.*;

class AgentIterationTest {

    public static final int BOARD_SIZE = 30;

    ForestPixel[][] board = new ForestPixel[BOARD_SIZE][BOARD_SIZE];
    AgentCreator agentCreator = new AgentCreator();
    AgentDashboard agentDashboard = new AgentDashboard(agentCreator);

    ForestPixelHelper forestPixelHelper = new ForestPixelHelper(30, 30, 5, 3);

    TestableFieldsGrouperByLocation testableFieldsGrouperByLocation = new TestableFieldsGrouperByLocation(forestPixelHelper);

    FireGroupFinder fireGroupFinder = new FireGroupFinder(forestPixelHelper);

    FireResourceAllocator fireResourceAllocator = new FireResourceAllocator(agentDashboard);

    AgentFinder agentFinder = new AgentFinder(agentDashboard);

    AgentIteration agentIteration = new AgentIteration(forestPixelHelper, testableFieldsGrouperByLocation, fireGroupFinder, agentDashboard, fireResourceAllocator, agentFinder);

    @BeforeEach
    void setup() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = ForestPixel.builder().id(i + ":" + j).fireParameter(new FireParameter(false, false, 0, ForestFireState.NONE, "", 0)).build();
            }
        }
        AgentResourcesRequest agentResourcesRequest = new AgentResourcesRequest(board, 3, 5, 60);
        agentDashboard.locateAgents(agentResourcesRequest, new HashSet<>(), new HashSet<>());
        forestPixelHelper.setBoard(board);
    }

    @Test
    void extinguishFire_OneFireZone_NotEqualPriority_OneTime(){

        agentDashboard.setFirefighterAgents(limitFirefightersCount(15));

        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(0.51), "", 0.51));
        board[4][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.6), "", 1.6));
        board[4][6].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.6), "", 5.6));
        var burnedPixels1 = new HashSet<>(Arrays.asList(board[4][4], board[4][5], board[4][6]));

        agentIteration.extinguishFire(null, burnedPixels1);

        assertFalse(board[4][4].getFireParameter().isBeingExtinguished());
        assertTrue(board[4][5].getFireParameter().isBeingExtinguished());
        assertTrue(board[4][6].getFireParameter().isBeingExtinguished());
        
        assertEquals(0, getFireFightersCountAssignedToPixel("4:4"));
        assertEquals(1, getFireFightersCountAssignedToPixel("4:5"));
        assertEquals(6, getFireFightersCountAssignedToPixel("4:6"));

    }

    @Test
    void extinguishFire_OneFireZone_EqualPriority_OneTime(){

        agentDashboard.setFirefighterAgents(limitFirefightersCount(15));

        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 1.51));
        board[4][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 1.51));
        board[4][6].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 1.51));
        var burnedPixels1 = new HashSet<>(Arrays.asList(board[4][4], board[4][5], board[4][6]));

        agentIteration.extinguishFire(null, burnedPixels1);

        assertTrue(board[4][4].getFireParameter().isBeingExtinguished());
        assertTrue(board[4][5].getFireParameter().isBeingExtinguished());
        assertTrue(board[4][6].getFireParameter().isBeingExtinguished());

        assertEquals(2, getFireFightersCountAssignedToPixel("4:4"));
        assertEquals(2, getFireFightersCountAssignedToPixel("4:5"));
        assertEquals(2, getFireFightersCountAssignedToPixel("4:6"));

    }

    @Test
    void extinguishFire_TwoFireZone_EqualPriority_Enough_Resource_OneTime(){

        agentDashboard.setFirefighterAgents(limitFirefightersCount(15));

        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 1.51));
        board[4][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 1.51));
        board[4][6].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 1.51));
        var burnedPixels1 = new HashSet<>(Arrays.asList(board[4][4], board[4][5], board[4][6]));

        board[7][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(0.51), "", 0.51));
        board[7][8].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(0.6), "", 0.6));
        board[7][9].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.6), "", 5.6));
        var burnedPixels2 = new HashSet<>(Arrays.asList(board[7][7], board[7][8], board[7][9]));

        burnedPixels1.addAll(burnedPixels2);
        agentIteration.extinguishFire(null, burnedPixels1);

        assertTrue(board[4][4].getFireParameter().isBeingExtinguished());
        assertTrue(board[4][5].getFireParameter().isBeingExtinguished());
        assertTrue(board[4][6].getFireParameter().isBeingExtinguished());

        assertEquals(2, getFireFightersCountAssignedToPixel("4:4"));
        assertEquals(2, getFireFightersCountAssignedToPixel("4:5"));
        assertEquals(2, getFireFightersCountAssignedToPixel("4:6"));

    }

    @Test
    void extinguishFire_TwoFireZone_EqualPriority_Enough_ResourceFireFightersAndFireControllerAgents_OneTime(){

        agentDashboard.setFirefighterAgents(limitFirefightersCount(15));
        agentDashboard.setFireControllerAgents(limitFireControllerAgentsCount(1));

        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 1.51));
        board[4][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 1.51));
        board[4][6].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.51), "", 5.51));
        board[4][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.51), "", 5.51));
        board[3][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.51), "", 5.51));
        board[5][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.51), "", 5.51));
        var burnedPixels1 = new HashSet<>(Arrays.asList(board[4][4], board[4][5], board[4][6], board[4][7], board[3][5], board[5][5]));

        board[17][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(0.51), "", 0.51));
        board[17][8].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(0.6), "", 0.6));
        board[17][9].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.6), "", 5.6));
        var burnedPixels2 = new HashSet<>(Arrays.asList(board[17][7], board[17][8], board[17][9]));

        burnedPixels1.addAll(burnedPixels2);
        agentIteration.extinguishFire(null, burnedPixels1);

        assertFalse(board[4][4].getFireParameter().isBeingExtinguished()); // lack of priority  * fireFightersCount < 1
        assertFalse(board[4][5].getFireParameter().isBeingExtinguished());
        assertTrue(board[4][6].getFireParameter().isBeingExtinguished());
        assertTrue(board[3][5].getFireParameter().isBeingExtinguished());
        assertTrue(board[5][5].getFireParameter().isBeingExtinguished());
        assertFalse(board[17][7].getFireParameter().isBeingExtinguished());
        assertFalse(board[17][8].getFireParameter().isBeingExtinguished());
        assertFalse(board[17][9].getFireParameter().isBeingExtinguished());

        assertEquals(0, getFireFightersCountAssignedToPixel("4:4"));
        assertEquals(0, getFireFightersCountAssignedToPixel("4:5"));
        assertEquals(4, getFireFightersCountAssignedToPixel("4:6"));
        assertEquals(4, getFireFightersCountAssignedToPixel("4:7"));
        assertEquals(3, getFireFightersCountAssignedToPixel("3:5"));
        assertEquals(4, getFireFightersCountAssignedToPixel("5:5"));

    }

    @Test
    void extinguishFire_TwoFireZone_EqualPriority_WithOneElementNoBorder_Enough_Resource_OneTime(){

        agentDashboard.setFirefighterAgents(limitFirefightersCount(15));

        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 1.51));
        board[4][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 1.51));
        board[4][6].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 1.51));
        board[5][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.21), "", 1.21));
        board[3][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.51), "", 2.51));
        var burnedPixels1 = new HashSet<>(Arrays.asList(board[4][4], board[4][5], board[4][6], board[5][5], board[3][5]));

        agentIteration.extinguishFire(null, burnedPixels1);

        assertTrue(board[4][4].getFireParameter().isBeingExtinguished());
        assertFalse(board[4][5].getFireParameter().isBeingExtinguished());
        assertTrue(board[4][6].getFireParameter().isBeingExtinguished());
        assertTrue(board[5][5].getFireParameter().isBeingExtinguished());
        assertTrue(board[3][5].getFireParameter().isBeingExtinguished());

        assertEquals(2, getFireFightersCountAssignedToPixel("4:4"));
        assertEquals(0, getFireFightersCountAssignedToPixel("4:5"));
        assertEquals(3, getFireFightersCountAssignedToPixel("4:6"));
        assertEquals(4, getFireFightersCountAssignedToPixel("3:5"));
        assertEquals(1, getFireFightersCountAssignedToPixel("5:5"));

    }

    @Test
    void extinguishFire_TwoFireZone_EqualPriority_Enough_Resource_5Time(){


        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 1.51));
        board[4][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 1.51));
        board[4][6].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.22), "", 5.22));
        board[4][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(3.51), "", 3.51));
        board[3][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.51), "", 2.51));
        board[5][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.11), "", 1.11));
        var burnedPixels1 = new HashSet<>(Arrays.asList(board[4][4], board[4][5], board[4][6], board[4][7], board[3][5], board[5][5]));


        agentIteration.extinguishFire(null, burnedPixels1);

        assertTrue(board[4][4].getFireParameter().isBeingExtinguished());
        assertFalse(board[4][5].getFireParameter().isBeingExtinguished());
        assertTrue(board[4][6].getFireParameter().isBeingExtinguished());
        assertTrue(board[3][5].getFireParameter().isBeingExtinguished());
        assertTrue(board[5][5].getFireParameter().isBeingExtinguished());

        agentIteration.extinguishFire(null, burnedPixels1);
        agentIteration.extinguishFire(null, burnedPixels1);
        agentIteration.extinguishFire(null, burnedPixels1);
        agentIteration.extinguishFire(null, burnedPixels1);
        board[4][4].setFireParameter(new FireParameter(false, true, 0, convertToForestFireState(0), "", 0));

        agentIteration.extinguishFire(null, new HashSet<>());


        assertTrue(board[4][5].getFireParameter().isBeingExtinguished());
        assertTrue(board[4][6].getFireParameter().isBeingExtinguished());
        assertTrue(board[3][5].getFireParameter().isBeingExtinguished());
        assertTrue(board[5][5].getFireParameter().isBeingExtinguished());

        assertEquals(0, getFireFightersCountAssignedToPixel("4:4"));
        assertEquals(1, getFireFightersCountAssignedToPixel("4:5"));
        assertEquals(6, getFireFightersCountAssignedToPixel("4:6"));
        assertEquals(4, getFireFightersCountAssignedToPixel("4:7"));
        assertEquals(3, getFireFightersCountAssignedToPixel("3:5"));
        assertEquals(1, getFireFightersCountAssignedToPixel("5:5"));

    }

    @Test
    void extinguishFire_TwoFireZone_EqualPriority_NotEnough_Resource_ButOneLaterOnOneZoneIsNotBurning_5Time(){

        agentDashboard.setFirefighterAgents(limitFirefightersCount(15));

        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 1.51));
        board[4][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 1.51));
        board[4][6].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.22), "", 5.22));
        board[4][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(3.51), "", 3.51));
        board[3][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.51), "", 2.51));
        board[5][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.11), "", 1.11));
        var burnedPixels1 = new HashSet<>(Arrays.asList(board[4][4], board[4][5], board[4][6], board[4][7], board[3][5], board[5][5]));


        board[17][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(0.51), "", 0.51));
        board[17][8].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(3.6), "", 3.6));
        board[17][9].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.6), "", 5.6));
        var burnedPixels2 = new HashSet<>(Arrays.asList(board[17][7], board[17][8], board[17][9]));

        burnedPixels1.addAll(burnedPixels2);

        agentIteration.extinguishFire(null, burnedPixels1);

        assertTrue(board[4][4].getFireParameter().isBeingExtinguished());
        assertFalse(board[4][5].getFireParameter().isBeingExtinguished());
        assertTrue(board[4][6].getFireParameter().isBeingExtinguished());
        assertTrue(board[3][5].getFireParameter().isBeingExtinguished());
        assertFalse(board[5][5].getFireParameter().isBeingExtinguished());
        assertEquals(1, getFireFightersCountAssignedToPixel("4:4"));
        assertEquals(4, getFireFightersCountAssignedToPixel("4:6"));
        assertEquals(3, getFireFightersCountAssignedToPixel("4:7"));
        assertEquals(2, getFireFightersCountAssignedToPixel("3:5"));
        assertEquals(3, getFireFightersCountAssignedToPixel("17:9"));
        assertEquals(2, getFireFightersCountAssignedToPixel("17:8"));
        assertEquals(0, getFireFightersCountAssignedToPixel("4:5"));

        agentIteration.extinguishFire(null, burnedPixels1);
        agentIteration.extinguishFire(null, burnedPixels1);
        agentIteration.extinguishFire(null, burnedPixels1);
        agentIteration.extinguishFire(null, burnedPixels1);
        board[17][7].setFireParameter(new FireParameter(false, true, 0, convertToForestFireState(0), "", 0));
        board[17][8].setFireParameter(new FireParameter(false, true, 0, convertToForestFireState(0), "", 0));
        board[17][9].setFireParameter(new FireParameter(false, true, 0, convertToForestFireState(0), "", 0));
        board[4][4].setFireParameter(new FireParameter(false, true, 0, convertToForestFireState(0), "", 0));

        agentIteration.extinguishFire(null, new HashSet<>());


        assertTrue(board[4][5].getFireParameter().isBeingExtinguished());
        assertTrue(board[4][6].getFireParameter().isBeingExtinguished());
        assertTrue(board[3][5].getFireParameter().isBeingExtinguished());
        assertTrue(board[5][5].getFireParameter().isBeingExtinguished());

        assertEquals(0, getFireFightersCountAssignedToPixel("4:4"));
        assertEquals(1, getFireFightersCountAssignedToPixel("4:5"));
        assertEquals(5, getFireFightersCountAssignedToPixel("4:6"));
        assertEquals(4, getFireFightersCountAssignedToPixel("4:7"));
        assertEquals(2, getFireFightersCountAssignedToPixel("3:5"));
        assertEquals(1, getFireFightersCountAssignedToPixel("5:5"));

    }

    @Test
    void extinguishFire_TwoFireZone_IntoOneZone(){

        agentDashboard.setFirefighterAgents(limitFirefightersCount(15));

        board[0][0].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 1.51));
        board[0][1].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 1.51));
        board[0][2].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.22), "", 5.22));
        var burnedPixels1 = new HashSet<>(Arrays.asList(board[0][0], board[0][1], board[0][2]));


        board[2][0].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(0.51), "", 0.51));
        board[2][1].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(3.6), "", 3.6));
        board[2][2].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.6), "", 5.6));
        var burnedPixels2 = new HashSet<>(Arrays.asList(board[2][0], board[2][1], board[2][2]));

        burnedPixels1.addAll(burnedPixels2);

        agentIteration.extinguishFire(null, burnedPixels1);

        assertTrue(board[0][0].getFireParameter().isBeingExtinguished());
        assertTrue(board[0][1].getFireParameter().isBeingExtinguished());
        assertTrue(board[0][2].getFireParameter().isBeingExtinguished());
        assertFalse(board[2][0].getFireParameter().isBeingExtinguished());
        assertTrue(board[2][2].getFireParameter().isBeingExtinguished());
        assertEquals(1, getFireFightersCountAssignedToPixel("0:0"));
        assertEquals(5, getFireFightersCountAssignedToPixel("2:2"));
        assertEquals(3, getFireFightersCountAssignedToPixel("2:1"));
        assertEquals(5, getFireFightersCountAssignedToPixel("0:2"));
        assertEquals(1, getFireFightersCountAssignedToPixel("0:1"));

        agentIteration.extinguishFire(null, new HashSet<>());
        agentIteration.extinguishFire(null, new HashSet<>());
        agentIteration.extinguishFire(null, new HashSet<>());
        agentIteration.extinguishFire(null, new HashSet<>());

        burnedPixels1.clear();
        board[1][1].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.0), "", 2.0));
        burnedPixels1.add(board[1][1]);


        agentIteration.extinguishFire(null, burnedPixels1);


        assertTrue(board[0][0].getFireParameter().isBeingExtinguished());
        assertTrue(board[0][1].getFireParameter().isBeingExtinguished());
        assertTrue(board[0][2].getFireParameter().isBeingExtinguished());
        assertTrue(board[2][1].getFireParameter().isBeingExtinguished());
        assertTrue(board[2][2].getFireParameter().isBeingExtinguished());
        assertFalse(board[2][0].getFireParameter().isBeingExtinguished());
        assertTrue(board[1][1].getFireParameter().isBeingExtinguished());

        assertEquals(5, getFireFightersCountAssignedToPixel("2:2"));
        assertEquals(4, getFireFightersCountAssignedToPixel("0:2"));
        assertEquals(3, getFireFightersCountAssignedToPixel("2:1"));
        assertEquals(1, getFireFightersCountAssignedToPixel("0:0"));
        assertEquals(1, getFireFightersCountAssignedToPixel("0:1"));
        assertEquals(1, getFireFightersCountAssignedToPixel("1:1"));
        assertEquals(0, getNonBusyFirefighters());
        assertEquals(4, getNonBusyFireControllerAgents());
        assertEquals(4, getNonBusyFireControllerAgents());
        assertTrue( getFireControllerAgentsCountAssignedToZonePixel("0:0").isBusy());

    }

    @Test
    void extinguishFire_ThreeFireZone_IntoOneZone(){

        agentDashboard.setFirefighterAgents(limitFirefightersCount(25));

        board[0][0].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 1.51));
        board[0][1].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 1.51));
        board[0][2].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.22), "", 5.22));
        var burnedPixels1 = new HashSet<>(Arrays.asList(board[0][0], board[0][1], board[0][2]));


        board[2][0].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(0.51), "", 0.51));
        board[2][1].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(3.6), "", 3.6));
        board[2][2].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.6), "", 5.6));
        var burnedPixels2 = new HashSet<>(Arrays.asList(board[2][0], board[2][1], board[2][2]));

        burnedPixels1.addAll(burnedPixels2);

        board[4][0].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(0.51), "", 0.51));
        board[4][1].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(3.6), "", 3.6));
        board[4][2].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.6), "", 5.6));
        var burnedPixels3 = new HashSet<>(Arrays.asList(board[4][0], board[4][1], board[4][2]));

        burnedPixels1.addAll(burnedPixels3);

        agentIteration.extinguishFire(null, burnedPixels1);

        assertTrue(board[0][0].getFireParameter().isBeingExtinguished());
        assertTrue(board[0][1].getFireParameter().isBeingExtinguished());
        assertTrue(board[0][2].getFireParameter().isBeingExtinguished());
        assertTrue(board[2][1].getFireParameter().isBeingExtinguished());
        assertTrue(board[2][2].getFireParameter().isBeingExtinguished());
        assertTrue(board[4][2].getFireParameter().isBeingExtinguished());
        assertTrue(board[4][1].getFireParameter().isBeingExtinguished());
        assertEquals(2, getFireFightersCountAssignedToPixel("0:0"));
        assertEquals(5, getFireFightersCountAssignedToPixel("2:2"));
        assertEquals(3, getFireFightersCountAssignedToPixel("2:1")); // later center
        assertEquals(6, getFireFightersCountAssignedToPixel("0:2"));
        assertEquals(1, getFireFightersCountAssignedToPixel("0:1"));
        assertEquals(5, getFireFightersCountAssignedToPixel("4:2"));
        assertEquals(3, getFireFightersCountAssignedToPixel("4:1"));
        assertEquals(0, getNonBusyFirefighters());

        agentIteration.extinguishFire(null, new HashSet<>());
        agentIteration.extinguishFire(null, new HashSet<>());
        agentIteration.extinguishFire(null, new HashSet<>());
        agentIteration.extinguishFire(null, new HashSet<>());

        burnedPixels1.clear();
        board[1][1].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.0), "", 2.0));
        board[3][1].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.0), "", 2.0));
        burnedPixels1.add(board[1][1]);
        burnedPixels1.add(board[3][1]);


        agentIteration.extinguishFire(null, burnedPixels1);


        assertTrue(board[0][0].getFireParameter().isBeingExtinguished());
        assertTrue(board[0][1].getFireParameter().isBeingExtinguished());
        assertTrue(board[0][2].getFireParameter().isBeingExtinguished());
        assertTrue(board[2][1].getFireParameter().isBeingExtinguished()); //center
        assertTrue(board[2][2].getFireParameter().isBeingExtinguished());
        assertFalse(board[2][0].getFireParameter().isBeingExtinguished());
        assertTrue(board[1][1].getFireParameter().isBeingExtinguished());

        assertEquals(0, getFireFightersCountAssignedToPixel("2:1")); //center
        assertEquals(5, getFireFightersCountAssignedToPixel("0:2"));
        assertEquals(1, getFireFightersCountAssignedToPixel("0:1"));
        assertEquals(5, getFireFightersCountAssignedToPixel("4:2"));
        assertEquals(2, getFireFightersCountAssignedToPixel("3:1"));
        assertEquals(2, getFireFightersCountAssignedToPixel("1:1"));
        assertEquals(4, getFireFightersCountAssignedToPixel("4:1"));
        assertEquals(0, getNonBusyFirefighters());
        assertEquals(4, getNonBusyFireControllerAgents());
        assertEquals(4, getNonBusyFireControllerAgents());
        assertEquals(0, getNonBusyFirefighters());
        assertEquals(4, getNonBusyFireControllerAgents());
        assertTrue( getFireControllerAgentsCountAssignedToZonePixel("0:0").isBusy());

    }

    @Test
    void extinguishFire_5FireFireZone_TwoZonesCombinedOneLeft(){


        board[5][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 1.51));
        board[5][6].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.21), "", 2.21));
        board[5][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.22), "", 5.22));
        var burnedPixels1 = new HashSet<>(Arrays.asList(board[5][5], board[5][6], board[5][7]));


        board[7][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(0.51), "", 0.51));
        board[7][6].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(3.6), "", 3.6));
        board[7][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.6), "", 5.6));
        var burnedPixels2 = new HashSet<>(Arrays.asList(board[7][5], board[7][6], board[7][7]));

        burnedPixels1.addAll(burnedPixels2);

        board[10][8].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(0.51), "", 0.51));
        board[10][9].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(3.6), "", 3.6));
        var burnedPixels3 = new HashSet<>(Arrays.asList(board[10][8], board[10][9]));

        burnedPixels1.addAll(burnedPixels3);


        board[13][8].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 1.51));
        board[13][9].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(3.6), "", 3.6));
        var burnedPixels4= new HashSet<>(Arrays.asList(board[13][8], board[13][9]));

        burnedPixels1.addAll(burnedPixels4);

        board[7][12].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(0.51), "", 0.51));
        board[6][13].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(3.6), "", 3.6));
        board[7][13].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(4.3), "", 4.3));
        board[7][14].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(3.6), "", 3.6));
        board[8][13].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.5), "", 2.5));
        var burnedPixels5 = new HashSet<>(Arrays.asList(board[7][12], board[6][13], board[7][13], board[7][14], board[8][13]));

        burnedPixels1.addAll(burnedPixels5);

        agentIteration.extinguishFire(null, burnedPixels1);

        assertEquals(5, agentIteration.getFireResourceAllocator().getIdToFireInformation().keySet().size());

        assertTrue(board[5][7].getFireParameter().isBeingExtinguished());
        assertFalse(board[7][13].getFireParameter().isBeingExtinguished()); //center
        assertEquals(5, getFireFightersCountAssignedToPixel("7:14"));
        assertEquals(5, getFireFightersCountAssignedToPixel("6:13"));
        assertEquals(4, getFireFightersCountAssignedToPixel("10:9"));
        assertEquals(4, getFireFightersCountAssignedToPixel("13:9"));
        assertEquals(5, getFireFightersCountAssignedToPixel("7:7"));
        assertEquals(8, agentIteration.getFireResourceAllocator().getIdToFireInformation().get("5:5").get(0).getFirefightersCount());
        assertEquals(8, agentIteration.getFireResourceAllocator().getIdToFireInformation().get("7:6").get(0).getFirefightersCount());
        assertEquals(5, agentIteration.getFireResourceAllocator().getIdToFireInformation().get("13:8").get(0).getFirefightersCount());
        assertEquals(4, agentIteration.getFireResourceAllocator().getIdToFireInformation().get("10:8").get(0).getFirefightersCount());
        assertEquals(12, agentIteration.getFireResourceAllocator().getIdToFireInformation().get("8:13").get(0).getFirefightersCount());

        agentIteration.extinguishFire(null, new HashSet<>());
        agentIteration.extinguishFire(null, new HashSet<>());
        agentIteration.extinguishFire(null, new HashSet<>());
        agentIteration.extinguishFire(null, new HashSet<>());

        burnedPixels1.clear();
        board[6][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.0), "", 2.0));
        board[11][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.0), "", 2.0));
        board[12][8].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.0), "", 2.0));
        burnedPixels1.add(board[6][4]);
        burnedPixels1.add(board[11][7]);
        burnedPixels1.add(board[12][8]);


        agentIteration.extinguishFire(null, burnedPixels1);
        assertEquals(3, agentIteration.getFireResourceAllocator().getIdToFireInformation().keySet().size());


        assertEquals(18, agentIteration.getFireResourceAllocator().getIdToFireInformation().get("5:5").get(5).getFirefightersCount());
        assertEquals(13, agentIteration.getFireResourceAllocator().getIdToFireInformation().get("13:8").get(5).getFirefightersCount());
        assertEquals(12, agentIteration.getFireResourceAllocator().getIdToFireInformation().get("8:13").get(5).getFirefightersCount());
        assertEquals(2, getNonBusyFireControllerAgents());
        assertTrue( getFireControllerAgentsCountAssignedToZonePixel("5:5").isBusy());
        assertTrue( getFireControllerAgentsCountAssignedToZonePixel("13:8").isBusy());
        assertTrue( getFireControllerAgentsCountAssignedToZonePixel("8:13").isBusy());

    }

    @Test
    void extinguishFire_TwoFireZone_CombiningIntoOne_ALotOfFieldsBeingExtinguishedNoBurning_5Time(){

        board[4][4].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 1.51));
        board[4][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.51), "", 1.51));
        board[4][6].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.22), "", 5.22));
        board[4][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(3.51), "", 3.51));
        board[3][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(2.51), "", 2.51));
        board[5][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(1.11), "", 1.11));
        var burnedPixels1 = new HashSet<>(Arrays.asList(board[4][4], board[4][5], board[4][6], board[4][7], board[3][5], board[5][5]));


        board[7][5].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(0.51), "", 0.51));
        board[7][6].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(3.6), "", 3.6));
        board[7][7].setFireParameter(new FireParameter(true, false, 0, convertToForestFireState(5.6), "", 5.6));
        var burnedPixels2 = new HashSet<>(Arrays.asList(board[7][7], board[7][6], board[7][5]));

        burnedPixels1.addAll(burnedPixels2);

        agentIteration.extinguishFire(null, burnedPixels1);

        assertTrue(board[4][4].getFireParameter().isBeingExtinguished());
        assertFalse(board[4][5].getFireParameter().isBeingExtinguished());
        assertTrue(board[4][6].getFireParameter().isBeingExtinguished());
        assertTrue(board[3][5].getFireParameter().isBeingExtinguished());
        assertTrue(board[5][5].getFireParameter().isBeingExtinguished());

        assertEquals(2, agentIteration.getFireResourceAllocator().getIdToFireInformation().keySet().size());

        agentIteration.extinguishFire(null, new HashSet<>());
        agentIteration.extinguishFire(null, new HashSet<>());
        agentIteration.extinguishFire(null, new HashSet<>());
        agentIteration.extinguishFire(null, new HashSet<>());

        board[4][7].setFireParameter(new FireParameter(false, true, 0, convertToForestFireState(0), "", 0));
        board[3][5].setFireParameter(new FireParameter(false, true, 0, convertToForestFireState(0), "", 0));
        board[5][5].setFireParameter(new FireParameter(false, true, 0, convertToForestFireState(0), "", 0));
        board[6][5].setFireParameter(new FireParameter(false, true, 0, convertToForestFireState(0), "", 0));
        agentIteration.extinguishFire(null, new HashSet<>(Collections.singletonList(board[6][5])));

        assertEquals(1, agentIteration.getFireResourceAllocator().getIdToFireInformation().keySet().size());


        assertEquals(4, getNonBusyFireControllerAgents());

    }




    private long getFireFightersCountAssignedToPixel(String pixelId) {
        return agentDashboard.getFirefighterAgents().stream().filter(agent -> agent.getCurrentExtinguishPixelId().equals(pixelId)).count();
    }

    private FireControllerAgent getFireControllerAgentsCountAssignedToZonePixel(String pixelId) {
        return agentDashboard.getFireControllerAgents().stream().filter(agent -> agent.getFireZoneId().equals(pixelId)).findAny().orElseThrow(IllegalAccessError::new);
    }

    private List<FirefighterAgent> limitFirefightersCount(int i) {
        return agentDashboard.getFirefighterAgents().stream().limit(i).collect(Collectors.toList());
    }

    private List<FireControllerAgent> limitFireControllerAgentsCount(int i) {
        return agentDashboard.getFireControllerAgents().stream().limit(i).collect(Collectors.toList());
    }

    public ForestFireState convertToForestFireState(double fireSpeed) {
        if (fireSpeed == 0) return NONE;
        else if (fireSpeed <= 1.25) return LOW;
        else if (fireSpeed <= 2.5) return MEDIUM;
        else if (fireSpeed <= 5.0) return HIGH;
        else return EXTREME;
    }
    private long getNonBusyFirefighters() {
        return agentDashboard.getFirefighterAgents().stream().filter(not(FirefighterAgent::isBusy)).count();
    }

    private long getNonBusyFireControllerAgents() {
        return agentDashboard.getFireControllerAgents().stream().filter(not(FireControllerAgent::isBusy)).count();
    }

}
