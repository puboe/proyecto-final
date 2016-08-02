package ar.com.itba.piedpiper.model.entity;

public enum Channel {

	IR2("ir2"), IR3("ir3"), IR4("ir4");

	private String value;

	private Channel(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

}
