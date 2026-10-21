package com.sgu.client;

import java.nio.file.Path;
import java.util.List;
import com.sgu.client.Models.*;

/** Hop dong giua Client Java va cac Java Microservices. */
public interface ClientApi {
    boolean isDemo();
    Admin login(String username,String password) throws Exception;
    Admin me() throws Exception;
    void logout() throws Exception;
    List<Poi> pois() throws Exception;
    Poi savePoi(Poi poi) throws Exception;
    void visiblePoi(long id,boolean visible) throws Exception;
    List<Narration> narrations() throws Exception;
    Narration saveNarration(Narration n) throws Exception;
    void visibleNarration(long id, boolean visible) throws Exception;
    void uploadAudio(long narrationId, Path path) throws Exception;
    void listenEvent(long poiId,String lang,boolean simulated) throws Exception;
}
