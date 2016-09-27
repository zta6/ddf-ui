/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.commands.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.felix.gogo.commands.Command;
import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.codice.ddf.commands.catalog.facade.Provider;
import org.geotools.filter.text.cql2.CQL;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortOrder;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceProcessingDetails;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.util.impl.ServiceComparator;

@Command(scope = CatalogCommands.NAMESPACE, name = "migrate", description = "Migrates Metacards "
        + "from a Configured External Provider into the Catalog.  The migrate command currently "
        + "picks the first Catalog provider as the FROM and the second Catalog provider as the TO. "
        + "Additional support will be added to specify which provider will be migrated TO and FROM "
        + "in the future.")
public class MigrateCommand extends DuplicateCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(MigrateCommand.class);

    @Override
    protected Object executeWithSubject() throws Exception {
        if (batchSize > MAX_BATCH_SIZE || batchSize < 1) {
            console.println("Batch Size must be between 1 and 1000.");
            return null;
        }

        List<CatalogProvider> providers = getCatalogProviders();

        if (providers.isEmpty() || providers.size() < 2) {
            console.println("Not enough CatalogProviders installed to migrate");
            return null;
        }

        console.println("The \"FROM\" provider is: " + providers.get(0)
                .getClass()
                .getSimpleName());
        CatalogProvider provider = providers.get(1);
        console.println("The \"TO\" provider is: " + provider.getClass()
                .getSimpleName());
        String answer = getInput("Do you wish to continue? (yes/no)");
        if (!"yes".equalsIgnoreCase(answer)) {
            console.println();
            console.println("Now exiting...");
            console.flush();
            return null;
        }

        CatalogFacade ingestProvider = new Provider(provider);

        CatalogFacade framework = getCatalog();

        start = System.currentTimeMillis();

        final Filter filter = (cqlFilter != null) ? CQL.toFilter(cqlFilter) : getFilter(
                getFilterStartTime(start),
                start,
                getTemporalProperty());

        console.println("Starting migration.");

        duplicateInBatches(framework, ingestProvider, filter);

        console.println();
        long end = System.currentTimeMillis();
        String completed = String.format(
                " %d record(s) migrated; %d record(s) failed; completed in %3.3f seconds.",
                ingestedCount.get(),
                failedCount.get(),
                (end - start) / MS_PER_SECOND);
        LOGGER.debug("Migration Complete: {}", completed);
        console.println(completed);

        return null;
    }

    @Override
    protected SourceResponse query(CatalogFacade framework, Filter filter, int startIndex,
            long querySize) {
        QueryImpl query = new QueryImpl(filter);
        query.setRequestsTotalResultsCount(true);
        query.setPageSize((int) querySize);
        query.setSortBy(new SortByImpl(Metacard.MODIFIED, SortOrder.DESCENDING));
        QueryRequest queryRequest = new QueryRequestImpl(query);
        query.setStartIndex(startIndex);
        SourceResponse response;
        try {
            LOGGER.debug("Querying with startIndex: {}", startIndex);
            response = framework.query(queryRequest);
        } catch (UnsupportedQueryException | SourceUnavailableException | FederationException e) {
            printErrorMessage(String.format("Received error from Frameworks: %s%n",
                    e.getMessage()));
            return null;
        }
        if (response.getProcessingDetails() != null && !response.getProcessingDetails()
                .isEmpty()) {
            for (SourceProcessingDetails details : response.getProcessingDetails()) {
                LOGGER.debug("Got Issues: {}", details.getWarnings());
            }
            return null;
        }

        return response;
    }

    private List<CatalogProvider> getCatalogProviders() {
        ServiceTracker st = new ServiceTracker(getBundleContext(),
                CatalogProvider.class.getName(),
                null);
        st.open();
        ServiceReference<CatalogProvider>[] serviceRefs = st.getServiceReferences();

        Map<ServiceReference<CatalogProvider>, CatalogProvider> map =
                new TreeMap<>(new ServiceComparator());

        if (null != serviceRefs) {
            for (ServiceReference<CatalogProvider> serviceReference : serviceRefs) {
                map.put(serviceReference, (CatalogProvider) st.getService(serviceReference));
            }
        }

        return new ArrayList<>(map.values());
    }
}