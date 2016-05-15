package ar.com.itba.piedpiper.web.page;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

import ar.com.itba.piedpiper.web.NavbarBuilder;

@SuppressWarnings("serial")
public abstract class AbstractWebPage extends WebPage {

	@SpringBean
	private NavbarBuilder _navBarBuilder;

	public AbstractWebPage() {}

	public AbstractWebPage(PageParameters parameters) {
		super(parameters);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		add(_navBarBuilder.apply("navbarPanel"));
	}

}
