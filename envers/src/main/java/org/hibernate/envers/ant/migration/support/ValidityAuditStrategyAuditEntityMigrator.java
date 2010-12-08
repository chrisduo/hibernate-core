/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.envers.ant.migration.support;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.configuration.AuditEntitiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migrate an AuditEntity that is <strong>not</strong> a Middle Entity.
 * 
 * @author Erik-Berndt Scheper
 * 
 */
public class ValidityAuditStrategyAuditEntityMigrator implements
        AuditEntityMigrator {

    private final Logger logger = LoggerFactory
            .getLogger(ValidityAuditStrategyAuditEntityMigrator.class);

    private final SessionFactory sessionFactory;
    private final AuditConfiguration auditConfiguration;
    private final AuditEntitiesConfiguration auditEntCfg;
    private final AuditEntity auditEntity;
    private final TaskLogger taskLogger;
    private final boolean isRevisionEndTimestampEnabled;
    private final boolean nullifyExistingRevendColumns;
    private final long batchSize;
    private final long maxRowsConverted;

    private final Set<String> keysetAuditRow = new HashSet<String>();
    private final Set<String> keysetOriginalId = new HashSet<String>();
    private long rowCount;

    /**
     * Initializing constructor
     * 
     * @param sessionFactory
     * @param auditConfiguration
     * @param auditEntity
     * @param taskLogger
     * @param nullifyExistingRevendColumns
     * @param batchSize
     * @param maxRowsConverted
     */
    public ValidityAuditStrategyAuditEntityMigrator(
            SessionFactory sessionFactory,
            AuditConfiguration auditConfiguration, AuditEntity auditEntity,
            TaskLogger taskLogger, boolean nullifyExistingRevendColumns,
            long batchSize, long maxRowsConverted) {
        this.sessionFactory = sessionFactory;
        this.auditConfiguration = auditConfiguration;
        this.auditEntity = auditEntity;
        this.taskLogger = taskLogger;
        this.nullifyExistingRevendColumns = nullifyExistingRevendColumns;
        this.batchSize = batchSize;
        this.maxRowsConverted = maxRowsConverted;

        this.auditEntCfg = this.auditConfiguration.getAuditEntCfg();
        this.isRevisionEndTimestampEnabled = auditEntCfg
                .isRevisionEndTimestampEnabled();

        initKeySets(sessionFactory, auditEntity);

        if (!this.keysetAuditRow.isEmpty()) {
            // this audit entity is not empty; verify join columns

            if (!keysetOriginalId.contains(auditEntity.getIdColumn())) {
                throw new BuildException("Invalid idColumn ["
                        + auditEntity.getIdColumn() + "] for AuditEntity "
                        + auditEntity.getName() + ";  should be in "
                        + keysetOriginalId);
            }
        }
    }

    public void migrate() {
        String logMsg;

        logMsg = "Migrating " + auditEntity;
        logger.info(logMsg);
        taskLogger.log(logMsg, 0);

        if (this.nullifyExistingRevendColumns) {
            // set all revend columns to null
            initializeRevEndColumns();
        }

        logger.info("Found  " + this.rowCount + " rows to update");
        taskLogger.log("Found  " + this.rowCount + " rows to update", 0);

        // return immediately if there are no middleEntity rows to update
        if (this.rowCount == 0) {
            logMsg = "No rows to update for " + auditEntity;
            logger.info(logMsg);
            taskLogger.log(logMsg, 0);
            return;
        }

        String query = createAuditEntityQuery(false);

        Session session = sessionFactory.openSession();

        ScrollableResults auditRows = session.createQuery(query)
                .setCacheMode(CacheMode.IGNORE).scroll(ScrollMode.FORWARD_ONLY);

        Transaction tx = session.beginTransaction();

        long count = 0;
        long lastFlushCount = 0;

        Object idColumnId = null;
        Object rev = null;
        Object revTstamp = null;

        Object lastIdColumnId = null;
        Object endRev = null;
        Object endRevTstamp = null;

        while (auditRows.next() && count < maxRowsConverted) {
            idColumnId = auditRows.get(1);

            if (idColumnId.equals(lastIdColumnId)) {
                // the same idColumn, update endRev properties
                endRev = rev;
                endRevTstamp = revTstamp;

            } else {
                // a new idColumn, set the endRevision to null
                endRev = null;
                endRevTstamp = null;
            }

            Object qryAuditObject = auditRows.get(0);
            rev = auditRows.get(2);
            Object revId = auditRows.get(3);

            if (isRevisionEndTimestampEnabled) {
                revTstamp = auditRows.get(4);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> auditRow = (Map<String, Object>) qryAuditObject;

            if (logger.isDebugEnabled()) {
                logger.debug(auditEntity.getIdColumn() + " = " + idColumnId);
                logger.debug("rev = " + rev);
                logger.debug("revId = " + revId);
                if (isRevisionEndTimestampEnabled) {
                    logger.debug("revTstamp = " + revTstamp);
                }
                logger.debug("auditRow = " + auditRow.toString());
            }

            if (logger.isDebugEnabled()) {
                StringBuilder msgBuilder = new StringBuilder();
                msgBuilder.append("updating row = { ");
                msgBuilder.append(auditEntity.getIdColumn()).append(" = ")
                        .append(idColumnId);
                msgBuilder.append(", revId = ").append(rev);
                if (isRevisionEndTimestampEnabled) {
                    msgBuilder.append(" ; revTstamp = ").append(revTstamp);
                }
                // msgBuilder.append(" ; auditRow = ").append(auditRow);
                msgBuilder.append("}");

                msgBuilder.append("--> Setting revEnd = ").append(endRev);
                if (isRevisionEndTimestampEnabled) {
                    msgBuilder.append(" & revEndTstamp = ")
                            .append(endRevTstamp);
                }
                logger.debug(msgBuilder.toString());
            }

            if (logger.isTraceEnabled()) {
                // Setting the end revision to be the current rev
                for (Map.Entry<String, Object> entry : auditRow.entrySet()) {
                    if (entry.getValue() != null) {
                        logger.trace(entry.getKey() + " - "
                                + entry.getValue().getClass());
                    } else {
                        logger.trace(entry.getKey() + " - null");
                    }

                }
                logger.trace("----");
            }

            // Setting the end revision to be the current rev
            auditRow.put(auditEntCfg.getRevisionEndFieldName(), endRev);

            if (isRevisionEndTimestampEnabled) {
                if (endRevTstamp != null) {
                    if (endRevTstamp instanceof Date) {
                        endRevTstamp = (Date) endRevTstamp;
                    } else {
                        endRevTstamp = new Date((Long) endRevTstamp);
                    }
                }

                // Setting the end revision timestamp
                auditRow.put(auditEntCfg.getRevisionEndTimestampFieldName(),
                        endRevTstamp);
            }

            if (++count % batchSize == 0) {
                String msg = "Committing rows " + lastFlushCount + " - "
                        + count + " of " + this.rowCount + " for "
                        + auditEntity.getName();

                logger.info(msg);
                if (taskLogger.isDebugEnabled()) {
                    taskLogger.log(msg, 0);
                }

                // flush a batch of updates and release memory:
                session.flush();
                session.clear();
                lastFlushCount = count;
                tx.commit();

                tx = session.beginTransaction();
            }

            lastIdColumnId = idColumnId;
        }

        if (count % batchSize != 0) {
            String msg = "Committing rows " + lastFlushCount + " - " + count
                    + " of " + this.rowCount + " for " + auditEntity.getName();

            logger.info(msg);
            if (taskLogger.isDebugEnabled()) {
                taskLogger.log(msg, 0);
            }

            // flush a batch of updates and release memory:
            session.flush();
            session.clear();
        }

        tx.commit();
        session.close();

        if (logger.isDebugEnabled()) {
            logger.debug("===============================================================\r\n");
        }
    }

    private void initializeRevEndColumns() {
        String logMsg;

        logMsg = "Set revend columns to null for " + auditEntity;
        logger.info(logMsg);
        taskLogger.log(logMsg, 0);

        String revEndProp = "aud." + auditEntCfg.getRevisionEndFieldName();

        String revEndTstampProp = null;
        if (isRevisionEndTimestampEnabled) {
            revEndTstampProp = "aud."
                    + auditEntCfg.getRevisionEndTimestampFieldName();
        }

        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();

        StringBuilder updBuilder = new StringBuilder();

        // update auditEntity aud set aud.REVEND = null , aud.REVEND_TSTMP =
        // null
        updBuilder.append("update ").append(auditEntity.getName())
                .append(" aud ");
        updBuilder.append(" set ").append(revEndProp).append(" = null ");
        updBuilder.append(" , ").append(revEndTstampProp).append(" = null ");
        // updBuilder.append(" where aud.originalId.REV.id < 0");

        String updateQry = updBuilder.toString();

        logger.info("Executing query: " + updateQry);
        int updatedEntities = session.createQuery(updateQry).executeUpdate();

        tx.commit();
        session.close();

        logger.info("Updated entities = " + updatedEntities);
    }

    private String createAuditEntityQuery(boolean countOnly) {
        String idColumnIdProp_aud = "aud."
                + auditEntCfg.getOriginalIdPropName() + "."
                + auditEntity.getIdColumn();
        String idColumnIdProp_aud2 = "aud2."
                + auditEntCfg.getOriginalIdPropName() + "."
                + auditEntity.getIdColumn();

        String revProp = "aud." + auditEntCfg.getOriginalIdPropName() + "."
                + auditEntCfg.getRevisionFieldName();

        String revIdProp_aud = "aud." + auditEntCfg.getRevisionNumberPath();
        String revIdProp_aud2 = "aud2." + auditEntCfg.getRevisionNumberPath();

        String revTstampProp = null;
        if (isRevisionEndTimestampEnabled) {
            revTstampProp = "aud."
                    + auditEntCfg.getRevisionPropPath("timestamp");
        }

        StringBuilder qryBuilder = new StringBuilder();

        if (countOnly) {
            qryBuilder.append("select count(").append(idColumnIdProp_aud)
                    .append(")");

        } else {

            // select aud, aud.originalId.id as idColumnId, aud.originalId.REV
            // as rev
            // , aud.originalId.REV.id as revId, aud.originalId.REV.timestamp as
            // revTstamp
            qryBuilder.append("select aud ");
            qryBuilder.append(", ").append(idColumnIdProp_aud)
                    .append(" as idColumnId");
            qryBuilder.append(", ").append(revProp).append(" as rev");
            qryBuilder.append(", ").append(revIdProp_aud).append(" as revId");
            if (isRevisionEndTimestampEnabled) {
                qryBuilder.append(", ").append(revTstampProp)
                        .append(" as revTstamp");
            }

        }

        // from auditEntity aud
        qryBuilder.append(" from ").append(auditEntity.getName())
                .append(" aud ");

        // where exists
        // ( from auditEntity as aud2 where aud2.originalId.id =
        // aud.originalId.id
        // and aud2.originalId.REV.id != aud.originalId.REV.id)
        qryBuilder.append(" where exists ( from ")
                .append(auditEntity.getName());
        qryBuilder.append(" as aud2 where ");
        qryBuilder.append(idColumnIdProp_aud2).append(" = ")
                .append(idColumnIdProp_aud);
        qryBuilder.append(" and ");
        qryBuilder.append(revIdProp_aud2).append(" != ").append(revIdProp_aud);
        qryBuilder.append(")");

        if (!countOnly) {
            // order by idColumnId asc, revId desc
            qryBuilder.append(" order by idColumnId asc, revId desc");
        }

        String query = qryBuilder.toString();

        logger.info("Executing query: " + query);

        return query;
    }

    private void initKeySets(SessionFactory sessionFactory,
            AuditEntity auditEntity) {
        String logMsg;

        logMsg = "Initializing ValidityAuditStrategy migration for : "
                + auditEntity;
        logger.info(logMsg);
        taskLogger.log(logMsg);

        String originalIdProp = "aud." + auditEntCfg.getOriginalIdPropName();

        StringBuilder qryBuilder = new StringBuilder("select ");
        qryBuilder.append("aud ");
        qryBuilder.append(", ").append(originalIdProp).append(" as originalId");
        qryBuilder.append(" from ").append(auditEntity.getName())
                .append(" aud ");
        String query = qryBuilder.toString();

        logger.info("Executing query: " + query);

        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        ScrollableResults auditRows = session.createQuery(query)
                .setCacheMode(CacheMode.IGNORE).scroll(ScrollMode.FORWARD_ONLY);

        if (auditRows.next()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> auditRow = (Map<String, Object>) auditRows
                    .get(0);
            this.keysetAuditRow.addAll(auditRow.keySet());

            @SuppressWarnings("unchecked")
            Map<String, Object> originalId = (Map<String, Object>) auditRows
                    .get(1);
            this.keysetOriginalId.addAll(originalId.keySet());

            if (logger.isDebugEnabled()) {
                logger.debug("keyset AuditRow = " + keysetAuditRow);
                logger.debug("keyset OriginalId = " + keysetOriginalId);
            }
        }

        // get row count:
        String auditEntityQuery = createAuditEntityQuery(true);

        Object rowCountObj = session.createQuery(auditEntityQuery)
                .uniqueResult();
        this.rowCount = ((Long) rowCountObj).longValue();

        tx.rollback();
        session.close();

        this.keysetOriginalId.remove(this.auditEntCfg.getRevisionFieldName());
    }

    /**
     * An Audit Entity Element
     * 
     * @author Erik-Berndt Scheper
     * 
     */
    public static class AuditEntity {

        private String name;
        private String idColumn;

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @param name
         *            the name
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * @return the idColumn
         */
        public String getIdColumn() {
            return idColumn;
        }

        /**
         * @param idColumn
         *            the idColumn
         */
        public void setIdColumn(String idColumn) {
            this.idColumn = idColumn;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("AuditEntity: {");
            builder.append("name = \"").append(name).append("\"");
            builder.append(", idColumn = \"").append(idColumn).append("\"");
            builder.append("}");
            return builder.toString();
        }

    }

}
