package org.hibernate.envers.test.integration.manytomany.extracolumns;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.manytomany.extracolumns.Address;
import org.hibernate.envers.test.entities.manytomany.extracolumns.Person;
import org.hibernate.envers.test.entities.manytomany.extracolumns.PersonAddress;
import org.hibernate.envers.test.entities.manytomany.extracolumns.PersonAddressCorrespondence;
import org.hibernate.envers.test.entities.manytomany.extracolumns.PersonAddressHome;
import org.hibernate.envers.test.entities.manytomany.extracolumns.PersonAddressId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

/**
 * Test a many-to-many relation with extra columns, mapped with an embedded id.
 * 
 * See HHH-xxxx And its description
 * 
 * @author Erik-Berndt Scheper
 */
public class ManyToManyExtraColumnsTest extends AbstractEntityTest {

	private final Logger log = LoggerFactory
			.getLogger(ManyToManyExtraColumnsTest.class);

	private Long psnId1;
	private Long psnId2;
	private Long adsId1;
	private Long adsId2;

	public void configure(Ejb3Configuration cfg) {
		cfg.addAnnotatedClass(Person.class);
		cfg.addAnnotatedClass(Address.class);
		cfg.addAnnotatedClass(PersonAddress.class);
		cfg.addAnnotatedClass(PersonAddressCorrespondence.class);
		cfg.addAnnotatedClass(PersonAddressHome.class);
	}

	@Test
	public void initData() throws Exception {
		EntityManager em;

		newEntityManager();
		em = getEntityManager();
		em.getTransaction().begin();
		em.joinTransaction();
		Person psn = new Person();
		psn.setName("Mark Fleming");
		psn.setBirthDate(dateFor(1970, 5, 12));

		em.persist(psn);
		psnId1 = psn.getId();

		em.getTransaction().commit();
		assert psnId1 != null;

		// add a 1st address to the person
		newEntityManager();
		em = getEntityManager();
		em.getTransaction().begin();
		em.joinTransaction();

		Address ads1 = new Address();
		ads1.setAddress("Achter t zand, Dwingelo");
		ads1.setGpsLatitude("N 52 48.514");
		ads1.setGpsLongitude("E 6 25.095");
		ads1.setStartDate(dateFor(2000, 1, 1));

		PersonAddressHome psnAds1 = new PersonAddressHome(psn, ads1);
		psnAds1.setStartDate(dateFor(2010, 10, 31));

		psn.addHomeAddress(psnAds1);
		em.merge(psn);

		em.getTransaction().commit();

		assert psnAds1.getPrimaryKey() != null;

		adsId1 = psnAds1.getPrimaryKey().getAddressId();

		assert psnId1 == psnAds1.getPrimaryKey().getPersonId();
		assert adsId1 != null;

		// add a 2nd address to the person
		newEntityManager();
		em = getEntityManager();
		em.getTransaction().begin();
		em.joinTransaction();

		Person psn2 = em.find(Person.class, psnId1);

		assert psn.equals(psn2);
		psnId2 = psn2.getId();
		assert psnId1 == psnId2;

		psn2.getHomeAddresses().hashCode();
		psnAds1 = psn2.getHomeAddresses().get(0);
		psnAds1.getAddress().setEndDate(dateFor(2011, 11, 30));

		Address ads2 = new Address();
		ads2.setAddress("Spieregerweg, Dwingelo");
		ads2.setGpsLatitude("N 52 49.305");
		ads2.setGpsLongitude("E 6 26.104");
		ads2.setStartDate(dateFor(2000, 1, 1));

		PersonAddressHome psnAds2 = new PersonAddressHome(psn2, ads2);
		psnAds2.setStartDate(dateFor(2010, 10, 31));

		psn2.addHomeAddress(psnAds2);
		em.merge(psn2);

		em.getTransaction().commit();

		assert psnAds2.getPrimaryKey() != null;

		adsId2 = psnAds2.getPrimaryKey().getAddressId();

		assert psnId2 == psnAds2.getPrimaryKey().getPersonId();
		assert adsId2 != null;

		// modify 1st personAddressHome and delete 2nd PersonAddressHome from the person
		newEntityManager();
		em = getEntityManager();
		em.getTransaction().begin();
		em.joinTransaction();

		psn2 = em.find(Person.class, psnId1);

		psn2.getHomeAddresses().hashCode();
		
		PersonAddressHome psnAdrIterated;
		
		Iterator<PersonAddressHome> iter = psn2.getHomeAddresses().iterator();
		
		// set enddate for 1st personAddressHome
		psnAdrIterated = iter.next();
		psnAdrIterated.setEndDate(dateFor(2011, 12, 31));
		
		// remove 2nd personAddressHome
		psnAdrIterated = iter.next();
		iter.remove();

		em.merge(psn2);

		em.getTransaction().commit();

		assert psn2.getHomeAddresses().size() == 1 ;
	}

