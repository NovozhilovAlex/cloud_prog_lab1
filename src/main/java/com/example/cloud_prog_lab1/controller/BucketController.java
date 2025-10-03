package com.example.cloud_prog_lab1.controller;

import com.example.cloud_prog_lab1.dto.FileDTO;
import com.example.cloud_prog_lab1.service.BucketService;
import com.example.cloud_prog_lab1.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/bucket")
public class BucketController {
    @Autowired
    private BucketService bucketService;
    @Autowired
    private FileService fileService;

    @GetMapping("/show/{bucketName}")
    public ResponseEntity<List<FileDTO>> list(@PathVariable String bucketName) {
        return ResponseEntity.ok(fileService.listFiles(bucketName));
    }

    @GetMapping("/list")
    public ResponseEntity<List<String>> list() {
        return ResponseEntity.ok(bucketService.listBuckets());
    }

    @PostMapping("/create")
    public ResponseEntity<String> upload(@RequestBody String bucketName) {
        try {
            return new ResponseEntity<>(bucketService.createBucket(bucketName), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error bucket creating", HttpStatus.resolve(500));
        }
    }

    @DeleteMapping("/delete/{bucketName}")
    public ResponseEntity<String> delete(@PathVariable String bucketName) {
        return ResponseEntity.ok(bucketService.deleteBucket(bucketName));
    }
}
