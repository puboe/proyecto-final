package ar.com.itba.piedpiper.web.page;

import java.util.List;
import java.util.Set;

import javax.servlet.http.Cookie;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebRequest;

import com.google.common.collect.Sets;

import ar.com.itba.piedpiper.model.entity.SavedState;
import jersey.repackaged.com.google.common.collect.Lists;

@SuppressWarnings("serial")
public class SavedStatesPage extends AbstractWebPage {

	@Override
	protected void onInitialize() {
		super.onInitialize();
		long itemsPerPage = 20;
		List<Cookie> cookies = ((WebRequest)RequestCycle.get().getRequest()).getCookies();
		Set<SavedState> savedStates = Sets.newHashSet(); 
		for (Cookie cookie : cookies) {
			if(cookie.getValue().equals("1")) {
				savedStates.add(new SavedState(cookie.getName()));
			}
		}
		DataView<SavedState> savedStateView = new DataView<SavedState>("savedStateList", new ListDataProvider<>(Lists.newArrayList(savedStates))) {
			@Override
			protected void populateItem(Item<SavedState> item) {
				final SavedState savedState = (SavedState) item.getModelObject();
				item.add(new Label("dateTime", savedState.dateTime()));
				item.add(new Label("channel", savedState.channel().name()));
				item.add(new Label("steps", savedState.steps()));
				item.add(new Label("enhanced", savedState.enhanced() ? "Sí" : "No"));
				item.add(new Link<Void>("getState") {
					@Override
					public void onClick() {
						setResponsePage(
							new MainPage(((SavedState) item.getModelObject()).dateTime(),
								((SavedState) item.getModelObject()).steps(),
								((SavedState) item.getModelObject()).channel(),
								((SavedState) item.getModelObject()).enhanced())
						);
					}
				});
//				item.add(new AjaxLink<Void>("removeState") {
//					@Override
//					public void onClick(AjaxRequestTarget target) {
//						Cookie cookie = new Cookie(savedState.toString(), "0");
//						cookie.setMaxAge(0);
//						((WebResponse) getRequestCycle().getResponse()).addCookie(cookie);
//						savedStates.remove(savedState);
//						target.add(this);
//					}
//				}.add(new TooltipBehavior(Model.of("Remover"))));
			}
		};
		savedStateView.setItemsPerPage(itemsPerPage);
		add(new PagingNavigator("navigator", savedStateView));
		add(new WebMarkupContainer("container").add(savedStateView));
	}

}
