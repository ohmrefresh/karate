package com.karate.mockmgr.model;

import lombok.Data;

@Data
public class MockServerConfig {
    private String id;
    private int port = 8080;
    private String featureFile;
    private String pathPrefix = null;
    private boolean ssl = false;
    private boolean watch = true;
}
