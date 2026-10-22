package com.sgu.narration;
import com.sgu.core.*;
import com.sgu.narration.controller.*;
public class Application {
    public static void main(String[] args)throws Exception {
        Settings s=new Settings();
        NarrationController c=new NarrationController(s);
        int port=s.integer("narration.port",8083);
        Http.serve(s.get("services.bind","127.0.0.1"),port,c::handle);
        System.out.println("narration-server :"+port+" mode="+s.mode());
    }
}
