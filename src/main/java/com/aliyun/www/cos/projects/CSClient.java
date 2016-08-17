package com.aliyun.www.cos.projects;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;

import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;


public class CSClient {

    public char[] KEY_STORE_PASSWORD = "".toCharArray();
    public CertificateFactory cf;
    public Certificate caCert;
    public Certificate clientCert;
    public PEMKeyPair clientKeyPair;
    public PKCS8EncodedKeySpec spec;
    public KeyFactory kf;
    public PrivateKey clientKey;
    public KeyStore trustStore;
    public KeyStore keyStore;
    public CloseableHttpClient httpclient;



    public CSClient(String caCertS, String clientCertS, String ClientkeyS){
        try{
            setCredentials(caCertS, clientCertS, ClientkeyS);
            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(trustStore, null)
                    .loadKeyMaterial(keyStore, KEY_STORE_PASSWORD)
                    .build();
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                    sslContext,
                    SSLConnectionSocketFactory.getDefaultHostnameVerifier());
            //httpclient连接
            httpclient = HttpClients.custom()
                    .setSSLSocketFactory(sslsf)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CloseableHttpClient getHttpClient(){
        return httpclient;
    }

    public void setCredentials(String caCertS, String clientCertS, String ClientkeyS){
        InputStream caCertIn = new ByteArrayInputStream(caCertS.getBytes(Charset.forName("UTF-8")));
        InputStream clientCertIn = new ByteArrayInputStream(clientCertS.getBytes(Charset.forName("UTF-8")));
        InputStream clientKeyIn = new ByteArrayInputStream(ClientkeyS.getBytes(Charset.forName("UTF-8")));
        BufferedReader clientKeyReader = new BufferedReader(new InputStreamReader(clientKeyIn,Charset.defaultCharset()));
        try {
            cf = CertificateFactory.getInstance("X.509");
            caCert = cf.generateCertificate(caCertIn);
            clientCert = cf.generateCertificate(clientCertIn);
            clientKeyPair = (PEMKeyPair) new PEMParser(clientKeyReader).readObject();
            spec = new PKCS8EncodedKeySpec(
                    clientKeyPair.getPrivateKeyInfo().getEncoded());
            kf = KeyFactory.getInstance("RSA");
            clientKey = kf.generatePrivate(spec);
            //设置信任的证书
            trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            trustStore.setEntry("ca", new KeyStore.TrustedCertificateEntry(caCert), null);
            //设置私钥
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("client", clientCert);
            keyStore.setKeyEntry("key", clientKey, KEY_STORE_PASSWORD, new Certificate[]{clientCert});
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}