	@Test(dependsOnMethods = "initData")
	public void testRevisionsCounts() throws Exception {

		List<Number> psnId1Revs = getAuditReader().getRevisions(Person.class,
				psnId1);

		List<Number> adsId1Revs = getAuditReader().getRevisions(Address.class,
				adsId1);
		List<Number> adsId2Revs = getAuditReader().getRevisions(Address.class,
				adsId2);

		PersonAddressId psnAdsId1 = new PersonAddressId();
		psnAdsId1.setPersonId(psnId1);
		psnAdsId1.setAddressId(adsId1);
		List<Number> psnAdsId1Revs = getAuditReader().getRevisions(
				PersonAddressHome.class, psnAdsId1);

		PersonAddressId psnAdsId2 = new PersonAddressId();
		psnAdsId2.setPersonId(psnId1);
		psnAdsId2.setAddressId(adsId2);
		List<Number> psnAdsId2Revs = getAuditReader().getRevisions(
				PersonAddressHome.class, psnAdsId2);

		assert Arrays.asList(1).equals(psnId1Revs);
		assert Arrays.asList(2, 3).equals(adsId1Revs);
		assert Arrays.asList(3, 4).equals(adsId2Revs);
		assert Arrays.asList(2 ,4).equals(psnAdsId1Revs);
		assert Arrays.asList(3, 4).equals(psnAdsId2Revs);

		@SuppressWarnings("unchecked")
		List<Object> auditedPsn = getAuditReader().createQuery()
				.forRevisionsOfEntity(Person.class, false, true)
				.add(AuditEntity.id().eq(psnId1))
				.getResultList();

		@SuppressWarnings("unchecked")
		List<Object> auditedAds1 = getAuditReader().createQuery()
				.forRevisionsOfEntity(Address.class, false, true)
				.add(AuditEntity.id().eq(adsId1))
				.getResultList();

		@SuppressWarnings("unchecked")
		List<Object> auditedAds2 = getAuditReader().createQuery()
				.forRevisionsOfEntity(Address.class, false, true)
				.add(AuditEntity.id().eq(adsId2))
				.getResultList();

		@SuppressWarnings("unchecked")
		List<Object> auditedPsnAds1 = getAuditReader().createQuery()
				.forRevisionsOfEntity(PersonAddressHome.class, false, true)
				.add(AuditEntity.id().eq(psnAdsId1))
				.getResultList();

		@SuppressWarnings("unchecked")
		List<Object> auditedPsnAds2 = getAuditReader().createQuery()
				.forRevisionsOfEntity(PersonAddressHome.class, false, true)
				.add(AuditEntity.id().eq(psnAdsId2))
				.getResultList();

		assert auditedPsn.size() == 1;
		assert auditedAds1.size() == 2;
		assert auditedAds2.size() == 2;
		assert auditedPsnAds1.size() == 2;
		assert auditedPsnAds2.size() == 2;

	}

	private static Date dateFor(int year, int month, int dayOfMonth) {
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.setLenient(false);
		cal.set(year, month - 1, dayOfMonth);
		return cal.getTime();
	}

