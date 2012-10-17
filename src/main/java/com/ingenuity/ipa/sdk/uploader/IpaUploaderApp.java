package com.ingenuity.ipa.sdk.uploader;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class IpaUploaderApp {

    private static final Logger logger = Logger.getLogger(IpaUploaderApp.class);

    private static final String SSO_ENDPOINT = "https://apps.ingenuity.com";
    private static final String IPA_ENDPOINT = "https://analysis.ingenuity.com";

//    private static final String SSO_ENDPOINT = "https://apps-stable.ingenuity.com";
//    private static final String IPA_ENDPOINT = "https://analysis-stable.ingenuity.com";

    public static void main(String[] args) throws Exception {

        if (args.length < 4) {
            System.out.println("Usage: IpaUploaderApp <proxyHost:port> <username> <password> \"<htmlUploadFile>\"");
            System.exit(1);
        }

        HttpClient httpClient = create();

        configureHttpProxy(args[0], httpClient);

        String username = args[1];
        String password = args[2];

        login(httpClient, username, password);

        for (int i = 3; i < args.length; i++) {
            String pathToHtmlFile = args[i];
            upload(httpClient, pathToHtmlFile);
        }
    }

    private static void configureHttpProxy(String config, HttpClient httpClient) {
        String host = null;
        int port = -1;
        if (config.contains(":")) {
            String[] hostAndPort = config.split(":");
            host = hostAndPort[0].trim();
            try {
                port = Integer.parseInt(hostAndPort[1]);
            } catch (Exception ignore) {}
        }
        if (StringUtils.hasLength(host) && port != -1) {
            HttpHost httpProxy = new HttpHost(host, port);
            httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, httpProxy);
            logger.info("proxy configured to " + httpProxy);
        } else {
            logger.info("proxy not configured for config string: " + config);
        }
    }

    private static void upload(HttpClient httpClient, String pathToHtmlFile) throws Exception {

        UrlEncodedFormEntity urlEncodedFormEntity = load(pathToHtmlFile);

        HttpPost httpPost = new HttpPost(IPA_ENDPOINT + "/pa/api/v2/dataanalysis");
        httpPost.setEntity(urlEncodedFormEntity);
        HttpResponse response = httpClient.execute(httpPost);

        HttpEntity responseEntity = response.getEntity();
//        InputStream stream = responseEntity.getContent();
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

    private static HttpClient create() {
        DefaultHttpClient  httpClient = new DefaultHttpClient();
//        httpClient = useTrustingTrustManager(httpClient);
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

    /**
     * for testing in burp proxy
     */
    public static DefaultHttpClient useTrustingTrustManager(DefaultHttpClient httpClient) {
        try {
            // First create a trust manager that won't care.
            X509TrustManager trustManager = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                    // Don't do anything.
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                    // Don't do anything.
                }

                public X509Certificate[] getAcceptedIssuers() {
                    // Don't do anything.
                    return null;
                }
            };

            // Now put the trust manager into an SSLContext.
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, new TrustManager[]{trustManager}, null);

            // Use the above SSLContext to create your socket factory
            // (I found trying to extend the factory a bit difficult due to a
            // call to createSocket with no arguments, a method which doesn't
            // exist anywhere I can find, but hey-ho).
            SSLSocketFactory sf = new SSLSocketFactory(sslcontext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            // If you want a thread safe client, use the ThreadSafeConManager, but
            // otherwise just grab the one from the current client, and get hold of its
            // schema registry. THIS IS THE KEY THING.
            ClientConnectionManager ccm = httpClient.getConnectionManager();
            SchemeRegistry schemeRegistry = ccm.getSchemeRegistry();

            // Register our new socket factory with the typical SSL port and the
            // correct protocol name.
            schemeRegistry.register(new Scheme("https", 443, sf));

            // Finally, apply the ClientConnectionManager to the Http Client
            // or, as in this example, create a new one.
            return new DefaultHttpClient(ccm, httpClient.getParams());
        } catch (Throwable t) {
            // AND NEVER EVER EVER DO THIS, IT IS LAZY AND ALMOST ALWAYS WRONG!
            t.printStackTrace();
            return null;
        }
    }
}
