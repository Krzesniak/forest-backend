package pl.krzesniak.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import pl.krzesniak.model.ForestPixel;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
@Data
@Log4j2
public class ForestFireSimulator {

    private final ForestPixelHelper forestPixelHelper;
    private final FireCalculator fireCalculator;
    private final PixelStartingBurningManager startingBurningManager;

    private ForestPixel[][] board;
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(4);

    public void start() {
        if (scheduledExecutorService.isShutdown()) scheduledExecutorService = Executors.newScheduledThreadPool(4);
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            log.info("XD");
            runFireForestIteration();
            //TODO call to mesearement service agentDashboard.agentIteration(board);
        }, 0, 4, TimeUnit.SECONDS);
    }

    public void stop() {
        this.scheduledExecutorService.shutdown();

        //TODO notify measurement service
    }

    public void runFireForestIteration() {
        var copyBoard = BoardCopier.createCopyOfBoard(board);
        forestPixelHelper.setBoard(board);
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                var pixel = board[i][j];
                List<ForestPixel> neighbours = forestPixelHelper.createTestingSurroundingsForPixel(pixel.getId());
                if (!pixel.isBeingBurned() && startingBurningManager.isPixelStartingBurning(pixel, neighbours)) {
                    startingBurningManager.setPixelValueToBeBurned(copyBoard[i][j]);
                } else if (pixel.isBeingBurned() && pixel.isNotDestroyed()) {
                    double firePixelDamage = fireCalculator.calculateFirePixelDamage(copyBoard[i][j], neighbours);
                    fireCalculator.updatePixelBurning(copyBoard[i][j], firePixelDamage);
                }
            }
        }
        this.board = copyBoard;
    }

    public ForestPixel getPixelById(String id) {
        Integer[] idRowAndColumn = Arrays.stream(id.split(":")).map(Integer::parseInt).toArray(Integer[]::new);
        return this.board[idRowAndColumn[0]][idRowAndColumn[1]];
    }


}

