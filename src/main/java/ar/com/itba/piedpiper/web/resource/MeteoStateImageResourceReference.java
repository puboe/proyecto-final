package ar.com.itba.piedpiper.web.resource;

import org.apache.wicket.request.resource.DynamicImageResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;

import com.google.common.base.Optional;

import ar.com.itba.piedpiper.model.entity.MeteoState;

@SuppressWarnings("serial")
public class MeteoStateImageResourceReference extends ResourceReference {

	private Optional<MeteoState> meteoState;

	public MeteoStateImageResourceReference(Optional<MeteoState> meteoState) {
		super(MeteoStateImageResourceReference.class, meteoState.get().filename());
		this.meteoState = meteoState;
	}

	@Override
	public IResource getResource() {
		return new Image();
	}

	private class Image extends DynamicImageResource {
		@Override
		protected byte[] getImageData(Attributes attributes) {
			return meteoState.isPresent() ? meteoState.get().image() : null;
		}
	}
}
