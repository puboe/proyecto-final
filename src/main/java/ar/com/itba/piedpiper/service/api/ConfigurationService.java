package ar.com.itba.piedpiper.service.api;

import ar.com.itba.piedpiper.model.entity.Configuration;

public interface ConfigurationService extends AbstractService<Configuration, Integer> {

	Configuration findByName(String name);
	
}
