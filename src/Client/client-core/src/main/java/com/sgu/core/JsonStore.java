package com.sgu.core;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;
/** Atomic file replacement; callers serialize writes via synchronized repository methods. Demo only. */
public final class JsonStore {
    private final Path file;
    public JsonStore(Path file) {
        this.file=file;
    }
    public List<Map<String,Object>> read(List<Map<String,Object>> seed)throws IOException {
        if(!Files.exists(file)) {
            write(seed);
            return new ArrayList<>(seed);
        }
        try {
            return new ArrayList<>(Data.list(Json.parse(Files.readString(file))));
        }
        catch(Exception e) {
            throw new IOException("Invalid demo store; data was NOT reset: "+file,e);
        }
    }
    public void write(List<Map<String,Object>> data)throws IOException {
        Files.createDirectories(file.getParent());
        Path temp=Files.createTempFile(file.getParent(),"store-",".tmp");
        try {
            Files.writeString(temp,Json.stringify(data),StandardCharsets.UTF_8);
            try {
                Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            }
            catch(AtomicMoveNotSupportedException e) {
                Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);
            }
        }
        finally {
            Files.deleteIfExists(temp);
        }
    }
    public static long next(List<Map<String,Object>> data) {
        return data.stream().mapToLong(m->Data.id(m,"id")).max().orElse(0)+1;
    }
}
