package ar.com.itba.piedpiper.service.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.google.common.base.Optional;

import ar.com.itba.piedpiper.model.entity.MeteoState;

public interface MeteoStateService extends AbstractService<MeteoState, Integer> {

	Optional<MeteoState> findByFilename(String filename);
	
	Page<MeteoState> suggest(String input, Pageable pageable);
	
	MeteoState getLast();
}
