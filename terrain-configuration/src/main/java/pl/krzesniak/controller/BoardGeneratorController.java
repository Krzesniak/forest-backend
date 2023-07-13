package pl.krzesniak.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.krzesniak.model.ForestPixel;
import pl.krzesniak.service.BoardConfigurationReader;
import pl.krzesniak.service.BoardTemporaryHolder;
import pl.krzesniak.dto.*;


import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;


@RestController
@RequiredArgsConstructor
@RequestMapping("/configurations/maps/")
public class BoardGeneratorController {

    private final BoardTemporaryHolder boardTemporaryHolder;
    private final BoardConfigurationReader boardConfigurationReader;

    @GetMapping("temp-game-view")
    public ForestPixel[][] getGameView() {
        return boardTemporaryHolder.getBoard();
    }

    @PostMapping("/terrains")
    public ResponseEntity<ForestPixel[][]> createBoard(@RequestBody TerrainGeneratorRequest terrainGeneratorRequest) {
        var board = boardTemporaryHolder.createTerrain(terrainGeneratorRequest);
        return new ResponseEntity<>(board, HttpStatus.CREATED);
    }

    @PostMapping("/fire-index")
    public ResponseEntity<ForestPixel[][]> createForestPixel(@RequestBody ForestPixelRequest forestPixelRequest) {
        var board = boardTemporaryHolder.applyForestFireIndex(forestPixelRequest);
        return new ResponseEntity<>(board, HttpStatus.CREATED);
    }

    @PutMapping("/agents")
    public ResponseEntity<ForestPixel[][]> locateAgents(@RequestBody AgentResourcesRequest agentResourcesRequest) {
        return new ResponseEntity<>(boardTemporaryHolder.locateAgents(agentResourcesRequest), OK);
    }

    @PutMapping("/fires")
    public ResponseEntity<ForestPixel[][]> addFire(@RequestBody ForestPixel[][] forestPixels) {
        return new ResponseEntity<>(boardTemporaryHolder.addFires(forestPixels), OK);
    }

    @PostMapping("/generate")
    public ResponseEntity<Void> generateBoard(){
        boardTemporaryHolder.exchangeBoard();
        return new ResponseEntity<>(OK);
    }

    @PostMapping("/load")
    public ResponseEntity<ForestPixel[][]> loadConfiguration(@RequestParam("file")MultipartFile file) {
        ForestPixel[][] board = boardConfigurationReader.loadConfigurationFromFile(file);
        boardTemporaryHolder.setBoard(board);
        return new ResponseEntity<>(board, CREATED);
    }

}
