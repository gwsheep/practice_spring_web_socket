package com.devgwon.practice.springwebsocket.service;

import org.springframework.beans.factory.annotation.Value;
import com.devgwon.practice.springwebsocket.domain.ChatFile;
import com.devgwon.practice.springwebsocket.dto.FileResponse;
import com.devgwon.practice.springwebsocket.repository.ChatFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final ChatFileRepository chatFileRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public FileResponse uploadChatFile(MultipartFile files) throws IOException {

        //File Directory
        Path uploadPath = Path.of(uploadDir);
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("File 디렉토리 생성이 실패하였습니다");
        }

        //File name + Path
        String originalName =
                StringUtils.cleanPath(files.getOriginalFilename() == null ? "unknown" : files.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + "-" + originalName;
        Path tartgetPath = uploadPath.resolve(storedFileName).normalize();

        //File Save
        try {
            files.transferTo(tartgetPath.toFile());
        } catch (IOException e) {
            throw new RuntimeException("File 저장이 실패하였습니다");
        }

        //DB 저장
        ChatFile chatFile = new ChatFile(
                                        originalName,
                                        storedFileName,
                                        files.getSize(),
                                        files.getContentType(),
                                        tartgetPath.toString()
                                );
        ChatFile savedFile = chatFileRepository.save(chatFile);

        return new FileResponse(
                savedFile.getId(),
                savedFile.getOriginalFileName(),
                savedFile.getFileSize(),
                savedFile.getContentType(),
                "/socket/file/download/" + savedFile.getId()
        );

    }

    public ChatFile getChatFile(Long fileId) {

        return chatFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다."));

    }

    public Resource downloadChatFile(ChatFile chatFile) throws IOException {

        //File path
        Path path = Path.of(chatFile.getStoragePath());

        //File read
        Resource resource = new UrlResource(path.toUri());
        if(!resource.exists() || !resource.isReadable()) {
            throw new IllegalArgumentException("파일을 읽을 수 없습니다.");
        }
        return resource;

    }



}
