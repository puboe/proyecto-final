package ar.com.itba.piedpiper.web.page;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.joda.time.LocalDateTime;
import org.joda.time.format.ISODateTimeFormat;

import ar.com.itba.piedpiper.model.entity.MeteoState;
import ar.com.itba.piedpiper.service.api.MeteoStateService;
import ar.com.itba.piedpiper.service.api.TransactionService;
import ar.com.itba.piedpiper.service.api.TransactionService.TransactionalOperationWithoutReturn;
import ar.com.itba.piedpiper.web.panel.StateFilterPanel;
import ar.com.itba.piedpiper.web.panel.StateFilterPanel.StateFilterModel;

@SuppressWarnings("serial")
public class MainPage extends AbstractWebPage {

	@SpringBean
	private MeteoStateService meteoStateService;
	
	@SpringBean
	private TransactionService transactions;
	
	private Label stateDate;
	private Label stateFilename;
	private Image imageAnimation;
	private Image predictionImage;
	private StateFilterModel stateFilterModel;
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		final MeteoState lastState = meteoStateService.getLast();
		final LocalDateTime date = lastState.date();
		stateDate = new Label("stateDate", date);
		stateDate.setOutputMarkupId(true);
		stateFilename = new Label("stateFilename", lastState.filename());
		stateFilename.setOutputMarkupId(true);
		imageAnimation = new NonCachingImage("imageAnim", Model.of(new DynamicImageResource() {
			@Override
			protected byte[] getImageData(Attributes attributes) {
				return meteoStateService.getLast().image();
			}
		}));
		imageAnimation.setOutputMarkupId(true);
		predictionImage = new NonCachingImage("imagePred", Model.of(new DynamicImageResource() {
			@Override
			protected byte[] getImageData(Attributes attributes) {
				return meteoStateService.getLast().image();
			}
		}));
		add(stateDate).add(stateFilename).add(predictionImage).add(imageAnimation);
		stateFilterModel = new StateFilterModel();
		add(new StateFilterPanel("filterPanel", stateFilterModel, this) {
			@Override
			public void onSearch(AjaxRequestTarget target) {
				target.add(imageAnimation);
				//XXX: Get stuff through API here
				System.out.println(stateFilterModel.channelModel());
				System.out.println(stateFilterModel.dataRangeModel().getObject().from().toString(ISODateTimeFormat.dateTimeNoMillis()));
				System.out.println(stateFilterModel.dataRangeModel().getObject().to().toString(ISODateTimeFormat.dateTimeNoMillis()));
			}
		});
		add(new AjaxLink<Void>("next") {
			@Override
			public void onClick(AjaxRequestTarget target) {
				transactions.execute(new TransactionalOperationWithoutReturn() {
					@Override
					public void execute() {
						final MeteoState state = meteoStateService.findOne(1).get();
						imageAnimation.setDefaultModelObject(
							new NonCachingImage("image", new DynamicImageResource() {
								@Override
								protected byte[] getImageData(Attributes attributes) {
									return state.image();
								}
							})
						);
						stateDate.setDefaultModelObject(state.date());
						stateFilename.setDefaultModelObject(state.filename());
					}
				});
				target.add(stateDate, stateFilename, imageAnimation);
			}
		});
		add(new AjaxLink<Void>("previous") {
			@Override
			public void onClick(AjaxRequestTarget target) {
				transactions.execute(new TransactionalOperationWithoutReturn() {
					@Override
					public void execute() {
						final MeteoState state = meteoStateService.findOne(2).get();
						imageAnimation.setDefaultModelObject(
							new NonCachingImage("image", new DynamicImageResource() {
								@Override
								protected byte[] getImageData(Attributes attributes) {
									return state.image();
								}
							})	
						);
						stateDate.setDefaultModelObject(state.date());
						stateFilename.setDefaultModelObject(state.filename());
					}
				});
				target.add(stateDate, stateFilename, imageAnimation);
			}
		});
	}
	
}
