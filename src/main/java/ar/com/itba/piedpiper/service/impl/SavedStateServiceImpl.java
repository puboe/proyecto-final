package ar.com.itba.piedpiper.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import ar.com.itba.piedpiper.model.entity.SavedState;
import ar.com.itba.piedpiper.repository.SavedStateRepository;
import ar.com.itba.piedpiper.service.api.SavedStateService;

@Service
public class SavedStateServiceImpl extends AbstractServiceImpl<SavedState, Integer> implements SavedStateService {

	@Autowired
	SavedStateRepository repo;

	@Override
	public JpaRepository<SavedState, Integer> repository() {
		return repo;
	}
	
	@Override
	public SavedState findOne(SavedState savedState) {
		return repo.findOne(savedState.dateTime(), savedState.steps());
	}

}
