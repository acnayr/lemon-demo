package com.naturalprogrammer.spring.lemondemo.entities;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonView;
import com.naturalprogrammer.spring.lemon.domain.AbstractUser;

@Entity
@Table(name="usr")
public class User extends AbstractUser<User,Long> {

    private static final long serialVersionUID = 2716710947175132319L;

    public static final int NAME_MIN = 1;
    public static final int NAME_MAX = 50;

	public static class Tag implements Serializable {
		
		private static final long serialVersionUID = -2129078111926834670L;

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public User() {}
	
	public User(String email, String password, String name) {
		this.email = email;
		this.password = password;
		this.name = name;
	}

	@JsonView(SignupInput.class)
	@NotBlank(message = "{blank.name}", groups = {SignUpValidation.class, UpdateValidation.class})
    @Size(min=NAME_MIN, max=NAME_MAX, groups = {SignUpValidation.class, UpdateValidation.class})
    @Column(nullable = false, length = NAME_MAX)
    private String name;
	
	@Override
	public Tag toTag() {
		
		Tag tag = new Tag();
		tag.setName(name);
		return tag;
	}
	
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}