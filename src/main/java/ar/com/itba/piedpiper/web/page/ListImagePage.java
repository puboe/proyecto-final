package ar.com.itba.piedpiper.web.page;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import ar.com.itba.piedpiper.model.entity.MeteoState;
import ar.com.itba.piedpiper.service.api.MeteoStateService;
import ar.com.itba.piedpiper.web.dataprovider.api.SpringPageDataProvider;

@SuppressWarnings("serial")
public class ListImagePage extends AbstractWebPage {

	@SpringBean
	private MeteoStateService meteoStates;
	
	String filter = "";
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		meteoStates.getLast();
		long itemsPerPage = 20;
		IDataProvider<MeteoState> meteoStateDataProvider = new SpringPageDataProvider<MeteoState>(itemsPerPage) {
			@Override
			protected Page<MeteoState> getPage(Pageable pageable) {
				return meteoStates.suggest(filter, pageable);
			}

			public void detach() {}
		};
		DataView<MeteoState> meteoStateView = new DataView<MeteoState>("meteoStateList", meteoStateDataProvider) {
			@Override
			protected void populateItem(Item<MeteoState> item) {
				final MeteoState meteoState = (MeteoState) item.getModelObject();
				item.add(new Label("filename", meteoState.filename()));
				item.add(new Label("channel", meteoState.channel()));
				item.add(new Label("zone", meteoState.zone()));
				item.add(new Label("date", meteoState.date()));
				item.add(new Image("image", new DynamicImageResource() {
					@Override
					protected byte[] getImageData(Attributes attributes) {
						return meteoStates.findByFilename(((MeteoState) item.getModelObject()).filename()).get().image();
					}
				}));
			}
		};
		meteoStateView.setItemsPerPage(itemsPerPage);
		final PagingNavigator navigator = new PagingNavigator("navigator", meteoStateView);
		add(navigator.setOutputMarkupId(true));
		final WebMarkupContainer listContainer = new WebMarkupContainer("container");
		add(listContainer.add(meteoStateView).setOutputMarkupId(true));
		Form<Void> form = new Form<>("form");
		final TextField<String> filenameTF = new TextField<>("filename", Model.of(filter));
		form.add(filenameTF);
		form.add(new AjaxButton("filter") {
			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				filter = filenameTF.getModelObject();
				target.add(listContainer);
				target.add(navigator);
			}
		});
		add(form);
//		add(new Image("image", imageResource));
	}
}
