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
 * Migrate an middle AuditEntity.
 * 
 * @author Erik-Berndt Scheper
 * 
 */
public class ValidityAuditStrategyMiddleEntityMigrator implements
        AuditEntityMigrator {

    private final Logger logger = LoggerFactory
            .getLogger(ValidityAuditStrategyMiddleEntityMigrator.class);

    private final SessionFactory sessionFactory;
    private final AuditConfiguration auditConfiguration;
    private final AuditEntitiesConfiguration auditEntCfg;
    private final MiddleAuditEntity middleEntity;
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
     * @param middleEntity
     * @param taskLogger
     * @param nullifyExistingRevendColumns
     * @param batchSize
     * @param maxRowsConverted
     */
    public ValidityAuditStrategyMiddleEntityMigrator(
            SessionFactory sessionFactory,
            AuditConfiguration auditConfiguration,
            MiddleAuditEntity middleEntity, TaskLogger taskLogger,
            boolean nullifyExistingRevendColumns, long batchSize,
            long maxRowsConverted) {
        this.sessionFactory = sessionFactory;
        this.auditConfiguration = auditConfiguration;
        this.middleEntity = middleEntity;
        this.taskLogger = taskLogger;
        this.nullifyExistingRevendColumns = nullifyExistingRevendColumns;
        this.batchSize = batchSize;
        this.maxRowsConverted = maxRowsConverted;

        this.auditEntCfg = this.auditConfiguration.getAuditEntCfg();
        this.isRevisionEndTimestampEnabled = auditEntCfg
                .isRevisionEndTimestampEnabled();

        initKeySets(sessionFactory, middleEntity);

        if (!this.keysetAuditRow.isEmpty()) {
            // this audit entity is not empty; verify join columns

            if (!keysetOriginalId.contains(middleEntity.getJoinColumn())) {
                throw new BuildException("Invalid joinColumn ["
                        + middleEntity.getJoinColumn() + "] for MiddleEntity "
                        + middleEntity.getName() + ";  should be in "
                        + keysetOriginalId);
            }

            if (!keysetOriginalId.contains(middleEntity.getInverseJoinColumn())) {
                throw new BuildException("Invalid inverseJoinColumn ["
                        + middleEntity.getInverseJoinColumn()
                        + "] for MiddleEntity " + middleEntity.getName()
                        + ";  should be in " + keysetOriginalId);
            }
        }
    }

    public void migrate() {
        String logMsg;

        logMsg = "Migrating " + middleEntity;
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
            logMsg = "No rows to update for " + middleEntity;
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

        Object joinColumnId = null;
        Object inverseJoinColumnId = null;
        Object rev = null;
        Object revTstamp = null;

        Object lastJoinColumnId = null;
        Object lastInverseJoinColumnId = null;
        Object endRev = null;
        Object endRevTstamp = null;

        while (auditRows.next() && count < maxRowsConverted) {
            joinColumnId = auditRows.get(1);
            inverseJoinColumnId = auditRows.get(2);

            if ((joinColumnId.equals(lastJoinColumnId))
                    && (inverseJoinColumnId.equals(lastInverseJoinColumnId))) {
                // the same joinColumn + inverseJoinColumn, update endRev
                // properties
                endRev = rev;
                endRevTstamp = revTstamp;

            } else {
                // a new joinColumn and/or inverseJoinColumn, set the
                // endRevision to null
                endRev = null;
                endRevTstamp = null;
            }

            Object qryAuditObject = auditRows.get(0);
            rev = auditRows.get(3);
            Object revId = auditRows.get(4);

            if (isRevisionEndTimestampEnabled) {
                revTstamp = auditRows.get(5);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> auditRow = (Map<String, Object>) qryAuditObject;

            if (logger.isDebugEnabled()) {
                logger.debug(middleEntity.getJoinColumn() + " = "
                        + joinColumnId);
                logger.debug(middleEntity.getInverseJoinColumn() + " = "
                        + inverseJoinColumnId);
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
                msgBuilder.append(middleEntity.getJoinColumn()).append(" = ")
                        .append(joinColumnId);
                msgBuilder.append(", ")
                        .append(middleEntity.getInverseJoinColumn())
                        .append(" = ").append(inverseJoinColumnId);
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
                        + middleEntity.getName();

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

            lastJoinColumnId = joinColumnId;
            lastInverseJoinColumnId = inverseJoinColumnId;
        }

        if (count % batchSize != 0) {
            String msg = "Committing rows " + lastFlushCount + " - " + count
                    + " of " + this.rowCount + " for " + middleEntity.getName();

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

        logMsg = "Set revend columns to null for " + middleEntity;
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

        // update middleEntity aud set aud.REVEND = null , aud.REVEND_TSTMP =
        // null
        updBuilder.append("update ").append(middleEntity.getName())
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
        String joinColumnIdProp_aud = "aud."
                + auditEntCfg.getOriginalIdPropName() + "."
                + middleEntity.getJoinColumn();
        String joinColumnIdProp_aud2 = "aud2."
                + auditEntCfg.getOriginalIdPropName() + "."
                + middleEntity.getJoinColumn();
        String inverseJoinColumnIdProp_aud = "aud."
                + auditEntCfg.getOriginalIdPropName() + "."
                + middleEntity.getInverseJoinColumn();
        String inverseJoinColumnIdProp_aud2 = "aud2."
                + auditEntCfg.getOriginalIdPropName() + "."
                + middleEntity.getInverseJoinColumn();

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
            qryBuilder.append("select count(").append(joinColumnIdProp_aud)
                    .append(")");

        } else {

            // select aud , aud.originalId.joinColumnId as joinColumnId
            // , aud.originalId.inverseJoinColumnId as inverseJoinColumnId
            // , aud.originalId.REV as rev, aud.originalId.REV.id as revId
            // , aud.originalId.REV.timestamp as revTstamp
            qryBuilder.append("select aud ");
            qryBuilder.append(", ").append(joinColumnIdProp_aud)
                    .append(" as joinColumnId");
            qryBuilder.append(", ").append(inverseJoinColumnIdProp_aud)
                    .append(" as inverseJoinColumnId");
            qryBuilder.append(", ").append(revProp).append(" as rev");
            qryBuilder.append(", ").append(revIdProp_aud).append(" as revId");
            if (isRevisionEndTimestampEnabled) {
                qryBuilder.append(", ").append(revTstampProp)
                        .append(" as revTstamp");
            }
        }

        // from middleEntity aud
        qryBuilder.append(" from ").append(middleEntity.getName())
                .append(" aud ");

        // where exists
        // ( from middleEntity as aud2
        // where aud2.originalId.joinColumnId = aud.originalId.joinColumnId
        // and aud2.originalId.inverseJoinColumnId =
        // aud.originalId.inverseJoinColumnId
        // and aud2.originalId.REV.id != aud.originalId.REV.id)
        qryBuilder.append(" where exists ( from ").append(
                middleEntity.getName());
        qryBuilder.append(" as aud2 where ");
        qryBuilder.append(joinColumnIdProp_aud2).append(" = ")
                .append(joinColumnIdProp_aud);
        qryBuilder.append(" and ");
        qryBuilder.append(inverseJoinColumnIdProp_aud2).append(" = ")
                .append(inverseJoinColumnIdProp_aud);
        qryBuilder.append(" and ");
        qryBuilder.append(revIdProp_aud2).append(" != ").append(revIdProp_aud);
        qryBuilder.append(")");

        if (!countOnly) {
            // order by joinColumnId asc, inverseJoinColumnId asc, revId desc
            qryBuilder
                    .append(" order by joinColumnId asc, inverseJoinColumnId asc, revId desc");
        }

        String query = qryBuilder.toString();

        logger.info("Executing query: " + query);

        return query;
    }

    private void initKeySets(SessionFactory sessionFactory,
            MiddleAuditEntity middleEntity) {
        String logMsg;

        logMsg = "Initializing ValidityAuditStrategy migration for : "
                + middleEntity;
        logger.info(logMsg);
        taskLogger.log(logMsg);

        String originalIdProp = "aud." + auditEntCfg.getOriginalIdPropName();

        StringBuilder qryBuilder = new StringBuilder("select ");
        qryBuilder.append("aud ");
        qryBuilder.append(", ").append(originalIdProp).append(" as originalId");
        qryBuilder.append(" from ").append(middleEntity.getName())
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
     * A Middle Audit Entity Element
     * 
     * @author Erik-Berndt Scheper
     * 
     */
    public static class MiddleAuditEntity {

        private String name;
        private String joinColumn;
        private String inverseJoinColumn;

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
         * @return the joinColumn
         */
        public String getJoinColumn() {
            return joinColumn;
        }

        /**
         * @param joinColumn
         *            the joinColumn
         */
        public void setJoinColumn(String joinColumn) {
            this.joinColumn = joinColumn;
        }

        /**
         * @return the inverseJoinColumn
         */
        public String getInverseJoinColumn() {
            return inverseJoinColumn;
        }

        /**
         * @param inverseJoinColumn
         *            the inverseJoinColumn
         */
        public void setInverseJoinColumn(String inverseJoinColumn) {
            this.inverseJoinColumn = inverseJoinColumn;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("MiddleAuditEntity: {");
            builder.append("name = \"").append(name).append("\"");
            builder.append(", joinColumn = \"").append(joinColumn).append("\"");
            builder.append(", inverseJoinColumn = \"")
                    .append(inverseJoinColumn).append("\"");
            builder.append("}");
            return builder.toString();
        }

    }

}
