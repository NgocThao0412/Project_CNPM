package com.sgu.client.controller;

import com.sgu.client.BUS.ClientBUS;
import com.sgu.client.DAO.*;
import com.sgu.client.DTO.*;
import com.sgu.client.config.ClientConfig;
import java.nio.file.Path;
import java.util.List;

/** Controller trung gian giua giao dien Swing va BUS. */
public final class ClientController {
    private final ClientConfig config;
    private ClientBUS bus;
    public ClientController(ClientConfig config) {this.config=config;setDemo(true);}
    public void setDemo(boolean demo) { bus=new ClientBUS(demo?new DemoDAO():new HttpDAO(config)); }
    public boolean isDemo() {return bus.isDemo();}
    public AdminDTO login(String user,String password)throws Exception{return bus.login(user,password);}
    public AdminDTO me()throws Exception{return bus.me();}
    public void logout()throws Exception{bus.logout();}
    public List<POIDTO> pois()throws Exception{return bus.pois();}
    public POIDTO savePoi(POIDTO p)throws Exception{return bus.savePoi(p);}
    public void visiblePoi(long id,boolean visible)throws Exception{bus.visiblePoi(id,visible);}
    public List<NarrationDTO> narrations()throws Exception{return bus.narrations();}
    public NarrationDTO saveNarration(NarrationDTO n)throws Exception{return bus.saveNarration(n);}
    public void visibleNarration(long id,boolean visible)throws Exception{bus.visibleNarration(id,visible);}
    public void uploadAudio(long id,Path path)throws Exception{bus.uploadAudio(id,path);}
    public void uploadAudio(long id,Path path,double seconds)throws Exception{bus.uploadAudio(id,path,seconds);}
    public void listenEvent(long id,String lang,boolean simulated)throws Exception{bus.listenEvent(id,lang,simulated);}
}
