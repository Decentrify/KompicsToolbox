/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.p2ptoolbox.example;

import java.io.IOException;
import java.net.InetSocketAddress;
import se.sics.p2ptoolbox.videostream.VideoStreamMngrImpl;
import se.sics.p2ptoolbox.videostream.http.BaseHandler;
import se.sics.p2ptoolbox.videostream.http.JwHttpServer;
import se.sics.p2ptoolbox.videostream.http.RangeCapableMp4Handler;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class Main {

    public static void main(String[] args) throws IOException {
        System.out.println("starting");
        InetSocketAddress addr = new InetSocketAddress(58026);
        BaseHandler handler1 = new RangeCapableMp4Handler(new VideoStreamMngrImpl("/Users/Alex/Documents/Work/Code/globalcommon/videostream/example/src/main/resources/messi.mp4", 1024 * 1024));
        BaseHandler handler2 = new RangeCapableMp4Handler(new VideoStreamMngrImpl("/Users/Alex/Documents/Work/Code/globalcommon/videostream/ws-example/src/main/resources/knight.mp4", 1024 * 1024));
        JwHttpServer.startOrUpdate(addr, "/messi.mp4/", handler1);
        JwHttpServer.startOrUpdate(addr, "/knight.mp4/", handler2);
    }
}
