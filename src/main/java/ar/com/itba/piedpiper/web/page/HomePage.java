package ar.com.itba.piedpiper.web.page;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import ar.com.itba.piedpiper.service.api.ConfigurationService;

@SuppressWarnings("serial")
public class HomePage extends AbstractWebPage {

	@SpringBean
	ConfigurationService configurations;
	
	private String mainWebTargetPath;
	
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
	}

	@Override
	protected void onInitialize() {
		mainWebTargetPath = configurations.findByName("mainWebTargetPath").value();
		super.onInitialize();
		add(new Label("welcome", Model.of("Use the menu up top to begin.")));
		dumpToImageTest();
		//restClientGetImageTest();
//		restClient();
	}

	void dumpToImageTest() {
		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("weather", "121212piedpiper");
		client.register(feature);
		WebTarget webTarget = client.target(mainWebTargetPath + "/argentina/2016-06-16T21:35:00/static/goeseast/ir2/image.png");
		Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_OCTET_STREAM);                                                  
		Response response = invocationBuilder.get();
		InputStream input = (InputStream)response.getEntity();
		byte[] SWFByteArray;
		try {
			SWFByteArray = IOUtils.toByteArray(input);
			FileOutputStream fos = new FileOutputStream(new File("myfile.png"));
			fos.write(SWFByteArray);
			fos.flush();
			fos.close();
		} catch (Exception e) {
		}
		
	}
	
	void restClientGetImageTest() {
		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("weather", "121212piedpiper");
		client.register(feature);
		WebTarget webTarget = client.target(mainWebTargetPath + "/argentina/2016-06-16T21:35:00/static/goeseast/ir2/image.png");
//		WebTarget resourceWebTarget = webTarget.path("argentina");
		Invocation.Builder invocationBuilder =
				webTarget.request();
		//invocationBuilder.header("some-header", "true");
		Response response = invocationBuilder.get();
		System.out.println(response.getStatus());
		System.out.println(response.readEntity(String.class));
	}
	
	void restClientTest() {
		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("weather", "121212piedpiper");
		client.register(feature);
		WebTarget webTarget = client.target(mainWebTargetPath);
		WebTarget resourceWebTarget = webTarget.path("argentina");
		Invocation.Builder invocationBuilder =
				resourceWebTarget.request(MediaType.APPLICATION_JSON);
		//invocationBuilder.header("some-header", "true");
		Response response = invocationBuilder.get();
		System.out.println(response.getStatus());
		System.out.println(response.readEntity(String.class));
	}
	
}
