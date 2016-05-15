package ar.com.itba.piedpiper.service.api;

public interface TransactionService {

	<T> T execute(TransactionalOperation<T> block);

	void execute(TransactionalOperationWithoutReturn block);

	public interface TransactionalOperation<T> {
		T execute();
	}

	public interface TransactionalOperationWithoutReturn {
		void execute();
	}

}
