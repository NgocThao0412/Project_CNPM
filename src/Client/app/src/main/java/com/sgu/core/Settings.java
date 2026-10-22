package com.sgu.core;
import java.io.*;
import java.nio.file.*;
import java.util.*;
/** Environment overrides local.properties, which overrides config.properties. Never commits secrets. */
public final class Settings {
    private final Properties p=new Properties();
    private final Path home;
    public Settings()throws IOException {
        home=Path.of(System.getProperty("client.home",System.getenv().getOrDefault("CLIENT_HOME","."))).toAbsolutePath().normalize();
        for(String f:List.of("config.properties","local.properties")) {
            Path x=home.resolve(f);
            if(Files.isRegularFile(x))try(var in=Files.newBufferedReader(x)) {
                p.load(in);
            }
        }
    }
    public String get(String key,String def) {
        String env=System.getenv(key.toUpperCase(Locale.ROOT).replace('.','_').replace('-','_'));
        return env!=null?env:p.getProperty(key,def);
    }
    public int integer(String key,int def) {
        return Integer.parseInt(get(key,""+def));
    }
    public boolean demo() {
        return get("app.mode","demo").equals("demo");
    }
    public String mode() {
        return demo()?"demo":"live";
    }
    public Path home() {
        return home;
    }
    public Path data(String dir)throws IOException {
        Path x=home.resolve(get("data.dir","data")).resolve(mode()).resolve(dir).normalize();
        Files.createDirectories(x);
        return x;
    }
    public String auth() {
        return demo()?get("gateway.url","http://127.0.0.1:8080"):get("auth.url","http://127.0.0.1:8081");
    }
    public String poi() {
        return demo()?get("gateway.url","http://127.0.0.1:8080"):get("poi.url","http://127.0.0.1:8082");
    }
    public String gateway() {
        return get("gateway.url","http://127.0.0.1:8080");
    }
    public String narration() {
        return get("narration.url","http://127.0.0.1:8083");
    }
    public String audio() {
        return get("audio.url","http://127.0.0.1:8084");
    }
}
