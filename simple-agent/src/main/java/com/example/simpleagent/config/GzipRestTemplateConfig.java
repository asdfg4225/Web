package com.example.simpleagent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.zip.GZIPInputStream;

@Configuration
public class GzipRestTemplateConfig {

    @Bean
    @Primary
    public RestTemplate gzipRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);

        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(Collections.singletonList(new GzipInterceptor()));
        return restTemplate;
    }

    /**
     * GZIP解压缩拦截器
     */
    private static class GzipInterceptor implements ClientHttpRequestInterceptor {

        @Override
        public ClientHttpResponse intercept(org.springframework.http.HttpRequest request, byte[] body,
                                            org.springframework.http.client.ClientHttpRequestExecution execution)
                throws IOException {
            request.getHeaders().add("Accept-Encoding", "gzip, deflate");
            ClientHttpResponse response = execution.execute(request, body);

            String contentEncoding = response.getHeaders().getFirst("Content-Encoding");
            if (contentEncoding != null && contentEncoding.contains("gzip")) {
                return new GzipDecompressingResponse(response);
            }
            return response;
        }
    }

    /**
     * GZIP解压缩响应包装器
     */
    private static class GzipDecompressingResponse implements ClientHttpResponse {

        private final ClientHttpResponse response;
        private byte[] decompressedBody;

        public GzipDecompressingResponse(ClientHttpResponse response) {
            this.response = response;
        }

        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return response.getStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return response.getStatusText();
        }

        @Override
        public void close() {
            response.close();
        }

        @Override
        public org.springframework.http.HttpHeaders getHeaders() {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.putAll(response.getHeaders());
            headers.remove("Content-Encoding");
            if (decompressedBody != null) {
                headers.setContentLength(decompressedBody.length);
            }
            return headers;
        }

        @Override
        public java.io.InputStream getBody() throws IOException {
            if (decompressedBody == null) {
                decompressedBody = decompress(response.getBody());
            }
            return new ByteArrayInputStream(decompressedBody);
        }

        private byte[] decompress(java.io.InputStream compressed) throws IOException {
            try (GZIPInputStream gzipInputStream = new GZIPInputStream(compressed);
                 java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream()) {

                byte[] buffer = new byte[1024];
                int len;
                while ((len = gzipInputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, len);
                }
                return outputStream.toByteArray();
            }
        }
    }
}