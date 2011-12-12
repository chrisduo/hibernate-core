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
package org.hibernate.envers.test.integration.onetoone.bidirectional;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import javax.persistence.EntityManager;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.onetoone.BidirectionalEagerHbmRefEdPK;
import org.hibernate.envers.test.entities.onetoone.BidirectionalEagerHbmRefIngPK;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test HHH-3854: NullPointerException in AbstractEntityTuplizer.createProxy() when using eager loading. 
 * 
 * @author Erik-Berndt Scheper, Amar Singh
 */
public class BidirectionalEagerHbmTest extends AbstractEntityTest {
	private Long ed1_id;

	@Override
	public void configure(Ejb3Configuration cfg) {
		URL path = Thread.currentThread().getContextClassLoader().getResource("mappings/oneToOne/bidirectional/bidirectionalEagerLoading.hbm.xml");
		try {
			cfg.addFile(new File(path.toURI()));
			
		} catch (URISyntaxException uriex) {
			throw new IllegalStateException("Cannot load mapping", uriex);
		}
	}

	@BeforeClass(dependsOnMethods = "init")
	public void initData() {
		BidirectionalEagerHbmRefEdPK ed1 = new BidirectionalEagerHbmRefEdPK(1,
				"data_ed_1");
		BidirectionalEagerHbmRefIngPK ing1 = new BidirectionalEagerHbmRefIngPK(
				3, "data_ing_1");

		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		ing1.setReference(ed1);

		em.persist(ed1);
		em.persist(ing1);
		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		ing1 = em.find(BidirectionalEagerHbmRefIngPK.class, ing1.getId());
		em.getTransaction().commit();

		ed1_id = ed1.getLongId();
	}

	@Test
	public void testRevisionsCounts() {
		BidirectionalEagerHbmRefIngPK referencing = getAuditReader().find(
				BidirectionalEagerHbmRefIngPK.class, ed1_id, 1);
		assert referencing.getReference().getData() != null;
	}

}
