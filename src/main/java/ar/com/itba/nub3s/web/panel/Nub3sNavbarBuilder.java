package ar.com.itba.nub3s.web.panel;

import static de.agilecoders.wicket.core.markup.html.bootstrap.navbar.NavbarComponents.transform;

import org.apache.wicket.model.Model;
import org.springframework.stereotype.Component;

import ar.com.itba.nub3s.web.NavbarBuilder;
import ar.com.itba.nub3s.web.page.AboutPage;
import ar.com.itba.nub3s.web.page.MainPage;
import ar.com.itba.nub3s.web.page.SavedStatesPage;
import de.agilecoders.wicket.core.markup.html.bootstrap.image.GlyphIconType;
import de.agilecoders.wicket.core.markup.html.bootstrap.navbar.Navbar;
import de.agilecoders.wicket.core.markup.html.bootstrap.navbar.NavbarButton;

@SuppressWarnings("serial")
@Component
public class Nub3sNavbarBuilder implements NavbarBuilder {

	@Override
	public Navbar apply(String id) {
		Navbar navbar = new Navbar(id);
		navbar.setBrandName(Model.of("Nub3s"));
		navbar.fluid();
		navbar
			.addComponents(transform(Navbar.ComponentPosition.LEFT, new NavbarButton<Void>(MainPage.class, Model.of("Principal")).setIconType(GlyphIconType.cloud)))
			.addComponents(transform(Navbar.ComponentPosition.LEFT, new NavbarButton<Void>(SavedStatesPage.class, Model.of("Estados guardados")).setIconType(GlyphIconType.floppysaved)))
			.addComponents(transform(Navbar.ComponentPosition.LEFT, new NavbarButton<Void>(AboutPage.class, Model.of("Información")).setIconType(GlyphIconType.infosign)));
		return navbar;
	}

}

