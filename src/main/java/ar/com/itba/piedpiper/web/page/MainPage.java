package ar.com.itba.piedpiper.web.page;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.json.JSONArray;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.cookies.CookieUtils;
import org.apache.wicket.util.time.Duration;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import com.madgag.gif.fmsware.AnimatedGifEncoder;

import ar.com.itba.piedpiper.model.entity.Channel;
import ar.com.itba.piedpiper.model.entity.SavedState;
import ar.com.itba.piedpiper.service.api.ConfigurationService;
import ar.com.itba.piedpiper.service.api.TransactionService;
import ar.com.itba.piedpiper.service.api.TransactionService.TransactionalOperationWithoutReturn;
import ar.com.itba.piedpiper.web.panel.StateFilterPanel;
import ar.com.itba.piedpiper.web.panel.StateFilterPanel.StateFilterModel;
import ar.com.itba.piedpiper.web.util.ImageResource;
import de.agilecoders.wicket.core.markup.html.bootstrap.common.NotificationPanel;

@SuppressWarnings("serial")
public class MainPage extends AbstractWebPage {

	@SpringBean
	private ConfigurationService configurations;
	
	@SpringBean
	private TransactionService transactions;
	
	private StateFilterModel stateFilterModel;
	private int datesLength = 0;
	// XXX: Move to config
	private final String trailsFilename = "trails.png";
	private final String arrowsFilename = "arrows.png";
	private final String imageFilename = "image.png";
	private final String differenceFilename = "diff_image.png";
	private final String enhancedImageFilename = "image_enhanced.png";
	private final String mapFilename = "map_image.png";
	private final String predictionFilename = "prediction.png";
	private List<InputStream> streams;
	private NotificationPanel feedback;
	private final String resourcePath = configurations.findByName("imagePath").value();

	public MainPage() {
		// Required by Navbar
	}
	
