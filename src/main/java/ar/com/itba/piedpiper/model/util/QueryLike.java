package ar.com.itba.piedpiper.model.util;

public class QueryLike {

	public enum QueryLikeType {
		LEFT, RIGHT, BOTH, NONE;
	}

	public static String like(Number value, QueryLikeType type) {
		return like(value == null ? "" : value.toString(), type);
	}

	public static String like(String value, QueryLikeType type) {
		return like(value, type, true);
	}

	public static String like(String value, QueryLikeType type, boolean allIfNull) {
		if (value == null) {
			return allIfNull ? "%" : "";
		}
		switch (type) {
		case LEFT:
			return "%" + value;
		case RIGHT:
			return value + "%";
		default:
			return "%" + value + "%";
		}
	}
}
