package ar.com.itba.piedpiper.web.dataprovider.api;

import java.util.Collections;
import java.util.Iterator;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@SuppressWarnings("serial")
public abstract class SpringPageDataProvider<T> extends AbstractDataProvider<T> {

	long itemsPerPage;

	public SpringPageDataProvider() {
		this(0);
	}

	public SpringPageDataProvider(long itemsPerPage) {
		this.itemsPerPage = itemsPerPage;
	}

	@Override
	public long size() {
		try {
			return getPage(new PageRequest(0, 1)).getTotalElements();
		} catch (Exception e) {
			return 0l;
		}
	}

	@Override
	public Iterator<? extends T> iterator(long first, long count) {
		try {
			// TODO: raro esto, y el count??
			long pageNumber = (first / itemsPerPage);
			return getPage(new PageRequest((int) pageNumber, (int) itemsPerPage)).getContent().iterator();
		} catch (Exception e) {
			return Collections.emptyIterator();
		}
	}

	protected abstract Page<T> getPage(Pageable pageable);

}
