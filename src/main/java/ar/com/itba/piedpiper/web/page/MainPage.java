package ar.com.itba.piedpiper.web.page;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.json.JSONArray;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import com.madgag.gif.fmsware.AnimatedGifEncoder;

import ar.com.itba.piedpiper.service.api.ConfigurationService;
import ar.com.itba.piedpiper.web.panel.StateFilterPanel;
import ar.com.itba.piedpiper.web.panel.StateFilterPanel.StateFilterModel;
import ar.com.itba.piedpiper.web.res.ApplicationResources;

@SuppressWarnings("serial")
public class MainPage extends AbstractWebPage {

//	@SpringBean
//	private TransactionService transactions;
	
	@SpringBean
	private ConfigurationService configurations;
	
	private IModel<String> stateDateInfoModel;
	private IModel<PackageResourceReference> animationModel;
	private Image prediction;
	private StateFilterModel stateFilterModel;
//	private int currentImageNumber = 0;
	private int datesLength = 11;
	//XXX: Move to config
	private String separator = System.getProperty("file.separator");
	private String resourcePath = configurations.findByName("imagePath").value();
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		resourcePath = addEndingSeparator(resourcePath);
		stateDateInfoModel = Model.of("");
		Label stateDateInfo = new Label("stateDate", stateDateInfoModel);
		stateDateInfo.setOutputMarkupId(true);
		IModel<PackageResourceReference> trailModel = Model.of(new PackageResourceReference(ApplicationResources.class, "trails.png"));
		Image trail = new Image("trail", trailModel);
		animationModel = Model.of(new PackageResourceReference(ApplicationResources.class, "animation.gif"));
		NonCachingImage animation = new NonCachingImage("imageAnim", animationModel);
		animation.setOutputMarkupId(true);
		prediction = new Image("imagePred", Model.of(new PackageResourceReference(ApplicationResources.class, datesLength - 1 + ".png")));
		add(stateDateInfo).add(prediction).add(animation).add(trail);
		stateFilterModel = new StateFilterModel();
//		add(new AbstractAjaxTimerBehavior(Duration.milliseconds(200)) {
//            @Override
//            protected void onTimer(AjaxRequestTarget target) {
//            	imageModel = Model.of(
//				new PackageResourceReference(	
//					ApplicationResources.class, (currentImageNumber++) + ".png")
//				);
//            	if(currentImageNumber == datesLength - 1) {
//            		currentImageNumber = 0;
//            	}
//				animation.setDefaultModel(imageModel);
//				System.out.println(imageModel.getObject().getKey());
//				target.add(animation);
//            }
//        });
		add(new StateFilterPanel("filterPanel", stateFilterModel, this) {
			@Override
			public void onSearch(AjaxRequestTarget target) {
				//XXX: Get stuff through API here
				DateTime from = stateFilterModel.dataRangeModel().getObject().from();
				DateTime to = stateFilterModel.dataRangeModel().getObject().to();
				WebTarget webTarget = setupApiConnection();
				JSONArray dates = stateDatesBetween(from, to, webTarget);
				datesLength = dates.length();
				imagesForDatesToDisk(dates, webTarget);
				trailForImageToDisk(dates, webTarget);
				buildGif();
				stateDateInfoModel = Model.of("Playing " + datesLength + " frames from " + from.toString(ISODateTimeFormat.dateHourMinuteSecond()) + " to " + to.toString(ISODateTimeFormat.dateHourMinuteSecond()) + ".");
				stateDateInfo.setDefaultModel(stateDateInfoModel);
				target.add(animation, stateDateInfo);
				setResponsePage(MainPage.class);
			}
		});
//		add(new AjaxLink<Void>("next") {
//			@Override
//			public void onClick(AjaxRequestTarget target) {
//				transactions.execute(new TransactionalOperationWithoutReturn() {
//					@Override
//					public void execute() {
//						animationModel = Model.of(
//							new PackageResourceReference(
//								ApplicationResources.class,  ((currentImageNumber < datesLength-1) ? currentImageNumber++ : currentImageNumber) + ".png")
//						);
//						animation.setDefaultModel(animationModel);
//						System.out.println(animationModel.getObject().getKey());
//					}
//				});
//				target.add(stateDateInfo, animation);
//			}
//		});
//		add(new AjaxLink<Void>("previous") {
//			@Override
//			public void onClick(AjaxRequestTarget target) {
//				transactions.execute(new TransactionalOperationWithoutReturn() {
//					@Override
//					public void execute() {
//						animationModel = Model.of(
//							new PackageResourceReference(	
//								ApplicationResources.class, ((currentImageNumber > 0) ? currentImageNumber-- : currentImageNumber) + ".png")
//							);
//						animation.setDefaultModel(animationModel);
//						System.out.println(animationModel.getObject().getKey());
//					}
//				});
//				target.add(stateDateInfo, animation);
//			}
//		});
	}
	
	private String addEndingSeparator(String resourcePath) {
		return String.valueOf(resourcePath.charAt(resourcePath.length() - 1)) == separator ? resourcePath : resourcePath + separator;
	}

	private JSONArray stateDatesBetween(DateTime startTime, DateTime endTime, WebTarget webTarget) {
		String startTimeString = startTime.toString(ISODateTimeFormat.dateTimeNoMillis());
		String endTimeString = endTime.toString(ISODateTimeFormat.dateTimeNoMillis());
		WebTarget resourceWebTarget = webTarget.path("argentina/" + startTimeString + "/" + endTimeString);
		Invocation.Builder invocationBuilder = resourceWebTarget.request(MediaType.APPLICATION_JSON);
		Response response = invocationBuilder.get();
		return new JSONArray(response.readEntity(String.class));
	}

	private void imagesForDatesToDisk(JSONArray dates, WebTarget webTarget) {
		for (int i = 0; i < dates.length(); i++) {
			String date = (String) dates.get(i);
			System.out.println(date);
			WebTarget resourceWebTarget = webTarget.path("argentina/" + date + "/static/goeseast/ir2/image.png");
			Invocation.Builder invocationBuilder = resourceWebTarget.request(MediaType.APPLICATION_OCTET_STREAM);                                                  
			Response response = invocationBuilder.get();
			try {
				byte[] SWFByteArray;
				SWFByteArray = IOUtils.toByteArray((InputStream) response.getEntity());
				FileOutputStream fos = new FileOutputStream(new File(resourcePath + i + ".png"));
				fos.write(SWFByteArray);
				fos.flush();
				fos.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void trailForImageToDisk(JSONArray dates, WebTarget webTarget) {
		String startTime = (String) dates.get(1);
		String endTime= (String) dates.get(dates.length() - 1);
		WebTarget resourceWebTarget = webTarget.path("argentina/" + startTime + "/" + endTime + "/trails.png");
		Invocation.Builder invocationBuilder = resourceWebTarget.request(MediaType.APPLICATION_OCTET_STREAM);                                                  
		Response response = invocationBuilder.get();
		try {
			byte[] SWFByteArray;
			SWFByteArray = IOUtils.toByteArray((InputStream) response.getEntity());
			FileOutputStream fos = new FileOutputStream(new File(resourcePath + "trails.png"));
			fos.write(SWFByteArray);
			fos.flush();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
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
	
	private void buildGif() {
		AnimatedGifEncoder gif = new AnimatedGifEncoder();
		gif.setRepeat(0);
		gif.setDelay(100);
		gif.start(resourcePath + "/animation.gif");
		for (int i = 0; i < datesLength; i++) {
			try {
				BufferedImage image;
				image = ImageIO.read(new File(resourcePath + i +".png"));
				gif.addFrame(image);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		gif.finish();
	}
}
