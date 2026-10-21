package com.sgu.client.DAO;

import java.nio.file.Path;
import java.util.List;
import com.sgu.client.DTO.*;

/** Hop dong giua Client Java va cac Java Microservices. */
public interface ClientDAO {
    boolean isDemo();
    AdminDTO login(String username,String password) throws Exception;
    AdminDTO me() throws Exception;
    void logout() throws Exception;
    List<POIDTO> pois() throws Exception;
    POIDTO savePoi(POIDTO poi) throws Exception;
    void visiblePoi(long id,boolean visible) throws Exception;
    List<NarrationDTO> narrations() throws Exception;
    NarrationDTO saveNarration(NarrationDTO n) throws Exception;
    void visibleNarration(long id, boolean visible) throws Exception;
    void uploadAudio(long narrationId, Path path) throws Exception;
    default void uploadAudio(long narrationId,Path path,double durationSeconds)throws Exception{
        uploadAudio(narrationId,path);
    }
    void listenEvent(long poiId,String lang,boolean simulated) throws Exception;
}
