package fr.echoes.lab.foremanapi.model;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Host {

	@JsonProperty("name") public String name;
	@JsonProperty("id") public String id;

}