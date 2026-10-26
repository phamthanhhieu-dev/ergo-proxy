package com.ergoproxy.platform.config.web;

import com.ergoproxy.platform.AnimeApplication;
import com.ergoproxy.platform.dto.GlobalApiResponse;
import com.ergoproxy.platform.exception.LogicException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.ResourceRegionHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@RestControllerAdvice(basePackageClasses = AnimeApplication.class)
@RequiredArgsConstructor
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter type, Class<? extends HttpMessageConverter<?>> converter) {
        boolean response =
            !ResourceHttpMessageConverter.class.isAssignableFrom(converter) && !ResourceRegionHttpMessageConverter.class.isAssignableFrom(converter);
        return response;
    }

    @Override
    public Object beforeBodyWrite(
        Object body,
        MethodParameter type,
        MediaType contentType,
        Class<? extends HttpMessageConverter<?>> converter,
        ServerHttpRequest request,
        ServerHttpResponse response
    ) {
        if (
            body instanceof GlobalApiResponse<?> ||
            body instanceof Resource ||
            body instanceof byte[] ||
            response.getHeaders().containsHeader(HttpHeaders.CONTENT_DISPOSITION) ||
            MediaType.TEXT_EVENT_STREAM.isCompatibleWith(contentType)
        ) {
            return body;
        }
        GlobalApiResponse<Object> result = GlobalApiResponse.success(body);
        response.getHeaders().remove(HttpHeaders.CONTENT_LENGTH);
        if (StringHttpMessageConverter.class.isAssignableFrom(converter)) {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            try {
                Object returnValue = objectMapper.writeValueAsString(result);
                return returnValue;
            } catch (JacksonException exception) {
                throw new LogicException(exception, "BBWGRA_00001", "request.response.serialization.failed");
            }
        }
        return result;
    }
}
