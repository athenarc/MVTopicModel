package org.madgik.utils;

public class Constants {

    /** Databases **/
    public static final String SQL_SERVER = "SQL_SERVER";
    public static final String ORACLE = "ORACLE";
    public static final String POSTGRESQL = "POSTGRESQL";

    public static final String PERSISTENCE_PACKAGE = "org.madgik.persistence";
    public static final String HIBERNATE_SQL_SERVER_DIALECT = "org.hibernate.dialect.SQLServer2008Dialect";
    public static final String HIBERNATE_ORACLE_DIALECT = "org.hibernate.dialect.Oracle10gDialect";
    public static final String HIBERNATE_POSTGRESQL_DIALECT  ="org.hibernate.dialect.PostgreSQLDialect";
    public static final String HIBERNATE_NAMING_STRATEGY = "hibernate.ejb.naming_strategy";
    public static final String HIBERNATE_ENTITY_MANAGER_FACTORY_NAME = "hibernate.ejb.entitymanager_factory_name";
    public static final String MADGIK_PACKAGE = "org.madgik";

    /** Services **/
    public static final String DATASOURCE = "dataSource";
    public static final String TRANSACTION_MANAGER = "transactionManager";
    public static final String ENTITY_MANAGER_FACTORY = "entityManagerFactory";
    public static final String ENTITY_MANAGER = "entityManagerProcedure";
    public static final String MODEL_MAPPER = "modelMapper";
    public static final String MAPPER_SERVICE = "mapperService";
    public static final String VISUALIZATION_DOCUMENT_SERVICE = "visualizationDocumentService";
    public static final String TOPIC_CURATION_SERVICE = "topicCurationService";
    public static final String TOPIC_SERVICE = "topicService";
    public static final String VISUALIZATION_EXPERIMENT_SERVICE = "visualizationExperimentService";
    public static final String DOC_TOPIC_SERVICE = "docTopicService";
    public static final String VISUALIZATION_TOPIC_DOCS_PER_JOURNAL_SERVICE = "visualizationTopicDocsPerJournalService";

    /** Enums **/
    public static final String SUCCESS = "SUCCESS";
    public static final String FAIL = "FAIL";
}
