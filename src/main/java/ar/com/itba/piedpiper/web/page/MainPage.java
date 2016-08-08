package ar.com.itba.piedpiper.web.page;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.json.JSONArray;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.apache.wicket.util.time.Duration;
import org.glassfish.jersey.client.ClientConfig;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import com.madgag.gif.fmsware.AnimatedGifEncoder;

import ar.com.itba.piedpiper.model.entity.Channel;
import ar.com.itba.piedpiper.model.entity.SavedState;
import ar.com.itba.piedpiper.web.ApplicationSession;
import ar.com.itba.piedpiper.web.panel.StateFilterPanel;
import ar.com.itba.piedpiper.web.panel.StateFilterPanel.StateFilterModel;
import ar.com.itba.piedpiper.web.res.ApplicationResources;
import ar.com.itba.piedpiper.web.util.DiskImageResource;
import de.agilecoders.wicket.core.markup.html.bootstrap.common.NotificationPanel;

@SuppressWarnings("serial")
public class MainPage extends AbstractWebPage {

	private StateFilterModel stateFilterModel;
	private IModel<String> stateDateInfoModel;
	// XXX: Move to config
	private final String trailsFilename = "trails.png";
	private final String arrowsFilename = "arrows.png";
	private final String imageFilename = "image.png";
	private final String differenceFilename = "diff_image.png";
	private final String lastStateFilename = "lastState.png";
	private final String enhancedImageFilename = "image_enhanced.png";
	private final String mapFilename = "map_image.png";
	private final String predictionFilename = "prediction.png";
	private NotificationPanel feedback;
	private String resourcePath;
	private Properties prop;
	private JSONArray dates;
	
	public MainPage() {
		ApplicationSession session = ApplicationSession.get(); 
		loadProperties();
		if(session.mainPageLoadCount() == 0) {
			session.increaseMainPageLoadCount();
			DateTime defaultDateTime = new DateTime().minusDays(1);
			int defaultSteps = Integer.valueOf(prop.getProperty("startupStates"));
			Channel defaultChannel = Channel.IR2;
			boolean defaultEnhanced = false;
			dates = buildPage(defaultDateTime, Integer.valueOf(defaultSteps), defaultChannel, defaultEnhanced);
			stateDateInfoModel = infoString(defaultSteps,  dates,  defaultChannel,  defaultEnhanced);
			session.stateFilterModel(stateFilterModel = new StateFilterModel(defaultChannel, defaultDateTime.toDate(), defaultSteps, defaultEnhanced));
		} else {
			stateFilterModel = session.stateFilterModel();
		}
	}
	
