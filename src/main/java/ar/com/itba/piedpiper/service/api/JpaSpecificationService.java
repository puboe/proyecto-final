package ar.com.itba.piedpiper.service.api;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JpaSpecificationService<T> {

	JpaSpecificationExecutor<T> specificationExecutor();

}
