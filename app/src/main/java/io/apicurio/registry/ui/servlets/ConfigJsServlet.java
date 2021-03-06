/*
 * Copyright 2020 Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.apicurio.registry.ui.servlets;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.apicurio.registry.ui.beans.ConfigJs;
import io.apicurio.registry.utils.StringUtil;

/**
 * Generates the 'config.js' file imported by the UI.
 * @author eric.wittmann@gmail.com
 */
public class ConfigJsServlet extends HttpServlet {

    private static final long serialVersionUID = 1624928159818173418L;

    @Inject
    @ConfigProperty(name = "registry.ui.features.readOnly")
    Boolean featureReadOnly;

    @Inject
    @ConfigProperty(name = "registry.ui.config.uiUrl")
    String uiUrl;

    @Inject
    @ConfigProperty(name = "registry.ui.config.apiUrl")
    String apiUrl;


    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String ct = "application/javascript; charset=" + StandardCharsets.UTF_8;
        response.setContentType(ct);
        JsonFactory f = new JsonFactory();
        try (JsonGenerator g = f.createGenerator(response.getOutputStream(), JsonEncoding.UTF8)) {
            response.getOutputStream().write("var ApicurioRegistryConfig = ".getBytes("UTF-8")); //$NON-NLS-1$ //$NON-NLS-2$
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(Include.NON_NULL);
            g.setCodec(mapper);
            g.useDefaultPrettyPrinter();

            ConfigJs config = new ConfigJs();
            config.mode = "prod";

            config.artifacts.url = this.generateApiUrl(request);
            
            config.ui.url = this.generateUiUrl(request);
            config.ui.contextPath = "/ui";
            
            config.features.readOnly = this.isFeatureReadOnly();
            
            g.writeObject(config);

            g.flush();
            response.getOutputStream().write(";".getBytes("UTF-8")); //$NON-NLS-1$ //$NON-NLS-2$
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /**
     * Generates a URL that the caller can use to access the API.
     * @param request
     */
    private String generateApiUrl(HttpServletRequest request) {
        try {
            if (!"_".equals(apiUrl) && !StringUtil.isEmpty(apiUrl)) {
                return apiUrl;
            }
            
            String url = resolveUrlFromXForwarded(request, "/api");
            if (url != null) {
                return url;
            }
            
            url = request.getRequestURL().toString();
            url = new URI(url).resolve("/api").toString();
            if (url.startsWith("http:") && request.isSecure()) {
                url = url.replaceFirst("http", "https");
            }
            return url;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates a URL that the caller can use to access the UI.
     * @param request
     */
    private String generateUiUrl(HttpServletRequest request) {
        try {
            if (!"_".equals(uiUrl) && !StringUtil.isEmpty(uiUrl)) {
                return uiUrl;
            }
            
            String url = resolveUrlFromXForwarded(request, "/ui");
            if (url != null) {
                return url;
            }

            url = request.getRequestURL().toString();
            url = new URI(url).resolve("/ui").toString();
            if (url.startsWith("http:") && request.isSecure()) {
                url = url.replaceFirst("http", "https");
            }
            return url;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Resolves a URL path relative to the information found in X-Forwarded-Host and X-Forwarded-Proto.
     * @param path
     */
    private String resolveUrlFromXForwarded(HttpServletRequest request, String path) {
        try {
            String fproto = request.getHeader("X-Forwarded-Proto");
            String fhost = request.getHeader("X-Forwarded-Host");
            if (!StringUtil.isEmpty(fproto) && !StringUtil.isEmpty(fhost)) {
                return new URI(fproto + "://" + fhost).resolve(path).toString();
            }
        } catch (URISyntaxException e) {
        }
        return null;
    }

    /**
     * Returns true if the "read only" feature is enabled.
     */
    private boolean isFeatureReadOnly() {
        return featureReadOnly == null ? false : featureReadOnly;
    }

}