	public MainPage(DateTime dateTime, int steps, Channel channel, Boolean enhanced, StateFilterModel filtermodel) {
		loadProperties();
		dates = buildPage(dateTime, steps, channel, enhanced);
		stateDateInfoModel = infoString(steps,  dates,  channel,  enhanced);
		ApplicationSession.get().stateFilterModel(stateFilterModel = new StateFilterModel(channel, dateTime.toDate(), steps, enhanced));
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		feedback = new NotificationPanel("feedback");
		feedback.setOutputMarkupId(true);
		feedback.hideAfter(Duration.seconds(5));
		add(feedback);
		Label stateDateInfo = new Label("stateDate", stateDateInfoModel);
		stateDateInfo.setOutputMarkupId(true);
		IModel<DynamicImageResource> mapModel = Model.of(new DiskImageResource(resourcePath, mapFilename));
		Image animationMap = new Image("animationMap", mapModel);
		animationMap.setOutputMarkupId(true);
		IModel<DynamicImageResource> arrowsModel = Model.of(new DiskImageResource(resourcePath, arrowsFilename));
		NonCachingImage arrows = new NonCachingImage("arrows", arrowsModel);
		arrows.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true);
		IModel<DynamicImageResource> trailModel = Model.of(new DiskImageResource(resourcePath, trailsFilename));
		NonCachingImage trails = new NonCachingImage("trails", trailModel);
		trails.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true);
		IModel<DiskImageResource> animationModel = Model.of(new DiskImageResource(resourcePath, "animation.gif"));
		NonCachingImage animation = new NonCachingImage("imageAnim", animationModel);
		animation.setOutputMarkupId(true);
		IModel<DiskImageResource> predictionModel = Model.of(new DiskImageResource(resourcePath, predictionFilename));
		Image predictionMap = new Image("predictionMap", mapModel);
		predictionMap.setOutputMarkupId(true);
		NonCachingImage prediction = new NonCachingImage("prediction", predictionModel);
		prediction.setOutputMarkupId(true);
		IModel<DiskImageResource> lastStateModel = Model.of(new DiskImageResource(resourcePath, lastStateFilename));
		NonCachingImage lastState = new NonCachingImage("lastState", lastStateModel);
		lastState.setOutputMarkupId(true);
		IModel<DiskImageResource> differenceModel = Model.of(new DiskImageResource(resourcePath, differenceFilename));
		NonCachingImage difference = new NonCachingImage("difference", differenceModel);
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
				DateTime dateTime = new DateTime(stateFilterModel.toModelObject());
				int steps = new Integer(stateFilterModel.stepsModelObject());
				Channel channel = stateFilterModel.channelModelObject();
				boolean enhanced = stateFilterModel.enhancedModelObject();
				System.out.println(dates);
				SavedState savedState = new SavedState(dateTime, steps, channel, enhanced);
				if(!cookieExists(savedState)) {
					Cookie cookie = new Cookie(savedState.toString(), "1");
					((WebResponse) getRequestCycle().getResponse()).addCookie(cookie);
					success("Estado guardado!");
				} else {
					error("State is already saved.");
				}
				target.add(feedback);
			}
		});
		add(stateDateInfo, prediction, predictionMap, animation, trails, animationMap, arrows, lastState, difference);
		add(new StateFilterPanel("filterPanel", stateFilterModel, this) {
			@Override
			public void onSearch(AjaxRequestTarget target) {
				DateTime dateTime = new DateTime(stateFilterModel.toModelObject());
				int steps = new Integer(stateFilterModel.stepsModelObject());
				Channel channel = stateFilterModel.channelModelObject();
				boolean enhanced = stateFilterModel.enhancedModelObject();
				dates = buildPage(dateTime, steps, channel, enhanced);
				stateDateInfo.setDefaultModel(stateDateInfoModel = infoString(steps,  dates,  channel,  enhanced));
				target.add(animation, stateDateInfo, trails, prediction, predictionMap, animationMap, arrows, lastState, difference);
			}
		});
	}

	private JSONArray buildPage(DateTime dateTime, int steps, Channel channel, boolean enhanced) {
		WebTarget webTarget = setupApiConnection();
		JSONArray dates = statesUpTo(dateTime, steps, webTarget);
		imagesForDatesToDisk(dates, webTarget, resourcePath, channel, enhanced);
		zoneMapToDisk(webTarget, resourcePath);
		dumpToDiskBySteps(dates, steps, webTarget, resourcePath, arrowsFilename);
		dumpToDiskBySteps(dates, steps, webTarget, resourcePath, trailsFilename);
		singlemageToDisk(dates.get(dates.length()-1).toString(), webTarget, resourcePath, imageFilename, predictionFilename);
		singlemageToDisk(dates.get(dates.length()-1).toString(), webTarget, resourcePath, differenceFilename, differenceFilename);
		buildGif(resourcePath);
		return dates;
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
		ApplicationSession session = ApplicationSession.get();
		for (int i = 0; i < dates.length(); i++) {
			String date = (String) dates.get(i);
			String path = "argentina/" + date + "/static/goeseast/"+ ((Channel) channel).value() +
				"/" + (enhanced ? enhancedImageFilename : imageFilename);
			System.out.println("Requesting: " + path);
			WebTarget resourceWebTarget = webTarget.path(path);
			Invocation.Builder invocationBuilder = resourceWebTarget.request(MediaType.APPLICATION_OCTET_STREAM);
			Response response = invocationBuilder.get();
			session.addGifStream((InputStream) response.getEntity());
			if(i == dates.length()-1) {
				try {
					byte[] SWFByteArray;
					SWFByteArray = IOUtils.toByteArray((InputStream) response.getEntity());
					FileOutputStream fos = new FileOutputStream(new File(resourcePath + lastStateFilename));
					fos.write(SWFByteArray);
					fos.flush();
					fos.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
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
		String mainWebTargetPath = prop.getProperty("mainWebTargetPath");
		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		return client.target(mainWebTargetPath);
	}

	private void buildGif(String resourcePath) {
		AnimatedGifEncoder gif = new AnimatedGifEncoder();
		gif.setRepeat(0);
		gif.setDelay(100);
		gif.start(resourcePath + "animation.gif");
		for (InputStream stream : ApplicationSession.get().getGifStream()) {
			try {
				BufferedImage image;
				image = ImageIO.read(stream);
				gif.addFrame(image);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		gif.finish();
	}
	
	private boolean cookieExists(SavedState savedState) {
		List<Cookie> cookies = ((WebRequest)RequestCycle.get().getRequest()).getCookies();
		for (Cookie cookie : cookies) {
			if(cookie.getName().equals(savedState.toString())){
				return true;
			}
		}
		return false;
	}

	private void loadProperties() {
		prop = new Properties();
		try {
			prop.load(ApplicationResources.class.getResourceAsStream("config.properties"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		resourcePath = prop.getProperty("imagePath");
	}
	
	private String[] parseDateTimeString(String dateTime) {
		String[] dateAndTime = dateTime.split("T");
		String[] date = dateAndTime[0].split("-");
		dateAndTime[0] = new DateTime(Integer.valueOf(date[0]),Integer.valueOf(date[1]),Integer.valueOf(date[2]), 0, 0).toString("dd-MM-yyyy");
		return dateAndTime;
	}
	
	private Model<String> infoString(int steps, JSONArray dates, Channel channel, boolean enhanced) {
		String[] fromDateAndTime = parseDateTimeString(dates.get(1).toString());
		String[] toDateAndTime = parseDateTimeString(dates.get(dates.length() - 1).toString());
		return Model.of("Mostrando " + steps + " cuadros desde " + fromDateAndTime[0] + " a las " +
				fromDateAndTime[1] + " hasta " + toDateAndTime[0] + " a las " + toDateAndTime[1] + " en el canal " + channel.name() +
				(enhanced ? " con contraste mejorado." : "."));
	}
}
