package com.example.cloud_prog_lab1.service;

import com.example.cloud_prog_lab1.dto.FileDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileService {
    @Autowired
    private S3Client s3Client;

    public byte[] downloadFile(String fileName, String bucketName) {
        ResponseBytes<GetObjectResponse> object = s3Client.getObject(request -> request
                .bucket(bucketName)
                .key(fileName),
                ResponseTransformer.toBytes());
        return object.asByteArray();
    }

    public String uploadFile(MultipartFile file, String bucketName) throws IOException {
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        File fileObj = new File("src/main/resources/tmp_file.txt");
        try (OutputStream os = new FileOutputStream(fileObj)) {
            os.write(file.getBytes());
        }
        s3Client.putObject(PutObjectRequest.builder().bucket(bucketName).key(fileName).build(), fileObj.toPath());
        fileObj.delete();
        return "File uploaded: " + fileName;
    }

    public String deleteFile(String fileName, String bucketName) {
        s3Client.deleteObject(request -> request
                        .bucket(bucketName)
                        .key(fileName));
        return "File deleted: " + fileName;
    }

    public List<FileDTO> listFiles(String bucketName) {
        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();
        ListObjectsV2Response listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);
        List<S3Object> contents = listObjectsV2Response.contents();

        return contents.stream().map(obj -> new FileDTO(obj.key(), bucketName)).collect(Collectors.toList());
    }
}
