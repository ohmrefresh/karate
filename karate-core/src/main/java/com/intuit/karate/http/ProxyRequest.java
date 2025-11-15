/*
 * The MIT License
 *
 * Copyright 2022 Karate Labs Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.intuit.karate.http;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import java.util.List;
import java.util.Map;

/**
 *
 * @author pthomas3
 */
public class ProxyRequest {

    public final ProxyContext context;
    public final FullHttpRequest request;
    private Request karateRequest; // parsed request with multipart support

    public String uri() {
        return request.uri();
    }

    public ProxyRequest(ProxyContext context, FullHttpRequest request) {
        this.context = context;
        this.request = request;
    }

    public ProxyResponse fake(int status, String body) {
        FullHttpResponse response = HttpUtils.createResponse(status, body);
        return new ProxyResponse(null, null, response);
    }

    /**
     * Get parsed multipart form data if the request is multipart/form-data.
     * Returns a map where keys are field names and values are lists of parts.
     * Each part is a map containing: name, value, and optionally: filename, contentType, charset, transferEncoding
     *
     * @return Map of multipart parts, or null if request is not multipart
     */
    public Map<String, List<Map<String, Object>>> getMultiParts() {
        ensureKarateRequest();
        return karateRequest != null ? karateRequest.getMultiParts() : null;
    }

    /**
     * Check if this is a multipart/form-data request
     *
     * @return true if request contains multipart data
     */
    public boolean isMultiPart() {
        ensureKarateRequest();
        return karateRequest != null && karateRequest.isMultiPart();
    }

    private void ensureKarateRequest() {
        if (karateRequest == null && request != null) {
            try {
                ByteBuf content = request.content();
                if (content != null && content.readableBytes() > 0) {
                    byte[] bytes = new byte[content.readableBytes()];
                    content.getBytes(content.readerIndex(), bytes);
                    karateRequest = new Request();
                    karateRequest.setMethod(request.method().name());
                    karateRequest.setPath(request.uri());
                    // copy headers
                    request.headers().forEach(entry ->
                        karateRequest.addHeader(entry.getKey(), entry.getValue())
                    );
                    karateRequest.setBody(bytes);
                    karateRequest.processBody(); // parse multipart if applicable
                }
            } catch (Exception e) {
                // ignore parsing errors, multipart will be null
            }
        }
    }

}
