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
import org.apache.wicket.ajax.json.JSONArray;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import ar.com.itba.piedpiper.service.api.ConfigurationService;

@SuppressWarnings("serial")
public class HomePage extends AbstractWebPage {

	@SpringBean
	ConfigurationService configurations;
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		add(new Label("welcome", Model.of("Use the menu up top to begin.")));
		//WebTarget webTarget = setupApiConnection();
//		dumpToImageTest(webTarget);
//		restClientTest(webTarget);
//		JSONArray dates = stateDatesBetween(DateTime.parse("2016-06-16T15:00:00"), DateTime.parse("2016-06-16T20:00:00"), webTarget);
//		imagesForDates(dates, webTarget);
	}

	private WebTarget setupApiConnection() {
		String mainWebTargetPath = configurations.findByName("mainWebTargetPath").value();
		String username = configurations.findByName("username").value();
		String password = configurations.findByName("password").value();
		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic(username, password);
		client.register(feature);
		return client.target(mainWebTargetPath);
	}
	
	private void dumpToImageTest(WebTarget webTarget) {
		WebTarget resourceWebTarget = webTarget.path("argentina/2016-06-16T21:35:00/static/goeseast/ir2/image.png");
		Invocation.Builder invocationBuilder = resourceWebTarget.request(MediaType.APPLICATION_OCTET_STREAM);                                                  
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
			e.printStackTrace();
		}
	}
	
	private void restClientTest(WebTarget webTarget) {
		WebTarget resourceWebTarget = webTarget.path("argentina/2016-06-16T15:00:00/2016-06-16T20:00:00");
		Invocation.Builder invocationBuilder =
				resourceWebTarget.request(MediaType.APPLICATION_JSON);
		//invocationBuilder.header("some-header", "true");
		Response response = invocationBuilder.get();
		System.out.println(response.getStatus());
		System.out.println(response.readEntity(String.class));
	}
	
	private JSONArray stateDatesBetween(DateTime startTime, DateTime endtime, WebTarget webTarget) {
		String starTimeString = startTime.toString(ISODateTimeFormat.dateTimeNoMillis());
		String endTimeString = endtime.toString(ISODateTimeFormat.dateTimeNoMillis());
		WebTarget resourceWebTarget = webTarget.path("argentina/" + starTimeString + "/" + endTimeString);
		Invocation.Builder invocationBuilder =
				resourceWebTarget.request(MediaType.APPLICATION_JSON);
		Response response = invocationBuilder.get();
//		System.out.println(response.getStatus());
		return new JSONArray(response.readEntity(String.class));
	}

	private void imagesForDates(JSONArray dates, WebTarget webTarget) {
		for (int i = 0; i < dates.length(); i++) {
			String date = (String) dates.get(i);
			System.out.println(date);
			WebTarget resourceWebTarget = webTarget.path("argentina/" + date + "/static/goeseast/ir2/image.png");
			Invocation.Builder invocationBuilder = resourceWebTarget.request(MediaType.APPLICATION_OCTET_STREAM);                                                  
			Response response = invocationBuilder.get();
			InputStream input = (InputStream) response.getEntity();
			byte[] SWFByteArray;
			try {
				SWFByteArray = IOUtils.toByteArray(input);
				FileOutputStream fos = new FileOutputStream(new File(i + ".png"));
				fos.write(SWFByteArray);
				fos.flush();
				fos.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
}

