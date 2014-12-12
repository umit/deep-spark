/*
 * Copyright 2014, Stratio.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stratio.deep.jdbc.config;

import com.stratio.deep.commons.config.DeepJobConfig;
import com.stratio.deep.commons.config.ExtractorConfig;
import com.stratio.deep.commons.config.HadoopConfig;
import com.stratio.deep.commons.entity.Cells;
import com.stratio.deep.commons.filter.Filter;
import com.stratio.deep.commons.filter.FilterType;
import com.stratio.deep.jdbc.extractor.JdbcNativeCellExtractor;
import com.stratio.deep.jdbc.extractor.JdbcNativeEntityExtractor;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.lib.db.DBConfiguration;

import java.io.Serializable;
import java.util.Map;

import static com.stratio.deep.commons.extractor.utils.ExtractorConstants.*;

/**
 * Configuration class for Jdbc-Spark integration
 *
 * @param <T>
 */
public class JdbcDeepJobConfig<T> extends DeepJobConfig<T, JdbcDeepJobConfig<T>> implements
        IJdbcDeepJobConfig<T, JdbcDeepJobConfig<T>>, Serializable {

    private static final long serialVersionUID = -6487437723098215934L;

    private String driverClass;

    private String query;

    private int upperBound = Integer.MAX_VALUE;

    private int lowerBound = 0;

    private int numPartitions = 1;


    /**
     * Constructor for Entity class-based configuration.
     *
     * @param entityClass
     */
    public JdbcDeepJobConfig(Class<T> entityClass) {
        super(entityClass);
        if (Cells.class.isAssignableFrom(entityClass)) {
            extractorImplClass = JdbcNativeCellExtractor.class;
        } else {
            extractorImplClass = JdbcNativeEntityExtractor.class;
        }
    }

    public JdbcDeepJobConfig driverClass(String driverClass) {
        this.driverClass = driverClass;
        return this;
    }

    public String getDriverClass() {
        return this.driverClass;
    }


    public String getJdbcUrl() {
        StringBuilder sb = new StringBuilder();
        sb.append("jdbc:");
        sb.append(getJdbcProvider());
        sb.append("://");
        sb.append(host.get(0));
        sb.append(":");
        sb.append(port);
        sb.append("/");
        sb.append(catalog);
        sb.append("?");
        return sb.toString();
    }

    private String getJdbcProvider(){
        int firstIndex = driverClass.indexOf(".");
        int secondIndex = driverClass.indexOf(".", ++firstIndex);
        return driverClass.substring(firstIndex, secondIndex);
    }

    @Override
    public JdbcDeepJobConfig username(String username) {
        this.username = username;
        return this;
    }

    @Override
    public JdbcDeepJobConfig password(String password) {
        this.password = password;
        return this;
    }

    @Override
    public JdbcDeepJobConfig database(String database) {
        this.catalog = database;
        return this;
    }

    @Override
    public String getDatabase() {
        return catalog;
    }

    @Override
    public JdbcDeepJobConfig table(String table) {
        this.table = table;
        return this;
    }

    @Override
    public String getTable() {
        return table;
    }

    @Override
    public JdbcDeepJobConfig query(String query) {
        this.query = query;
        return this;
    }

    @Override
    public String getQuery() {
        return this.query;
    }

    @Override
    public JdbcDeepJobConfig upperBound(int upperBound) {
        this.upperBound = upperBound;
        return this;
    }

    @Override
    public int getUpperBound() {
        return this.upperBound;
    }

    @Override
    public JdbcDeepJobConfig lowerBound(int lowerBound) {
        this.lowerBound = lowerBound;
        return this;
    }

    @Override
    public int getLowerBound() {
        return this.lowerBound;
    }

    @Override
    public JdbcDeepJobConfig numPartitions(int numPartitions) {
        this.numPartitions = numPartitions;
        return this;
    }

    @Override
    public int getNumPartitions() {
        return this.numPartitions;
    }

    @Override
    public JdbcDeepJobConfig<T> initialize() throws IllegalStateException {
        validate();
        return this;
    }

    private void validate() {
        if(host == null || host.isEmpty()) {
            throw new IllegalArgumentException("Host must be specified");
        }
        if(port <= 0) {
            throw new IllegalArgumentException("Port must be valid");
        }
        if(driverClass == null || driverClass.isEmpty()) {
            throw new IllegalArgumentException("Driver class must be specified");
        }
        if(catalog == null || catalog.isEmpty()) {
            throw new IllegalArgumentException("Schema name must be specified");
        }
        if(table == null || table.isEmpty()) {
            throw new IllegalArgumentException("Table name must be specified");
        }
        if(query == null || query.isEmpty()) {
            query("SELECT * FROM " + getTable());
        }
    }

    @Override
    public JdbcDeepJobConfig<T> initialize(ExtractorConfig extractorConfig) {
        super.initialize(extractorConfig);

        Map<String, Serializable> values = extractorConfig.getValues();

        if (values.get(JDBC_DRIVER_CLASS) != null) {
            driverClass(extractorConfig.getString(JDBC_DRIVER_CLASS));
        }

        if (values.get(JDBC_QUERY) != null) {
            query(extractorConfig.getString(JDBC_QUERY));
        }
        this.initialize();

        return this;
    }


    private String getSqlOperatorFromFilter(Filter filter) {
        FilterType type = filter.getFilterType();
        if(FilterType.EQ.equals(type)) {
            return "=";
        } else if(FilterType.GT.equals(type)) {
            return ">";
        } else if(FilterType.GTE.equals(type)) {
            return ">=";
        } else if(FilterType.LT.equals(type)) {
            return "<";
        } else if(FilterType.LTE.equals(type)) {
            return "<=";
        } else if(FilterType.NEQ.equals(type)) {
            return "!=";
        } else if(FilterType.IN.equals(type)) {
            return "IN";
        }
        throw new UnsupportedOperationException();
    }
}
