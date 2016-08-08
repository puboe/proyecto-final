package ar.com.itba.nub3s.model.entity;

public enum Channel {

	IR2("ir2"), IR3("ir3"), IR4("ir4");

	private String value;

	private Channel(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

	public static Channel fromString(String string) {
		switch (string) {
		case "IR2":
			return Channel.IR2;
		case "IR3":
			return Channel.IR3;
		case "IR4":
			return Channel.IR4;
		default:
			return null;
		}
	}
}
