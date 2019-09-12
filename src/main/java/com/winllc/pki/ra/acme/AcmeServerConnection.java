package com.winllc.pki.ra.acme;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winllc.acme.common.Settings;
import com.winllc.pki.ra.domain.AcmeServerConnectionInfo;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AcmeServerConnection {

    private static final String SAVE = "save";
    private static final String FIND_BY_NAME = "findSettingsByName";
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
    public String saveEntity(Settings settings, String base){
        String url = connectionInfo.getUrl()+"/"+base+"/"+SAVE;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String json = objectMapper.writeValueAsString(settings);

            return runPost(url, json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        //todo
        return null;
    }

    public <T> T getEntityByName(String base, String name, Class<T> clazz){
        String url = connectionInfo.getUrl()+"/"+base+"/"+FIND_BY_NAME+"/"+name;
        String result = runGet(url);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            T val = objectMapper.readValue(result, clazz);
            return val;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }


    public <T> List<T> getAllEntities(String base, Class<T> clazz){
        //todo
        String url = connectionInfo.getUrl()+"/"+base+"/"+FIND_ALL;
        String result = runGet(url);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            List<T> val = objectMapper.readValue(result, ArrayList.class);
            return val;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;

    }

    public void deleteEntity(String base, String name){
        //todo
        String url = connectionInfo.getUrl()+"/"+base+"/"+DELETE+"/"+name;
        String result = runDelete(url);

    }

    private String runPost(String url, String json){

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
                    //todo
                }
                try (InputStream instream = entity.getContent()) {
                    //TODO do something useful, return true or false
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            httppost.completed();
        }
        return null;
    }

    private String runGet(String url){
        //todo

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
                    //todo
                }

                return IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8.name());
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            httpGet.completed();
        }

        return null;
    }

    private String runDelete(String url){
        //todo
        return null;
    }
}
