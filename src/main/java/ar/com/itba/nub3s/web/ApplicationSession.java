package ar.com.itba.nub3s.web;

import java.io.InputStream;
import java.util.List;

import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.Request;

import com.google.common.collect.Lists;

import ar.com.itba.nub3s.model.entity.Channel;
import ar.com.itba.nub3s.web.panel.StateFilterPanel.StateFilterModel;

@SuppressWarnings("serial")
public class ApplicationSession extends WebSession {

	private int mainPageLoadCount = 0;
	private List<InputStream> gifStreams = Lists.newArrayList();
	private StateFilterModel stateFilterModel;
	private String firstDate;
	private String lastDate;
	
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

	public int currentSteps() {
		return Integer.valueOf(stateFilterModel.stepsModelObject());
	}

	public Channel currentChannel() {
		return stateFilterModel.channelModelObject();
	}

	public boolean currentEnhanced() {
		return stateFilterModel.enhancedModelObject();
	}
	
	public String firstDate() {
		return firstDate;
	}
	
	public void firstDate(String firstDate) {
		this.firstDate = firstDate;
	}
	
	public String lastDate() {
		return lastDate;
	}
	
	public void lastDate(String lastDate) {
		this.lastDate = lastDate;
	}

}
