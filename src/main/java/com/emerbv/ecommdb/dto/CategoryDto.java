package com.emerbv.ecommdb.dto;

import lombok.Data;

@Data
public class CategoryDto {
    private Long id;
    private String name;
    private String imageFileName;
    private String imageFileType;
    private String imageDownloadUrl;
}
