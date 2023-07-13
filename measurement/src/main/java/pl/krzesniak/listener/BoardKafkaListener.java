package pl.krzesniak.listener;


import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import pl.krzesniak.model.UniqueForestBoard;
import pl.krzesniak.service.AgentIteration;

@Service
@Log4j2
@RequiredArgsConstructor
public class BoardKafkaListener {

    private final AgentIteration agentIteration;

    @KafkaListener(topics = {"${board.topic.name}"})
    public void boardReader(UniqueForestBoard uniqueForestBoard) {
        agentIteration.agentIteration(uniqueForestBoard.board());
        log.info(uniqueForestBoard);
    }
}
