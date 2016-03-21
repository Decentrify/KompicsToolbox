package se.sics.ktoolbox.videostream.manager;


import se.sics.ktoolbox.videostream.manager.util.FileStatus;
import se.sics.ktoolbox.videostream.wsmodel.VideoInfo;

import java.util.Map;

/**
 * Created by babbar on 2015-03-12.
 */
public interface VodManager {


    /**
     * Fetch the information about the files available in gvod.
     * @return Map of files and there status.
     */
    public Map<String,FileStatus> fetchFiles();

    /**
     *  Upload the Video to gvod.
     * @param info Video information
     * @return true or false
     */
    public boolean uploadVideo(VideoInfo info);

}
