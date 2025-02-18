package com.igalblech.igalsquizserver.network;

import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import org.java_websocket.server.WebSocketServer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Paths;
import java.security.KeyStore;

import javax.net.ssl.TrustManagerFactory;

@Deprecated
public class SSLHandler {


    public static DefaultSSLWebSocketServerFactory getContext() throws Exception {
        // load up the key store
        String STORETYPE = "JKS";
        String KEYSTORE = Paths.get("src", "test", "java", "org", "java_websocket", "keystore.jks")
                .toString();
        String STOREPASSWORD = "123456";
        String KEYPASSWORD = "123456";

        KeyStore ks = KeyStore.getInstance(STORETYPE);
        File kf = new File("keystore.jks");
        ks.load(new FileInputStream(kf), STOREPASSWORD.toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, KEYPASSWORD.toCharArray());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext sslContext = null;
        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return new DefaultSSLWebSocketServerFactory(sslContext);
    }

}
