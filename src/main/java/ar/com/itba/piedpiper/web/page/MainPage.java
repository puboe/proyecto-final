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
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.time.Duration;
import org.glassfish.jersey.client.ClientConfig;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import com.madgag.gif.fmsware.AnimatedGifEncoder;

import ar.com.itba.piedpiper.model.entity.Channel;
import ar.com.itba.piedpiper.model.entity.SavedState;
import ar.com.itba.piedpiper.service.api.ConfigurationService;
import ar.com.itba.piedpiper.web.ApplicationSession;
import ar.com.itba.piedpiper.web.panel.StateFilterPanel;
import ar.com.itba.piedpiper.web.panel.StateFilterPanel.StateFilterModel;
import ar.com.itba.piedpiper.web.util.DiskImageResource;
import de.agilecoders.wicket.core.markup.html.bootstrap.common.NotificationPanel;

@SuppressWarnings("serial")
public class MainPage extends AbstractWebPage {

	@SpringBean
	private ConfigurationService configurations;
	
	private StateFilterModel stateFilterModel;
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
	private final String resourcePath = configurations.findByName("imagePath").value();
	private int datesLength;

	public MainPage() {
		ApplicationSession session = ApplicationSession.get(); 
		if(session.mainPageLoadCount() == 0) {
			session.increaseMainPageLoadCount();
			buildPage(new DateTime().minusDays(5), 10, Channel.IR2, false);
		}
	}
	
	public MainPage(DateTime dateTime, int steps, Channel channel, Boolean enhanced) {
		buildPage(dateTime, steps, channel, enhanced);
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		this.setVersioned(false);
		datesLength = ApplicationSession.get().datesArrayLength();
		feedback = new NotificationPanel("feedback");
		feedback.setOutputMarkupId(true);
		feedback.hideAfter(Duration.seconds(5));
		add(feedback);
		IModel<String> stateDateInfoModel = Model.of("");
		Label stateDateInfo = new Label("stateDate", stateDateInfoModel);
		stateDateInfo.setOutputMarkupId(true);
		IModel<DynamicImageResource> mapModel = Model.of(new DiskImageResource(resourcePath, mapFilename));
		Image animationMap = new NonCachingImage("animationMap", mapModel);
		animationMap.setOutputMarkupId(true);
		IModel<DynamicImageResource> arrowsModel = Model.of(new DiskImageResource(resourcePath, arrowsFilename));
		Image arrows = new NonCachingImage("arrows", arrowsModel);
		arrows.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true);
		IModel<DynamicImageResource> trailModel = Model.of(new DiskImageResource(resourcePath, trailsFilename));
		Image trails = new NonCachingImage("trails", trailModel);
		trails.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true);
		IModel<DiskImageResource> animationModel = Model.of(new DiskImageResource(resourcePath, "animation.gif"));
		Image animation = new NonCachingImage("imageAnim", animationModel);
		animation.setOutputMarkupId(true);
		IModel<DiskImageResource> predictionModel = Model.of(new DiskImageResource(resourcePath, predictionFilename));
		Image predictionMap = new NonCachingImage("predictionMap", mapModel);
		predictionMap.setOutputMarkupId(true);
		Image prediction = new NonCachingImage("prediction", predictionModel);
		prediction.setOutputMarkupId(true);
		IModel<DiskImageResource> lastStateModel = Model.of(new DiskImageResource(resourcePath, lastStateFilename));
		Image lastState = new NonCachingImage("lastState", lastStateModel);
		lastState.setOutputMarkupId(true);
		IModel<DiskImageResource> differenceModel = Model.of(new DiskImageResource(resourcePath, differenceFilename));
		Image difference = new NonCachingImage("difference", differenceModel);
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
		stateFilterModel = new StateFilterModel(configurations.findByName("startupStates").value());
		add(new StateFilterPanel("filterPanel", stateFilterModel, this) {
			@Override
			public void onSearch(AjaxRequestTarget target) {
				// XXX: Get stuff through API here
				DateTime dateTime = new DateTime(stateFilterModel.toModelObject());
				int steps = new Integer(stateFilterModel.stepsModelObject());
				Channel channel = stateFilterModel.channelModelObject();
				boolean enhanced = stateFilterModel.enhancedModelObject();
				JSONArray dates = buildPage(dateTime, steps, channel, enhanced);
				stateDateInfo.setDefaultModel(
					Model.of("Mostrando " + datesLength + " cuadros desde " + dates.get(1) + " hasta "	+ dates.get(dates.length() - 1) + " en el canal " + channel.name() + ".")
				);
				target.add(animation, stateDateInfo, trails, prediction, predictionMap, animationMap, arrows, lastState, difference);
			}
		});
	}

	private JSONArray buildPage(DateTime dateTime, int steps, Channel channel, boolean enhanced) {
		WebTarget webTarget = setupApiConnection();
		JSONArray dates = statesUpTo(dateTime, steps, webTarget);
		ApplicationSession.get().datesArrayLength(datesLength = dates.length());
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
			if(i == dates.length()-1){
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
		String mainWebTargetPath = configurations.findByName("mainWebTargetPath").value();
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

}
