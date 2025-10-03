package com.example.cloud_prog_lab1.service;

import com.example.cloud_prog_lab1.dto.FileDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BucketService {
    @Autowired
    private S3Client s3Client;
    @Autowired
    private FileService fileService;

    public String createBucket(String bucketName) {
        if (bucketExists(bucketName)) {
            return "Bucket already exists";
        }
        s3Client.createBucket(request -> request.bucket(bucketName));
        return "Bucket " + bucketName + " created";
    }

    public List<String> listBuckets() {
        List<Bucket> allBuckets = new ArrayList<>();
        String nextToken = null;

        do {
            String continuationToken = nextToken;
            ListBucketsResponse listBucketsResponse = s3Client.listBuckets(
                    request -> request.continuationToken(continuationToken)
            );

            allBuckets.addAll(listBucketsResponse.buckets());
            nextToken = listBucketsResponse.continuationToken();
        } while (nextToken != null);
        return allBuckets.stream().map(Bucket::name).collect(Collectors.toList());
    }

    public String deleteBucket(String bucketName) {
        if (!bucketExists(bucketName)) {
            return "Bucket not found";
        }
        try {
            List<FileDTO> fileNames = fileService.listFiles(bucketName);
            fileNames.forEach(file -> fileService.deleteFile(file.getFileName(), bucketName));
            s3Client.deleteBucket(request -> request.bucket(bucketName));
        } catch (S3Exception e) {
            return "Error: " + e;
        }
        return "Bucket " + bucketName + " deleted";
    }

    private boolean bucketExists(String bucketName) {
        try {
            s3Client.headBucket(request -> request.bucket(bucketName));
            return true;
        }
        catch (NoSuchBucketException exception) {
            return false;
        }
    }
}
