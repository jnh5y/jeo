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
package io.jeo.postgis;

import java.util.Map;

import io.jeo.sql.DbTypes;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;

public class PostGISTypes extends DbTypes {

    @Override
    protected Map<String, Class<?>> createNameMappings() {
        Map<String, Class<?>> name = super.createNameMappings();
        name.put("GEOGRAPHY", Geometry.class);
        name.put("POINTM", Point.class);
        name.put("LINESTRINGM", LineString.class);
        name.put("MULTIPOINTM", MultiPoint.class);
        name.put("MULTILINESTRINGM", MultiLineString.class);
        name.put("MULTIPOLYGONM", MultiPolygon.class);
        name.put("GEOMETRYCOLLECTIONM", GeometryCollection.class);
        name.put("BYTEA", byte[].class);
        return name;
    }
}
