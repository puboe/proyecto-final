package ar.com.itba.piedpiper.model.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.joda.time.DateTime;

@Entity
@Table(name = "saved_state")
public class SavedState extends PersistentEntity<Integer> {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Integer id;
	
	@Column(name = "date_time", columnDefinition="DATETIME")
	private DateTime dateTime;
	
	@Column(name = "steps")
	private int steps;
	
	public SavedState() {
		//Required by hibernate
	}
	
	public SavedState(DateTime dateTime, int steps) {
		this.dateTime = dateTime;
		this.steps = steps;
	}
	
	@Override
	public Integer getId() {
		return id;
	}
	
	
}
