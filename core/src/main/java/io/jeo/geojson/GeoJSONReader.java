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
package io.jeo.geojson;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;

import io.jeo.data.Cursor;
import io.jeo.data.Cursors;
import io.jeo.geojson.parser.GeometryHandler;
import io.jeo.geojson.parser.RootHandler;
import io.jeo.json.parser.JSONParser;
import io.jeo.json.parser.ParseException;
import io.jeo.vector.Feature;
import io.jeo.geojson.parser.BaseHandler;
import io.jeo.geojson.parser.FeatureCollectionHandler;
import io.jeo.geojson.parser.FeatureHandler;
import io.jeo.geojson.parser.UnkownHandler;
import io.jeo.util.Convert;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * GeoJSON reader class.
 * <p>
 * See <a href="http://www.geojson.org/geojson-spec.html">GeoJSON Specification</a>.
 * </p>
 * <p>
 * Methods of this class take any input that be converted to a {@link Reader}. See the 
 * {@link Convert#toReader(Object)} method for details on accepted inputs. 
 * </p>
 * <p>
 * Example:
 * <pre><code>
 * GeoJSONReader reader = new GeoJSONReader();
 * reader.read("{ 'type': 'Point', coordinates: [1.0, 2.0] }");
 * </code></pre>
 * </p>
 * @author Justin Deoliveira, Boundless
 */
public class GeoJSONReader {

    /**
     * Reads a geometry object.
     * 
     * @param json Input object, see {@link Convert#toReader(Object)}.
     * 
     * @return The geometry. 
     */
    public Geometry geometry(Object json) {
        return (Geometry) parse(json, new GeometryHandler());
    }

    /**
     * Reads a point object.
     * 
     * @param json Input object, see {@link Convert#toReader(Object)}.
     * 
     * @return The point. 
     */
    public Point point(Object json) {
        return (Point) geometry(json); 
    }

    /**
     * Reads a linestring object.
     * 
     * @param json Input object, see {@link Convert#toReader(Object)}.
     * 
     * @return The linestring. 
     */
    public LineString lineString(Object json) {
        return (LineString) geometry(json);
    }

    /**
     * Reads a polygon object.
     * 
     * @param json Input object, see {@link Convert#toReader(Object)}.
     * 
     * @return The polygon. 
     */
    public Polygon polygon(Object json) {
        return (Polygon) geometry(json);
    }

    /**
     * Reads a multipoint object.
     * 
     * @param json Input object, see {@link Convert#toReader(Object)}.
     * 
     * @return The multipoint. 
     */
    public MultiPoint multiPoint(Object json) {
        return (MultiPoint) geometry(json);
    }

    /**
     * Reads a multilinestring object.
     * 
     * @param json Input object, see {@link Convert#toReader(Object)}.
     * 
     * @return The multilinetring. 
     */
    public MultiLineString multiLineString(Object json) {
        return (MultiLineString) geometry(json);
    }

    /**
     * Reads a multipolygon object.
     * 
     * @param json Input object, see {@link Convert#toReader(Object)}.
     * 
     * @return The multipolygon. 
     */
    public MultiPolygon multiPolygon(Object json) {
        return (MultiPolygon) geometry(json);
    }

    /**
     * Reads a geometry collection object.
     * 
     * @param json Input object, see {@link Convert#toReader(Object)}.
     * 
     * @return The geometry collection. 
     */
    public GeometryCollection geometryCollection(Object json) {
        return (GeometryCollection) geometry(json);
    }

    /**
     * Reads a feature object.
     * 
     * @param json Input object, see {@link Convert#toReader(Object)}.
     * 
     * @return The feature. 
     */
    public Feature feature(Object json) {
        return (Feature) parse(json, new FeatureHandler());
    }

    /**
     * Reads a feature collection object.
     * 
     * @param json Input object, see {@link Convert#toReader(Object)}.
     * 
     * @return The feature collection as a cursor. 
     */
    public Cursor<Feature> features(Object json) {
        try {
            return new GeoJSONCursor(toReader(json));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads a geojson object.
     * 
     * @param json Input object, see {@link Convert#toReader(Object)}.
     * 
     * @return The geojson object. 
     */
    public Object read(Object json) {
        UnkownHandler h = new UnkownHandler();
        Object result = parse(json, h);

        if (h.getHandler() instanceof FeatureCollectionHandler) {
            return Cursors.create((Collection<Feature>) result);
        }

        return result;
    }

    Reader toReader(Object input) throws IOException {
        return Convert.toReader(input).get("unable to turn " + input + " into reader");
    }

    Object parse(Object input, BaseHandler handler) {
        try {
            return parse(toReader(input), handler);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    Object parse(Reader input, BaseHandler handler) throws IOException {
        JSONParser p = new JSONParser();
        RootHandler h = new RootHandler(handler);
        try {
            p.parse(input, h);
            return h.getValue();
        } catch (ParseException e) {
            throw new IOException(e);
        }
    }
}
