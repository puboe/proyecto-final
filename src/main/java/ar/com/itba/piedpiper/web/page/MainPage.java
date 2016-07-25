package ar.com.itba.piedpiper.web.page;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.imageio.ImageIO;
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
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import com.madgag.gif.fmsware.AnimatedGifEncoder;

import ar.com.itba.piedpiper.service.api.ConfigurationService;
import ar.com.itba.piedpiper.web.panel.StateFilterPanel;
import ar.com.itba.piedpiper.web.panel.StateFilterPanel.StateFilterModel;
import ar.com.itba.piedpiper.web.util.ImageResource;

@SuppressWarnings("serial")
public class MainPage extends AbstractWebPage {

	@SpringBean
	private ConfigurationService configurations;

	private StateFilterModel stateFilterModel;
	private int datesLength = 0;
	// XXX: Move to config
	private final String trailsFilename = "trails.png";
	private final String arrowsFilename = "arrows.png";
	private final String imageFilename = "image.png";
	private final String mapFilename = "map_image.png";
	private List<InputStream> streams;

	@Override
	protected void onInitialize() {
		super.onInitialize();
		final String resourcePath = configurations.findByName("imagePath").value();
		createDir(resourcePath);
		IModel<String> stateDateInfoModel = Model.of("");
		Label stateDateInfo = new Label("stateDate", stateDateInfoModel);
		stateDateInfo.setOutputMarkupId(true);
		IModel<DynamicImageResource> mapModel = Model.of(new ImageResource(resourcePath, mapFilename));
		Image map = new Image("map", mapModel);
		map.setOutputMarkupId(true);
		IModel<DynamicImageResource> arrowsModel = Model.of(new ImageResource(resourcePath, arrowsFilename));
		Image arrows = new Image("arrows", arrowsModel);
		arrows.setOutputMarkupId(true);
		IModel<DynamicImageResource> trailModel = Model.of(new ImageResource(resourcePath, trailsFilename));
		Image trail = new Image("trails", trailModel);
		trail.setOutputMarkupId(true);
		IModel<ImageResource> animationModel = Model.of(new ImageResource(resourcePath, "animation.gif"));
		NonCachingImage animation = new NonCachingImage("imageAnim", animationModel);
		animation.setOutputMarkupId(true);
		// TODO: Load Prediction Image here
//		ImageResource predictionResource = new ImageResource(resourcePath, datesLength + ".png");
//		Image prediction = new Image("imagePred", Model.of(predictionResource));
//		prediction.setOutputMarkupId(true);
//		prediction.setVisible(predictionResource.fileFound());
		add(stateDateInfo, /*prediction,*/ animation, trail, map, arrows);
		stateFilterModel = new StateFilterModel();
		add(new StateFilterPanel("filterPanel", stateFilterModel, this) {
			@Override
			public void onSearch(AjaxRequestTarget target) {
				// XXX: Get stuff through API here
				resetDir(resourcePath);
				DateTime to = new DateTime(stateFilterModel.toModel().getObject());
				int steps = new Integer(stateFilterModel.stepsModel().getObject());
				WebTarget webTarget = setupApiConnection();
				JSONArray dates = statesUpTo(to, steps, webTarget);
				datesLength = dates.length();
				imagesForDatesToDisk(dates, webTarget, resourcePath);
				zoneMapToDisk(webTarget, resourcePath);
				dumpToDiskBySteps(dates, steps, webTarget, resourcePath, arrowsFilename);
				dumpToDiskBySteps(dates, steps, webTarget, resourcePath, trailsFilename);
				buildGif(resourcePath);
				stateDateInfo.setDefaultModel(Model.of("Playing " + datesLength + " frames from " + dates.get(1)
					+ " leading to " + dates.get(dates.length() - 1) + "."));
				target.add(animation, stateDateInfo, trail, /*prediction,*/ map, arrows);
			}
		});
	}

	private JSONArray buildJSONArray(WebTarget webTarget, String path) {
		WebTarget resourceWebTarget = webTarget.path(path);
		Invocation.Builder invocationBuilder = resourceWebTarget.request(MediaType.APPLICATION_JSON);
		Response response = invocationBuilder.get();
		return new JSONArray(response.readEntity(String.class));
	}

	private void imagesForDatesToDisk(JSONArray dates, WebTarget webTarget, String resourcePath) {
		for (int i = 0; i < dates.length(); i++) {
			String date = (String) dates.get(i);
			String path = "argentina/" + date + "/static/goeseast/ir2/" + imageFilename;
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
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}
