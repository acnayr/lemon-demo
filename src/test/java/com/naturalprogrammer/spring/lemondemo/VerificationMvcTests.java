package com.naturalprogrammer.spring.lemondemo;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.naturalprogrammer.spring.lemon.security.JwtService;
import com.naturalprogrammer.spring.lemon.security.LemonSecurityConfig;
import com.naturalprogrammer.spring.lemon.util.LemonUtils;
import com.naturalprogrammer.spring.lemondemo.entities.User;

public class VerificationMvcTests extends AbstractMvcTests {
	
	private String verificationCode;
	
	@Autowired
	private JwtService jwtService;
	
	@Before
	public void setUp() {
		
		verificationCode = jwtService.createToken(JwtService.VERIFY_AUDIENCE,
				Long.toString(UNVERIFIED_USER_ID), 60000L,
				LemonUtils.mapOf("email", UNVERIFIED_USER_EMAIL));
	}
	
	@Test
	public void testEmailVerification() throws Exception {
		
		mvc.perform(post("/api/core/users/{userId}/verification", UNVERIFIED_USER_ID)
                .param("code", verificationCode)
                .header("contentType",  MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().is(200))
				.andExpect(header().string(LemonSecurityConfig.TOKEN_RESPONSE_HEADER_NAME, containsString(".")))
				.andExpect(jsonPath("$.id").value(UNVERIFIED_USER_ID))
				.andExpect(jsonPath("$.roles").value(hasSize(0)))
				.andExpect(jsonPath("$.unverified").value(false))
				.andExpect(jsonPath("$.goodUser").value(true));
		
		// Already verified
		mvc.perform(post("/api/core/users/{userId}/verification", UNVERIFIED_USER_ID)
                .param("code", verificationCode)
                .header("contentType",  MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().is(422));
	}
	
	@Test
	public void testEmailVerificationNonExistingUser() throws Exception {
		
		mvc.perform(post("/api/core/users/99/verification")
                .param("code", verificationCode)
                .header("contentType",  MediaType.MULTIPART_FORM_DATA)
				.header(LemonSecurityConfig.TOKEN_REQUEST_HEADER, tokens.get(UNVERIFIED_USER_ID)))
                .andExpect(status().is(404));
	}
	
	@Test
	public void testEmailVerificationWrongToken() throws Exception {
		
		// null token
		mvc.perform(post("/api/core/users/{userId}/verification", UNVERIFIED_USER_ID)
                .header("contentType",  MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().is(400));

		// blank token
		mvc.perform(post("/api/core/users/{userId}/verification", UNVERIFIED_USER_ID)
                .param("code", "")
                .header("contentType",  MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().is(401));

		// Wrong audience
		String token = jwtService.createToken("wrong-audience",
				Long.toString(UNVERIFIED_USER_ID), 60000L,
				LemonUtils.mapOf("email", UNVERIFIED_USER_EMAIL));
		mvc.perform(post("/api/core/users/{userId}/verification", UNVERIFIED_USER_ID)
                .param("code", token)
                .header("contentType",  MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().is(403));
		
		// Wrong email
		token = jwtService.createToken(JwtService.VERIFY_AUDIENCE,
				Long.toString(UNVERIFIED_USER_ID), 60000L,
				LemonUtils.mapOf("email", "wrong.email@example.com"));
		mvc.perform(post("/api/core/users/{userId}/verification", UNVERIFIED_USER_ID)
                .param("code", token)
                .header("contentType",  MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().is(403));

		// expired token
		token = jwtService.createToken(JwtService.VERIFY_AUDIENCE,
				Long.toString(UNVERIFIED_USER_ID), 1L,
				LemonUtils.mapOf("email", UNVERIFIED_USER_EMAIL));	
		// Thread.sleep(1001L);
		mvc.perform(post("/api/core/users/{userId}/verification", UNVERIFIED_USER_ID)
                .param("code", token)
                .header("contentType",  MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().is(403));
	}
	
	@Test
	public void testEmailVerificationAfterCredentialsUpdate() throws Exception {
		
		// Credentials updated after the verification token is issued
		// Thread.sleep(1001L);
		User user = userRepository.findById(UNVERIFIED_USER_ID).get();
		user.setCredentialsUpdatedMillis(System.currentTimeMillis());
		userRepository.save(user);
		
		mvc.perform(post("/api/core/users/{userId}/verification", UNVERIFIED_USER_ID)
                .param("code", verificationCode)
                .header("contentType",  MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().is(403));
	}
}
