/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.p2ptoolbox.example;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import se.sics.p2ptoolbox.util.managedStore.FileMngr;
import se.sics.p2ptoolbox.util.managedStore.StorageFactory;
import se.sics.p2ptoolbox.util.managedStore.StorageMngrFactory;
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
        int pieceSize = 1024;
        int blockSize = 1024*1024;
        
        String filePath1 = "/Users/Alex/Documents/Temp/videos/gvod.mp4"; 
        File file1 = new File(filePath1);
        FileMngr fm1 = StorageMngrFactory.getCompleteFileMngr(filePath1, file1.length(), blockSize, pieceSize);
        BaseHandler handler1 = new RangeCapableMp4Handler(new VideoStreamMngrImpl(fm1, pieceSize, file1.length()));
        
        String filePath2 = "/Users/Alex/Documents/Temp/videos/gvod2.mp4"; 
        File file2 = new File(filePath2);
        FileMngr fm2 = StorageMngrFactory.getCompleteFileMngr(filePath2, file2.length(), blockSize, pieceSize);
        BaseHandler handler2 = new RangeCapableMp4Handler(new VideoStreamMngrImpl(fm2, pieceSize, file2.length()));
        
        JwHttpServer.startOrUpdate(addr, "/gvod.mp4/", handler1);
        JwHttpServer.startOrUpdate(addr, "/gvod2.mp4/", handler2);
    }
}
