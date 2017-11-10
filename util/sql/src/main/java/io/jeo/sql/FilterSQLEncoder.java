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
package io.jeo.sql;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.jeo.filter.FilterWalker;
import io.jeo.filter.Self;
import io.jeo.vector.Field;
import io.jeo.vector.Schema;
import io.jeo.filter.All;
import io.jeo.filter.Comparison;
import io.jeo.filter.Expression;
import io.jeo.filter.Filter;
import io.jeo.filter.Function;
import io.jeo.filter.Id;
import io.jeo.filter.Literal;
import io.jeo.filter.Logic;
import io.jeo.filter.Mixed;
import io.jeo.filter.None;
import io.jeo.filter.Property;
import io.jeo.filter.Spatial;
import io.jeo.util.Pair;

import org.locationtech.jts.geom.Geometry;

import io.jeo.filter.In;
import io.jeo.filter.Like;
import io.jeo.filter.Math;
import io.jeo.filter.Null;

/**
 * Transforms a filter object into SQL.
 * <p>
 * This base implementation encodes using "standard" SQL and SFS (simple features for SQL)
 * conventions. Format implementations should subclass and override methods as need be.
 * </p>  
 * <p>
 * The encoder operates in two modes in one of two modes determined by {@link #isPrepared()}. When
 * <code>true</code> the encoder will emit prepared statement sql. Arguments for the prepared 
 * statement are stored in {@link #getArgs()}. When <code>false</code> the encoder will encode 
 * literals directly. 
 * </p>
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class FilterSQLEncoder extends FilterWalker<Object> {

    protected PrimaryKey pkey;
    protected DbTypes dbtypes = new DbTypes();
    protected Schema schema;

    protected SQL sql;

    protected boolean prepared = true;
    protected List<Pair<Object, Integer>> args;

    public FilterSQLEncoder() {
        sql = new SQL();
        args = new ArrayList<Pair<Object, Integer>>();
    }

    public void setPrimaryKey(PrimaryKey pkey) {
        this.pkey = pkey;
    }

    public void setDbTypes(DbTypes dbtypes) {
        this.dbtypes = dbtypes;
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    public SQL getSQL() {
        return sql;
    }

    public List<Pair<Object, Integer>> getArgs() {
        return args;
    }

    public boolean isPrepared() {
        return prepared;
    }

    public void setPrepared(boolean prepared) {
        this.prepared = prepared;
    }

    public String encode(Filter<?> filter, Object obj) {
        sql.clear();
        args.clear();

        filter.accept(this, obj);
        return sql.toString();
    }

    protected void abort(Object obj, String reason) {
        throw new FilterSQLException(
            String.format(Locale.ROOT,"Unable to encode %s as sql, %s @ %s", obj, reason, sql.toString())); 
    }

    public Object visit(Literal literal, Object obj) {
        Object val = literal.evaluate(null);

        if (val == null) {
            if (prepared) {
                sql.add("?");
                args.add(new Pair<Object, Integer>(null, dbtypes.toSQL(Geometry.class)));
            }
            else {
                sql.add("NULL");
            }
        }
        else {
            if (val instanceof Geometry) {
                encode((Geometry)val, obj);
            }
            else {
                if (prepared) {
                    sql.add("?");

                    Integer sqlType = null;
                    if (obj instanceof Field) {
                        // if field passed in as context use it to determine the type
                        Field fld = (Field) obj;
                        sqlType = dbtypes.toSQL(fld.type());
                    }
                    else {
                        // use the value class
                        sqlType = dbtypes.toSQL(val.getClass());
                    }
                    args.add(new Pair<Object, Integer>(val, sqlType));
                }
                else {
                    if (val instanceof Number) {
                        sql.add(val);
                    }
                    else if (val instanceof Date) {
                        sql.add(encode((Date)val, obj));
                    }
                    else {
                        sql.str(val.toString());
                    }
                }
            }
        }

        return obj;
    }

    protected void encode(Geometry geo, Object obj) {
        int srid = srid(geo, obj);

        if (prepared) {
            sql.add("ST_GeomFromText(?,?)");
            args.add(new Pair<Object,Integer>(geo.toText(), Types.VARCHAR));
            args.add(new Pair<Object,Integer>(srid, Types.INTEGER));
        }
        else {
            sql.add("ST_GeomFromText(").str(geo.toText()).add(",").add(srid).add(")");
        }
    }

    protected String encode(Date date, Object obj) {
        abort(date, "not implemented");
        return null;
    }

    protected int srid(Geometry geo, Object obj) {
        return geo.getSRID();
    }

    public Object visit(Property property, Object obj) {
        sql.name(property.property());
        return obj;
    }

    public Object visit(Function function, Object obj) {
        sql.add(function.name()).add("(");
        for (Expression e : function.args()) {
            e.accept(this, obj);
        }
        sql.add(")");
        return obj;
    }

    @Override
    public Object visit(Mixed mixed, Object obj) {
        abort(mixed, "Encoding mixed expressions not supported");
        return null;
    }

    @Override
    public Object visit(Self self, Object obj) {
        abort(self, "Self expressions not supported");
        return null;
    }

    @Override
    public Object visit(Expression expr, Object obj) {
        abort(expr, "Unknown expression not supported");
        return null;
    }

    public final Object visit(All<?> all, Object obj) {
        sql.add("1 = 1");
        return obj;
    }

    public Object visit(None<?> none, Object obj) {
        sql.add("1 = 0");
        return obj;
    }

    public Object visit(Id<?> id, Object obj) {
        if (pkey == null) {
            abort(id, "Id filter requires primary key");
        }
        if (pkey.getColumns().size() != 1) {
            abort(id, "Id filter only supported for single column primary key");
        }

        PrimaryKeyColumn pkeyCol = pkey.getColumns().get(0);
        sql.name(pkeyCol.getName()).add(" IN (");

        // grab the field for the primary key so we can properly handle the type
        for (Expression e : id.ids()) {
            e.accept(this, pkeyCol.getField());
            sql.add(",");
        }
        sql.trim(1).add(")");
        return obj;
    }

    public Object visit(Logic<?> logic, Object obj) {
        switch(logic.type()) {
        case NOT:
            sql.add("NOT (");
            logic.parts().get(0).accept(this, obj);
            sql.add(")");
            break;
        default:
            String op = logic.type().name();
            for (Filter<?> f : logic.parts()) {
                sql.add("(");
                f.accept(this, obj);
                sql.add(") ").add(op).add(" ");
            }
            sql.trim(op.length()+2);
        }

        return obj;
    }

    public Object visit(Comparison<?> compare, Object obj) {
        Field fld = field(compare.left(), compare.right());

        compare.left().accept(this, fld);
        sql.add(" ").add(compare.type().toString()).add(" ");
        compare.right().accept(this,  fld);
        return obj;
    }

    public Object visit(Spatial<?> spatial, Object obj) {
        Field fld = field(spatial.left(), spatial.right());
        
        String function = null;
        boolean dist = false;
        switch(spatial.type()) {
        case INTERSECTS:
            function = "ST_Intersects";
            break;
        case CONTAINS:
            function = "ST_Contains";
            break;
        case COVERS:
            function = "ST_Covers";
            break;
        case CROSSES:
            function = "ST_Crosses";
            break;
        case DISJOINT:
            function = "ST_Disjoint";
            break;
        case EQUALS:
            function = "ST_Equals";
            break;
        case OVERLAPS:
            function = "ST_Overlaps";
            break;
        case TOUCHES:
            function = "ST_Touches";
            break;
        case WITHIN:
            function = "ST_Within";
            break;
        case BEYOND:
            function = "ST_Beyond";
            dist = true;
            break;
        case DWITHIN:
            function = "ST_DWithin";
            dist = true;
            break;
        default:
            abort(spatial, "unsupported spatial filter");
        }


        sql.add(function).add("(");
        spatial.left().accept(this, fld);
        sql.add(", ");
        spatial.right().accept(this, fld);
        if (dist) {
            sql.add(", ");
            spatial.distance().accept(this, fld);
        }
        sql.add(")");
        return obj;
    }

    @Override
    public Object visit(Math math, Object obj) {
        sql.add('(');
        math.left().accept(this, obj);
        sql.add(math.operator());
        math.right().accept(this, obj);
        sql.add(')');
        return obj;
    }

    @Override
    public Object visit(Like like, Object obj) {
        like.property().accept(this, obj);
        sql.add(" LIKE ");
        sql.str((String) like.match().evaluate(null));
        return obj;
    }

    @Override
    public Object visit(In in, Object obj) {
        in.property().accept(this, obj);
        sql.add(" IN ");
        sql.add('(');
        List<Expression> vals = in.values();
        for (int i = 0; i < vals.size(); i++) {
            Object evaluate = vals.get(i).evaluate(null);
            if (evaluate instanceof String) {
                sql.str(evaluate.toString());
            } else {
                sql.add(evaluate);
            }
            if (i + 1 < vals.size()) {
                sql.add(',');
            }
        }
        sql.add(')');
        return obj;
    }

    @Override
    public Object visit(Null isNull, Object obj) {
        isNull.property().accept(this, obj);
        sql.add(" IS");
        if (isNull.negated()) {
            sql.add(" NOT");
        }
        sql.add(" NULL");
        return obj;
    }

    Field field(Expression e1, Expression e2) {
        if (schema == null) {
            return null;
        }

        Property prop = null;
        if (e1 instanceof Property) {
            prop = (Property) e1;
        }
        else if (e2 instanceof Property) {
            prop = (Property) e2;
        }

        if (prop != null) {
            return schema.field(prop.property());
        }

        return null;
    }
}
