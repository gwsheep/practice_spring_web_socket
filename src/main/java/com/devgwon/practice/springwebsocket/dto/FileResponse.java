package com.devgwon.practice.springwebsocket.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileResponse {

    private Long fileId;

    private String originalFileName;

    private Long fileSize;

    private String contentType;

    private String downloadUrl;

}
