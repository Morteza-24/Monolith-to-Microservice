package com.spring.netflix.oss.microservices.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class CardVO {

	private Long id;
	private String cardHolderName;
	private String pan;
	private String validDate;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getCardHolderName() {
		return cardHolderName;
	}
	public void setCardHolderName(String cardHolderName) {
		this.cardHolderName = cardHolderName;
	}
	public String getPan() {
		return pan;
	}
	public void setPan(String pan) {
		this.pan = pan;
	}
	public String getValidDate() {
		return validDate;
	}
	public void setValidDate(String validDate) {
		this.validDate = validDate;
	}
	@Override
	public String toString() {
		return "CardVO [id=" + id + ", cardHolderName=" + cardHolderName + ", pan=" + pan + ", validDate=" + validDate
				+ "]";
	}
}
