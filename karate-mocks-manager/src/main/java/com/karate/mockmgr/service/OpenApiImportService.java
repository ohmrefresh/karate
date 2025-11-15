package com.karate.mockmgr.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OpenApiImportService {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public String convertToKarateFeature(String openApiContent, String featureName) {
        try {
            OpenAPI openAPI = new OpenAPIV3Parser().readContents(openApiContent).getOpenAPI();

            if (openAPI == null) {
                throw new RuntimeException("Failed to parse OpenAPI specification");
            }

            StringBuilder feature = new StringBuilder();
            feature.append("Feature: ").append(featureName != null ? featureName : "Generated from OpenAPI").append("\n\n");

            // Add background section for common setup
            feature.append("Background:\n");
            feature.append("  * def apiData = {}\n\n");

            // Process each path and operation
            if (openAPI.getPaths() != null) {
                openAPI.getPaths().forEach((path, pathItem) -> {
                    processPath(path, pathItem, feature);
                });
            }

            // Add fallback scenario
            feature.append("Scenario:\n");
            feature.append("  * def responseStatus = 404\n");
            feature.append("  * def response = { error: 'Not found', path: requestPath }\n");

            return feature.toString();

        } catch (Exception e) {
            log.error("Failed to convert OpenAPI to Karate feature", e);
            throw new RuntimeException("Failed to convert OpenAPI spec: " + e.getMessage(), e);
        }
    }

    private void processPath(String path, PathItem pathItem, StringBuilder feature) {
        Map<PathItem.HttpMethod, Operation> operations = pathItem.readOperationsMap();

        operations.forEach((method, operation) -> {
            String karateMethod = method.name().toLowerCase();
            String karatePath = convertPathToKarate(path);

            feature.append("Scenario: ");
            if (operation.getSummary() != null) {
                feature.append(operation.getSummary());
            } else {
                feature.append(method.name()).append(" ").append(path);
            }
            feature.append("\n");

            // Path matching condition
            feature.append("  * def pathMatch = pathMatches('").append(karatePath).append("')\n");
            feature.append("  * def methodMatch = methodIs('").append(karateMethod).append("')\n");
            feature.append("  * if (!pathMatch || !methodMatch) karate.abort()\n\n");

            // Extract path parameters
            List<Parameter> pathParams = getPathParameters(operation, pathItem);
            if (!pathParams.isEmpty()) {
                feature.append("  # Path parameters\n");
                for (Parameter param : pathParams) {
                    String paramName = param.getName();
                    feature.append("  * def ").append(paramName)
                           .append(" = pathParams.").append(paramName).append("\n");
                }
                feature.append("\n");
            }

            // Extract query parameters
            List<Parameter> queryParams = getQueryParameters(operation, pathItem);
            if (!queryParams.isEmpty()) {
                feature.append("  # Query parameters\n");
                for (Parameter param : queryParams) {
                    String paramName = param.getName();
                    feature.append("  * def ").append(paramName)
                           .append(" = paramValue('").append(paramName).append("')\n");
                }
                feature.append("\n");
            }

            // Generate response
            if (operation.getResponses() != null) {
                ApiResponse successResponse = getSuccessResponse(operation);
                if (successResponse != null) {
                    String statusCode = getSuccessStatusCode(operation);
                    feature.append("  * def responseStatus = ").append(statusCode).append("\n");

                    String responseExample = getResponseExample(successResponse);
                    if (responseExample != null) {
                        feature.append("  * def response = ").append(responseExample).append("\n");
                    } else {
                        feature.append("  * def response = { message: 'Success' }\n");
                    }
                }
            } else {
                feature.append("  * def responseStatus = 200\n");
                feature.append("  * def response = { message: 'Success' }\n");
            }

            feature.append("\n");
        });
    }

    private String convertPathToKarate(String openApiPath) {
        // Convert OpenAPI path parameters {id} to Karate {id}
        return openApiPath;
    }

    private List<Parameter> getPathParameters(Operation operation, PathItem pathItem) {
        List<Parameter> params = new ArrayList<>();

        if (operation.getParameters() != null) {
            params.addAll(operation.getParameters().stream()
                .filter(p -> "path".equals(p.getIn()))
                .collect(Collectors.toList()));
        }

        if (pathItem.getParameters() != null) {
            params.addAll(pathItem.getParameters().stream()
                .filter(p -> "path".equals(p.getIn()))
                .collect(Collectors.toList()));
        }

        return params;
    }

    private List<Parameter> getQueryParameters(Operation operation, PathItem pathItem) {
        List<Parameter> params = new ArrayList<>();

        if (operation.getParameters() != null) {
            params.addAll(operation.getParameters().stream()
                .filter(p -> "query".equals(p.getIn()))
                .collect(Collectors.toList()));
        }

        if (pathItem.getParameters() != null) {
            params.addAll(pathItem.getParameters().stream()
                .filter(p -> "query".equals(p.getIn()))
                .collect(Collectors.toList()));
        }

        return params;
    }

    private ApiResponse getSuccessResponse(Operation operation) {
        if (operation.getResponses() == null) {
            return null;
        }

        // Try 200, 201, 204, or first 2xx
        for (String code : Arrays.asList("200", "201", "204")) {
            if (operation.getResponses().containsKey(code)) {
                return operation.getResponses().get(code);
            }
        }

        return operation.getResponses().values().stream()
            .findFirst()
            .orElse(null);
    }

    private String getSuccessStatusCode(Operation operation) {
        if (operation.getResponses() == null) {
            return "200";
        }

        for (String code : Arrays.asList("200", "201", "204")) {
            if (operation.getResponses().containsKey(code)) {
                return code;
            }
        }

        return "200";
    }

    private String getResponseExample(ApiResponse response) {
        if (response.getContent() == null) {
            return null;
        }

        Content content = response.getContent();
        MediaType mediaType = content.get("application/json");

        if (mediaType == null) {
            mediaType = content.values().stream().findFirst().orElse(null);
        }

        if (mediaType == null) {
            return null;
        }

        // Try to get example
        if (mediaType.getExample() != null) {
            return formatExample(mediaType.getExample());
        }

        if (mediaType.getExamples() != null && !mediaType.getExamples().isEmpty()) {
            Example example = mediaType.getExamples().values().stream().findFirst().orElse(null);
            if (example != null && example.getValue() != null) {
                return formatExample(example.getValue());
            }
        }

        // Generate from schema
        if (mediaType.getSchema() != null) {
            return generateExampleFromSchema(mediaType.getSchema());
        }

        return null;
    }

    private String formatExample(Object example) {
        if (example instanceof String) {
            return (String) example;
        }
        return gson.toJson(example);
    }

    private String generateExampleFromSchema(Schema schema) {
        Map<String, Object> example = new HashMap<>();

        if (schema.getProperties() != null) {
            schema.getProperties().forEach((name, prop) -> {
                Object value = getExampleValue((Schema) prop);
                example.put(name, value);
            });
        }

        if (example.isEmpty()) {
            return "{ message: 'Success' }";
        }

        return gson.toJson(example);
    }

    private Object getExampleValue(Schema schema) {
        if (schema.getExample() != null) {
            return schema.getExample();
        }

        String type = schema.getType();
        if (type == null) {
            return "value";
        }

        switch (type) {
            case "string":
                return schema.getFormat() != null && schema.getFormat().equals("date-time")
                    ? "2024-01-01T00:00:00Z"
                    : "string";
            case "integer":
            case "number":
                return 0;
            case "boolean":
                return false;
            case "array":
                return new ArrayList<>();
            case "object":
                return new HashMap<>();
            default:
                return "value";
        }
    }
}
