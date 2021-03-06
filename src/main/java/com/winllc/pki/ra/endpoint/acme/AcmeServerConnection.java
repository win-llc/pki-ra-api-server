package com.winllc.pki.ra.endpoint.acme;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winllc.acme.common.Settings;
import com.winllc.acme.common.util.HttpCommandUtil;
import com.winllc.acme.common.domain.AcmeServerConnectionInfo;
import com.winllc.pki.ra.exception.AcmeConnectionException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class AcmeServerConnection {

    private static final Logger log = LogManager.getLogger(AcmeServerConnection.class);

    private static final String SAVE = "save";
    private static final String FIND_BY_NAME = "findSettingsByName";
    private static final String FIND_BY_ID = "findSettingsById";
    private static final String DELETE = "delete";
    private static final String FIND_ALL = "findAllSettings";

    private AcmeServerConnectionInfo connectionInfo;

    public AcmeServerConnection(AcmeServerConnectionInfo connectionInfo){
        this.connectionInfo = connectionInfo;
    }

    private AcmeServerConnection() {
    }

    public AcmeServerConnectionInfo getConnectionInfo() {
        return connectionInfo;
    }

    //base is either directory, ca, accountProvider
    public String saveEntity(Settings settings, String base) throws AcmeConnectionException {
        String url = connectionInfo.getUrl()+"/"+base+"/"+SAVE;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String json = objectMapper.writeValueAsString(settings);

            return runPost(url, json);
        } catch (JsonProcessingException e) {
            log.error("Could not process", e);
            throw new AcmeConnectionException(e);
        }
    }

    public <T> T getEntityByName(String base, String name, Class<T> clazz) throws AcmeConnectionException {
        String url = connectionInfo.getUrl()+"/"+base+"/"+FIND_BY_NAME+"/"+name;
        return getEntity(url, clazz);
    }

    public <T> T getEntityById(String base, String id, Class<T> clazz) throws AcmeConnectionException {
        String url = connectionInfo.getUrl()+"/"+base+"/"+FIND_BY_ID+"/"+id;
        return getEntity(url, clazz);
    }

    private <T> T getEntity(String url, Class<T> clazz) throws AcmeConnectionException {
        String result = runGet(url);

        if(StringUtils.isNotBlank(result)) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                T val = objectMapper.readValue(result, clazz);
                return val;
            } catch (Exception e) {
                log.error("Could not get entity by name", e);
                throw new AcmeConnectionException(e);
            }
        }else{
            return null;
        }
    }

    public <T> List<T> getAllEntitiesCustom(String path, Class<T> clazz) throws AcmeConnectionException {
        String url = connectionInfo.getUrl()+"/"+path;
        String result = runGet(url);

        if(result != null) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                List<LinkedHashMap> val = objectMapper.readValue(result, ArrayList.class);
                return val.stream()
                        .map(v -> objectMapper.convertValue(v, clazz))
                        .collect(Collectors.toList());
                //return objectMapper.convertValue(val, new TypeReference<List<T>>(){});
                //return val;
            } catch (Exception e) {
                log.error("Could not get allEntities", e);
                throw new AcmeConnectionException(e);
            }
        }else{
            throw new AcmeConnectionException("GET response was empty");
        }
    }

    public <T> List<T> getAllEntities(String base, Class<T> clazz) throws AcmeConnectionException{
        String path = base+"/"+FIND_ALL;
        return getAllEntitiesCustom(path, clazz);
    }

    public boolean deleteEntity(String base, String name) throws AcmeConnectionException {
        String url = connectionInfo.getUrl()+"/"+base+"/"+DELETE+"/"+name;
        return runDelete(url);
    }

    private String runPost(String url, String json) throws AcmeConnectionException {

        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(url);


        try {
            StringEntity jsonEntity = new StringEntity(json);
            httppost.setEntity(jsonEntity);
            httppost.setHeader("Accept", "application/json");
            httppost.setHeader("Content-type", "application/json");

            //Execute and get the response.
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                if(response.getStatusLine().getStatusCode() == 200){
                    StringWriter writer = new StringWriter();
                    String encoding = StandardCharsets.UTF_8.name();
                    IOUtils.copy(entity.getContent(), writer, encoding);

                    return writer.toString();
                }
            }
        }catch (Exception e){
            log.error("Could not delete entity", e);
            throw new AcmeConnectionException(e);
        }
        return null;
    }

    private String runGet(String url){
        HttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);

        try {
            httpGet.setHeader("Accept", "application/json");
            httpGet.setHeader("Content-type", "application/json");

            //Execute and get the response.
            HttpResponse response = httpclient.execute(httpGet);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                if(response.getStatusLine().getStatusCode() == 200){
                    return IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8.name());
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    private boolean runDelete(String url) throws AcmeConnectionException{
        HttpDelete httpDelete = new HttpDelete(url);

        try {
            HttpCommandUtil.processCustom(httpDelete, 200, result -> {return true;});
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new AcmeConnectionException(e);
        }
    }
}
