package ar.com.itba.piedpiper.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.com.itba.piedpiper.model.entity.MeteoState;

public interface MeteoStateRepository extends JpaRepository<MeteoState, Integer>, JpaSpecificationExecutor<MeteoState> {

	@Query("SELECT meteoState FROM MeteoState meteoState WHERE filename = :filename")
	MeteoState findByFilename(@Param("filename") String filename);
	
	@Query("SELECT meteoState FROM MeteoState meteoState WHERE filename LIKE CONCAT('%',:like,'%')")
	Page<MeteoState> suggest(@Param("like") String like, Pageable pageable);
	
}
