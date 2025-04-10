package com.emerbv.ecommdb.service.category;

import com.emerbv.ecommdb.dto.CategoryDto;
import com.emerbv.ecommdb.model.Category;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ICategoryService {
    Category getCategoryById(Long id);
    Category getCategoryByName(String name);
    List<Category> getAllCategories();
    Category addCategory(Category category);
    Category updateCategory(Category category, Long id);
    void deleteCategoryById(Long id);

    Category uploadCategoryImage(Long categoryId, MultipartFile file);
    byte[] getCategoryImage(Long categoryId);
    void deleteCategoryImage(Long categoryId);
    CategoryDto convertToDto(Category category);
    List<CategoryDto> convertToDtoList(List<Category> categories);
}
