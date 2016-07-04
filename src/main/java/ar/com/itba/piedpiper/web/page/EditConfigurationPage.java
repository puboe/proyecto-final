package ar.com.itba.piedpiper.web.page;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import ar.com.itba.piedpiper.model.entity.Configuration;
import ar.com.itba.piedpiper.service.api.ConfigurationService;
import ar.com.itba.piedpiper.service.api.TransactionService;
import ar.com.itba.piedpiper.web.dataprovider.api.SpringPageDataProvider;
import ar.com.itba.piedpiper.web.panel.AjaxTransactionalEditableLabel;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipBehavior;

@SuppressWarnings("serial")
public class EditConfigurationPage extends AbstractWebPage {

	@SpringBean
	ConfigurationService configurations;

	@SpringBean
	TransactionService transactions;

	@Override
	protected void onInitialize() {
		super.onInitialize();
		long itemsPerPage = 20;
		IDataProvider<Configuration> configurationDataProvider = new SpringPageDataProvider<Configuration>(itemsPerPage) {
			@Override
			protected Page<Configuration> getPage(Pageable pageable) {
				return configurations.findAll(pageable);
			}

			public void detach() {}
		};
		DataView<Configuration> domainView = new DataView<Configuration>("configurationList", configurationDataProvider) {
			@Override
			protected void populateItem(Item<Configuration> item) {
				Configuration configuration = (Configuration) item.getModelObject();
				item.add(new Label("displayName", configuration.displayName()));
				item.add(new Label("description", configuration.description()));
				String value = configuration.value();
				IModel<Configuration> configurationModel = item.getModel();
				item.add(new AjaxTransactionalEditableLabel("editableValue", Model.of(value), "") {
					@Override
					public void transactionalOperation(String input) {
						configurationModel.getObject().value(input);
						configurations.flushCaches();
					}
				});
			}
		};
		domainView.setItemsPerPage(itemsPerPage);
		final PagingNavigator navigator = new PagingNavigator("navigator", domainView);
		add(navigator.setOutputMarkupId(true));
		final WebMarkupContainer listContainer = new WebMarkupContainer("container");
		add(listContainer.add(domainView).setOutputMarkupId(true));
		add(new FeedbackPanel("feedback"));
	}
}
