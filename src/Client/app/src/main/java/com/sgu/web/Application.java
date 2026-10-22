package com.sgu.web;
import com.sgu.core.*;
import com.sgu.web.controller.*;
public class Application {
    public static void main(String[] args)throws Exception {
        Settings s=new Settings();
        ClientController c=new ClientController(s);
        int port=s.integer("web.port",8080);
        Http.serve(s.get("web.bind","0.0.0.0"),port,c::handle);
        System.out.println("Saigon Audio Guide: http://localhost:"+port+"/  mode="+s.mode());
        System.out.println("Open the same address using this computer's LAN IP on your iPhone/Android.");
    }
}
