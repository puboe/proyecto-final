package ar.com.itba.nub3s.web;

import java.io.Serializable;

import org.apache.wicket.Component;

import com.google.common.base.Function;

public interface ComponentBuilder extends Function<String, Component>, Serializable {

}
