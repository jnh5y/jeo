/* Copyright 2013 The jeo project. All rights reserved.
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
package io.jeo.vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.jeo.data.Sort;
import io.jeo.data.Transaction;
import io.jeo.filter.Filter;
import io.jeo.filter.Filters;
import io.jeo.filter.cql.CQL;
import io.jeo.filter.cql.ParseException;
import io.jeo.geom.Bounds;
import io.jeo.proj.Proj;
import io.jeo.util.Pair;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.locationtech.jts.geom.Envelope;
import java.util.HashSet;

/**
 * Describes a query against a {@link VectorDataset} dataset.
 *  
 * @author Justin Deoliveira, OpenGeo
 */
public class VectorQuery {

    /**
     * Returns a vector query with no constraints.
     */
    public static VectorQuery all() {
        return new VectorQuery();
    }

    /** logger */
    static Logger LOGGER = LoggerFactory.getLogger(VectorQuery.class);

    /**
     * Fields to include in query.
     */
    Set<String> fields = new HashSet<String>(3);

    /**
     * Spatial bounds of the query.
     */
    Bounds bounds;

    /**
     * Filter of the query
     */
    Filter<Feature> filter;

    /**
     * Limit / offset
     */
    Integer limit, offset;

//    /**
//     * sorting
//     */
//    List<Sort> sort;

    /**
     * reprojection
     */
    Pair<CoordinateReferenceSystem,CoordinateReferenceSystem> reproject;

    /**
     * simplification
     */
    Double simplify;

    /**
     * Transaction associated with the query
     */
    Transaction transaction = Transaction.NULL;

    /**
     * New query instance.
     */
    public VectorQuery() {
    }

    /**
     * Set of Feature properties to query, an empty set means all properties.
     * <p>
     * The field list applies to {@link VectorDataset#read(VectorQuery)} and {@link VectorDataset#update(VectorQuery)}
     * operations.
     * </p>
     */
    public Set<String> fields() {
        return fields;
    }

    /**
     * Get the fields in order of appearance in the Schema. An empty set implies
     * all properties.
     * 
     * @param schema the schema to evaluate ordering against
     * @return list of fields in schema ordering or empty list
     */
    public List<String> fieldsIn(Schema schema) {
        List<String> ordered = new ArrayList<String>(fields.size());
        if (fields.size() > 0) {
            List<Field> schemaFields = schema.fields();
            for (Field f: schemaFields) {
                for (String s: fields) {
                    if (f.name().equals(s)) {
                        ordered.add(s);
                        break;
                    }
                }
            }
        }
        return ordered;
    }

    /**
     * Bounds constraints on the query, may be <code>null</code> meaning no bounds constraint.
     * <p>
     * The bounds constraint applies to {@link VectorDataset#read(VectorQuery)} and
     * {@link VectorDataset#update(VectorQuery)} operations.
     * </p>
     */
    public Bounds bounds() {
        return bounds;
    }

    /**
     * Constraint on the query, may be <code>null</code> meaning no constraint.
     * <p>
     * The filter constraint applies to {@link VectorDataset#read(VectorQuery)} and
     * {@link VectorDataset#update(VectorQuery)} operations.
     * </p>
     */
    public Filter<Feature> filter() {
        return filter;
    }

    /**
     * Limit on the number of features to return from the query, <code>null</code> meaning no limit.
     * <p>
     * The feature limit applies to {@link VectorDataset#read(VectorQuery)} and
     * {@link VectorDataset#update(VectorQuery)} operations.
     * </p>
     */
    public Integer limit() {
        return limit;
    }

    /**
     *  Offset into query result set from which to start returning features, <code>null</code> 
     *  meaning no offset.
     * <p>
     * The feature offset applies to {@link VectorDataset#read(VectorQuery)} and
     * {@link VectorDataset#update(VectorQuery)} operations.
     * </p>
     */
    public Integer offset() {
        return offset;
    }

    /**
     * Coordinate reference systems to reproject feature results between, <code>null</code> 
     * meaning no reprojection should occur.
     * <p>
     * The first element in the pair may be <code>null</code> to signify that the dataset crs 
     * (if available) should be used
     * </p>
     * <p>
     * The reproject field applies to {@link VectorDataset#read(VectorQuery)} and
     * {@link VectorDataset#update(VectorQuery)} operations.
     * </p>
     */
    public Pair<CoordinateReferenceSystem, CoordinateReferenceSystem> reproject() {
        return reproject;
    }

