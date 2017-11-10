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
package io.jeo.geojson.parser;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import io.jeo.json.parser.ParseException;
import io.jeo.proj.Proj;
import io.jeo.util.Consumer;
import io.jeo.vector.Feature;
import io.jeo.vector.MapFeature;
import org.osgeo.proj4j.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Geometry;

public class FeatureHandler extends BaseHandler {

    Integer id;

    public FeatureHandler() {
        this(null);
    }

    public FeatureHandler(Integer id) {
        this.id = id;
    }

    @Override
    public boolean startObject() throws ParseException, IOException {
        return true;
    }

    @Override
    public boolean startObjectEntry(String key) throws ParseException,
            IOException {
        if ("type".equals(key)) {
            push(key, new TypeHandler());
        }
        if ("crs".equals(key)) {
            push(key, new CRSHandler());
        }
        if ("geometry".equals(key)) {
            push(key, new GeometryHandler());
        }
        else if ("properties".equals(key)) {
            push(key, new PropertiesHandler());
        }
        else if ("id".equals(key)) {
            push(key, new IdHandler());
        }

        return true;
    }

    @Override
    public boolean endObjectEntry() throws ParseException, IOException {
        return true;
    }

    @Override
    public boolean endObject() throws ParseException, IOException {
        final Geometry geom = node.consume("geometry", Geometry.class).orElse(null);
        if (geom != null && Proj.crs(geom) == null) {
            node.consume("crs", CoordinateReferenceSystem.class).ifPresent(new Consumer<CoordinateReferenceSystem>() {
                @Override
                public void accept(CoordinateReferenceSystem val) {
                    Proj.crs(geom, val);
                }
            });
        }

        Map<String,Object> props = node.consume("properties", Map.class)
            .orElse(new LinkedHashMap<String, Object>());

        props.put("geometry", geom);

        String fid = node.consume("id", String.class).orElse(id != null ? String.valueOf(id) : null);

        node.setValue(new MapFeature(fid, props));

        pop();
        return true;
    }

    static class IdHandler extends BaseHandler {
        @Override
        public boolean primitive(Object value) throws ParseException, IOException {
            node.setValue(value);

            pop();
            return true;
        }
    }
}
