package ar.com.itba.piedpiper.web.page;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

@SuppressWarnings("serial")
public class HomePage extends AbstractWebPage {

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		add(new Label("welcome", Model.of("Use the menu up top to begin.")));
		restClientTest();
	}

	void restClientTest() {
		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("weather", "121212piedpiper");
		client.register(feature);
		WebTarget webTarget = client.target("http://weather.superfreak.com.ar");
		WebTarget resourceWebTarget = webTarget.path("argentina");
		Invocation.Builder invocationBuilder =
				resourceWebTarget.request(MediaType.APPLICATION_JSON);
		invocationBuilder.header("some-header", "true");
		Response response = invocationBuilder.get();
		System.out.println(response.getStatus());
		System.out.println(response.readEntity(String.class));
	}
	
	void restClient() {
		//ws connection tests
//		Client client = ClientBuilder.newClient();
//		HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("weather", "121212piedpiper");
//		client.register(feature);
//		WebTarget target = client.target("http://weather.superfreak.com.ar");
//
//		int response = target.path("argentina/").request().get().getStatus();

		ClientConfig clientConfig = new ClientConfig();
//		clientConfig.register(MyClientResponseFilter.class);
//		clientConfig.register(new AnotherClientFilter());
		 
		Client client = ClientBuilder.newClient(clientConfig);
//		client.register(ThirdClientFilter.class);
		 
		HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("weather", "121212piedpiper");
		client.register(feature);
		WebTarget webTarget = client.target("http://weather.superfreak.com.ar");
//		webTarget.register(FilterForExampleCom.class);
		WebTarget resourceWebTarget = webTarget.path("argentina");
		WebTarget helloworldWebTarget = resourceWebTarget.path("helloworld");
		WebTarget helloworldWebTargetWithQueryParam =
		        helloworldWebTarget.queryParam("greeting", "Hi World!");
		 
		Invocation.Builder invocationBuilder =
		        helloworldWebTargetWithQueryParam.request(MediaType.TEXT_PLAIN_TYPE);
		invocationBuilder.header("some-header", "true");
		 
		Response response = invocationBuilder.get();
		System.out.println(response.getStatus());
		System.out.println(response.readEntity(String.class));
	}

}
