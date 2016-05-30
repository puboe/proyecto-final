package ar.com.itba.piedpiper.web.page;

import org.apache.wicket.spring.injection.annot.SpringBean;

import ar.com.itba.piedpiper.service.api.MeteoStateService;

@SuppressWarnings("serial")
public class MainPage extends AbstractWebPage {

	@SpringBean
	MeteoStateService meteoStateService;
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
	}
	
}
