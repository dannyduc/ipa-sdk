package com.ingenuity.ipa.sdk.uploader;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.apache.tika.io.IOUtils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class IpaUploaderApp {

    private static final Logger logger = Logger.getLogger(IpaUploaderApp.class);

    private static final String SSO_ENDPOINT = "https://apps.ingenuity.com";
    private static final String IPA_ENDPOINT = "https://analysis.ingenuity.com";

//    private static final String SSO_ENDPOINT = "https://apps-stable.ingenuity.com";
//    private static final String IPA_ENDPOINT = "https://analysis-stable.ingenuity.com";

    public static void main(String[] args) throws Exception {

        if (args.length < 3) {
            System.out.println("Usage: IpaUploaderApp <username> <password> \"<htmlUploadFile>\"");
            System.exit(1);
        }

        String username = args[0];
        String password = args[1];

        HttpClient httpClient = create();

        login(httpClient, username, password);

        for (int i = 2; i < args.length; i++) {
            String pathToHtmlFile = args[i];
            upload(httpClient, pathToHtmlFile);
        }
    }

    private static void upload(HttpClient httpClient, String pathToHtmlFile) throws Exception {

        UrlEncodedFormEntity urlEncodedFormEntity = load(pathToHtmlFile);

        HttpPost httpPost = new HttpPost(IPA_ENDPOINT + "/pa/api/v2/dataanalysis");
        httpPost.setEntity(urlEncodedFormEntity);
        HttpResponse response = httpClient.execute(httpPost);

        HttpEntity responseEntity = response.getEntity();
        InputStream stream = responseEntity.getContent();
        String content = IOUtils.toString(stream);
//        System.out.println(IOUtils.toString(stream));
        EntityUtils.consume(responseEntity);

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 200) {
            logger.info("uploaded file " + pathToHtmlFile);
        } else {
            logger.error("got http status code " + statusCode);
        }
    }

    private static UrlEncodedFormEntity load(String filename) throws Exception {
        HtmlFormParser htmlFormParser = new HtmlFormParser();

        InputStream stream = new FileInputStream(filename);
        htmlFormParser.parse(stream);
        stream.close();

//        System.out.println(htmlFormParser.getAction());

        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        for (Input input : htmlFormParser.getInputs()) {
            nameValuePairs.add(new BasicNameValuePair(input.getName(), input.getValue()));
        }

        return new UrlEncodedFormEntity(nameValuePairs);
    }

    private static HttpClient create() {
        DefaultHttpClient  httpClient = new DefaultHttpClient();
        httpClient.setRedirectStrategy(new DefaultRedirectStrategy() {
            public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context)  {
                boolean isRedirect=false;
                try {
                    isRedirect = super.isRedirected(request, response, context);
                } catch (ProtocolException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if (!isRedirect) {
                    int responseCode = response.getStatusLine().getStatusCode();
                    if (responseCode == 301 || responseCode == 302) {
                        return true;
                    }
                }
                return isRedirect;
            }
        });
        return httpClient;
    }

    private static void login(HttpClient httpClient, String username, String password) throws Exception {
        HttpGet httpGet = new HttpGet(SSO_ENDPOINT + "/ingsso/login");
        HttpResponse response = httpClient.execute(httpGet);
        HttpEntity entity = response.getEntity();

        InputStream stream = entity.getContent();

        HtmlFormParser htmlFormParser = new HtmlFormParser();
        htmlFormParser.parse(stream);

        EntityUtils.consume(entity);

        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("username", username));
        nameValuePairs.add(new BasicNameValuePair("password", password));
        nameValuePairs.add(new BasicNameValuePair("rememberMe", htmlFormParser.getInput("rememberMe").getValue()));
        nameValuePairs.add(new BasicNameValuePair("lt", htmlFormParser.getInput("lt").getValue()));
        nameValuePairs.add(new BasicNameValuePair("_eventId", htmlFormParser.getInput("_eventId").getValue()));
        nameValuePairs.add(new BasicNameValuePair("submit", htmlFormParser.getInput("submit").getValue()));

        String postLoginUrl = SSO_ENDPOINT + htmlFormParser.getAction();
        HttpPost httpPost = new HttpPost(postLoginUrl);
        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        response = httpClient.execute(httpPost);
        entity = response.getEntity();

        stream = entity.getContent();

        LoginSuccessParser loginSuccessParser = new LoginSuccessParser();
        loginSuccessParser.parse(stream);

        EntityUtils.consume(entity);

        boolean loginSuccessful = loginSuccessParser.isSuccess();
        if (loginSuccessful) {
            logger.info("Successfully log into " + postLoginUrl);
        } else {
            logger.error("Unable to log into " + postLoginUrl + " with username " + username);
            throw new RuntimeException("Unable to log into IPA");
        }

    }
}
