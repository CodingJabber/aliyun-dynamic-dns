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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Main Class
 */
public class AliyunDynamicDNS {
    private static Logger logger = LoggerFactory.getLogger(AliyunDynamicDNS.class);
    private static IAcsClient client;

    public static void main(String[] args) {
        Config config;
        try {
            config = getConfig();
        } catch (Exception e) {
            logger.error("Read config error: {}", e.getMessage(), e);
            return;
        }
        client = new DefaultAcsClient(DefaultProfile.getProfile(config.getRegionId(), config.getAccessKeyId(), config.getSecret()));
        int interval = config.getInterval() * 60 * 1000;
        logger.info("Task start...");
        while (true) {
            try {
                exec(config.getSubDomain(), config.getIpSegment());
            } catch (RuntimeException e) {
                logger.error("Task execution failed: {}", e.getMessage());
            } catch (Exception e) {
                logger.error("Task execution failed: {}", e.getMessage(), e);
            }
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                logger.error("Task sleep error: {}", e.getMessage(), e);
                break;
            }
        }
        logger.info("Task terminated.");
    }

    private static void exec(String subDomain, String ipSegment) throws ClientException, SocketException {
        String ip = getIpAddress(ipSegment);
        if (ip == null) {
            return;
        }
        DescribeSubDomainRecordsResponse.Record record = getDomainRecord(subDomain);
        if (ip.equals(record.getValue())) {
            return;
        }
        logger.info("Found local host address changes.");
        UpdateDomainRecordRequest request = new UpdateDomainRecordRequest();
        request.setRecordId(record.getRecordId());
        request.setRR(record.getRR());
        request.setValue(ip);
        request.setType(record.getType());
        request.setPriority(record.getPriority());
        request.setLine(record.getLine());
        client.getAcsResponse(request);
        logger.info("Re-describe domain success: {} -> {}.", subDomain, ip);
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

    private static String getIpAddress(String ipSegment) throws SocketException {
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

}