	public MainPage(DateTime dateTime, int steps, Channel channel, Boolean enhanced) {
		WebTarget webTarget = setupApiConnection();
		JSONArray dates = statesUpTo(dateTime, steps, webTarget);
		datesLength = dates.length();
		imagesForDatesToDisk(dates, webTarget, resourcePath, channel, enhanced);
		zoneMapToDisk(webTarget, resourcePath);
		dumpToDiskBySteps(dates, steps, webTarget, resourcePath, arrowsFilename);
		dumpToDiskBySteps(dates, steps, webTarget, resourcePath, trailsFilename);
		singlemageToDisk(dates.get(dates.length()-1).toString(), webTarget, resourcePath, imageFilename, predictionFilename);
		singlemageToDisk(dates.get(dates.length()-1).toString(), webTarget, resourcePath, differenceFilename, differenceFilename);
		buildGif(resourcePath);
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		createDir(resourcePath);
		feedback = new NotificationPanel("feedback");
		feedback.setOutputMarkupId(true);
		feedback.hideAfter(Duration.seconds(5));
		add(feedback);
		IModel<String> stateDateInfoModel = Model.of("");
		Label stateDateInfo = new Label("stateDate", stateDateInfoModel);
		stateDateInfo.setOutputMarkupId(true);
		IModel<DynamicImageResource> mapModel = Model.of(new ImageResource(resourcePath, mapFilename));
		Image animationMap = new Image("animationMap", mapModel);
		animationMap.setOutputMarkupId(true);
		IModel<DynamicImageResource> arrowsModel = Model.of(new ImageResource(resourcePath, arrowsFilename));
		Image arrows = new Image("arrows", arrowsModel);
		arrows.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true);
		IModel<DynamicImageResource> trailModel = Model.of(new ImageResource(resourcePath, trailsFilename));
		Image trails = new Image("trails", trailModel);
		trails.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true);
		IModel<ImageResource> animationModel = Model.of(new ImageResource(resourcePath, "animation.gif"));
		Image animation = new Image("imageAnim", animationModel);
		animation.setOutputMarkupId(true);
		IModel<ImageResource> predictionModel = Model.of(new ImageResource(resourcePath, predictionFilename));
		Image predictionMap = new Image("predictionMap", mapModel);
		predictionMap.setOutputMarkupId(true);
		Image prediction = new Image("prediction", predictionModel);
		prediction.setOutputMarkupId(true);
		IModel<ImageResource> lastStateModel = Model.of(new ImageResource(resourcePath, datesLength-1 + ".png"));
		Image lastState = new Image("lastState", lastStateModel);
		lastState.setOutputMarkupId(true);
		IModel<ImageResource> differenceModel = Model.of(new ImageResource(resourcePath, differenceFilename));
		Image difference = new Image("difference", differenceModel);
		difference.setOutputMarkupId(true);
		add(new AjaxLink<Void>("trailsToggle") {
			@Override
			public void onClick(AjaxRequestTarget target) {
				target.add(trails.setVisible(!trails.isVisible()));
			}
		});
		add(new AjaxLink<Void>("arrowsToggle") {
			@Override
			public void onClick(AjaxRequestTarget target) {
				target.add(arrows.setVisible(!arrows.isVisible()));
			}
		});
		add(new AjaxLink<Void>("saveState") {
			@Override
			public void onClick(AjaxRequestTarget target) {
				transactions.execute(new TransactionalOperationWithoutReturn() {
					@Override
					public void execute() {
						DateTime dateTime = new DateTime(stateFilterModel.toModelObject());
						int steps = new Integer(stateFilterModel.stepsModelObject());
						Channel channel = stateFilterModel.channelModelObject();
						boolean enhanced = stateFilterModel.enhancedModelObject();
						SavedState savedState = new SavedState(dateTime, steps, channel, enhanced);
//							if(ApplicationSession.get().saveState(savedState)) {
							if(true) {
								System.out.println();
								Cookie cookie = new Cookie(savedState.toString(), "1");
								((WebResponse) getRequestCycle().getResponse()).addCookie(cookie);
								//test
//								SavedState newstate = new SavedState(savedState.toString());
								success("Estado guardado!");
							} 
//							else {
//								error("State is already saved.");
//							}
						target.add(feedback);
					}
				});
			}
		});
		add(stateDateInfo, prediction, predictionMap, animation, trails, animationMap, arrows, lastState, difference);
		stateFilterModel = new StateFilterModel(configurations.findByName("startupStates").value());
		add(new StateFilterPanel("filterPanel", stateFilterModel, this) {
			@Override
			public void onSearch(AjaxRequestTarget target) {
				// XXX: Get stuff through API here
				resetDir(resourcePath);
				DateTime dateTime = new DateTime(stateFilterModel.toModelObject());
				int steps = new Integer(stateFilterModel.stepsModelObject());
				Channel channel = stateFilterModel.channelModelObject();
				boolean enhanced = stateFilterModel.enhancedModelObject();
				WebTarget webTarget = setupApiConnection();
				JSONArray dates = statesUpTo(dateTime, steps, webTarget);
				datesLength = dates.length();
				imagesForDatesToDisk(dates, webTarget, resourcePath, channel, enhanced);
				zoneMapToDisk(webTarget, resourcePath);
				dumpToDiskBySteps(dates, steps, webTarget, resourcePath, arrowsFilename);
				dumpToDiskBySteps(dates, steps, webTarget, resourcePath, trailsFilename);
				singlemageToDisk(dates.get(dates.length()-1).toString(), webTarget, resourcePath, imageFilename, predictionFilename);
				singlemageToDisk(dates.get(dates.length()-1).toString(), webTarget, resourcePath, differenceFilename, differenceFilename);
				buildGif(resourcePath);
				stateDateInfo.setDefaultModel(
					Model.of("Mostrando " + datesLength + " cuadros desde " + dates.get(1) + " hasta to "	+ dates.get(dates.length() - 1) + "en el canal " + channel.name() + ".")
				);
				lastState.setDefaultModel(Model.of(new ImageResource(resourcePath, datesLength-1 + ".png")));
				target.add(animation, stateDateInfo, trails, prediction, predictionMap, animationMap, arrows, lastState, difference);
			}
		});
	}

	private JSONArray buildJSONArray(WebTarget webTarget, String path) {
		WebTarget resourceWebTarget = webTarget.path(path);
		Invocation.Builder invocationBuilder = resourceWebTarget.request(MediaType.APPLICATION_JSON);
		Response response = invocationBuilder.get();
		return new JSONArray(response.readEntity(String.class));
	}

	private void singlemageToDisk(String dateTime, WebTarget webTarget, String resourcePath, String resourceName, String fileName) {
		String path = "argentina/" + dateTime + "/prediction/" + resourceName;
		System.out.println("Requesting: " + path);
		WebTarget resourceWebTarget = webTarget.path(path);
		Invocation.Builder invocationBuilder = resourceWebTarget.request(MediaType.APPLICATION_OCTET_STREAM);
		Response response = invocationBuilder.get();
		try {
			byte[] SWFByteArray;
			SWFByteArray = IOUtils.toByteArray((InputStream) response.getEntity());
			FileOutputStream fos = new FileOutputStream(new File(resourcePath + fileName));
			fos.write(SWFByteArray);
			fos.flush();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void imagesForDatesToDisk(JSONArray dates, WebTarget webTarget, String resourcePath, Enum<?> channel, Boolean enhanced) {
		for (int i = 0; i < dates.length(); i++) {
			String date = (String) dates.get(i);
			String path = "argentina/" + date + "/static/goeseast/"+ ((Channel) channel).value() +
				"/" + (enhanced ? enhancedImageFilename : imageFilename);
			System.out.println("Requesting: " + path);
			WebTarget resourceWebTarget = webTarget.path(path);
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private JSONArray statesUpTo(DateTime endTime, int steps, WebTarget webTarget) {
		String endTimeString = endTime.toString(ISODateTimeFormat.dateTimeNoMillis());
		String path = "argentina/" + endTimeString + "/flow/" + steps;
		System.out.println("Requesting: " + path);
		return buildJSONArray(webTarget, path);
	}

	private Response arrowsBySteps(String endTime, int steps, WebTarget webTarget, String filename) {
		String path = "argentina/" + endTime + "/flow/" + steps + "/" + filename;
		System.out.println("Requesting: " + path);
		WebTarget resourceWebTarget = webTarget.path(path);
		Invocation.Builder invocationBuilder = resourceWebTarget.request(MediaType.APPLICATION_OCTET_STREAM);
		return invocationBuilder.get();
	}

	private void zoneMapToDisk(WebTarget webTarget, String resourcePath) {
		String path = "argentina/" + mapFilename;
		System.out.println("Requesting: " + path);
		WebTarget resourceWebTarget = webTarget.path(path);
		Invocation.Builder invocationBuilder = resourceWebTarget.request(MediaType.APPLICATION_OCTET_STREAM);
		try {
			byte[] SWFByteArray;
			SWFByteArray = IOUtils.toByteArray((InputStream) invocationBuilder.get().getEntity());
			FileOutputStream fos = new FileOutputStream(new File(resourcePath + mapFilename));
			fos.write(SWFByteArray);
			fos.flush();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void dumpToDiskBySteps(JSONArray dates, int steps, WebTarget webTarget, String resourcePath, String fileName) {
		Response response = arrowsBySteps((String) dates.get(dates.length() - 1), steps, webTarget, fileName);
		try {
			byte[] SWFByteArray;
			SWFByteArray = IOUtils.toByteArray((InputStream) response.getEntity());
			FileOutputStream fos = new FileOutputStream(new File(resourcePath + fileName));
			fos.write(SWFByteArray);
			fos.flush();
			fos.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
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

	private void buildGif(String resourcePath) {
		AnimatedGifEncoder gif = new AnimatedGifEncoder();
		gif.setRepeat(0);
		gif.setDelay(100);
		gif.start(resourcePath + "animation.gif");
		for (int i = 0; i < datesLength; i++) {
			try {
				BufferedImage image;
				image = ImageIO.read(new File(resourcePath + i + ".png"));
				gif.addFrame(image);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		gif.finish();
	}

	private void resetDir(String resourcePath) {
		removeDir(resourcePath);
		createDir(resourcePath);
	}

	private void createDir(String resourcePath) {
		new File(resourcePath).mkdir();
	}

	private void removeDir(String resourcePath) {
		try {
			FileUtils.deleteDirectory(new File(resourcePath));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
