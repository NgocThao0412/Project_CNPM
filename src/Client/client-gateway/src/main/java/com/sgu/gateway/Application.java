package com.sgu.gateway;
import com.sgu.core.*;
import com.sgu.gateway.controller.*;
public class Application {
    public static void main(String[] args)throws Exception {
        Settings s=new Settings();
        GatewayController c=new GatewayController(s);
        int port=s.integer("gateway.port",8085);
        Http.serve(s.get("gateway.bind","127.0.0.1"),port,c::handle);
        System.out.println("client-gateway :"+port+" mode="+s.mode());
    }
}
