package ar.com.itba.piedpiper.service.api;

import org.joda.time.DateTime;

import ar.com.itba.piedpiper.model.entity.SavedState;

public interface SavedStateService extends AbstractService<SavedState, Integer> {

	SavedState findByDateTime(DateTime dateTime);
	
}
