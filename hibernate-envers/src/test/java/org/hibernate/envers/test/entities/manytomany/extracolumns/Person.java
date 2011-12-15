package org.hibernate.envers.test.entities.manytomany.extracolumns;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@Entity
@org.hibernate.envers.Audited
public class Person implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	@Column(name = "ID", nullable = false)
	private Long id;

	@Column(name = "NAME", nullable = false)
	private String name;

	@Column(name = "BIRTHDATE", nullable = false)
	private Date birthDate;

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "person")
	@org.hibernate.annotations.ForeignKey(name = "FK_PAE_PSN", inverseName = "FK_PAE_ADS")
	@org.hibernate.annotations.Fetch(org.hibernate.annotations.FetchMode.SUBSELECT)
	private List<PersonAddressCorrespondence> correspondenceAddresses = new ArrayList<PersonAddressCorrespondence>();

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "person")
	@org.hibernate.annotations.ForeignKey(name = "FK_PAE_PSN", inverseName = "FK_PAE_ADS")
	@org.hibernate.annotations.Fetch(org.hibernate.annotations.FetchMode.SUBSELECT)
	private List<PersonAddressHome> homeAddresses = new ArrayList<PersonAddressHome>();

	/**
	 * Default constructor.
	 */
	public Person() {
		// do nothing
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the birthDate
	 */
	public Date getBirthDate() {
		return birthDate;
	}

	/**
	 * @param birthDate
	 *            the birthDate to set
	 */
	public void setBirthDate(Date birthDate) {
		this.birthDate = birthDate;
	}

	/**
	 * @return the correspondenceAddresses
	 */
	public List<PersonAddressCorrespondence> getCorrespondenceAddresses() {
		return correspondenceAddresses;
	}

	/**
	 * @param correspondenceAddress
	 *            the correspondenceAddresses to add
	 */
	public void addCorrespondenceAddresses(
			PersonAddressCorrespondence correspondenceAddress) {
		this.correspondenceAddresses.add(correspondenceAddress);
	}

	/**
	 * @return the homeAddresses
	 */
	public List<PersonAddressHome> getHomeAddresses() {
		return homeAddresses;
	}

	/**
	 * @param homeAddress
	 *            the homeAddress to add
	 */
	public void addHomeAddress(PersonAddressHome homeAddress) {
		this.homeAddresses.add(homeAddress);
	}

	@Override
	public String toString() {
		StringBuilder output = new StringBuilder();
		output.append("Person {");
		output.append(" id = \"").append(this.id).append("\", ");
		output.append(" name = \"").append(this.name).append("\", ");
		output.append(" birthDate = \"").append(this.birthDate).append("\"}");
		return output.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((birthDate == null) ? 0 : birthDate.hashCode());
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
		Person other = (Person) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (birthDate == null) {
			if (other.birthDate != null)
				return false;
		} else if (!birthDate.equals(other.birthDate))
			return false;
		return true;
	}

}
