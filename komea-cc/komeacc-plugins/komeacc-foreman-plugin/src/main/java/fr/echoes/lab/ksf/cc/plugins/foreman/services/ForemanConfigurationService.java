package fr.echoes.lab.ksf.cc.plugins.foreman.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service("foremanConfiguration")
public class ForemanConfigurationService {

    @Value("${ksf.foreman.url}")
    private String url;

    @Value("${ksf.foreman.username}")
    private String username;

    @Value("${ksf.foreman.password}")
    private String password;

    public String getForemanUrl() {
        if ('/' == url.charAt(url.length() - 1)) {
            url = url.substring(0, url.length() - 1);
        }
        return this.url;
    }

    public String getForemanUsername() {
        return this.username;
    }

    public String getForemanPassword() {
        return this.password;
    }

}
