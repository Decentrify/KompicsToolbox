package se.sics.ktoolbox.videostream.manager.util;

/**
 * Created by babbar on 2015-03-12.
 */
public enum FileStatus {

    NONE ("NONE"),
    UPLOADED ("UPLOADED");

    private String status;
    private FileStatus(String status){
        this.status = status;
    }

    private String getStatus(){
        return this.status;
    }
}
