package telran.blocker;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class IpBlockingFilterTest {
	private static final String BLOCKED_IP = "192.161.1.1";
	private static final String ALLOWED_IP = "10.0.0.1";
	private static final String CLIENT_IP1 = "192.168.0.2";
	private static final String CLIENT_IP2 = "10.0.0.3";
	private static final String ADD_IP = "192.168.1.10";
	@Mock
	private HttpServletRequest mockRequest;
	@Mock
	private HttpServletResponse mockResponse;
	@Mock
	private FilterChain mockFilterChain; 
	@Mock
	private RestTemplate mockRestTemplate;
	private IpBlockingFilter ipBlockingFilter;
	
	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		ipBlockingFilter = new IpBlockingFilter();
		ipBlockingFilter.ipBlockSet = new HashSet<>();
		ipBlockingFilter.restTemplate = mockRestTemplate;
	}
	
	@Test
	void doFilterInternal_BlockIp() throws ServletException, IOException {
		String blockedIp = BLOCKED_IP;//"192.161.1.1";
		ipBlockingFilter.ipBlockSet.add(blockedIp);
		when(mockRequest.getHeader("X-Forwarded-For")).thenReturn(blockedIp);
		
		ipBlockingFilter.doFilterInternal(mockRequest, mockResponse, mockFilterChain);
		
		verify(mockResponse).sendError(HttpServletResponse.SC_FORBIDDEN, "access denied");
		verify(mockFilterChain, never()).doFilter(any(), any());
	}
	
	@Test
	void doFilterInternal_AllowIp() throws ServletException, IOException {
		String allowedIp = ALLOWED_IP;//"10.0.0.1";
		when(mockRequest.getHeader("X-Forwarded-For")).thenReturn(allowedIp);
		
		ipBlockingFilter.doFilterInternal(mockRequest, mockResponse, mockFilterChain);
		
		verify(mockResponse, never()).sendError(anyInt(), anyString());
		verify(mockFilterChain).doFilter(any(), any());
	}
	
	@Test
	void testGetClientIp_FromHeader() {
		String clientIp = CLIENT_IP1;//"192.168.0.2";
		when(mockRequest.getHeader("X-Forwarded-For")).thenReturn(clientIp);
		
		String res = ipBlockingFilter.getClientIp(mockRequest);
		assertEquals(clientIp, res);
	}
	
	@Test
	void testGetClientIp_FromRemoteAddr() {
		String clientIp = CLIENT_IP2;//"10.0.0.3";
		when(mockRequest.getHeader("X-Forwarded-For")).thenReturn(null);
		when(mockRequest.getRemoteAddr()).thenReturn(clientIp);
		String res = ipBlockingFilter.getClientIp(mockRequest);
		assertEquals(clientIp, res);
	}
	
	@Test
	void testGetUpdateIpBlockList_Success() {
		Set<String> updateIpBlockSet = new HashSet<>();
		updateIpBlockSet.add(ADD_IP);//"192.168.1.10"
		ResponseEntity<Set> mockResponseEntity = new ResponseEntity<Set>(updateIpBlockSet, HttpStatus.OK);
		when(mockRestTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(), eq(Set.class)))
			.thenReturn(mockResponseEntity);
		
		ipBlockingFilter.periodacallyCallingRemoteProvider();
		Set<String> result = ipBlockingFilter.ipBlockSet;
		assertEquals(updateIpBlockSet, result);
		
	}
	
	@Test
	void testGetUpdateIpBlockList_Failure() {
		when(mockRestTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(), eq(Set.class)))
			.thenThrow(new RuntimeException("Server unavailable"));
			
		Set<String> result = ipBlockingFilter.getUpdateIpBlockList();
		assertTrue(result.isEmpty());
	}
	
}
