package pl.krzesniak.client;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import pl.krzesniak.model.ForestPixel;

import java.util.UUID;

@HttpExchange
public interface SimulationClient {

    @PostExchange("/simulations/boards")
    UUID sendBoard(@RequestBody ForestPixel[][] board);
}
