package pl.krzesniak.configuration;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.krzesniak.service.ForestPixelHelper;

@Configuration
public class BoardSizeReader {


    @Value("${board.width}")
    private int boardWidth;

    @Value("${board.height}")
    private int boardHeight;

    @Value("${board.window.size}")
    private int boardWindowSize;

    @Value("${board.window.testing.size}")
    private int boardWindowTestingSize;

    @Bean
    public ForestPixelHelper getForestPixelHelper() {
        return new ForestPixelHelper(boardWidth, boardHeight, boardWindowSize, boardWindowTestingSize);
    }

}
