package telran.blocker;

import java.io.IOException;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sun.tools.sjavac.Log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component("ipBlockingFilter")
@Slf4j
public class IpBlockingFilter extends OncePerRequestFilter {
	
	RestTemplate restTemplate = new RestTemplate();
	
	String url = "/ip/exist";
	String host = "localhost";
	int port = 8484;
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String ipClient = getClientIp(request);

		if (ipShouldBlocked(ipClient)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "access denied");
			return;
		}

		// continue application's filter chain

		filterChain.doFilter(request, response);
	}

	private String getClientIp(HttpServletRequest request) {
		String ip = request.getHeader("X-Forwarded-For");
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		return ip;
	}

	private boolean ipShouldBlocked(String ip) {
		boolean res = false;
		try {
			ResponseEntity<?> responseEntity = restTemplate.exchange(getFullUrl(ip), HttpMethod.GET, null,
					Boolean.class);
			if (!responseEntity.getStatusCode().is2xxSuccessful()) {
				throw new Exception((String) responseEntity.getBody());
			}
			res = (boolean) responseEntity.getBody();
			log.debug("ip {} contain in blocking list: {}", ip, res);
		} catch (Exception e) {
			log.error("Blocking Data Provider's server is unavailable, {}", e.getMessage());
		}
		return res;
	}

	private String getFullUrl(String ip) {
		String res = String.format("hppt://%s:%d%s/%s", host, port, url, ip);
		return res;
	}
	
//	@GetMapping("${app.blocking.data.exist.url:/ip/exist}" + "/{ip}")
//	boolean existsById (@PathVariable(name="ip") String ip) {
//		log.debug("received IP: {}", ip);
//		boolean res = providerService.existsById(ip);
//		log.debug("IP: {} is exists: {}", ip, res);
//		return res;
//	}


}
