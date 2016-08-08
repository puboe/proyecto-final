package ar.com.itba.nub3s.web.page;

import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

import ar.com.itba.nub3s.web.NavbarBuilder;

@SuppressWarnings("serial")
public abstract class AbstractWebPage extends WebPage implements IAjaxIndicatorAware {

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
	
	@Override
	public String getAjaxIndicatorMarkupId() {
		return "ajaxveil";
	}
	
}
