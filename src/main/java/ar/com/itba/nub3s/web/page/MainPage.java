package ar.com.itba.nub3s.web.page;

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

import ar.com.itba.nub3s.model.entity.Channel;
import ar.com.itba.nub3s.model.entity.SavedState;
import ar.com.itba.nub3s.web.ApplicationSession;
import ar.com.itba.nub3s.web.panel.StateFilterPanel;
import ar.com.itba.nub3s.web.panel.StateFilterPanel.StateFilterModel;
import ar.com.itba.nub3s.web.res.ApplicationResources;
import ar.com.itba.nub3s.web.util.DateTimeUtils;
import ar.com.itba.nub3s.web.util.DiskImageResource;
import ar.com.itba.nub3s.web.util.Models;
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
	
	public MainPage() {
		ApplicationSession session = ApplicationSession.get(); 
		loadProperties();
		if(session.mainPageLoadCount() == 0) {
			session.increaseMainPageLoadCount();
			DateTime defaultDateTime = new DateTime();
			int defaultSteps = Integer.valueOf(prop.getProperty("startupStates"));
			Channel defaultChannel = Channel.IR2;
			boolean defaultEnhanced = false;
			buildPage(defaultDateTime, defaultSteps, defaultChannel, defaultEnhanced);
			stateDateInfoModel = infoString(defaultSteps, session.firstDate(), session.lastDate(), defaultChannel, defaultEnhanced);
			session.stateFilterModel(
				stateFilterModel = new StateFilterModel(defaultChannel, defaultDateTime.toDate(), defaultSteps, defaultEnhanced)
			);
		} else {
			stateFilterModel = session.stateFilterModel();
			stateDateInfoModel = infoString(
				Integer.valueOf(stateFilterModel.stepsModelObject()), session.firstDate(), session.lastDate(),
				stateFilterModel.channelModelObject(), stateFilterModel.enhancedModelObject()
			);
		}
	}
	
	public MainPage(DateTime dateTime, int steps, Channel channel, Boolean enhanced, StateFilterModel filtermodel) {
		ApplicationSession session = ApplicationSession.get(); 
		loadProperties();
		buildPage(dateTime, steps, channel, enhanced);
		stateDateInfoModel = infoString(steps, session.firstDate(), session.lastDate(), channel, enhanced);
		ApplicationSession.get().stateFilterModel(stateFilterModel = filtermodel);
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
				ApplicationSession session = ApplicationSession.get();
				SavedState savedState = new SavedState(
					session.lastDate(), session.currentSteps(), session.currentChannel(), session.currentEnhanced()
				);
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
		add(stateDateInfo, prediction, predictionMap, animation, trails, animationMap, arrows, lastState, difference,
				new Label("brief",Model.of("Aquí puede visualizar una animación generada por los estados que se obtuvieron como "
						+ "resultado de su búsqueda.<br/><br/>Las flechas azules indican la dirección y el sentido en la que se"
						+ " mueven las nubes.<br/><br/>Las flechas rojas se muestran donde hay mayor desplazamiento.<br/><br/>"
						+ "Puede ocultar ambas flechas utilizando los botones en la	parte superior derecha de la animación."
						+ "<br/><br/>Recomendamos seleccionar entre 5 y 15 estados para obtener resultados donde no falte, "
						+ "ni se sobrecargue de información.")).setEscapeModelStrings(false));
		add(new StateFilterPanel("filterPanel", stateFilterModel, this) {
			@Override
			public void onSearch(AjaxRequestTarget target) {
				ApplicationSession session = ApplicationSession.get();
				DateTime dateTime = new DateTime(stateFilterModel.toModelObject());
				int steps = new Integer(stateFilterModel.stepsModelObject());
				Channel channel = stateFilterModel.channelModelObject();
				boolean enhanced = stateFilterModel.enhancedModelObject();
				session.stateFilterModel(stateFilterModel);
				buildPage(dateTime, steps, channel, enhanced);
				stateDateInfo.setDefaultModel(stateDateInfoModel = infoString(steps, session.firstDate(), session.lastDate(), channel, enhanced));
				target.add(animation, stateDateInfo, trails, prediction, predictionMap, animationMap, arrows, lastState, difference);
			}
		});
	}

	private JSONArray buildPage(DateTime dateTime, int steps, Channel channel, boolean enhanced) {
		WebTarget webTarget = setupApiConnection();
		JSONArray dates = statesUpTo(dateTime, steps, webTarget);
		ApplicationSession session = ApplicationSession.get();
		session.firstDate(dates.getString(0));
		session.lastDate(dates.getString(dates.length() - 1));
		imagesForDatesToDisk(dates, webTarget, resourcePath, channel, enhanced);
		zoneMapToDisk(webTarget, resourcePath);
		dumpToDiskBySteps(steps, webTarget, resourcePath, arrowsFilename);
		dumpToDiskBySteps(steps, webTarget, resourcePath, trailsFilename);
		singlemageToDisk(webTarget, resourcePath, imageFilename, predictionFilename);
		singlemageToDisk(webTarget, resourcePath, differenceFilename, differenceFilename);
		buildGif(resourcePath);
		return dates;
	}
	
	private JSONArray buildJSONArray(WebTarget webTarget, String path) {
		WebTarget resourceWebTarget = webTarget.path(path);
		Invocation.Builder invocationBuilder = resourceWebTarget.request(MediaType.APPLICATION_JSON);
		Response response = invocationBuilder.get();
		return new JSONArray(response.readEntity(String.class));
	}

	private void singlemageToDisk(WebTarget webTarget, String resourcePath, String resourceName, String fileName) {
		String path = "argentina/" + ApplicationSession.get().lastDate() + "/prediction/" + resourceName;
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

	private JSONArray statesUpTo(DateTime dateTime, int steps, WebTarget webTarget) {
		String dateTimeString = dateTime.toString(ISODateTimeFormat.dateTimeNoMillis());
		String path = "argentina/" + dateTimeString + "/flow/" + steps;
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

	private void dumpToDiskBySteps(int steps, WebTarget webTarget, String resourcePath, String fileName) {
		Response response = arrowsBySteps(ApplicationSession.get().lastDate(), steps, webTarget, fileName);
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
	
	private Model<String> infoString(int steps, String firstDate, String lastDate, Channel channel, boolean enhanced) {
		String[] fromDateAndTime = DateTimeUtils.parseDateTimeString(firstDate);
		String[] toDateAndTime = DateTimeUtils.parseDateTimeString(lastDate);
		return Model.of("Mostrando " + steps + " cuadros desde el " + fromDateAndTime[0] + " a las " +
			fromDateAndTime[1] + " hasta el " + toDateAndTime[0] + " a las " + toDateAndTime[1] + 
			" en el canal " + Models.translate(channel, this).getObject() +
			(enhanced ? " con contraste mejorado." : "."));
	}
}
