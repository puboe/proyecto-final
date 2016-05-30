package ar.com.itba.piedpiper.web.page;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;

@SuppressWarnings("serial")
public class HomePage extends AbstractWebPage {

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
	}

	@Override
		protected void onInitialize() {
			super.onInitialize();
			add(new Label("welcome", Model.of("Use the menu up top to begin.")));
		}
	
}
