package org.hibernate.envers.test.entities.manytomany.extracolumns;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@org.hibernate.envers.Audited
public class Address {
	@Id
	@GeneratedValue
	@Column(name = "ID", nullable = false)
	private Long id;

	@Column(name = "GPS_LATITUDE", nullable = false)
	private String gpsLatitude;

	@Column(name = "GPS_LONGITUDE", nullable = false)
	private String gpsLongitude;

	@Column(name = "START_DATE", nullable = false)
	private Date startDate;

	@Column(name = "END_DATE", nullable = true)
	private Date endDate;

	@Column(name = "ADDRESS", nullable = true)
	private String address;

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the address
	 */
	public String getAddress() {
		return address;
	}

	/**
	 * @param address
	 *            the address to set
	 */
	public void setAddress(String address) {
		this.address = address;
	}

	/**
	 * @return the gpsLatitude
	 */
	public String getGpsLatitude() {
		return gpsLatitude;
	}

	/**
	 * @param gpsLatitude
	 *            the gpsLatitude to set
	 */
	public void setGpsLatitude(String gpsLatitude) {
		this.gpsLatitude = gpsLatitude;
	}

	/**
	 * @return the gpsLongitude
	 */
	public String getGpsLongitude() {
		return gpsLongitude;
	}

	/**
	 * @param gpsLongitude
	 *            the gpsLongitude to set
	 */
	public void setGpsLongitude(String gpsLongitude) {
		this.gpsLongitude = gpsLongitude;
	}

	/**
	 * @return the startDate
	 */
	public Date getStartDate() {
		return startDate;
	}

	/**
	 * @param startDate
	 *            the startDate to set
	 */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/**
	 * @return the endDate
	 */
	public Date getEndDate() {
		return endDate;
	}

	/**
	 * @param endDate
	 *            the endDate to set
	 */
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	@Override
	public String toString() {
		StringBuilder output = new StringBuilder();
		output.append("Address {");
		output.append(" id = \"").append(this.id).append("\", ");
		output.append(" gpsLatitude = \"").append(this.gpsLatitude)
				.append("\", ");
		output.append(" gpsLongitude = \"").append(this.gpsLongitude)
				.append("\", ");
		output.append(" startDate = \"").append(this.startDate).append("\", ");
		output.append(" endDate = \"").append(this.endDate).append("\", ");
		output.append(" address = \"").append(this.address).append("\"}");
		return output.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((gpsLatitude == null) ? 0 : gpsLatitude.hashCode());
		result = prime * result
				+ ((gpsLongitude == null) ? 0 : gpsLongitude.hashCode());
		result = prime * result
				+ ((startDate == null) ? 0 : startDate.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Address other = (Address) obj;
		if (gpsLatitude == null) {
			if (other.gpsLatitude != null)
				return false;
		} else if (!gpsLatitude.equals(other.gpsLatitude))
			return false;
		if (gpsLongitude == null) {
			if (other.gpsLongitude != null)
				return false;
		} else if (!gpsLongitude.equals(other.gpsLongitude))
			return false;
		if (startDate == null) {
			if (other.startDate != null)
				return false;
		} else if (!startDate.equals(other.startDate))
			return false;
		return true;
	}

}
