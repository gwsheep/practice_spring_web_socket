package com.devgwon.practice.springwebsocket.controller;

import com.devgwon.practice.springwebsocket.domain.ChatFile;
import com.devgwon.practice.springwebsocket.dto.FileResponse;
import com.devgwon.practice.springwebsocket.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/socket/file")
public class FileController {

    private final FileService fileService;

    //업로드
    @PostMapping("/upload")
    public ResponseEntity<FileResponse> upload(@RequestParam("file") MultipartFile file) {

        long size = file.getSize();

        //유효성 체크
        if (file == null || file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            throw new IllegalArgumentException("파일은 필수입니다.");
        }
        FileResponse res = null;
        try {
            res = fileService.uploadChatFile(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.status(HttpStatus.OK).body(res);

    }

    //다운로드
    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> download(@PathVariable Long fileId) throws IOException {

        if(fileId == null) {
            throw new IllegalArgumentException("File Id는 필수입니다");
        }
        ChatFile chatFile = fileService.getChatFile(fileId);
        Resource resource = fileService.downloadChatFile(chatFile);

        //MediaType (예외 발생 가능성?)
        MediaType mediaType =
                chatFile.getContentType() != null ?
                        MediaType.parseMediaType(chatFile.getContentType()) :
                        MediaType.APPLICATION_OCTET_STREAM;

        //Header
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(chatFile.getOriginalFileName(), StandardCharsets.UTF_8)
                .build();


        return ResponseEntity.status(HttpStatus.OK)
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentLength(chatFile.getFileSize())
                .body(resource);

    }

}
