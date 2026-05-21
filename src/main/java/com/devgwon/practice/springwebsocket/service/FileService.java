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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final ChatFileRepository chatFileRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final static Set<String> ALLOWED_EXTENSIONS = Set.of(
            "txt", "png", "jpeg", "jpg", "pdf"
    );

    private final static Set<String> ALLOWED_MIMES = Set.of(
            "text/plain",
            "image/png",
            "image/jpeg",
            "application/pdf"
    );


    public FileResponse uploadChatFile(MultipartFile files) throws IOException {

        //유효성 검증
        validate(files);

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

    private void validate(MultipartFile file) {

        if(file == null) {
            throw new IllegalArgumentException("파일은 필수입니다.");
        }

        validateExtension(file);
        validateMIME(file);
    }

    private void validateExtension(MultipartFile file) {
        if(file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            throw new IllegalArgumentException("파일 이름은 필수입니다");
        }
        String extension = getExtension(file.getOriginalFilename());
        if(!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("허용되지 않는 파일 확장자입니다");
        }
    }

    private String getExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf(".");
        if(lastIndex == -1) {
            throw new IllegalArgumentException("파일 확장자가 존재하지 않습니다");
        }
        return fileName.substring(lastIndex+1).toLowerCase();
    }

    private void validateMIME(MultipartFile file) {
        String contentType = file.getContentType();
        if(!ALLOWED_MIMES.contains(contentType)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다");
        }
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
