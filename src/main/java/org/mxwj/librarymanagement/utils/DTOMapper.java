package org.mxwj.librarymanagement.utils;

import java.lang.reflect.Field;
import java.util.Map;

public class DTOMapper {
    
    public static <T> T mapToDTO(Map<String, Object> input, Class<T> dtoClass) {
        try {
            T dto = dtoClass.getDeclaredConstructor().newInstance();
            
            for (Field field : dtoClass.getDeclaredFields()) {
                field.setAccessible(true);
                String fieldName = field.getName();
                Object value = input.get(fieldName);
                
                if (value != null) {
                    // 根据字段类型进行转换
                    if (field.getType() == String.class) {
                        field.set(dto, String.valueOf(value));
                    } else if (field.getType() == Integer.class) {
                        if (value instanceof String) {
                            field.set(dto, Integer.valueOf((String) value));
                        } else if (value instanceof Integer) {
                            field.set(dto, value);
                        }
                    } else if (field.getType() == Long.class) {
                        if (value instanceof String) {
                            field.set(dto, Long.valueOf((String) value));
                        } else if (value instanceof Integer) {
                            field.set(dto, ((Integer) value).longValue());
                        }
                    } else if (field.getType() == Short.class) {
                        if (value instanceof String) {
                            field.set(dto, Short.valueOf((String) value));
                        } else if (value instanceof Integer) {
                            field.set(dto, ((Integer) value).shortValue());
                        }
                    }
                    // 可以根据需要添加其他类型的转换
                }
            }
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("DTO映射失败", e);
        }
    }
}