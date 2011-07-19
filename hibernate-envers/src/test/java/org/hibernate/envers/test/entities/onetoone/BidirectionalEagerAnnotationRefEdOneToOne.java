/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.test.entities.onetoone;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.envers.NotAudited;

@Entity
@org.hibernate.envers.Audited
public final class BidirectionalEagerAnnotationRefEdOneToOne {

	/**
	 * ID column.
	 */
	@Id
	@GeneratedValue
	private Integer id;

	/**
	 * Field containting the referring entity.
	 */
	@OneToOne(mappedBy = "refedOne", fetch = FetchType.EAGER)
	@NotAudited
	private BidirectionalEagerAnnotationRefIngOneToOne refIng;

	/**
	 * Field containing some data.
	 */
	private String data;

	/* ----- Getters and setters ----- */

	/**
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @return the refIng
	 */
	public BidirectionalEagerAnnotationRefIngOneToOne getRefIng() {
		return refIng;
	}

	/**
	 * @param refIng
	 *            the refIng to set
	 */
	public void setRefIng(BidirectionalEagerAnnotationRefIngOneToOne refIng) {
		this.refIng = refIng;
	}

	/**
	 * @return the data
	 */
	public String getData() {
		return data;
	}

	/**
	 * @param data
	 *            the data to set
	 */
	public void setData(String data) {
		this.data = data;
	}

}
