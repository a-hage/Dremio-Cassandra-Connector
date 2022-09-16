/*
 * Copyright (C) 2017-2018 Dremio Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dremio.exec.store.jdbc.conf;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hibernate.validator.constraints.NotBlank;
import com.dremio.common.AutoCloseables;
import com.dremio.common.util.CloseableIterator;
import com.dremio.options.OptionManager;
import com.dremio.exec.catalog.conf.DisplayMetadata;
import com.dremio.exec.catalog.conf.NotMetadataImpacting;
import com.dremio.exec.catalog.conf.SourceType;

import com.dremio.security.CredentialsService;
//import com.dremio.exec.store.jdbc.*;
import com.dremio.exec.store.jdbc.CloseableDataSource;
import com.dremio.exec.store.jdbc.DataSources;
import com.dremio.exec.store.jdbc.JdbcPluginConfig;
import com.dremio.exec.store.jdbc.dialect.arp.ArpDialect;
import com.dremio.exec.store.jdbc.dialect.arp.ArpYaml;
import com.google.common.base.Strings;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;

import io.protostuff.Tag;

/**
 * Configuration for Cassandra sources.
 */
@SourceType(value = "CASSANDRA", label = "Cassandra", externalQuerySupported = false)
public class CassandraConf extends AbstractArpConf<CassandraConf> {
    private static final String ARP_FILENAME = "arp/implementation/cassandra-arp.yaml";
    private static final ArpDialect ARP_DIALECT =
            AbstractArpConf.loadArpFile(ARP_FILENAME, (ArpDialect::new));

    private static final String DRIVER = "com.ing.data.cassandra.jdbc.CassandraDriver";

    @NotBlank
    @Tag(1)
    @DisplayMetadata(label = "Host")
    public String host;

    @NotBlank
    @Tag(2)
    @DisplayMetadata(label = "Port")
    public String port;

    @NotBlank
    @Tag(3)
    @DisplayMetadata(label = "KeySpace")
    public String keyspace;

    @Tag(4)
    @DisplayMetadata(label = "Maximum idle connections")
    @NotMetadataImpacting
    public int maxIdleConns = 8;

    @Tag(5)
    @DisplayMetadata(label = "Connection idle time (s)")
    @NotMetadataImpacting
    public int idleTimeSec = 60;

    @Tag(6)
    @DisplayMetadata(label = "Record fetch size")
    @NotMetadataImpacting
    public int fetchSize = 200;

    /**
     *   "Grant External Query access (External Query allows creation of VDS from a Sybase query.
     *     Learn more here: https://docs.dremio.com/data-sources/external-queries.html#enabling-external-queries"
     */

    @Tag(6)
    @NotMetadataImpacting
    @DisplayMetadata(label = "Grant External Query access (External Query allows creation of VDS from a Sybase query. Learn more here: https://docs.dremio.com/data-sources/external-queries.html#enabling-external-queries)")
    public boolean enableExternalQuery = true;

    @VisibleForTesting
    public String toJdbcConnectionString() {
        final  String host = checkNotNull(this.host, "Missing host.");
        final  String port = checkNotNull(this.port, "Missing port.");
        final  String keyspace = checkNotNull(this.keyspace, "Missing keyspace.");
        return String.format("jdbc:cassandra://%s:%s/keyspace?localdatacenter=%s", host, port, keyspace);
    }

    @Override
    @VisibleForTesting
    public JdbcPluginConfig buildPluginConfig(JdbcPluginConfig.Builder configBuilder, CredentialsService credentialsService, OptionManager optionManager) {
        return configBuilder.withDialect(getDialect())
                .withDialect(getDialect())
                .withFetchSize(fetchSize)
                .withDatasourceFactory(this::newDataSource)
                .clearHiddenSchemas()
                .withAllowExternalQuery(enableExternalQuery)
                .build();
    }

    private CloseableDataSource newDataSource() {
        return DataSources.newGenericConnectionPoolDataSource(DRIVER,
                toJdbcConnectionString(),null,null,null, DataSources.CommitMode.DRIVER_SPECIFIED_COMMIT_MODE, maxIdleConns, idleTimeSec);
    }

    @Override
    public ArpDialect getDialect() {
        return ARP_DIALECT;
    }

    @VisibleForTesting
    public static ArpDialect getDialectSingleton() {
        return ARP_DIALECT;
    }
}