    /**
     * Simplification tolerance to apply to feature geometries, <code>null</code> meaning no 
     * simplification.
     * <p>
     * The simplification tolerance applies to {@link VectorDataset#read(VectorQuery)} and
     * {@link VectorDataset#update(VectorQuery)} operations.
     * </p>
     */
    public Double simplify() {
        return simplify;
    }

//    /**
//     * Sort criteria for the query, <code>null</code> meaning no sorting.
//     * <p>
//     * The sorting criteria applies to {@link VectorDataset#read(VectorQuery)} and
//     * {@link VectorDataset#update(VectorQuery)} operations.
//     * </p>
//     */
//    public List<Sort> sort() {
//        return sort;
//    }

    /**
     * Transaction of the query, may be <code>null</code>.
     * <p>
     * The transaction applies to {@link VectorDataset#update(VectorQuery)} and {@link VectorDataset#append(VectorQuery)}
     * operations.
     * </p>
     */
    public Transaction transaction() {
        return transaction;
    }

    /**
     * Sets the field list of the query.
     * 
     * @return This object.
     */
    public VectorQuery fields(String field, String... fields) {
        List<String> l = new ArrayList<>();
        l.add(field);
        l.addAll(Arrays.asList(fields));
        return fields(l);
    }

    /**
     * Sets the field list of the query.
     * 
     * @return This object.
     */
    public VectorQuery fields(Collection<String> fields) {
        this.fields.clear();
        return appendFields(fields);
    }

    /**
     * Appends to the field list of this query.
     *
     * @return This object.
     */
    public VectorQuery appendFields(Collection<String> fields) {
        this.fields.addAll(fields);
        return this;
    }

