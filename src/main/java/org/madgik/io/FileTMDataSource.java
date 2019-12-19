package org.madgik.io;


import java.io.File;

public abstract class FileTMDataSource extends TMDataSource {
    public FileTMDataSource(String properties) {
        super(properties);
    }
    public abstract String getPath(String path);
    public abstract void writeObject(Object o, String path);
    public abstract Object readObject(String path);
    protected void deleteFile(String path){
        File file = new File(path);
        if (!file.exists()) return;
        if(file.delete()) LOGGER.info("Deleted file successfully:" + path);
        else LOGGER.error("Failed to deleted file:" + path);
    }
}
