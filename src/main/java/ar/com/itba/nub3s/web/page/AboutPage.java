package ar.com.itba.nub3s.web.page;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;

@SuppressWarnings("serial")
public class AboutPage extends AbstractWebPage {

	@Override
	protected void onInitialize() {
		super.onInitialize();
		add(new Label("tutor1", Model.of("Ignacio Alvarez-Hamelin - ihameli@itba.edu.ar ")));
	}
}