    /**
     * Sets the filter of the query from a CQL string.
     * 
     * @return This object.
     */
    public VectorQuery filter(String cql) {
        try {
            return filter(CQL.parse(cql));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the filter of the query.
     * 
     * @return This object.
     */
    public VectorQuery filter(Filter<Feature> filter) {
        this.filter = filter;
        return this;
    }

    /**
     * Sets the bounds constraint of the query.
     *
     * @return This object.
     */
    public VectorQuery bounds(Envelope env) {
        return bounds(new Bounds(env));
    }

    /**
     * Sets the bounds constraint of the query.
     * 
     * @return This object.
     */
    public VectorQuery bounds(Bounds bounds) {
        this.bounds = bounds;
        return this;
    }

    /**
     * Sets the limit on the number of results to return from the query.
     * 
     * @return This object.
     */
    public VectorQuery limit(Integer limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Sets the number of results to skip over from the query.
     * 
     * @return This object.
     */
    public VectorQuery offset(Integer offset) {
        this.offset = offset;
        return this;
    }

//    /**
//     * Sets the properties to sort results by.
//     *
//     * @return This object.
//     */
//    public VectorQuery sort(String... sort) {
//        List<Sort> list = new ArrayList<Sort>();
//        for (String s : sort) {
//            list.add(new Sort(s));
//        }
//        this.sort = list;
//        return this;
//    }

    /**
     * Sets the srs to re-project query results to. 
     * 
     * @return This object.
     */
    public VectorQuery reproject(String srs) {
        return reproject(null, srs);
    }

    /**
     * Sets the srs to re-project query results to. 
     * 
     * @return This object.
     */
    public VectorQuery reproject(String from, String to) {
        CoordinateReferenceSystem src = from != null ? Proj.crs(from) : null;
        CoordinateReferenceSystem dst = to != null ? Proj.crs(to) : null;
         
        if (from != null && src == null) {
            throw new IllegalArgumentException("Unknown crs: " + from);
        }
        if (to == null) {
            throw new IllegalArgumentException("Unknown crs: " + to);
        }
        return reproject(src, dst);
    }

    /**
     * Sets the crs to re-project query results to. 
     * 
     * @return This object.
     */
    public VectorQuery reproject(CoordinateReferenceSystem crs) {
        return reproject(null, crs);
    }

    /**
     * Sets the source/target crs to re-project query results from/to. 
     * 
     * @return This object.
     */
    public VectorQuery reproject(CoordinateReferenceSystem from, CoordinateReferenceSystem to) {
        reproject = new Pair<CoordinateReferenceSystem,CoordinateReferenceSystem>(from, to);
        return this;
    }

    /**
     * Sets the tolerance with which to simplify geometry of query results.   
     * 
     * @return This object.
     */
    public VectorQuery simplify(Double tolerance) {
        simplify = tolerance;
        return this;
    }

    /**
     * Sets the transaction of the query.
     * 
     * @return This object.
     */
    public VectorQuery transaction(Transaction tx) {
        this.transaction = tx;
        return this;
    }

    /**
     * Determines if the query constrains results with a bounds constraint or filter.
     * 
     * @return True if no bounds or filter constraint is applied, otherwise false.
     */
    public boolean isAll() {
        return Bounds.isNull(bounds) && !isFiltered();
    }

    /**
     * Determines if the query constrains results with or filter.
     */
    public boolean isFiltered() {
        return !Filters.isTrueOrNull(filter);
    }

    /**
     * Adjusts a raw count based on limit and offset of the query.
     * <p>
     * The adjusted count is equivalent to:
     * <pre>
     * min(max(0, count-offset), limit)
     * </pre>
     * </p>
     * @return The adjusted count.
     */
    public long adjustCount(long count) {
        // TODO: move this to QueryPlan?
        if (offset != null) {
            count = Math.max(0, count - offset);
        }
        if (limit != null) {
            count = Math.min(count, limit);
        }

        return count;
    }

    /**
     * Compute missing properties based on the provided schema and the current
     * filter. This allows a format to defer to CQL filtering instead of using
     * native (SQL for example) encoding that may result in errors or invalid
     * results.
     * @param schema non-null schema to evaluate the filter against
     * @return non-null Set of any missing properties
     */
    public Set<String> missingProperties(Schema schema) {
        Set<String> queryProperties = (Set<String>) (filter == null ?
                Collections.emptySet() : Filters.properties(filter));
        List<Field> f = schema.fields();
        for (int i = 0, ii = f.size(); i < ii && !queryProperties.isEmpty(); i++) {
            queryProperties.remove(f.get(i).name());
        }
        return queryProperties;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bounds == null) ? 0 : bounds.hashCode());
        result = prime * result + ((fields == null) ? 0 : fields.hashCode());
        result = prime * result + ((filter == null) ? 0 : filter.hashCode());
        result = prime * result + ((limit == null) ? 0 : limit.hashCode());
        result = prime * result + ((offset == null) ? 0 : offset.hashCode());
        result = prime * result
                + ((reproject == null) ? 0 : reproject.hashCode());
        result = prime * result
                + ((simplify == null) ? 0 : simplify.hashCode());
        //result = prime * result + ((sort == null) ? 0 : sort.hashCode());
        result = prime * result
                + ((transaction == null) ? 0 : transaction.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        VectorQuery other = (VectorQuery) obj;
        if (bounds == null) {
            if (other.bounds != null)
                return false;
        } else if (!bounds.equals(other.bounds))
            return false;
        if (fields == null) {
            if (other.fields != null)
                return false;
        } else if (!fields.equals(other.fields))
            return false;
        if (filter == null) {
            if (other.filter != null)
                return false;
        } else if (!filter.equals(other.filter))
            return false;
        if (limit == null) {
            if (other.limit != null)
                return false;
        } else if (!limit.equals(other.limit))
            return false;
        if (offset == null) {
            if (other.offset != null)
                return false;
        } else if (!offset.equals(other.offset))
            return false;
        if (reproject == null) {
            if (other.reproject != null)
                return false;
        } else if (!reproject.equals(other.reproject))
            return false;
        if (simplify == null) {
            if (other.simplify != null)
                return false;
        } else if (!simplify.equals(other.simplify))
            return false;
//        if (sort == null) {
//            if (other.sort != null)
//                return false;
//        } else if (!sort.equals(other.sort))
//            return false;
        if (transaction == null) {
            if (other.transaction != null)
                return false;
        } else if (!transaction.equals(other.transaction))
            return false;
        return true;
    }
}
