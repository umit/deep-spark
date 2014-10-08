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

package com.stratio.deep.config;

import static com.stratio.deep.commons.extractor.utils.ExtractorConstants.FILTER_QUERY;
import static com.stratio.deep.commons.extractor.utils.ExtractorConstants.INPUT_COLUMNS;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.JobConf;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.hadoop.mr.EsInputFormat;
import org.elasticsearch.hadoop.mr.EsOutputFormat;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.stratio.deep.commons.config.ExtractorConfig;
import com.stratio.deep.commons.entity.Cell;
import com.stratio.deep.commons.extractor.utils.ExtractorConstants;
import com.stratio.deep.commons.filter.Filter;
import com.stratio.deep.commons.filter.FilterOperator;
import com.stratio.deep.commons.utils.Utils;

/**
 * @param <T>
 */
public class DeepJobConfigES<T> implements IESDeepJobConfig<T> {
    private static final long serialVersionUID = -7179376653643603038L;

    /**
     * configuration to be broadcasted to every spark node
     */
    private transient JobConf configHadoop;

    /**
     * A list of elasticsearch host to connect
     */
    private List<String> hostList = new ArrayList<>();

    private int port;

    /**
     * username
     */
    private String username;

    /**
     * password
     */

    private String password;

    /**
     * Collection to get or insert data
     */
    private String collection;

    /**
     * Database to connect
     */
    private String database;

    /**
     * Entity class to map JSONObject
     */
    protected Class<T> entityClass;

    /**
     * VIP, this MUST be transient!
     */
    private transient Map<String, Cell> columnDefinitionMap;

    private String[] inputColumns;

    private JSONObject fields;
    /**
     * OPTIONAL filter query
     */
    private String query;

    private Map<String, Object> customConfiguration;

