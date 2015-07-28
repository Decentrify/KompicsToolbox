///*
// * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
// * 2009 Royal Institute of Technology (KTH)
// *
// * GVoD is free software; you can redistribute it and/or
// * modify it under the terms of the GNU General Public License
// * as published by the Free Software Foundation; either version 2
// * of the License, or (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software
// * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// */
//package se.sics.p2ptoolbox.videostream;
//
//import com.yammer.dropwizard.Service;
//import com.yammer.dropwizard.config.Bootstrap;
//import com.yammer.dropwizard.config.Configuration;
//import com.yammer.dropwizard.config.Environment;
//import java.io.File;
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.util.Map;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import org.eclipse.jetty.servlets.CrossOriginFilter;
//
//import javax.ws.rs.*;
//import javax.ws.rs.core.MediaType;
//import javax.ws.rs.core.Response;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import se.sics.p2ptoolbox.videostream.manager.VodManager;
//import se.sics.p2ptoolbox.videostream.manager.impl.VodManagerImpl;
//import se.sics.p2ptoolbox.videostream.manager.util.FileStatus;
//import se.sics.p2ptoolbox.util.managedStore.FileMngr;
//import se.sics.p2ptoolbox.util.managedStore.StorageMngrFactory;
//import se.sics.p2ptoolbox.videostream.http.BaseHandler;
//import se.sics.p2ptoolbox.videostream.http.JwHttpServer;
//import se.sics.p2ptoolbox.videostream.http.RangeCapableMp4Handler;
//import se.sics.p2ptoolbox.videostream.wsmodel.PlayPosInfo;
//import se.sics.p2ptoolbox.videostream.wsmodel.VideoInfo;
//
///**
// * @author Alex Ormenisan <aaor@sics.se>
// */
//public class VideoStreamWebService extends Service<Configuration> {
//
//    private static final Logger log = LoggerFactory.getLogger(VideoStreamWebService.class);
//    private static VodManager vodManager;
//    @Override
//    public void initialize(Bootstrap<Configuration> bootstrap) {
//
////        bootstrap.addBundle(new AssetsBundle("/assets/","/webapp","index.html"));
//        try {
//            InetSocketAddress addr = new InetSocketAddress(54321);
//            vodManager = new VodManagerImpl();
//
//            String filePath1 = "/Users/Alex/Documents/Temp/videos/gvod.mp4";
//            File file1 = new File(filePath1);
//            int pieceSize = 1024;
//            int blockSize = 1024 * 1024;
//            FileMngr fm1 = StorageMngrFactory.getCompleteFileMngr(filePath1, file1.length(), blockSize, pieceSize);
//            BaseHandler handler1 = new RangeCapableMp4Handler(new VideoStreamMngrImpl(fm1, pieceSize, file1.length(), new AtomicInteger(0)));
//            JwHttpServer.startOrUpdate(addr, "/messi.mp4/", handler1);
//        } catch (IOException ex) {
//            log.error("could not start player");
//            System.exit(1);
//        }
//
//    }
//
//    @Override
//    public void run(Configuration configuration, Environment environment) {
//        log.info("running");
////        //registering web service resources
//        environment.addProvider(new DownloadVideoResource());
//        environment.addProvider(new PlayPosResource());
//        environment.addProvider(new PlayVideoResource());
//        environment.addProvider(new FilesResource());
//        environment.addProvider(new UploadResource());
////
//        /*
//         * To allow cross origin resource request from angular js client
//         */
//        environment.addFilter(CrossOriginFilter.class, "/*").
//                setInitParam("allowedOrigins", "*").
//                setInitParam("allowedHeaders", "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin").
//                setInitParam("allowedMethods", "GET,PUT,POST,DELETE,OPTIONS").
//                setInitParam("preflightMaxAge", "5184000"). // 2 months
//                setInitParam("allowCredentials", "true");
//
//    }
//
//    @Path("/downloadvideo")
//    @Produces(MediaType.APPLICATION_JSON)
//    @Consumes(MediaType.APPLICATION_JSON)
//    public static class DownloadVideoResource {
//
//        @PUT
//        public Response downloadVideo(VideoInfo videoInfo) {
//            log.debug("video:{} download", videoInfo.getName());
//            return Response.status(Response.Status.OK).entity(true).build();
//        }
//    }
//
//    @Path("/play")
//    @Produces(MediaType.APPLICATION_JSON)
//    @Consumes(MediaType.APPLICATION_JSON)
//    public static class PlayVideoResource {
//
//        @PUT
//        public Response playVideo(VideoInfo videoInfo) {
//            log.debug("video:{} play", videoInfo.getName());
//            return Response.status(Response.Status.OK).entity(54321).build();
//        }
//    }
//
//    @Path("/playPos")
//    @Produces(MediaType.APPLICATION_JSON)
//    @Consumes(MediaType.APPLICATION_JSON)
//    public static class PlayPosResource {
//
//        @PUT
//        public Response playPos(PlayPosInfo playPosInfo) {
//            log.debug("video:{} pos:{}", playPosInfo.getVideoName(), playPosInfo.getPlayPos());
//            return Response.status(Response.Status.OK).entity(54321).build();
//        }
//    }
//
//
//    @Path("/files")
//    @Produces(MediaType.APPLICATION_JSON)
//    public static class FilesResource {
//
//        @GET
//        public Response getFiles() {
//            log.debug("fetch files called");
//            Map<String, FileStatus> fileStatusMap = vodManager.fetchFiles();
//            return Response.status(Response.Status.OK).entity(fileStatusMap).build();
//        }
//    }
//
//
//    @Path("/uploadvideo")
//    @Produces(MediaType.APPLICATION_JSON)
//    @Consumes(MediaType.APPLICATION_JSON)
//    public static class UploadResource {
//
//        @PUT
//        public Response playPos(VideoInfo videoInfo) {
//            log.debug("{}: video upload invoked.");
//            return Response.status(Response.Status.OK).entity(vodManager.uploadVideo(videoInfo)).build();
//        }
//    }
//
//}
