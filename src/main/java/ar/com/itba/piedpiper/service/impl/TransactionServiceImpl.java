package ar.com.itba.piedpiper.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import ar.com.itba.piedpiper.service.api.TransactionService;

@Service
public class TransactionServiceImpl implements TransactionService {

	private final TransactionTemplate _transactionTemplate;

	@Autowired
	public TransactionServiceImpl(PlatformTransactionManager transactionManager) {
		_transactionTemplate = new TransactionTemplate(transactionManager);
	}

	@Override
	public <T> T execute(final TransactionalOperation<T> block) {
		return _transactionTemplate.execute(new TransactionCallback<T>() {
			@Override
			public T doInTransaction(TransactionStatus status) {
				return block.execute();
			}
		});
	}

	@Override
	public void execute(final TransactionalOperationWithoutReturn block) {
		_transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				block.execute();
			}
		});
	}

}
