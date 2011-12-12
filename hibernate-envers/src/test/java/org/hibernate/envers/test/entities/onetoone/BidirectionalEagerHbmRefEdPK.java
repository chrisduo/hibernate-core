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

import org.hibernate.envers.Audited;

/**
 * <b>BidirectionalRefEdPK</b>
 * <p>
 *
 * @author Quartet Financial Systems
 * @see
 */
@Audited
public class BidirectionalEagerHbmRefEdPK {
    private long longId;
    private String data;
    private BidirectionalEagerHbmRefIngPK referencing;

    public BidirectionalEagerHbmRefEdPK() {}

    public BidirectionalEagerHbmRefEdPK(long id, String data) {
        this.data = data;
    }

    public BidirectionalEagerHbmRefEdPK(long id, String data, BidirectionalEagerHbmRefIngPK referencing) {
        this.data = data;
        this.referencing = referencing;
    }

    public long getLongId() {
        return longId;
    }

    public void setLongId(Long id) {
        this.longId = id;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public BidirectionalEagerHbmRefIngPK getReferencing() {
        return referencing;
    }

    public void setReferencing(BidirectionalEagerHbmRefIngPK referencing) {
        this.referencing = referencing;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BidirectionalEagerHbmRefEdPK)) return false;

        BidirectionalEagerHbmRefEdPK that = (BidirectionalEagerHbmRefEdPK) o;

        if (data != null ? !data.equals(that.data) : that.data != null) return false;
        Long longId = new Long(this.longId);
        Long thatLongId = new Long(that.longId);
        if (longId != null ? !longId.equals(that.longId) : thatLongId != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        Long longId = new Long(this.longId);
        result = (longId != null ? longId.hashCode() : 0);
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }
}