package com.naturalprogrammer.spring.lemondemo;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;

import com.naturalprogrammer.spring.lemon.domain.ChangePasswordForm;
import com.naturalprogrammer.spring.lemon.security.LemonSecurityConfig;
import com.naturalprogrammer.spring.lemon.util.LemonUtils;

@Sql({"/test-data/initialize.sql", "/test-data/finalize.sql"})
public class ChangePasswordMvcTests extends AbstractMvcTests {
	
	private static final String NEW_PASSWORD = "a-new-password";
	
	private ChangePasswordForm changePasswordForm(String oldPassword) {
		
		ChangePasswordForm form = new ChangePasswordForm();
		form.setOldPassword(oldPassword);
		form.setPassword(NEW_PASSWORD);
		form.setRetypePassword(NEW_PASSWORD);
		
		return form;		
	}

	/**
	 * A non-admin user should be able to change his password.
	 */
	@Test
	public void testChangePassword() throws Exception {
		
		mvc.perform(post("/api/core/users/{id}/password", UNVERIFIED_USER_ID)
				.header(LemonSecurityConfig.TOKEN_REQUEST_HEADER_NAME, tokens.get(UNVERIFIED_USER_ID))
				.contentType(MediaType.APPLICATION_JSON)
				.content(LemonUtils.toJson(changePasswordForm(USER_PASSWORD))))
				.andExpect(status().is(204))
				.andExpect(header().string(LemonSecurityConfig.TOKEN_RESPONSE_HEADER_NAME, containsString(".")));
		
		// Ensure able to login with new password
		login(UNVERIFIED_USER_EMAIL, NEW_PASSWORD);
	}
	
	/**
	 * An good admin user should be able to change the password of another user.
	 */
	@Test
	public void testAdminChangePasswordAnotherUser() throws Exception {
		
		mvc.perform(post("/api/core/users/{id}/password", UNVERIFIED_USER_ID)
				.header(LemonSecurityConfig.TOKEN_REQUEST_HEADER_NAME, tokens.get(ADMIN_ID))
				.contentType(MediaType.APPLICATION_JSON)
				.content(LemonUtils.toJson(changePasswordForm(ADMIN_PASSWORD))))
				.andExpect(status().is(204))
				.andExpect(header().string(LemonSecurityConfig.TOKEN_RESPONSE_HEADER_NAME, containsString(".")));
		
		// Ensure able to login with new password
		login(UNVERIFIED_USER_EMAIL, NEW_PASSWORD);
	}
	
	/**
	 * Providing an unknown id should return 404.
	 */
	@Test
	public void testChangePasswordUnknownId() throws Exception {
		
		mvc.perform(post("/api/core/users/99/password")
				.header(LemonSecurityConfig.TOKEN_REQUEST_HEADER_NAME, tokens.get(ADMIN_ID))
				.contentType(MediaType.APPLICATION_JSON)
				.content(LemonUtils.toJson(changePasswordForm(ADMIN_PASSWORD))))
				.andExpect(status().is(404));
	}
	
	/**
	 * A non-admin user should not be able to change others' password.
	 */
	@Test
	public void testChangePasswordAnotherUser() throws Exception {
		
		mvc.perform(post("/api/core/users/{id}/password", UNVERIFIED_USER_ID)
				.header(LemonSecurityConfig.TOKEN_REQUEST_HEADER_NAME, tokens.get(USER_ID))
				.contentType(MediaType.APPLICATION_JSON)
				.content(LemonUtils.toJson(changePasswordForm(USER_PASSWORD))))
				.andExpect(status().is(403));
		
		// Ensure password didn't change
		login(UNVERIFIED_USER_EMAIL, USER_PASSWORD);		
	}

	/**
	 * A  bad admin user should not be able to change others' password.
	 */
	@Test
	public void testBadAdminChangePasswordAnotherUser() throws Exception {
		
		mvc.perform(post("/api/core/users/{id}/password", UNVERIFIED_USER_ID)
				.header(LemonSecurityConfig.TOKEN_REQUEST_HEADER_NAME, tokens.get(UNVERIFIED_ADMIN_ID))
				.contentType(MediaType.APPLICATION_JSON)
				.content(LemonUtils.toJson(changePasswordForm(ADMIN_PASSWORD))))
				.andExpect(status().is(403));
		
		// Ensure password didn't change
		login(UNVERIFIED_USER_EMAIL, USER_PASSWORD);		
	}
	
	@Test
	public void testChangePasswordInvalidData() throws Exception {
		
		// All fields null
		mvc.perform(post("/api/core/users/{id}/password", UNVERIFIED_USER_ID)
				.header(LemonSecurityConfig.TOKEN_REQUEST_HEADER_NAME, tokens.get(UNVERIFIED_USER_ID))
				.contentType(MediaType.APPLICATION_JSON)
				.content(LemonUtils.toJson(new ChangePasswordForm())))
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.errors[*].field").value(hasSize(3)))
				.andExpect(jsonPath("$.errors[*].field").value(hasItems(
						"changePasswordForm.oldPassword",
						 "changePasswordForm.retypePassword",
						 "changePasswordForm.password")));
		
		// All fields too short
		ChangePasswordForm form = new ChangePasswordForm();
		form.setOldPassword("short");
		form.setPassword("short");
		form.setRetypePassword("short");

		mvc.perform(post("/api/core/users/{id}/password", UNVERIFIED_USER_ID)
				.header(LemonSecurityConfig.TOKEN_REQUEST_HEADER_NAME, tokens.get(UNVERIFIED_USER_ID))
				.contentType(MediaType.APPLICATION_JSON)
				.content(LemonUtils.toJson(form)))
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.errors[*].field").value(hasSize(3)))
				.andExpect(jsonPath("$.errors[*].field").value(hasItems(
						"changePasswordForm.oldPassword",
						 "changePasswordForm.retypePassword",
						 "changePasswordForm.password")));
		
		form = changePasswordForm(USER_PASSWORD);
		form.setRetypePassword("different-retype-password");

		mvc.perform(post("/api/core/users/{id}/password", UNVERIFIED_USER_ID)
				.header(LemonSecurityConfig.TOKEN_REQUEST_HEADER_NAME, tokens.get(UNVERIFIED_USER_ID))
				.contentType(MediaType.APPLICATION_JSON)
				.content(LemonUtils.toJson(form)))
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.errors[*].field").value(hasSize(2)))
				.andExpect(jsonPath("$.errors[*].field").value(hasItems(
						 "changePasswordForm.retypePassword",
						 "changePasswordForm.password")));
	}
}
