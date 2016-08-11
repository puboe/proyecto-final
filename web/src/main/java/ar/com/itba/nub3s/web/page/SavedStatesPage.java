package ar.com.itba.nub3s.web.page;

import java.util.Collections;
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
import org.joda.time.DateTime;

import com.google.common.collect.Sets;

import ar.com.itba.nub3s.model.entity.Channel;
import ar.com.itba.nub3s.model.entity.SavedState;
import ar.com.itba.nub3s.web.panel.StateFilterPanel.StateFilterModel;
import ar.com.itba.nub3s.web.util.Models;
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
		List<SavedState> list = Lists.newArrayList(savedStates);
		Collections.sort(list);
		DataView<SavedState> savedStateView = new DataView<SavedState>("savedStateList", new ListDataProvider<>(list)) {
			@Override
			protected void populateItem(Item<SavedState> item) {
				final SavedState savedState = (SavedState) item.getModelObject();
				DateTime dateTime = savedState.dateTime();
				item.add(new Label("dateTime", dateTime.toString("dd-MM-yyyy HH:mm")));
				item.add(new Label("channel", Models.translate(savedState.channel(), this)));
				item.add(new Label("steps", savedState.steps()));
				item.add(new Label("enhanced", savedState.enhanced() ? "Sí" : "No"));
				item.add(new Link<Void>("getState") {
					@Override
					public void onClick() {
						DateTime dateTime = ((SavedState) item.getModelObject()).dateTime();
						int steps = ((SavedState) item.getModelObject()).steps();
						Channel channel = ((SavedState) item.getModelObject()).channel();
						boolean enhanced = ((SavedState) item.getModelObject()).enhanced();
						setResponsePage(
							new MainPage(dateTime,steps,channel,enhanced,
								new StateFilterModel(channel, dateTime.toDate(), steps, enhanced))
						);
					}
				});
			}
		};
		savedStateView.setItemsPerPage(itemsPerPage);
		add(new PagingNavigator("navigator", savedStateView));
		add(new WebMarkupContainer("container").add(savedStateView));
	}
}
