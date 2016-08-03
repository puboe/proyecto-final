package ar.com.itba.piedpiper.model.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "configuration")
public class Configuration extends PersistentEntity<Integer> {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	private Integer id;

	@Column(name = "name")
	private String name;

	@Column(name = "description")
	private String description;

	@Column(name = "value")
	private String value;

	@Column(name = "display_name")
	private String displayName;

	@Column(name = "active")
	private boolean isActive;
	
	@Override
	public Integer getId() {
		return id;
	}

	public Configuration() {
		// Required by Hibernate
	}

	public Configuration(String name, String description, String value, String displayName) {
		this.name = name;
		this.description = description;
		this.value = value;
		this.displayName = displayName;
	}

	public int intValue() {
		return Integer.valueOf(value);
	}

	public long longValue() {
		return Long.valueOf(value);
	}

	public String value() {
		return value;
	}

	public void value(String value) {
		this.value = value;
	}

	public String name() {
		return name;
	}

	public String description() {
		return description;
	}
	
	public String displayName() {
		return displayName;
	}
	
	public boolean isActive() {
		return isActive;
	}
}
