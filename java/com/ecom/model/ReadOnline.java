package com.ecom.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
public class ReadOnline {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(length = 500)
	private String title;

	@Column(length = 5000)
	private String description;

	@Column(length = 255)
	private String category;
	
	@Column(length = 255)
	private String file;

	@Column(length = 255)
	private String coverImage;

	@Override
	public String toString() {
    	return "ReadOnline [id=" + id + ", title=" + title + ", category=" + category + "]";
	}
}
