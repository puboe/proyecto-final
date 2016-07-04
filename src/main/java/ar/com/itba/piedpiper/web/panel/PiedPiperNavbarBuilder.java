package ar.com.itba.piedpiper.web.panel;

import static de.agilecoders.wicket.core.markup.html.bootstrap.navbar.NavbarComponents.transform;

import org.apache.wicket.model.Model;
import org.springframework.stereotype.Component;

import ar.com.itba.piedpiper.web.NavbarBuilder;
import ar.com.itba.piedpiper.web.page.EditConfigurationPage;
import ar.com.itba.piedpiper.web.page.MainPage;
import de.agilecoders.wicket.core.markup.html.bootstrap.image.GlyphIconType;
import de.agilecoders.wicket.core.markup.html.bootstrap.navbar.Navbar;
import de.agilecoders.wicket.core.markup.html.bootstrap.navbar.NavbarButton;

@SuppressWarnings("serial")
@Component
public class PiedPiperNavbarBuilder implements NavbarBuilder {

	@Override
	public Navbar apply(String id) {
		Navbar navbar = new Navbar(id);
		navbar.setBrandName(Model.of("Welcome"));
		navbar.fluid();
		navbar
			.addComponents(transform(Navbar.ComponentPosition.LEFT, new NavbarButton<Void>(MainPage.class, Model.of("Main")).setIconType(GlyphIconType.cloud)))
			.addComponents(transform(Navbar.ComponentPosition.LEFT, new NavbarButton<Void>(EditConfigurationPage.class, Model.of("Configuration")).setIconType(GlyphIconType.cog)));
		return navbar;
	}

}