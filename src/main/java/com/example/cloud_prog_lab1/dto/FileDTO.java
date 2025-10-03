package com.example.cloud_prog_lab1.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class FileDTO {
    private String fileName;

    public FileDTO(String fileName, String bucketName) {
        this.fileName = fileName;
        this.bucketName = bucketName;
    }

    private String bucketName;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }
}
