package ar.com.itba.piedpiper.web;

import java.util.List;
import java.util.Set;

import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.Request;

import com.google.common.collect.Sets;

import ar.com.itba.piedpiper.model.entity.SavedState;
import jersey.repackaged.com.google.common.collect.Lists;

@SuppressWarnings("serial")
public class ApplicationSession extends WebSession {

//	private Set<SavedState> savedStates = Sets.newHashSet();
//	
//	public static ApplicationSession get() {
//		return (ApplicationSession) Session.get();
//	}
//	
	public ApplicationSession(Request request) {
		super(request);
	}
//
//	public boolean saveState(SavedState savedState) {
//		return savedStates.add(savedState);
//	}
//	
//	public void removeState(SavedState savedState) {
//		savedStates.remove(savedState);
//	}
//	
//	public Set<SavedState> getStates() {
//		return savedStates;
//	}
//	
//	public boolean containsState(SavedState savedState){
//		return savedStates.contains(savedState);
//	}
//	
//	public List<SavedState> getStatesAsList() {
//		return Lists.newLinkedList(savedStates);
//	}
}
