package ar.com.itba.piedpiper.model.entity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

@SuppressWarnings("serial")
public class SavedState implements Serializable {

	private DateTime dateTime;

	private int steps;

	private Channel channel;

	private boolean enhanced;

	public SavedState() {
		// Required by hibernate
	}

	public SavedState(DateTime dateTime, int steps, Channel channel, boolean enhanced) {
		this.dateTime = dateTime;
		this.steps = steps;
		this.channel = channel;
		this.enhanced = enhanced;
	}

	public SavedState(String savedState) {
		String[] split = savedState.split(",");
		DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
		dateTime = formatter.parseDateTime(split[0]);
		steps = Integer.valueOf(split[1]);
		channel = Channel.fromString(split[2]);
		enhanced = Boolean.valueOf(split[3]);
	}

	public DateTime dateTime() {
		return dateTime;
	}

	public int steps() {
		return steps;
	}

	public Channel channel() {
		return channel;
	}

	public boolean enhanced() {
		return enhanced;
	}

	@Override
	public String toString() {
		return dateTime.toString() + "," + steps + "," + channel + "," + enhanced;
	}

	public String serialize() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos;
		try {
			oos = new ObjectOutputStream(baos);
			oos.writeObject(this);
			oos.close();
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return Base64.getEncoder().encodeToString(baos.toByteArray());
	}

	public static SavedState deSerialize(String serializedObject) throws IOException, ClassNotFoundException {
		byte[] data = Base64.getDecoder().decode(serializedObject);
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
		SavedState savedState = (SavedState) ois.readObject();
		ois.close();
		return savedState;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((channel == null) ? 0 : channel.hashCode());
		result = prime * result + ((dateTime == null) ? 0 : dateTime.hashCode());
		result = prime * result + (enhanced ? 1231 : 1237);
		result = prime * result + steps;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SavedState other = (SavedState) obj;
		if (channel != other.channel)
			return false;
		if (dateTime == null) {
			if (other.dateTime != null)
				return false;
		} else if (!dateTime.equals(other.dateTime))
			return false;
		if (enhanced != other.enhanced)
			return false;
		if (steps != other.steps)
			return false;
		return true;
	}

}
