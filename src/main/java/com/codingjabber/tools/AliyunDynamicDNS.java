package com.codingjabber.tools;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.alidns.model.v20150109.DescribeSubDomainRecordsRequest;
import com.aliyuncs.alidns.model.v20150109.DescribeSubDomainRecordsResponse;
import com.aliyuncs.alidns.model.v20150109.UpdateDomainRecordRequest;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.utils.StringUtils;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main Class
 */
public class AliyunDynamicDNS {
    private static Logger log = LoggerFactory.getLogger(AliyunDynamicDNS.class);
    private static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private static IAcsClient client;
    private static CloseableHttpClient httpClient;
    private static Pattern ipPattern = Pattern.compile("\\d+\\.\\d+.\\d+\\.\\d+");

    public static void main(String[] args) {
        Config config;
        try {
            config = getConfig();
        } catch (Exception e) {
            log.error("Read config error: {}", e.getMessage(), e);
            return;
        }
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        if(!StringUtils.isEmpty(config.getProxy())) {
            httpClientBuilder.setProxy(HttpHost.create(config.getProxy()));
        }
        httpClient = httpClientBuilder.build();
        client = new DefaultAcsClient(DefaultProfile.getProfile(config.getRegionId(), config.getAccessKeyId(), config.getSecret()));
        log.info("Task start...");
        executor.scheduleWithFixedDelay(() -> {
            try {
                exec(config);
            } catch (RuntimeException e) {
                log.error("Task execution failed: {}", e.getMessage());
            } catch (Exception e) {
                log.error("Task execution failed: {}", e.getMessage(), e);
            }
        }, 0, config.getInterval(), TimeUnit.MINUTES);
    }

    private static void exec(Config config) throws ClientException, IOException {
        String ip = getIpAddress(config);
        if (ip == null) {
            return;
        }
        DescribeSubDomainRecordsResponse.Record record = getDomainRecord(config.getSubDomain());
        if (ip.equals(record.getValue())) {
            return;
        }
        log.info("Found local host address changes.");
        UpdateDomainRecordRequest request = new UpdateDomainRecordRequest();
        request.setRecordId(record.getRecordId());
        request.setRR(record.getRR());
        request.setValue(ip);
        request.setType(record.getType());
        request.setPriority(record.getPriority());
        request.setLine(record.getLine());
        client.getAcsResponse(request);
        log.info("Re-describe domain success: {} -> {}.", config.getSubDomain(), ip);
    }

    private static DescribeSubDomainRecordsResponse.Record getDomainRecord(String subDomain) throws ClientException {
        DescribeSubDomainRecordsRequest request = new DescribeSubDomainRecordsRequest();
        request.setSubDomain(subDomain);
        DescribeSubDomainRecordsResponse response = client.getAcsResponse(request);
        if (response.getDomainRecords().isEmpty()) {
            throw new RuntimeException("Domain record not exists.");
        }
        return response.getDomainRecords().get(0);
    }

    private static Config getConfig() throws IOException {
        String json = FileUtils.readFileToString(new File("config.json"), "UTF-8");
        return new Gson().fromJson(json, Config.class);
    }

    private static String getIpAddress(Config config) throws IOException {
        if(config.getIpSource() == 1) {
            return getNetworkInterfaceIp(config.getIpSegment());
        }
        if(config.getHttpUrls() == null) {
            return null;
        }
        for(String url : config.getHttpUrls()) {
            String ip = getHttpResponseIp(url);
            if(ip != null) {
                return ip;
            }
        }
        return null;
    }

    private static String getNetworkInterfaceIp(String ipSegment) throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface ni = interfaces.nextElement();
            Enumeration<InetAddress> address = ni.getInetAddresses();
            while (address.hasMoreElements()) {
                InetAddress nextElement = address.nextElement();
                String hostAddress = nextElement.getHostAddress();
                if (StringUtils.isEmpty(ipSegment)) {
                    return hostAddress;
                } else if (ipSegment.equals(hostAddress.substring(0, ipSegment.length()))) {
                    return hostAddress;
                }
            }
        }
        return null;
    }

    private static String getHttpResponseIp(String httpUrl) throws IOException {
        HttpGet httpGet = new HttpGet(httpUrl);
        CloseableHttpResponse response = httpClient.execute(httpGet);
        String result = EntityUtils.toString(response.getEntity());
        Matcher matcher = ipPattern.matcher(result);
        if(matcher.find()) {
            return matcher.group();
        }
        return null;
    }

}
