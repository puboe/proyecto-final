package ar.com.itba.piedpiper.model.entity;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.joda.time.LocalDateTime;

@Entity
@Table(name = "meteo_state")
public class MeteoState extends PersistentEntity<Integer> {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID")
	private Integer id;

	@Column(name = "filename", nullable = false)
	String filename;

	@Column(name = "satellite", nullable = false)
	String satellite;

	@Column(name = "channel", nullable = false)
	String channel;
	
	@Column(name = "zone", nullable = false)
	String zone;

	@Column(name = "date", nullable = false)
	@Type(type="org.jadira.usertype.dateandtime.joda.PersistentLocalDateTime")
	private LocalDateTime date;

	@Lob
	@Basic(fetch = FetchType.LAZY)
	@Column(name = "image", columnDefinition = "MEDIUMBLOB")
	private byte[] image;

	protected MeteoState() {
		// Required by Hibernate
	}

	@Override
	public Integer getId() {
		return id;
	}

	public String filename() {
		return filename;
	}

	public void filename(String filename) {
		this.filename = filename;
	}

	public String satellite() {
		return satellite;
	}

	public String channel() {
		return channel;
	}

	public void channel(String channel) {
		this.channel = channel;
	}

	public String zone() {
		return zone;
	}
	
	public void zone(String zone) {
		this.zone = zone;
	}
	
	public void image(byte[] image) {
		this.image = image;
	}

	public byte[] image() {
		return image;
	}
	
	public LocalDateTime date() {
		return date;
	}
	
	public void date(LocalDateTime date) {
		this.date = date;
	}
	
}
