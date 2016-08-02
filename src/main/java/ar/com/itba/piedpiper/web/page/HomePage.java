package ar.com.itba.piedpiper.web.page;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import ar.com.itba.piedpiper.service.api.ConfigurationService;

@SuppressWarnings("serial")
public class HomePage extends AbstractWebPage {

	@SpringBean
	ConfigurationService configurations;
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		add(new Label("welcome", Model.of("Use the menu up top to begin.")));
	}

}

