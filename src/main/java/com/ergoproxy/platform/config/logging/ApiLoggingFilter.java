package com.ergoproxy.platform.config.logging;

import com.ergoproxy.platform.constant.Const;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

@Component
@RequiredArgsConstructor
public class ApiLoggingFilter extends OncePerRequestFilter {

    private final ApiLogWriter apiLogWriter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        boolean response = !apiLogWriter.enabled() || !request.getRequestURI().startsWith(request.getContextPath() + "/api/");
        return response;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        long started = System.nanoTime();
        ContentCachingRequestWrapper input = new ContentCachingRequestWrapper(request, apiLogWriter.limit());
        LoggedResponse output = new LoggedResponse(response, apiLogWriter.limit());
        AtomicBoolean logged = new AtomicBoolean();
        // Forms are small metadata requests; never read video/ZIP content to log it.
        String parameters = apiLogWriter.parameters(input.getParameterMap());
        Runnable finish = () -> {
            if (!logged.compareAndSet(false, true)) {
                return;
            }
            String requestLog =
                input.getContentType() != null && input.getContentType().contains(Const.Common.JSON)
                    ? apiLogWriter.body(input.getContentAsByteArray(), input.getContentType(), false)
                    : parameters;
            String responseLog = output.getStatus() + " " + apiLogWriter.body(output.prefix.toByteArray(), output.getContentType(), output.truncated);
            apiLogWriter.write(URI.create(request.getRequestURL().toString()), request.getMethod(), requestLog, responseLog, started);
        };
        try {
            chain.doFilter(input, output);
        } finally {
            if (input.isAsyncStarted()) {
                try {
                    input.getAsyncContext().addListener(
                        new AsyncListener() {
                            public void onComplete(AsyncEvent event) {
                                finish.run();
                            }

                            public void onTimeout(AsyncEvent event) {
                                finish.run();
                            }

                            public void onError(AsyncEvent event) {
                                finish.run();
                            }

                            public void onStartAsync(AsyncEvent event) {
                                event.getAsyncContext().addListener(this);
                            }
                        }
                    );
                } catch (IllegalStateException completed) {
                    finish.run();
                }
            } else {
                finish.run();
            }
        }
    }

    /**
     * Bounded tee: bytes go immediately to Chrome, never accumulate an entire video/ZIP.
     */
    private static final class LoggedResponse extends HttpServletResponseWrapper {

        private final int limit;
        private final ByteArrayOutputStream prefix = new ByteArrayOutputStream();
        private boolean truncated;
        private ServletOutputStream stream;
        private PrintWriter writer;

        LoggedResponse(HttpServletResponse response, int limit) {
            super(response);
            this.limit = limit;
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (stream == null) {
                ServletOutputStream actual = super.getOutputStream();
                stream = new ServletOutputStream() {
                    public boolean isReady() {
                        boolean response = actual.isReady();
                        return response;
                    }

                    public void setWriteListener(WriteListener listener) {
                        actual.setWriteListener(listener);
                    }

                    public void write(int value) throws IOException {
                        write(new byte[] { (byte) value }, 0, 1);
                    }

                    public void write(byte[] bytes, int offset, int length) throws IOException {
                        actual.write(bytes, offset, length);
                        if (getContentType() != null && getContentType().contains(Const.Common.JSON)) {
                            int count = Math.min(length, limit - prefix.size());
                            prefix.write(bytes, offset, count);
                            truncated |= count < length;
                        }
                    }

                    public void flush() throws IOException {
                        actual.flush();
                    }

                    public void close() throws IOException {
                        actual.close();
                    }
                };
            }
            return stream;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (writer == null) {
                writer = new PrintWriter(new OutputStreamWriter(getOutputStream(), getCharacterEncoding()));
            }
            return writer;
        }

        @Override
        public void flushBuffer() throws IOException {
            if (writer != null) {
                writer.flush();
            }
            super.flushBuffer();
        }
    }
}
