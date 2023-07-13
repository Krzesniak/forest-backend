package pl.krzesniak.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pl.krzesniak.model.ForestPixel;
import pl.krzesniak.service.exception.ParseReadConfigurationException;

import java.io.IOException;

@RequiredArgsConstructor
@Service
public class BoardConfigurationReader {

    private final ObjectMapper objectMapper;

    public ForestPixel[][] loadConfigurationFromFile(MultipartFile file) {
        try {
            return objectMapper.readValue(file.getInputStream(), ForestPixel[][].class);
        } catch (IOException e) {
            throw new ParseReadConfigurationException("Error with handling file: " + e.getMessage());
        }
    }
}
