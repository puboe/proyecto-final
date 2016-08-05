package ar.com.itba;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;

public class Start {
    public static void main(String[] args) throws Exception {
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setSoLingerTime(-1);
        connector.setPort(7001);
        server.addConnector(connector);
        Resource keystore = Resource.newClassPathResource("/keystore");
        if (keystore != null && keystore.exists()) {
            SslContextFactory factory = new SslContextFactory();
            factory.setKeyStoreResource(keystore);
            factory.setKeyStorePassword("wicket");
            factory.setTrustStoreResource(keystore);
            factory.setKeyManagerPassword("wicket");
            System.out.println("SSL access to the quickstart has been enabled on port 8443");
            System.out.println("You can access the application using SSL on https://localhost:8443");
            System.out.println();
        }

        WebAppContext bb = new WebAppContext();
        bb.setServer(server);
        bb.setContextPath("/");
        bb.setWar("src/main/webapp");

        server.setHandler(bb);
        try {
            System.out.println(">>> STARTING EMBEDDED JETTY SERVER, PRESS ANY KEY TO STOP");
            server.start();
            System.in.read();
            System.out.println(">>> STOPPING EMBEDDED JETTY SERVER");
            server.stop();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
