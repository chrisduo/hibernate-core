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
package org.hibernate.envers.ant.migration;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.hibernate.MappingNotFoundException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.envers.ant.AnnotationConfigurationTaskWithEnvers;
import org.hibernate.envers.ant.ConfigurationTaskWithEnvers;
import org.hibernate.envers.ant.JPAConfigurationTaskWithEnvers;
import org.hibernate.envers.ant.migration.support.AuditEntityMigrator;
import org.hibernate.envers.ant.migration.support.TaskLogger;
import org.hibernate.envers.ant.migration.support.ValidityAuditStrategyAuditEntityMigrator;
import org.hibernate.envers.ant.migration.support.ValidityAuditStrategyMiddleEntityMigrator;
import org.hibernate.envers.ant.migration.support.ValidityAuditStrategyAuditEntityMigrator.AuditEntity;
import org.hibernate.envers.ant.migration.support.ValidityAuditStrategyMiddleEntityMigrator.MiddleAuditEntity;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.strategy.AuditStrategy;
import org.hibernate.envers.strategy.ValidityAuditStrategy;
import org.hibernate.hql.ast.util.SessionFactoryHelper;
import org.hibernate.impl.SessionFactoryImpl;
import org.hibernate.property.DirectPropertyAccessor;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.tool.ant.AnnotationConfigurationTask;
import org.hibernate.tool.ant.ConfigurationTask;
import org.hibernate.tool.ant.JPAConfigurationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ant task for the migration to the {@link ValidityAuditStrategy}.
 * 
 * @author Erik-Berndt Scheper
 * 
 */
public class ValidityAuditStrategyMigrationTask extends Task {

    private final Logger logger = LoggerFactory
            .getLogger(ValidityAuditStrategyMigrationTask.class);

    private ConfigurationTask configurationTask;
    private Path classPath;
    private boolean nullifyExistingRevendColumns = false;
    private boolean debugEnabled = true;
    private final List<AuditEntity> auditEntities = new ArrayList<AuditEntity>();
    private final List<MiddleAuditEntity> middleEntities = new ArrayList<MiddleAuditEntity>();

    private long batchSize = 100;
    private long maxRowsConverted = Long.MAX_VALUE;

    /**
     * @return a Hibernate tools {@link ConfigurationTask}
     */
    public ConfigurationTask createConfiguration() {
        checkConfiguration();
        ConfigurationTaskWithEnvers task = new ConfigurationTaskWithEnvers();
        configurationTask = task;
        return task;
    }

    /**
     * @return a Hibernate tools {@link AnnotationConfigurationTask}
     */
    public AnnotationConfigurationTask createAnnotationConfiguration() {
        checkConfiguration();
        AnnotationConfigurationTaskWithEnvers task = new AnnotationConfigurationTaskWithEnvers();
        configurationTask = task;
        return task;
    }

    /**
     * @return a Hibernate tools {@link JPAConfigurationTask}
     */
    public JPAConfigurationTask createJpaConfiguration() {
        checkConfiguration();
        JPAConfigurationTask task = new JPAConfigurationTaskWithEnvers();
        configurationTask = task;
        return task;
    }

    /**
     * Set the classPath using an Ant {@link Path} type.
     * 
     * @param classPath
     *            an Ant {@link Path} type
     */
    public void setClasspath(Path classPath) {
        this.classPath = classPath;
    }

    /**
     * Enable or disable nullification of existing revend columns.
     * 
     * @param nullifyExistingRevendColumns
     *            {@code true} if nullification of existing revend columns
     *            should be enabled
     */
    public void setNullifyExistingRevendColumns(
            boolean nullifyExistingRevendColumns) {
        this.nullifyExistingRevendColumns = nullifyExistingRevendColumns;
    }

    /**
     * Enable or disable debug logging.
     * 
     * @param debugEnabled
     *            {@code true} if debug logging should be enabled
     */
    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    /**
     * @param batchSize
     *            batch size after which a commit is issued
     */
    public void setBatchSize(long batchSize) {
        if (batchSize < 1) {
            throw new BuildException(
                    "Attribute \"batchSize\" must be greater than zero");
        }

        this.batchSize = batchSize;
    }

