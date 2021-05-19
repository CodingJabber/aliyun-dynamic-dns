package com.codingjabber.tools;

import java.util.List;

/**
 * Configuration Class
 */
public class Config {
    private String regionId;
    private String accessKeyId;
    private String secret;
    private String subDomain;
    private Integer interval;
    /**
     * 1: Network interface
     * 2: Http response
     */
    private Integer ipSource;
    private String ipSegment;
    private List<String> httpUrls;

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getSubDomain() {
        return subDomain;
    }

    public void setSubDomain(String subDomain) {
        this.subDomain = subDomain;
    }

    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    public Integer getIpSource() {
        return ipSource;
    }

    public void setIpSource(Integer ipSource) {
        this.ipSource = ipSource;
    }

    public String getIpSegment() {
        return ipSegment;
    }

    public void setIpSegment(String ipSegment) {
        this.ipSegment = ipSegment;
    }

    public List<String> getHttpUrls() {
        return httpUrls;
    }

    public void setHttpUrls(List<String> httpUrls) {
        this.httpUrls = httpUrls;
    }
}
