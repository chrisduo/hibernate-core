package org.hibernate.envers.test.entities.manytomany.extracolumns;

import java.io.Serializable;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;

/**
 * Embedded {@code id} for the join table between {@link Person} and
 * {@link Address}.
 * 
 * @author Erik-Berndt Scheper
 */
@Embeddable
@Access(AccessType.PROPERTY)
public final class PersonAddressId implements Serializable {

	private static final long serialVersionUID = 7092451584458452091L;

	private Long personId;
	private Long addressId;

	/**
	 * @return the personId
	 */
	public Long getPersonId() {
		return personId;
	}

	/**
	 * @param personId
	 *            the personId to set
	 */
	public void setPersonId(Long personId) {
		this.personId = personId;
	}

	/**
	 * @return the addressId
	 */
	public Long getAddressId() {
		return addressId;
	}

	/**
	 * @param addressId
	 *            the addressId to set
	 */
	public void setAddressId(Long addressId) {
		this.addressId = addressId;
	}

	@Override
	public String toString() {
		StringBuilder output = new StringBuilder();
		output.append("PersonAddressId {");
		output.append(" personId = \"").append(this.personId).append("\", ");
		output.append(" addressId = \"").append(this.addressId).append("\"}");
		return output.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((personId == null) ? 0 : personId.hashCode());
		result = prime * result
				+ ((addressId == null) ? 0 : addressId.hashCode());
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
		PersonAddressId other = (PersonAddressId) obj;
		if (personId == null) {
			if (other.personId != null)
				return false;
		} else if (!personId.equals(other.personId))
			return false;
		if (addressId == null) {
			if (other.addressId != null)
				return false;
		} else if (!addressId.equals(other.addressId))
			return false;
		return true;
	}

}