    /**
     * @param maxRowsConverted
     *            the maximum number of rows converted for an audit table
     */
    public void setMaxRowsConverted(long maxRowsConverted) {
        if (maxRowsConverted == -1) {
            maxRowsConverted = Long.MAX_VALUE;
        }

        if (maxRowsConverted < 1) {
            throw new BuildException(
                    "Attribute \"maxRowsConverted\" must either be greater than zero or -1 (all rows)");
        }

        this.maxRowsConverted = maxRowsConverted;
    }

    /**
     * Create a classpath element.
     * 
     * @return the classPath, as an Ant {@link Path} type
     */
    public Path createClasspath() {
        this.classPath = new Path(getProject());
        return this.classPath;
    }

    /**
     * Add a configured {@link AuditEntity} to be migrated.
     * 
     * @param auditEntity
     *            the {@link AuditEntity}
     */
    public void addConfiguredAuditEntity(AuditEntity auditEntity) {
        this.auditEntities.add(auditEntity);
    }

    /**
     * Add a configured {@link MiddleAuditEntity} to be migrated.
     * 
     * @param middleEntity
     *            the {@link MiddleAuditEntity}
     */
    public void addConfiguredMiddleEntity(MiddleAuditEntity middleEntity) {
        this.middleEntities.add(middleEntity);
    }

    @Override
    public void execute() {
        if (this.configurationTask == null) {
            throw new BuildException(
                    "No configuration specified. <"
                            + getTaskName()
                            + "> must have one of the following: <configuration>, <jpaconfiguration>, <annotationconfiguration> or <jdbcconfiguration>");
        }

        log("Executing ValidityAuditStrategyMigrationTask with a "
                + this.configurationTask.getDescription());

        if (logger.isInfoEnabled()) {
            logger.info("Executing ValidityAuditStrategyMigrationTask with a "
                    + this.configurationTask.getDescription());
        }

        AntClassLoader loader = getProject().createClassLoader(this.classPath);

        try {
            ClassLoader classLoader = getClass().getClassLoader();
            loader.setParent(classLoader);
            loader.setThreadContextLoader();

            AuditConfiguration auditConfiguration = AuditConfiguration
                    .getFor(this.getConfiguration());
            SessionFactory sessionFactory = this.getConfiguration()
                    .buildSessionFactory();

            AuditStrategy strategy = auditConfiguration.getAuditStrategy();
            if (!(strategy instanceof ValidityAuditStrategy)) {
                throw new BuildException(
                        "Unexpected AuditStrategy implementation: " + strategy);
            }

            if (this.auditEntities.isEmpty() && this.middleEntities.isEmpty()) {
                logAvailableEntities((SessionFactoryImpl) sessionFactory);
                return;
            }

            checkEntities((SessionFactoryImpl) sessionFactory);

            // build a list of all entity migration (inherently verifying that
            // they are ok)
            List<AuditEntityMigrator> entityMigrators = new ArrayList<AuditEntityMigrator>();

            for (AuditEntity auditEntity : this.auditEntities) {
                ValidityAuditStrategyAuditEntityMigrator migrator = new ValidityAuditStrategyAuditEntityMigrator(
                        sessionFactory, auditConfiguration, auditEntity,
                        new TaskLoggerImpl(this, this.debugEnabled),
                        this.nullifyExistingRevendColumns, this.batchSize,
                        this.maxRowsConverted);

                entityMigrators.add(migrator);
            }

            for (MiddleAuditEntity middleEntity : this.middleEntities) {
                ValidityAuditStrategyMiddleEntityMigrator migrator = new ValidityAuditStrategyMiddleEntityMigrator(
                        sessionFactory, auditConfiguration, middleEntity,
                        new TaskLoggerImpl(this, this.debugEnabled),
                        this.nullifyExistingRevendColumns, this.batchSize,
                        this.maxRowsConverted);

                entityMigrators.add(migrator);
            }

            // now migrate the entities
            for (AuditEntityMigrator auditEntityMigrator : entityMigrators) {
                auditEntityMigrator.migrate();
            }

        } catch (Throwable re) {
            reportException(re);

        } finally {
            if (loader != null) {
                loader.resetThreadContextLoader();
                loader.cleanup();
            }
        }
    }

    /**
     * @return de Hibernate {@link Configuration}
     */
    public Configuration getConfiguration() {
        return this.configurationTask.getConfiguration();
    }

    private void checkConfiguration() {
        if (this.configurationTask != null)
            throw new BuildException("Only a single configuration is allowed.");
    }

