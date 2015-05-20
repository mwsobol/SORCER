package sorcer.river;
/**
 *
 * Copyright 2013 Rafał Krupiński.
 * Copyright 2013 Sorcersoft.com S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.lookup.ServiceItemFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic set of simple {@link ServiceItemFilter}s
 *
 * @author Rafał Krupiński
 */
public class Filters {
    private static ServiceItemFilter trueFilter = new AnyFilter();

    private Filters() {
        //static only class
    }

    /**
     * Find first matching {@link ServiceItem}
     *
     * @param items  {@link ServiceItem}s to look in
     * @param filter {@link ServiceItemFilter} to match
     * @return first item matching the filter
     */
    public static ServiceItem firstMatching(ServiceItem[] items, ServiceItemFilter filter) {
        for (ServiceItem item : items) {
            if (filter.check(item)) return item;
        }
        return null;
    }

    /**
     * Find and return all matching service items
     * @param items  {@link ServiceItem}s to look in
     * @param filter {@link ServiceItemFilter} to match
     * @return array of items matching the filter
     */
    public static ServiceItem[] matching(ServiceItem[] items, ServiceItemFilter filter) {
        List<ServiceItem> result = new ArrayList<ServiceItem>(items.length);
        for (ServiceItem item : items) {
            if (filter.check(item)) result.add(item);
        }
        return result.toArray(new ServiceItem[result.size()]);
    }

    /**
     * @return filter that matches any ServiceItem
     */
    public static ServiceItemFilter any() {
        return trueFilter;
    }

    public static ServiceItemFilter and(ServiceItemFilter... filters) {
        assert filters != null;
        return new AndFilter(filters);
    }

    public static ServiceItemFilter or(ServiceItemFilter... filters) {
        assert filters != null;
        return new OrFilter(filters);
    }

    public static ServiceItemFilter not(ServiceItemFilter filter) {
        assert filter != null;
        return new NotFilter(filter);
    }

    public static ServiceItemFilter serviceId(ServiceID serviceID) {
        return new ServiceIDFilter(serviceID);
    }
}

class AnyFilter implements ServiceItemFilter {
    @Override
    public boolean check(ServiceItem item) {
        return true;
    }
}

class AndFilter implements ServiceItemFilter {
    private ServiceItemFilter[] filters;

    AndFilter(ServiceItemFilter[] filters) {
        this.filters = filters;
    }

    @Override
    public boolean check(ServiceItem item) {
        for (ServiceItemFilter filter : filters) {
            if (!filter.check(item)) return false;
        }
        return true;
    }
}

class OrFilter implements ServiceItemFilter {
    private ServiceItemFilter[] filters;

    OrFilter(ServiceItemFilter[] filters) {
        this.filters = filters;
    }

    @Override
    public boolean check(ServiceItem item) {
        for (ServiceItemFilter filter : filters) {
            if (filter.check(item)) return true;
        }
        return false;
    }
}

class NotFilter implements ServiceItemFilter {
    private ServiceItemFilter filter;

    NotFilter(ServiceItemFilter filter) {
        this.filter = filter;
    }

    @Override
    public boolean check(ServiceItem item) {
        return !filter.check(item);
    }
}

class ServiceIDFilter implements ServiceItemFilter {
    private ServiceID serviceID;

    ServiceIDFilter(ServiceID serviceID) {
        this.serviceID = serviceID;
    }

    @Override
    public boolean check(ServiceItem item) {
        return serviceID == null || item.serviceID.equals(serviceID);
    }
}
