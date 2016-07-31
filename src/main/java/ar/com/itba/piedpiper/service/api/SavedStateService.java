package ar.com.itba.piedpiper.service.api;

import ar.com.itba.piedpiper.model.entity.SavedState;

public interface SavedStateService extends AbstractService<SavedState, Integer> {

	SavedState findOne(SavedState savedState);
	
}
