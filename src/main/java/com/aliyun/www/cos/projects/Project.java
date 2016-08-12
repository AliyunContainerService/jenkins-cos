package com.aliyun.www.cos.projects;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Project {
    public String masterurl;
    public String caCertS;
    public String clientCertS;
    public String clientkeyS;
    public List<String> projectNameList = new ArrayList<String>();
    public CSClient csClient;
    CloseableHttpClient httpclient;


    public class ReturnMsg{
        boolean isSuccess = true;
        Integer returnCode;
        String detailMsg = null;

        public void setIsSuccess(boolean isSuccess){this.isSuccess = isSuccess;}
        public boolean getIsSuccess(){return isSuccess;}
        public void setReturnCode(Integer returnCode){this.returnCode = returnCode;}
        public Integer getReturnCode(){return returnCode;}
        public void setDetailMsg(String detailMsg){this.detailMsg = detailMsg;}
        public String getDetailMsg(){return detailMsg;}
    }

    public Project(String url,String caCertS, String clientCertS,String clientkeyS){
        masterurl=url + "/projects/";
        this.caCertS=caCertS;
        this.clientCertS = clientCertS;
        this.clientkeyS = clientkeyS;
        csClient = new CSClient(caCertS,clientCertS,clientkeyS);
        httpclient = csClient.getHttpClient();
        GetProjects();
    };

    public int IsProjectExist(String appname) {
        int flag = -1;
        for(int i = 0; i < projectNameList.size() && flag != 0; i++) {
            flag = projectNameList.get(i).compareTo(appname);
        }
        return flag;
    }

    public void GetProjects(){
        try {
            HttpGet httpGet = new HttpGet(masterurl);
            CloseableHttpResponse response = httpclient.execute(httpGet);
            String bodyAsString = EntityUtils.toString(response.getEntity());
            JSONArray array = JSONArray.fromObject(bodyAsString);
            for (int i = 0; i < array.size(); i++) {
                projectNameList.add(array.getJSONObject(i).get("name").toString());
            }
            response.close();
            httpGet.abort();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String QueryProjectStatus(String projectName){
        String queryUrl = masterurl + projectName;
        String status = null;
        try {
            HttpGet httpGet = new HttpGet(queryUrl);
            CloseableHttpResponse response = httpclient.execute(httpGet);
            String bodyAsString = EntityUtils.toString(response.getEntity());
            System.out.println(bodyAsString);
            JSONObject object = JSONObject.fromObject(bodyAsString);
            status = object.get("current_state").toString();
            response.close();
            httpGet.abort();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return status;
    }

    public ArrayList<String> ListAllProjects() {
        return (ArrayList<String>) projectNameList;
    }

    public ReturnMsg AddProject(String projectName, String compose) {
        ReturnMsg returnMsg = new ReturnMsg();
        int returnCode = 0;
        CloseableHttpResponse response = null;
        HttpPost httpPost = GenerateHttpPost(masterurl, compose, projectName);
        try{
            response = httpclient.execute(httpPost);
            returnCode = response.getStatusLine().getStatusCode();
            returnMsg.setReturnCode(Integer.valueOf(returnCode));
            returnMsg.setDetailMsg(EntityUtils.toString(response.getEntity()));
            if(returnCode == 201) {
                projectNameList.add(projectName);
                returnMsg.setIsSuccess(true);
            } else {
                returnMsg.setIsSuccess(false);
            }
            response.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        return returnMsg;
    }

    public ReturnMsg RefreshProject(String projectName, String compose, String version){
        ReturnMsg returnMsg = new ReturnMsg();
        int returnCode = 0;
        CloseableHttpResponse response;
        String updateUrl = masterurl + projectName+ "/update";
        HttpPost httpPost = GenerateHttpPost(updateUrl, compose, projectName, version);
        try {
            response = httpclient.execute(httpPost);
            returnCode = response.getStatusLine().getStatusCode();
            returnMsg.setReturnCode(Integer.valueOf(returnCode));
            returnMsg.setDetailMsg(EntityUtils.toString(response.getEntity()));
            if (returnCode == 202) {
                returnMsg.setIsSuccess(true);
            } else {
                returnMsg.setIsSuccess(false);
            }
            response.close();
            httpPost.abort();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  returnMsg;
    }

    public HttpPost GenerateHttpPost(String url, String yamlPathName, String projectName) {
        HttpPost httpPost = new HttpPost(url);
        File yamlFile = new File(yamlPathName);
        String yamlString = new String();
        try {
            FileReader fr = new FileReader(yamlFile);
            StringBuilder sb = new StringBuilder();
            BufferedReader bfr = new BufferedReader(fr);
            for (String s = bfr.readLine(); s != null; s = bfr.readLine())
                sb.append(s).append('\n');
            yamlString = sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("name", projectName);
        jsonParam.put("template", yamlString);
        System.out.println(jsonParam.toString());
        if (null != jsonParam) {
                StringEntity entity = new StringEntity(jsonParam.toString(),"utf-8");
                entity.setContentEncoding("UTF-8");
                entity.setContentType("application/json");
                httpPost.setEntity(entity);
            }
        return httpPost;
        }

    public HttpPost GenerateHttpPost(String url, String compose, String projectName, String version){
        HttpPost httpPost = new HttpPost(url);
//        File yamlFile = new File(yamlPathName);
//        String yamlString = new String();
//        try {
//            FileReader fr = new FileReader(yamlFile);
//            StringBuilder sb = new StringBuilder();
//            BufferedReader bfr = new BufferedReader(fr);
//            for (String s = bfr.readLine(); s != null; s = bfr.readLine())
//                sb.append(s).append('\n');
//            yamlString = sb.toString();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("name",projectName);
        jsonParam.put("template",compose);
        jsonParam.put("version",version);
        System.out.println(jsonParam.toString());
        if(null!=jsonParam){
            StringEntity entity = new StringEntity(jsonParam.toString(),"utf-8");
            entity.setContentEncoding("UTF-8");
            entity.setContentType("application/json");
            httpPost.setEntity(entity);
        }
        return httpPost;
    }

    public void destroy(){
        try {
            httpclient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
