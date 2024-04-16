package telran.blocker;

import java.io.IOException;
import java.util.*;

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

	private static final long TIME_UPDATED = 600000; // 10 min for example; this is list update period

	RestTemplate restTemplate = new RestTemplate();

	String url = "/ip/get_ips"; // ???
	String host = "localhost";
	int port = 8484;

	List<String> ipBlockList;

	@Scheduled(fixedRate = TIME_UPDATED)
	public void periodacallyCallingRemoteProvider() {
		getUpdateIpBlockList();
	}
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String ipClient = getClientIp(request);

		if (ipBlockList.contains(ipClient)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "access denied");

		} else {

			// continue application's filter chain

			filterChain.doFilter(request, response);
		}

	}

	private String getClientIp(HttpServletRequest request) {
		String ip = request.getHeader("X-Forwarded-For");
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		return ip;
	}

	@SuppressWarnings("unchecked")
	private List<String> getUpdateIpBlockList() {
		ipBlockList.clear();
		try {
			ResponseEntity<?> responseEntity = restTemplate.exchange(getFullUrl(), HttpMethod.GET, null, List.class);
			if (!responseEntity.getStatusCode().is2xxSuccessful()) {
				throw new Exception((String) responseEntity.getBody());
			}
			ipBlockList = (List<String>) responseEntity.getBody();
			log.debug("updated block list is {} ", ipBlockList);
		} catch (Exception e) {
			log.error("Blocking Data Provider's server is unavailable, {}", e.getMessage());
		}

		return ipBlockList;
	}

	private String getFullUrl() {
		return String.format("http://%s:%d%s", host, port, url);
	}

//This is supposed end-point method for BlockingDataprovider controller:

//	@GetMapping("${app.blocking.data.list.url:/ip/get_ips}")
//	List<String> getBlockingList (String ip) {
//		log.debug("received IP: {}", ip);
//		List<String> res = providerService.getBlockingList();
//		log.trace("received blocking ip list: {}", res);
//		return res;
//	}

}
