package ar.com.itba.piedpiper.model.entity;

public enum Channel {

	IR1("IR1"), IR2("IR2"), IR3("IR3");

	private String value;

	private Channel(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

}
