package ar.com.itba.piedpiper.model.entity;

import java.io.Serializable;

import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class PersistentEntity<ID extends Serializable> {

	abstract public ID getId();

}
