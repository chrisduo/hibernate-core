package org.hibernate.envers.test.entities.manytomany.extracolumns;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.MapsId;

/**
 * Join table between a person and his addresses.
 * 
 * @author Erik-Berndt Scheper
 */
@MappedSuperclass
@Access(AccessType.PROPERTY)
@org.hibernate.envers.Audited
public abstract class PersonAddress implements Serializable {

	private static final long serialVersionUID = -2545333901216432653L;

	private PersonAddressId primaryKey = new PersonAddressId();

	private Person person;

	private Address address;

	private Date startDate;

	private Date endDate;

	/**
	 * Default no-args constructor.
	 */
	public PersonAddress() {
		// do nothing
	}

	/**
	 * Initializing constructor.
	 * 
	 * @param person
	 *            the joined {@link Person} instance
	 * @param address
	 *            the joined {@link Address} instance
	 */
	public PersonAddress(Person person, Address address) {
		this();
		setPerson(person);
		setAddress(address);
	}

	/**
	 * @return the primary key value
	 */
	@EmbeddedId
	public PersonAddressId getPrimaryKey() {
		return primaryKey;
	}

	/**
	 * @param primaryKey
	 *            the new primary key value
	 */
	public void setPrimaryKey(PersonAddressId primaryKey) {
		this.primaryKey = primaryKey;
	}

	/**
	 * @return the associated {@code Person} instance
	 */
	@MapsId("personId")
	@JoinColumns({ @JoinColumn(name = "PSN_ID", nullable = false, insertable = false, updatable = false) })
	@ManyToOne(cascade = { CascadeType.REFRESH }, optional = false)
	@Access(AccessType.PROPERTY)
	public Person getPerson() {
		return this.person;
	}

	/**
	 * @param person
	 *            the associated {@code Person} instance to set
	 */
	public void setPerson(Person person) {
		this.person = person;
	}

	/**
	 * @return the associated {@code Address} instance
	 */
	@MapsId("addressId")
	@JoinColumns({ @JoinColumn(name = "ADS_ID", nullable = false, insertable = true, updatable = false) })
	@ManyToOne(cascade = { CascadeType.ALL }, optional = false)
	@Access(AccessType.PROPERTY)
	public Address getAddress() {
		return this.address;
	}

	/**
	 * @param address
	 *            the associated {@code Address} instance to set
	 */
	public void setAddress(Address address) {
		this.address = address;
	}

	/**
	 * @return the startDate
	 */
	@Column(name = "START_DATE", nullable = false)
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
	@Column(name = "END_DATE", nullable = true)
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
		output.append(this.getClass().getSimpleName());
		output.append(" { personId = \"").append(this.person.getId())
				.append("\", ");
		output.append(" addressId = \"").append(this.address.getId())
				.append("\", ");
		output.append(" startDate = \"").append(this.startDate).append("\", ");
		output.append(" endDate = \"").append(this.endDate).append("\"}");

		return output.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((person == null) ? 0 : person.hashCode());
		result = prime * result + ((address == null) ? 0 : address.hashCode());
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
		PersonAddress other = (PersonAddress) obj;
		if (person == null) {
			if (other.person != null)
				return false;
		} else if (!person.equals(other.person))
			return false;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		if (startDate == null) {
			if (other.startDate != null)
				return false;
		} else if (!startDate.equals(other.startDate))
			return false;
		return true;
	}

}