    /**
     * Default constructor
     */
    public DeepJobConfigES(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    public IESDeepJobConfig<T> customConfiguration(Map<String, Object> customConfiguration) {
        this.customConfiguration = customConfiguration;
        return this;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Map<String, Cell> columnDefinitions() {
        return columnDefinitionMap;
    }

    @Override
    public IESDeepJobConfig<T> pageSize(int pageSize) {
        return null;
    }

    @Override
    public Class<T> getEntityClass() {
        return entityClass;
    }

    @Override
    public String getHost() {
        if (hostList.isEmpty()) {
            return null;
        }
        return hostList.get(0);
    }

    @Override
    public String[] getInputColumns() {
        return (String[]) fields.keySet().toArray(new String[fields.keySet().size()]);
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public IESDeepJobConfig<T> host(String host) {
        hostList.add(host);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DeepJobConfigES<T> initialize() {
        validate();
        configHadoop = new JobConf();
        configHadoop.setInputFormat(EsInputFormat.class);
        configHadoop.setOutputFormat(EsOutputFormat.class);
        configHadoop.set("es.resource", database);
        configHadoop.set("es.field.read.empty.as.null", "false");
        configHadoop.set("index.mapper.dynamic", "true");

        if (query != null) {
            // "?q=message:first"
            // es.query = { "query" : { "term" : { "user" : "costinl" } } }
            // "query": { "match_all": {} }
            configHadoop.set("es.query", query);
        }

        // if (fields != null) {
        // //String query = { "query" : { "match_all" : { } } }
        // configHadoop.set("es.query", fields.toString());
        // }

        configHadoop.set("es.nodes", Utils.splitHosts(hostList));
        configHadoop.set("es.port", String.valueOf(port));
        configHadoop.set("es.input.json", "yes");

        // index (default)
        // new data is added while existing data (based on its id) is replaced (reindexed).
        // create
        // adds new data - if the data already exists (based on its id), an exception is thrown.
        //
        // update
        // updates existing data (based on its id). If no data is found, an exception is thrown.
        //
        // upsert
        // known as merge or insert if the data does not exist, updates if the data exists (based on its id).

        // configHadoop.set("es.write.operation","");

        if (customConfiguration != null) {
            Set<Map.Entry<String, Object>> set = customConfiguration.entrySet();
            Iterator<Map.Entry<String, Object>> iterator = set.iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Object> entry = iterator.next();
                configHadoop.set(entry.getKey(), entry.getValue().toString());
            }

        }
        return this;
    }

    /**
     * validates connection parameters
     */
    private void validate() {
        if (hostList.isEmpty()) {
            throw new IllegalArgumentException("host cannot be null");
        }
        if (database == null) {
            throw new IllegalArgumentException("type cannot be null");
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IESDeepJobConfig<T> inputColumns(String... columns) {
        JSONObject jsonFields = fields != null ? fields : new JSONObject();
        // jsonFields.put("query", "match_all");
        JSONArray jsonArray = new JSONArray();
        for (String column : columns) {
            jsonArray.add(column);
        }
        jsonFields.put("fields", jsonArray);

        fields = jsonFields;
        System.out.printf(" -- " + fields.toJSONString());
        return this;
    }

    @Override
    public IESDeepJobConfig<T> password(String password) {
        this.password = password;
        return this;
    }

    @Override
    public IESDeepJobConfig<T> username(String username) {
        this.username = username;
        return this;
    }

    @Override
    public int getPageSize() {
        return 0;
    }

    @Override
    public IESDeepJobConfig<T> collection(String collection) {
        this.collection = collection;
        return this;
    }

    @Override
    public IESDeepJobConfig<T> database(String database) {
        this.database = database;
        return this;
    }

    @Override
    public IESDeepJobConfig<T> host(List<String> host) {
        hostList.addAll(host);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Configuration getHadoopConfiguration() {
        if (configHadoop == null) {
            initialize();
        }
        return configHadoop;
    }

    @Override
    public IESDeepJobConfig<T> filterQuery(String query) {
        this.query = query;
        return this;
    }

    @Override
    public IESDeepJobConfig<T> sort(String sort) {
        return null;
    }

    @Override
    public IESDeepJobConfig<T> inputKey(String inputKey) {
        return null;
    }

    @Override
    public List<String> getHostList() {
        return hostList;
    }

    @Override
    public IESDeepJobConfig<T> initialize(ExtractorConfig extractorConfig) {

        // TODO: Add filters & inputColumns

        Map<String, String> values = extractorConfig.getValues();

        if (values.get(ExtractorConstants.USERNAME) != null) {
            username(extractorConfig.getString(ExtractorConstants.USERNAME));
        }

        if (values.get(ExtractorConstants.PASSWORD) != null) {
            password(extractorConfig.getString(ExtractorConstants.PASSWORD));
        }

        if (values.get(ExtractorConstants.HOST) != null) {
            host(extractorConfig.getString(ExtractorConstants.HOST));
        }

        if (values.get(ExtractorConstants.PORT) != null) {
            port(extractorConfig.getInteger(ExtractorConstants.PORT));
        }

        if (values.get(ExtractorConstants.INDEX) != null && (values.get(ExtractorConstants.TYPE) != null)) {
            database(extractorConfig.getString(ExtractorConstants.INDEX).concat("/")
                            .concat(extractorConfig.getString(ExtractorConstants.TYPE)));
        }
        if (values.get(INPUT_COLUMNS) != null) {
            inputColumns(extractorConfig.getStringArray(INPUT_COLUMNS));
        }

        if (values.get(FILTER_QUERY) != null) {
            filterQuery(extractorConfig.getFilterArray(FILTER_QUERY));
        }

        this.initialize();

        return this;
    }

    /**
     * @param filterArray
     */
    private IESDeepJobConfig<T> filterQuery(Filter[] filterArray) {

        TransportClient elasticClient = new TransportClient();
        SearchRequestBuilder requestBuilder = elasticClient.prepareSearch();

        if (filterArray.length > 0) {

            // full-text queries are not supported => match all
            QueryBuilder queryBuilder = QueryBuilders.matchAllQuery();

            BoolFilterBuilder boolFilterBuilder = FilterBuilders.boolFilter();
            for (Filter filter : filterArray) {
                boolFilterBuilder.must(handleCompareFilter(filter));
            }

            requestBuilder.setQuery(QueryBuilders.filteredQuery(queryBuilder, boolFilterBuilder));

        }
        // Select
        // TODO
        if (inputColumns != null && inputColumns.length >= 1) {
            requestBuilder.addFields(inputColumns);
        }// TODO else exception?

        elasticClient.close();
        return this;
    }

    /**
     * @param filter
     * @return
     */
    private FilterBuilder handleCompareFilter(Filter filter) {
        FilterBuilder localFilterBuilder = null;

        String leftTerm = filter.getField();
        Object rightTerm = filter.getValue();
        switch (filter.getOperation()) {
        case FilterOperator.IS:
            localFilterBuilder = FilterBuilders.termFilter(leftTerm, rightTerm);
            break;
        case FilterOperator.NE:
            localFilterBuilder = FilterBuilders.notFilter(FilterBuilders.termFilter(leftTerm, rightTerm));
            break;
        case FilterOperator.LT:
            localFilterBuilder = FilterBuilders.rangeFilter(leftTerm).lt(rightTerm);
            break;
        case FilterOperator.LTE:
            localFilterBuilder = FilterBuilders.rangeFilter(leftTerm).lte(rightTerm);
            break;
        case FilterOperator.GT:
            localFilterBuilder = FilterBuilders.rangeFilter(leftTerm).gt(rightTerm);
            break;
        case FilterOperator.GTE:
            localFilterBuilder = FilterBuilders.rangeFilter(leftTerm).gte(rightTerm);
            break;

        }

        return localFilterBuilder;
    }

    public IESDeepJobConfig<T> port(int port) {
        this.port = port;
        return this;
    }

}
