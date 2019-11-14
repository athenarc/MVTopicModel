package org.madgik.MVTopicModel.io;


public abstract class FileTMDataSource extends TMDataSource {
    public FileTMDataSource(String properties) {
        super(properties);
    }
    public abstract String getPath(String path);
    public abstract void writeObject(Object o, String path);
    public abstract Object readObject(String path);
}
