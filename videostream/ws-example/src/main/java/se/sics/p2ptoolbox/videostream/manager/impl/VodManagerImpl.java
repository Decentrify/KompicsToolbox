package se.sics.p2ptoolbox.videostream.manager.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.p2ptoolbox.videostream.manager.VodManager;

import se.sics.p2ptoolbox.videostream.manager.util.FileStatus;
import se.sics.p2ptoolbox.videostream.wsmodel.VideoInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Vod Manager to mock the Rest Calls to the actual gvod manager.
 * Created by babbar on 2015-03-12.
 */
public class VodManagerImpl implements VodManager {

    private Map<String, FileStatus> files = new HashMap<String, FileStatus>();
    private Logger logger = LoggerFactory.getLogger(VodManagerImpl.class);

    public VodManagerImpl(){
        createInitialFilesInfo(files);
    }

    private void createInitialFilesInfo(Map<String,FileStatus> baseMap){
        baseMap.put("Flash.mp4",FileStatus.NONE);
        baseMap.put("Messi.mp4",FileStatus.NONE);
    }

    @Override
    public Map<String, FileStatus> fetchFiles() {
        return this.files;
    }

    @Override
    public boolean uploadVideo(VideoInfo info) {

        String videoName = info.getName();
        if(files.containsKey(videoName)){
            files.put(videoName, FileStatus.UPLOADED);
            return true;
        }

        return false;
    }
}
