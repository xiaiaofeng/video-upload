package com.idodok.video.bo;

import java.io.File;

public class FileInfo {

    private String fileName;

    private long length;

    public FileInfo(File file) {
        setFileName(file.getName());
        setLength(file.length());
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }
}
