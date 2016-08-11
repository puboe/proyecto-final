package ar.com.itba.nub3s.web.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.wicket.request.resource.DynamicImageResource;

@SuppressWarnings("serial")
public class DiskImageResource extends DynamicImageResource {

	private String path;
	private String name;

	public DiskImageResource(String path, String name) {
		this.path = path;
		this.name = name;
	}

	@Override
	protected byte[] getImageData(Attributes attributes) {
		try {
			File imgPath = new File(path + name);
			return Files.readAllBytes(imgPath.toPath());
		} catch (IOException e) {
		}
		return null;
	}

}
