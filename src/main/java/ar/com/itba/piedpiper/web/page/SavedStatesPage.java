package ar.com.itba.piedpiper.web.page;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import ar.com.itba.piedpiper.model.entity.SavedState;
import ar.com.itba.piedpiper.service.api.SavedStateService;
import ar.com.itba.piedpiper.web.dataprovider.api.SpringPageDataProvider;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipBehavior;

@SuppressWarnings("serial")
public class SavedStatesPage extends AbstractWebPage {

	@SpringBean
	private SavedStateService savedStates;

	@Override
	protected void onInitialize() {
		super.onInitialize();
		long itemsPerPage = 20;
		IDataProvider<SavedState> savedStateDataProvider = new SpringPageDataProvider<SavedState>(itemsPerPage) {
			@Override
			protected Page<SavedState> getPage(Pageable pageable) {
				return savedStates.findAll(pageable);
			}

			public void detach() {}
		};
		DataView<SavedState> savedStateView = new DataView<SavedState>("savedStateList", savedStateDataProvider) {
			@Override
			protected void populateItem(Item<SavedState> item) {
				final SavedState savedState = (SavedState) item.getModelObject();
				item.add(new Label("dateTime", savedState.dateTime()));
				item.add(new Label("channel", savedState.channel().name()));
				item.add(new Label("steps", savedState.steps()));
				item.add(new Label("enhanced", savedState.enhanced()));
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
				item.add(new Link<Void>("removeState") {
					@Override
					public void onClick() {
						savedStates.delete(item.getModelObject());
					}
				}.add(new TooltipBehavior(Model.of("Remove"))));
			}
		};
		savedStateView.setItemsPerPage(itemsPerPage);
		add(new PagingNavigator("navigator", savedStateView));
		add(new WebMarkupContainer("container").add(savedStateView));
	}

}
