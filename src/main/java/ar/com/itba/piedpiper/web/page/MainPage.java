package ar.com.itba.piedpiper.web.page;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.glassfish.jersey.client.ClientConfig;
import org.joda.time.LocalDateTime;

import ar.com.itba.piedpiper.model.entity.MeteoState;
import ar.com.itba.piedpiper.service.api.MeteoStateService;
import ar.com.itba.piedpiper.service.api.TransactionService;
import ar.com.itba.piedpiper.service.api.TransactionService.TransactionalOperationWithoutReturn;

@SuppressWarnings("serial")
public class MainPage extends AbstractWebPage {

	@SpringBean
	private MeteoStateService meteoStateService;
	
	@SpringBean
	private TransactionService transactions;
	
	private Label stateDate;
	private Label stateFilename;
	private Image image;
	
	@Override
	protected void onInitialize() {
		//client connect test
		super.onInitialize();
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
		 
		WebTarget webTarget = client.target("http://example.com/rest");
//		webTarget.register(FilterForExampleCom.class);
		WebTarget resourceWebTarget = webTarget.path("resource");
		WebTarget helloworldWebTarget = resourceWebTarget.path("helloworld");
		WebTarget helloworldWebTargetWithQueryParam =
		        helloworldWebTarget.queryParam("greeting", "Hi World!");
		 
		Invocation.Builder invocationBuilder =
		        helloworldWebTargetWithQueryParam.request(MediaType.TEXT_PLAIN_TYPE);
		invocationBuilder.header("some-header", "true");
		 
		Response response = invocationBuilder.get();
		System.out.println(response.getStatus());
		System.out.println(response.readEntity(String.class));
		
		final MeteoState lastState = meteoStateService.getLast();
		final LocalDateTime date = lastState.date();
		stateDate = new Label("stateDate", date);
		stateDate.setOutputMarkupId(true);
		stateFilename = new Label("stateFilename", lastState.filename());
		stateFilename.setOutputMarkupId(true);
		image = new NonCachingImage("image", Model.of(new DynamicImageResource() {
			@Override
			protected byte[] getImageData(Attributes attributes) {
				return meteoStateService.getLast().image();
			}
		}));
		image.setOutputMarkupId(true);
		add(stateDate);
		add(stateFilename);
		add(image);
		add(new AjaxLink<Void>("next") {
			@Override
			public void onClick(AjaxRequestTarget target) {
				transactions.execute(new TransactionalOperationWithoutReturn() {
					@Override
					public void execute() {
						final MeteoState state = meteoStateService.findOne(1).get();
						image.setDefaultModelObject(
							new NonCachingImage("image", new DynamicImageResource() {
								@Override
								protected byte[] getImageData(Attributes attributes) {
									return state.image();
								}
							})
						);
						stateDate.setDefaultModelObject(state.date());
						stateFilename.setDefaultModelObject(state.filename());
					}
				});
				target.add(stateDate);
				target.add(stateFilename);
				target.add(image);
			}
		});
		add(new AjaxLink<Void>("previous") {
			@Override
			public void onClick(AjaxRequestTarget target) {
				transactions.execute(new TransactionalOperationWithoutReturn() {
					@Override
					public void execute() {
						final MeteoState state = meteoStateService.findOne(2).get();
						image.setDefaultModelObject(
							new NonCachingImage("image", new DynamicImageResource() {
								@Override
								protected byte[] getImageData(Attributes attributes) {
									return state.image();
								}
							})	
						);
						stateDate.setDefaultModelObject(state.date());
						stateFilename.setDefaultModelObject(state.filename());
					}
				});
				target.add(stateDate);
				target.add(stateFilename);
				target.add(image);
			}
		});
	}
	
}
