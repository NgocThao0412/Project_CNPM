package com.sgu.client;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/** Phan tan cac service Java tai Gateway hoac endpoint rieng. */
public final class ClientConfig {
    private final Properties props = new Properties();
    public ClientConfig(Path file) throws IOException {
        if(Files.exists(file)) try(InputStream in=Files.newInputStream(file)){props.load(in);}
    }
    public String get(String key,String fallback) {
        String sys=System.getProperty("client."+key);if(sys!=null&&!sys.isBlank())return sys;
        String env=System.getenv("CLIENT_"+key.toUpperCase().replace('.','_'));if(env!=null&&!env.isBlank())return env;
        return props.getProperty(key,fallback).trim();
    }
    public boolean enabled(String key){return Boolean.parseBoolean(get("feature."+key,"false"));}
    public String endpoint(String service) {return trimSlash(get("service."+service,get("service.gateway","http://localhost:8080")));}
    private static String trimSlash(String s){return s.replaceAll("/+$","");}
}
