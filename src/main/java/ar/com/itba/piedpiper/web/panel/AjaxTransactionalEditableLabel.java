package ar.com.itba.piedpiper.web.panel;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import com.google.common.base.Strings;

import ar.com.itba.piedpiper.service.api.TransactionService;
import ar.com.itba.piedpiper.service.api.TransactionService.TransactionalOperationWithoutReturn;


@SuppressWarnings("serial")
public abstract class AjaxTransactionalEditableLabel extends AjaxEditableLabel<String> {

	@SpringBean
	private TransactionService transactionService;

	private String _placeholder;

	public AjaxTransactionalEditableLabel(String id, IModel<String> model, String placeholder) {
		super(id, model);
		_placeholder = placeholder;
	}

	@Override
	protected final void onSubmit(AjaxRequestTarget target) {
		transactionService.execute(new TransactionalOperationWithoutReturn() {
			@Override
			public void execute() {
				transactionalOperation(Strings.nullToEmpty((String) getDefaultModelObject()));
			}
		});
		super.onSubmit(target);
	}

	@Override
	protected void onBeforeRender() {
		getEditor().add(AttributeModifier.replace("placeholder", _placeholder));
		super.onBeforeRender();
	}

	@Override
	protected String defaultNullLabel() {
		return "<i>(null)</i>";
	}

	public abstract void transactionalOperation(String input);
}

