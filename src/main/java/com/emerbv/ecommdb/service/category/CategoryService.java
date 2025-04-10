package com.emerbv.ecommdb.service.category;

import com.emerbv.ecommdb.dto.CategoryDto;
import com.emerbv.ecommdb.exceptions.AlreadyExistsException;
import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.Category;
import com.emerbv.ecommdb.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService implements ICategoryService {
    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;

    @Override
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Category not found!"));
    }

    @Override
    public Category getCategoryByName(String name) {
        return categoryRepository.findByName(name);
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public Category addCategory(Category category) {
        return  Optional.of(category)
                .filter(c -> !categoryRepository.existsByName(c.getName()))
                .map(categoryRepository::save)
                .orElseThrow(() -> new AlreadyExistsException(category.getName() + " already exists"));
    }

    @Override
    public Category updateCategory(Category category, Long id) {
        return Optional.ofNullable(getCategoryById(id)).map(oldCategory -> {
            oldCategory.setName(category.getName());
            return categoryRepository.save(oldCategory);
        }) .orElseThrow(()-> new ResourceNotFoundException("Category not found!"));
    }

    @Override
    public void deleteCategoryById(Long id) {
        categoryRepository.findById(id)
                .ifPresentOrElse(categoryRepository::delete, () -> {
                    throw new ResourceNotFoundException("Category not found!");
                });
    }

    @Override
    @Transactional
    public Category uploadCategoryImage(Long categoryId, MultipartFile file) {
        try {
            Category category = getCategoryById(categoryId);

            // Guardar la información de la imagen
            category.setImageFileName(file.getOriginalFilename());
            category.setImageFileType(file.getContentType());
            category.setImage(new SerialBlob(file.getBytes()));

            // Construir URL de descarga
            String buildDownloadUrl = "/api/v1/categories/images/download/";
            String downloadUrl = buildDownloadUrl + category.getId();
            category.setImageDownloadUrl(downloadUrl);

            return categoryRepository.save(category);
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Error uploading category image: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getCategoryImage(Long categoryId) {
        Category category = getCategoryById(categoryId);
        Blob imageBlob = category.getImage();

        if (imageBlob == null) {
            throw new ResourceNotFoundException("Image not found for category: " + categoryId);
        }

        try {
            int length = (int) imageBlob.length();
            return imageBlob.getBytes(1, length);
        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving category image: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteCategoryImage(Long categoryId) {
        Category category = getCategoryById(categoryId);

        category.setImage(null);
        category.setImageFileName(null);
        category.setImageFileType(null);
        category.setImageDownloadUrl(null);

        categoryRepository.save(category);
    }

    @Override
    public CategoryDto convertToDto(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setImageFileName(category.getImageFileName());
        dto.setImageFileType(category.getImageFileType());
        dto.setImageDownloadUrl(category.getImageDownloadUrl());
        return dto;
    }

    @Override
    public List<CategoryDto> convertToDtoList(List<Category> categories) {
        return categories.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
}
