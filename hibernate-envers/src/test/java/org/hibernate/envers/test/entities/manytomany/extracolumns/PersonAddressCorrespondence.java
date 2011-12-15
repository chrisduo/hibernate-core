package org.hibernate.envers.test.entities.manytomany.extracolumns;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * An {@link Address} used for correspondence with a {@link Person}.
 * 
 * @author Erik-Berndt Scheper
 * 
 */
@Entity
@Table(name = "PSNADR_CORR")
@org.hibernate.envers.Audited
@Access(AccessType.PROPERTY)
public class PersonAddressCorrespondence extends PersonAddress {

	private static final long serialVersionUID = -959455186568499962L;

	/**
	 * Default no-args constructor.
	 */
	public PersonAddressCorrespondence() {
		super();
	}

	/**
	 * Initializing constructor.
	 * 
	 * @param person
	 *            the joined {@link Person} instance
	 * @param address
	 *            the joined {@link Address} instance
	 */
	public PersonAddressCorrespondence(Person person, Address address) {
		super(person, address);
	}

}