	// @Test
	// public void initData() throws Exception {
	//
	// newEntityManager();
	// EntityManager em = getEntityManager();
	// em.getTransaction().begin();
	// em.joinTransaction();
	// MultipleCollectionEntity mce = new MultipleCollectionEntity();
	// mce.setText("MultipleCollectionEntity-1");
	// em.persist(mce);
	// mceId1 = mce.getId();
	//
	// em.getTransaction().commit();
	// assert mceId1 != null;
	//
	// //
	//
	// newEntityManager();
	// em = getEntityManager();
	// em.getTransaction().begin();
	// em.joinTransaction();
	//
	// // mce = em.find(MultipleCollectionEntity.class, mceId1);
	// // mce = em.merge(mce);
	//
	// MultipleCollectionRefEntity1 re1_1 = new MultipleCollectionRefEntity1();
	// re1_1.setText("MultipleCollectionRefEntity1-1");
	// re1_1.setMultipleCollectionEntity(mce);
	//
	// MultipleCollectionRefEntity1 re1_2 = new MultipleCollectionRefEntity1();
	// re1_2.setText("MultipleCollectionRefEntity1-2");
	// re1_2.setMultipleCollectionEntity(mce);
	//
	// mce.addRefEntity1(re1_1);
	// mce.addRefEntity1(re1_2);
	//
	// MultipleCollectionRefEntity2 re2_1 = new MultipleCollectionRefEntity2();
	// re2_1.setText("MultipleCollectionRefEntity2-1");
	// re2_1.setMultipleCollectionEntity(mce);
	//
	// MultipleCollectionRefEntity2 re2_2 = new MultipleCollectionRefEntity2();
	// re2_2.setText("MultipleCollectionRefEntity2-2");
	// re2_2.setMultipleCollectionEntity(mce);
	//
	// mce.addRefEntity2(re2_1);
	// mce.addRefEntity2(re2_2);
	//
	// mce = em.merge(mce);
	//
	// em.getTransaction().commit();
	//
	// // re1Id1 = re1_1.getId();
	// // re1Id2 = re1_2.getId();
	// // re2Id1 = re2_1.getId();
	// // re2Id2 = re2_2.getId();
	//
	// for (MultipleCollectionRefEntity1 refEnt1 : mce.getRefEntities1()) {
	// if (refEnt1.equals(re1_1)) {
	// re1Id1 = refEnt1.getId();
	// } else if (refEnt1.equals(re1_2)) {
	// re1Id2 = refEnt1.getId();
	// } else {
	// throw new IllegalStateException("unexpected instance");
	// }
	// }
	//
	// for (MultipleCollectionRefEntity2 refEnt2 : mce.getRefEntities2()) {
	// if (refEnt2.equals(re2_1)) {
	// re2Id1 = refEnt2.getId();
	// } else if (refEnt2.equals(re2_2)) {
	// re2Id2 = refEnt2.getId();
	// } else {
	// throw new IllegalStateException("unexpected instance");
	// }
	// }
	//
	// assert re1Id1 != null;
	// assert re1Id2 != null;
	// assert re2Id1 != null;
	// assert re2Id2 != null;
	//
	// //
	//
	// newEntityManager();
	// em = getEntityManager();
	// em.getTransaction().begin();
	// em.joinTransaction();
	//
	// // mce = em.find(MultipleCollectionEntity.class, mceId1);
	// // mce = em.merge(mce);
	//
	// assert mce.getRefEntities1().size() == 2;
	//
	// mce.removeRefEntity1(re1_2);
	// assert mce.getRefEntities1().size() == 1;
	//
	// MultipleCollectionRefEntity1 updatedRe1_1 = mce.getRefEntities1()
	// .get(0);
	// assert re1_1.equals(updatedRe1_1);
	// updatedRe1_1.setText("MultipleCollectionRefEntity1-1-updated");
	//
	// MultipleCollectionRefEntity1 re1_3 = new MultipleCollectionRefEntity1();
	// re1_3.setText("MultipleCollectionRefEntity1-3");
	// re1_3.setMultipleCollectionEntity(mce);
	// mce.addRefEntity1(re1_3);
	// assert mce.getRefEntities1().size() == 2;
	//
	// // -------------
	//
	// assert mce.getRefEntities2().size() == 2;
	//
	// mce.removeRefEntity2(re2_2);
	// assert mce.getRefEntities2().size() == 1;
	//
	// MultipleCollectionRefEntity2 updatedRe2_1 = mce.getRefEntities2()
	// .get(0);
	// assert re2_1.equals(updatedRe2_1);
	// updatedRe2_1.setText("MultipleCollectionRefEntity2-1-updated");
	//
	// MultipleCollectionRefEntity2 re2_3 = new MultipleCollectionRefEntity2();
	// re2_3.setText("MultipleCollectionRefEntity2-3");
	// re2_3.setMultipleCollectionEntity(mce);
	// mce.addRefEntity2(re2_3);
	// assert mce.getRefEntities2().size() == 2;
	//
	// mce = em.merge(mce);
	//
	// em.getTransaction().commit();
	//
	// // re1Id3 = re1_3.getId();
	// // re2Id3 = re2_3.getId();
	//
	// for (MultipleCollectionRefEntity1 adres : mce.getRefEntities1()) {
	// if (adres.equals(re1_3)) {
	// re1Id3 = adres.getId();
	// }
	// }
	//
	// for (MultipleCollectionRefEntity2 partner : mce.getRefEntities2()) {
	// if (partner.equals(re2_3)) {
	// re2Id3 = partner.getId();
	// }
	// }
	//
	// assert re1Id3 != null;
	// assert re2Id3 != null;
	// }
	//
	// @Test(dependsOnMethods = "initData")
	// public void testRevisionsCounts() throws Exception {
	//
	// List<Number> mceId1Revs = getAuditReader().getRevisions(
	// MultipleCollectionEntity.class, mceId1);
	// List<Number> re1Id1Revs = getAuditReader().getRevisions(
	// MultipleCollectionRefEntity1.class, re1Id1);
	// List<Number> re1Id2Revs = getAuditReader().getRevisions(
	// MultipleCollectionRefEntity1.class, re1Id2);
	// List<Number> re1Id3Revs = getAuditReader().getRevisions(
	// MultipleCollectionRefEntity1.class, re1Id3);
	// List<Number> re2Id1Revs = getAuditReader().getRevisions(
	// MultipleCollectionRefEntity2.class, re2Id1);
	// List<Number> re2Id2Revs = getAuditReader().getRevisions(
	// MultipleCollectionRefEntity2.class, re2Id2);
	// List<Number> re2Id3Revs = getAuditReader().getRevisions(
	// MultipleCollectionRefEntity2.class, re2Id3);
	//
	// assert Arrays.asList(1, 2, 3).equals(mceId1Revs);
	// assert Arrays.asList(2, 3).equals(re1Id1Revs);
	// assert Arrays.asList(2, 3).equals(re1Id2Revs);
	// assert Arrays.asList(3).equals(re1Id3Revs);
	// assert Arrays.asList(2, 3).equals(re2Id1Revs);
	// assert Arrays.asList(2, 3).equals(re2Id2Revs);
	// assert Arrays.asList(3).equals(re2Id3Revs);
	//
	// }
	//
	// @Test(dependsOnMethods = "initData")
	// public void testAuditJoinTable() throws Exception {
	// List<AuditJoinTableInfo> mceRe1AuditJoinTableInfos =
	// getAuditJoinTableRows(
	// "MCE_RE1_AUD", "MCE_ID",
	// "aud.originalId.MultipleCollectionEntity_id", "RE1_ID",
	// "aud.originalId.refEntities1_id", "aud.originalId.REV",
	// "aud.originalId.REV.id", "aud.REVTYPE");
	//
	// List<AuditJoinTableInfo> mceRe2AuditJoinTableInfos =
	// getAuditJoinTableRows(
	// "MCE_RE2_AUD", "MCE_ID",
	// "aud.originalId.MultipleCollectionEntity_id", "RE2_ID",
	// "aud.originalId.refEntities2_id", "aud.originalId.REV",
	// "aud.originalId.REV.id", "aud.REVTYPE");
	//
	// assert mceRe1AuditJoinTableInfos.size() == 4;
	// assert mceRe2AuditJoinTableInfos.size() == 4;
	//
	// DefaultRevisionEntity rev2 = new DefaultRevisionEntity();
	// rev2.setId(2);
	// DefaultRevisionEntity rev3 = new DefaultRevisionEntity();
	// rev3.setId(3);
	//
	// assert mceRe1AuditJoinTableInfos.get(0).equals(
	// new AuditJoinTableInfo("MCE_RE1_AUD", rev2, RevisionType.ADD,
	// "MCE_ID", 1L, "RE1_ID", 1L));
	// assert mceRe1AuditJoinTableInfos.get(1).equals(
	// new AuditJoinTableInfo("MCE_RE1_AUD", rev2, RevisionType.ADD,
	// "MCE_ID", 1L, "RE1_ID", 2L));
	// assert mceRe1AuditJoinTableInfos.get(2).equals(
	// new AuditJoinTableInfo("MCE_RE1_AUD", rev3, RevisionType.DEL,
	// "MCE_ID", 1L, "RE1_ID", 2L));
	// assert mceRe1AuditJoinTableInfos.get(3).equals(
	// new AuditJoinTableInfo("MCE_RE1_AUD", rev3, RevisionType.ADD,
	// "MCE_ID", 1L, "RE1_ID", 3L));
	//
	// assert mceRe2AuditJoinTableInfos.get(0).equals(
	// new AuditJoinTableInfo("MCE_RE2_AUD", rev2, RevisionType.ADD,
	// "MCE_ID", 1L, "RE2_ID", 1L));
	// assert mceRe2AuditJoinTableInfos.get(1).equals(
	// new AuditJoinTableInfo("MCE_RE2_AUD", rev2, RevisionType.ADD,
	// "MCE_ID", 1L, "RE2_ID", 2L));
	// assert mceRe2AuditJoinTableInfos.get(2).equals(
	// new AuditJoinTableInfo("MCE_RE2_AUD", rev3, RevisionType.DEL,
	// "MCE_ID", 1L, "RE2_ID", 2L));
	// assert mceRe2AuditJoinTableInfos.get(3).equals(
	// new AuditJoinTableInfo("MCE_RE2_AUD", rev3, RevisionType.ADD,
	// "MCE_ID", 1L, "RE2_ID", 3L));
	//
	// }
	//
	// private List<AuditJoinTableInfo> getAuditJoinTableRows(
	// String middleEntityName, String joinColumnIdName,
	// String joinColumnIdProp, String inverseJoinColumnIdName,
	// String inverseJoinColumnIdProp, String revProp, String revIdProp,
	// String revTypeProp) throws Exception {
	//
	// StringBuilder qryBuilder = new StringBuilder("select ");
	// qryBuilder.append("aud ");
	// qryBuilder.append(", ").append(joinColumnIdProp)
	// .append(" as joinColumnId");
	// qryBuilder.append(", ").append(inverseJoinColumnIdProp)
	// .append(" as inverseJoinColumnId");
	// qryBuilder.append(", ").append(revProp).append(" as rev");
	// qryBuilder.append(", ").append(revIdProp).append(" as revId");
	// qryBuilder.append(", ").append(revTypeProp).append(" as revType");
	// qryBuilder.append(" from ").append(middleEntityName).append(" aud ");
	// qryBuilder
	// .append(" order by joinColumnId asc, inverseJoinColumnId asc, revId asc");
	//
	// String query = qryBuilder.toString();
	//
	// newEntityManager();
	// EntityManager em = getEntityManager();
	// em.getTransaction().begin();
	// Query qry = em.createQuery(query);
	//
	// @SuppressWarnings("unchecked")
	// List<Object[]> auditJoinTableRows = qry.getResultList();
	// List<AuditJoinTableInfo> result = new ArrayList<AuditJoinTableInfo>(
	// auditJoinTableRows.size());
	//
	// for (Object[] auditJoinTableRow : auditJoinTableRows) {
	// Long joinColumnId = (Long) auditJoinTableRow[1];
	// Long inverseJoinColumnId = (Long) auditJoinTableRow[2];
	// DefaultRevisionEntity rev = (DefaultRevisionEntity) auditJoinTableRow[3];
	// RevisionType revType = (RevisionType) auditJoinTableRow[5];
	//
	// AuditJoinTableInfo info = new AuditJoinTableInfo(middleEntityName,
	// rev, revType, joinColumnIdName, joinColumnId,
	// inverseJoinColumnIdName, inverseJoinColumnId);
	// result.add(info);
	//
	// log.error("Found: " + info);
	//
	// }
	//
	// return result;
	// }
	//
	// private static class AuditJoinTableInfo {
	// private final String name;
	// private final Integer revId;
	// private final RevisionType revType;
	// private final String joinColumnName;
	// private final Long joinColumnId;
	// private final String inverseJoinColumnName;
	// private final Long inverseJoinColumnId;
	//
	// private AuditJoinTableInfo(String name, DefaultRevisionEntity rev,
	// RevisionType revType, String joinColumnName, Long joinColumnId,
	// String inverseJoinColumnName, Long inverseJoinColumnId) {
	// super();
	// this.name = name;
	// this.revId = rev.getId();
	// this.revType = revType;
	// this.joinColumnName = joinColumnName;
	// this.joinColumnId = joinColumnId;
	// this.inverseJoinColumnName = inverseJoinColumnName;
	// this.inverseJoinColumnId = inverseJoinColumnId;
	// }
	//
	// @Override
	// public String toString() {
	// return "AuditJoinTableInfo [name=" + name + ", revId=" + revId
	// + ", revType=" + revType + ", " + joinColumnName + "="
	// + joinColumnId + ", " + inverseJoinColumnName + "="
	// + inverseJoinColumnId + "]";
	// }
	//
	// @Override
	// public int hashCode() {
	// final int prime = 31;
	// int result = 1;
	// result = prime
	// * result
	// + ((inverseJoinColumnId == null) ? 0 : inverseJoinColumnId
	// .hashCode());
	// result = prime * result
	// + ((joinColumnId == null) ? 0 : joinColumnId.hashCode());
	// result = prime * result + ((name == null) ? 0 : name.hashCode());
	// result = prime * result + ((revId == null) ? 0 : revId.hashCode());
	// result = prime * result
	// + ((revType == null) ? 0 : revType.hashCode());
	// return result;
	// }
	//
	// @Override
	// public boolean equals(Object obj) {
	// if (this == obj)
	// return true;
	// if (obj == null)
	// return false;
	// if (getClass() != obj.getClass())
	// return false;
	// AuditJoinTableInfo other = (AuditJoinTableInfo) obj;
	// if (inverseJoinColumnId == null) {
	// if (other.inverseJoinColumnId != null)
	// return false;
	// } else if (!inverseJoinColumnId.equals(other.inverseJoinColumnId))
	// return false;
	// if (joinColumnId == null) {
	// if (other.joinColumnId != null)
	// return false;
	// } else if (!joinColumnId.equals(other.joinColumnId))
	// return false;
	// if (name == null) {
	// if (other.name != null)
	// return false;
	// } else if (!name.equals(other.name))
	// return false;
	// if (revId == null) {
	// if (other.revId != null)
	// return false;
	// } else if (!revId.equals(other.revId))
	// return false;
	// if (revType != other.revType)
	// return false;
	// return true;
	// }
	//
	// }

}