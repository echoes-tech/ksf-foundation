package fr.echoes.labs.foremanapi.model;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PuppetClass {

	@JsonProperty("id") public String id;
	@JsonProperty("name") public String name;
	@JsonProperty("module_name") public String module_name;
}
