package ar.com.itba.piedpiper.web;

import java.io.InputStream;
import java.util.List;

import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.Request;

import com.google.common.collect.Lists;

import ar.com.itba.piedpiper.web.panel.StateFilterPanel.StateFilterModel;

@SuppressWarnings("serial")
public class ApplicationSession extends WebSession {

	private int mainPageLoadCount = 0;
	private int datesArrayLength = 0;
	private List<InputStream> gifStreams = Lists.newArrayList();
	private StateFilterModel stateFilterModel;
	
	public static ApplicationSession get() {
		return (ApplicationSession) Session.get();
	}
	
	public ApplicationSession(Request request) {
		super(request);
	}
	
	public int mainPageLoadCount() {
		return mainPageLoadCount;
	}
	
	public void increaseMainPageLoadCount() {
		mainPageLoadCount++;
	}
	
	public int datesArrayLength() {
		return datesArrayLength;
	}
	
	public void datesArrayLength(int datesArrayLength) {
		this.datesArrayLength = datesArrayLength;
	}
	
	public void addGifStream(InputStream stream) {
		gifStreams.add(stream);
	}
	
	public List<InputStream> getGifStream() {
		return gifStreams;
	}
	
	public StateFilterModel stateFilterModel() {
		return stateFilterModel;
	}
	
	public void stateFilterModel(StateFilterModel stateFilterModel) {
		this.stateFilterModel = stateFilterModel;
	}
	
}
