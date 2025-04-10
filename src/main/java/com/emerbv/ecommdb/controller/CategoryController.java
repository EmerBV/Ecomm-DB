package com.emerbv.ecommdb.controller;

import com.emerbv.ecommdb.dto.CategoryDto;
import com.emerbv.ecommdb.exceptions.AlreadyExistsException;
import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.Category;
import com.emerbv.ecommdb.response.ApiResponse;
import com.emerbv.ecommdb.service.category.ICategoryService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/categories")
public class CategoryController {
    private final ICategoryService categoryService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse> getAllCategories() {
        try {
            List<Category> categories = categoryService.getAllCategories();
            List<CategoryDto> categoryDtos = categoryService.convertToDtoList(categories);
            return ResponseEntity.ok(new ApiResponse("Found!", categoryDtos));
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(new ApiResponse("Error:", INTERNAL_SERVER_ERROR));
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/add")
    public ResponseEntity<ApiResponse> addCategory(@RequestBody Category name) {
        try {
            Category theCategory = categoryService.addCategory(name);
            CategoryDto categoryDto = categoryService.convertToDto(theCategory);
            return ResponseEntity.ok(new ApiResponse("Category successfully created", categoryDto));
        } catch (AlreadyExistsException e) {
            return ResponseEntity.status(CONFLICT).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @GetMapping("/category/{id}/category")
    public ResponseEntity<ApiResponse> getCategoryById(@PathVariable Long id){
        try {
            Category theCategory = categoryService.getCategoryById(id);
            CategoryDto categoryDto = categoryService.convertToDto(theCategory);
            return ResponseEntity.ok(new ApiResponse("Category Found", categoryDto));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @GetMapping("/category/{name}/category")
    public ResponseEntity<ApiResponse> getCategoryByName(@PathVariable String name){
        try {
            Category theCategory = categoryService.getCategoryByName(name);
            if (theCategory == null) {
                return ResponseEntity.status(NOT_FOUND).body(new ApiResponse("Category not found", null));
            }
            CategoryDto categoryDto = categoryService.convertToDto(theCategory);
            return ResponseEntity.ok(new ApiResponse("Category Found", categoryDto));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/category/{id}/delete")
    public ResponseEntity<ApiResponse> deleteCategory(@PathVariable Long id){
        try {
            categoryService.deleteCategoryById(id);
            return ResponseEntity.ok(new ApiResponse("Category successfully deleted", null));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/category/{id}/update")
    public ResponseEntity<ApiResponse> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        try {
            Category updatedCategory = categoryService.updateCategory(category, id);
            CategoryDto categoryDto = categoryService.convertToDto(updatedCategory);
            return ResponseEntity.ok(new ApiResponse("Category successfully updated", categoryDto));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/{categoryId}/upload-image")
    public ResponseEntity<ApiResponse> uploadCategoryImage(
            @PathVariable Long categoryId,
            @RequestParam("file") MultipartFile file) {
        try {
            Category category = categoryService.uploadCategoryImage(categoryId, file);
            CategoryDto categoryDto = categoryService.convertToDto(category);
            return ResponseEntity.ok(new ApiResponse("Image uploaded successfully", categoryDto));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error uploading image: " + e.getMessage(), null));
        }
    }

    @GetMapping("/download/{categoryId}/image")
    public ResponseEntity<Resource> getCategoryImage(@PathVariable Long categoryId) {
        try {
            byte[] imageData = categoryService.getCategoryImage(categoryId);
            ByteArrayResource resource = new ByteArrayResource(imageData);

            Category category = categoryService.getCategoryById(categoryId);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(category.getImageFileType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + category.getImageFileName() + "\"")
                    .body(resource);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/image/{categoryId}/delete-image")
    public ResponseEntity<ApiResponse> deleteCategoryImage(@PathVariable Long categoryId) {
        try {
            categoryService.deleteCategoryImage(categoryId);
            return ResponseEntity.ok(new ApiResponse("Category image deleted successfully", null));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error deleting image: " + e.getMessage(), null));
        }
    }
}
