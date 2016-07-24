package ar.com.itba.piedpiper.web.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.wicket.request.resource.DynamicImageResource;

@SuppressWarnings("serial")
public class ImageResource extends DynamicImageResource {

	private String path;
	private String name;
	private boolean fileFound;
	
	public ImageResource(String path, String name) {
		this.path = path;
		this.name = name;
	}
	
	@Override
	protected byte[] getImageData(Attributes attributes) {
		try {
			File imgPath = new File(path + name);
			fileFound = true;
			return Files.readAllBytes(imgPath.toPath());
		} catch (IOException e) {
			fileFound = false;
		}
		return null;
	}
	
	public boolean fileFound() {
		return fileFound;
	}
}