    private void checkEntities(SessionFactoryImpl sessionFactory) {
        String entityName = null;
        SessionFactoryHelper sfHelper = new SessionFactoryHelper(sessionFactory);

        try {
            for (AuditEntity auditEntity : this.auditEntities) {
                entityName = auditEntity.getName();
                sfHelper.requireClassPersister(entityName);
            }

            for (MiddleAuditEntity middleEntity : this.middleEntities) {
                entityName = middleEntity.getName();
                sfHelper.requireClassPersister(entityName);
            }

        } catch (Exception ex) {
            logger.error("Unknown entity " + entityName, ex);

            log("Unknown entity " + entityName, 0);
            logAvailableEntities(sessionFactory);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            PrintStream prs = new PrintStream(bos);
            ex.printStackTrace(prs);
            prs.flush();
            log("Linked exception:\r\n" + bos.toString(), 0);

            throw new BuildException("Unknown entity ", ex);
        }

    }

    private void logAvailableEntities(SessionFactoryImpl sessionFactory) {
        PropertyAccessor accessor = new DirectPropertyAccessor();
        Getter entityPersistersGetter = accessor.getGetter(
                SessionFactoryImpl.class, "entityPersisters");

        @SuppressWarnings("unchecked")
        Map<String, Object> entityPersisters = (Map<String, Object>) entityPersistersGetter
                .get(sessionFactory);

        SortedSet<String> entityPersisterNames = new TreeSet<String>();
        entityPersisterNames.addAll(entityPersisters.keySet());

        log("Available entities: ", 0);
        for (String entityPersisterName : entityPersisterNames) {
            log("\t" + entityPersisterName, 0);
        }
    }

    private void reportException(Throwable re) {
        log("An exception occurred while running "
                + this.getClass().getSimpleName(), 0);

        String newbieMessage = getProbableSolutionOrCause(re);
        if (newbieMessage != null) {
            log(newbieMessage);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream prs = new PrintStream(bos);
        re.printStackTrace(prs);
        prs.flush();
        log("Linked exception:\r\n" + bos.toString(), 0);

        if ((re instanceof BuildException)) {
            throw ((BuildException) re);
        }
        throw new BuildException(re, getLocation());
    }

    private String getProbableSolutionOrCause(Throwable re) {
        if (re == null)
            return null;

        if ((re instanceof MappingNotFoundException)) {
            MappingNotFoundException mnf = (MappingNotFoundException) re;
            if ("resource".equals(mnf.getType())) {
                return "A " + mnf.getType() + " located at " + mnf.getPath()
                        + " was not found.\n" + "Check the following:\n" + "\n"
                        + "1) Is the spelling/casing correct ?\n" + "2)\tIs "
                        + mnf.getPath() + " available via the classpath ?\n"
                        + "3) Does it actually exist ?\n";
            }

            return "A " + mnf.getType() + " located at " + mnf.getPath()
                    + " was not found.\n" + "Check the following:\n" + "\n"
                    + "1) Is the spelling/casing correct ?\n"
                    + "2)\tDo you permission to access " + mnf.getPath()
                    + " ?\n" + "3) Does it actually exist ?\n";
        }

        if (((re instanceof ClassNotFoundException))
                || ((re instanceof NoClassDefFoundError))) {
            return "A class were not found in the classpath of the Ant task.\nEnsure that the classpath contains the classes needed for Hibernate and your code are in the classpath.\n";
        }

        if ((re instanceof UnsupportedClassVersionError)) {
            return "You are most likely running the ant task with a JRE that is older than the JRE required to use the classes.\ne.g. running with JRE 1.3 or 1.4 when using JDK 1.5 annotations is not possible.\nEnsure that you are using a correct JRE.";
        }

        if (re.getCause() != re) {
            return getProbableSolutionOrCause(re.getCause());
        }

        return null;
    }

    /**
     * Delegate logging to the logger of the task.
     * 
     * @author Erik-Berndt Scheper
     * 
     */
    private static class TaskLoggerImpl implements TaskLogger {
        private final ValidityAuditStrategyMigrationTask delegate;
        private final boolean debugEnabled;

        private TaskLoggerImpl(ValidityAuditStrategyMigrationTask delegate,
                boolean debugEnabled) {
            this.delegate = delegate;
            this.debugEnabled = debugEnabled;
        }

        public void log(String string) {
            delegate.log(string);
        }

        public void log(String string, int level) {
            delegate.log(string, level);
        }

        public boolean isDebugEnabled() {
            return debugEnabled;
        }

    }

}