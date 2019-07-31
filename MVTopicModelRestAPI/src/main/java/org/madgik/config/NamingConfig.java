package org.madgik.config;

import org.hibernate.cfg.ImprovedNamingStrategy;

public class NamingConfig extends ImprovedNamingStrategy {

    private static final long serialVersionUID = 4525967520725688164L;

    @Override
    public String tableName(String className) {
        String tableName = super.tableName(className);
        return tableName.toUpperCase();
    }

    @Override
    public String propertyToColumnName(String propertyName) {
        String property = super.propertyToColumnName(propertyName);
        return property.toUpperCase();
    }

}
