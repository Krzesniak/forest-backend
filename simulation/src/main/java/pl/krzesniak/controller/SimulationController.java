package pl.krzesniak.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import pl.krzesniak.model.ForestPixel;
import pl.krzesniak.model.UniqueForestBoard;
import pl.krzesniak.service.ForestFireSimulator;

import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequiredArgsConstructor
@RequestMapping("/simulations")
public class SimulationController {

    private final ForestFireSimulator forestFireSimulator;
    private final KafkaTemplate<String, UniqueForestBoard> kafkaTemplate;

    @Value("${board.topic.name}")
    String uniqueBoardTopic;

    @GetMapping("/boards")
    public ResponseEntity<ForestPixel[][]> getBoard() {
        return new ResponseEntity<>(forestFireSimulator.getBoard(), OK);
    }
    @PostMapping("/boards")
    public ResponseEntity<UUID> setBoard(@RequestBody ForestPixel[][] board) {
        forestFireSimulator.setBoard(board);
        return new ResponseEntity<>(UUID.randomUUID(), CREATED);
    }

    @PostMapping("/start")
    public ResponseEntity<Boolean> startSimulation() {
        forestFireSimulator.start();
        return new ResponseEntity<>(Boolean.TRUE, OK);
    }

    @PostMapping("/stop")
    public ResponseEntity<Boolean> stopSimulation() {
        forestFireSimulator.stop();
        return new ResponseEntity<>(Boolean.TRUE, OK);
    }

    @GetMapping("/pixels/{id}")
    public ResponseEntity<ForestPixel> getForestPixelById(@PathVariable String id) {
        return new ResponseEntity<>(forestFireSimulator.getPixelById(id), OK);
    }

    @PostMapping("/kafka")
    public void sendMessage() {
        var send = new UniqueForestBoard(UUID.randomUUID().toString(), forestFireSimulator.getBoard());
        kafkaTemplate.send(uniqueBoardTopic, send);
    }
}
