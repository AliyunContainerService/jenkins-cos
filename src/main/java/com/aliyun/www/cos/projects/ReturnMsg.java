package com.aliyun.www.cos.projects;

/**
 * Created by qinyujia on 16/8/17.
 */
public class ReturnMsg {
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
