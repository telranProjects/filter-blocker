package telran.blocker;

import java.io.IOException;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component("ipBlockingFilter")
@Slf4j
public class IpBlockingFilter extends OncePerRequestFilter {

//	private static final long TIME_UPDATED = 600000; // 10 min for example; this is list update period

	RestTemplate restTemplate = new RestTemplate();
	
	@Value("${app.filter.blocking.data.provider.url:/ip/get_ips}")
	String url;
	@Value("${app.filter.blocking.data.provider.host:localhost}")
	String host;
	@Value("${app.filter.blocking.data.provider.port:8484}")
	int port;

	Set<String> ipBlockSet;

	@Scheduled(fixedRateString = "${app.time.updated:600000}")  //10 min 
	public void periodacallyCallingRemoteProvider() {
		getUpdateIpBlockList();
	}
	 
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String ipClient = getClientIp(request);

		if (ipBlockSet.contains(ipClient)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "access denied");

		} else {

			// continue application's filter chain

			filterChain.doFilter(request, response);
		}

	}

	public String getClientIp(HttpServletRequest request) {
		String ip = request.getHeader("X-Forwarded-For");
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		return ip;
	}

	@SuppressWarnings("unchecked")
	public Set<String> getUpdateIpBlockList() {
		ipBlockSet.clear();
		try {
			ResponseEntity<?> responseEntity = restTemplate.exchange(getFullUrl(), HttpMethod.GET, null, Set.class);
			if (!responseEntity.getStatusCode().is2xxSuccessful()) {
				throw new Exception((String) responseEntity.getBody());
			}
			ipBlockSet = (Set<String>) responseEntity.getBody();
			log.debug("updated block list is {} ", ipBlockSet);
		} catch (Exception e) {
			log.error("Blocking Data Provider's server is unavailable, {}", e.getMessage());
		}

		return ipBlockSet;
	}

	public String getFullUrl() {
		return String.format("http://%s:%d%s", host, port, url);
	}


}
