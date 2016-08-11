package ar.com.itba.nub3s;

import org.apache.wicket.Session;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import ar.com.itba.nub3s.web.ApplicationSession;
import ar.com.itba.nub3s.web.page.AboutPage;
import ar.com.itba.nub3s.web.page.MainPage;
import ar.com.itba.nub3s.web.page.SavedStatesPage;
import de.agilecoders.wicket.core.Bootstrap;
import de.agilecoders.wicket.core.settings.BootstrapSettings;

@Component
@EnableScheduling
public class Nub3sApplication extends WebApplication {

	@Override
	public Class<? extends WebPage> getHomePage() {
		return MainPage.class;
	}

	@Override
	public void init() {
		super.init();
		getComponentInstantiationListeners().add(new SpringComponentInjector(this));
		Bootstrap.install(this, new BootstrapSettings());
		mountPage("/main", MainPage.class);
		mountPage("/saved", SavedStatesPage.class);
		mountPage("/about", AboutPage.class);
	}
	
	@Override
	public Session newSession(Request request, Response response) {
		return new ApplicationSession(request);
	}
}
