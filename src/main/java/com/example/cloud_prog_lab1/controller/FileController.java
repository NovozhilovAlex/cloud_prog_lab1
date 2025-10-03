package com.example.cloud_prog_lab1.controller;

import com.example.cloud_prog_lab1.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/file")
public class FileController {
    @Autowired
    private FileService fileService;

    @GetMapping("/download/{bucketName}/{fileName}")
    public ResponseEntity<ByteArrayResource> download(@PathVariable String bucketName, @PathVariable String fileName) {
        byte[] data = fileService.downloadFile(fileName, bucketName);
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity
                .ok()
                .contentLength(data.length)
                .header("Content-type", "application/octet-stream")
                .header("Content-disposition", "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }

    @GetMapping("/show/{bucketName}/{fileName}")
    public ResponseEntity<String> show(@PathVariable String bucketName, @PathVariable String fileName) {
        byte[] data = fileService.downloadFile(fileName, bucketName);

        return ResponseEntity.ok(new String(data));
    }

    @PostMapping("/{bucketName}/upload")
    public ResponseEntity<String> upload(@PathVariable String bucketName, @RequestBody MultipartFile file) {
        try {
            return new ResponseEntity<>(fileService.uploadFile(file, bucketName), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error file uploading", HttpStatus.resolve(500));
        }
    }

    @DeleteMapping("/delete/{bucketName}/{fileName}")
    public ResponseEntity<String> delete(@PathVariable String bucketName, @PathVariable String fileName) {
        return ResponseEntity.ok(fileService.deleteFile(fileName, bucketName));
    }
}

