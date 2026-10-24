package util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.springframework.web.multipart.MultipartFile;

public class InMemoryAudioFile implements MultipartFile {

    private final String filename;
    private final byte[] bytes;

    public InMemoryAudioFile(String filename, byte[] bytes) {
        this.filename = filename;
        this.bytes = bytes;
    }

    @Override
    public String getName() {
        return "audio";
    }

    @Override
    public String getOriginalFilename() {
        return filename;
    }

    @Override
    public String getContentType() {
        return "audio/mpeg";
    }

    @Override
    public boolean isEmpty() {
        return bytes.length == 0;
    }

    @Override
    public long getSize() {
        return bytes.length;
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public void transferTo(File destination) throws IOException {
        Files.write(destination.toPath(), bytes);
    }
}