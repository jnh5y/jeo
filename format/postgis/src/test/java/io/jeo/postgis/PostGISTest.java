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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;

import io.jeo.data.Cursor;
import io.jeo.data.Dataset;
import io.jeo.data.Handle;
import io.jeo.vector.FeatureCursor;
import io.jeo.vector.FeatureWriteCursor;
import io.jeo.vector.VectorQuery;
import io.jeo.vector.VectorDataset;
import io.jeo.vector.Feature;
import io.jeo.vector.Schema;
import io.jeo.vector.SchemaBuilder;
import io.jeo.geom.GeomBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.postgresql.ds.PGPoolingDataSource;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

public class PostGISTest {

    PostGISWorkspace pg;

    //uncomment to view debug logs during test
//    @BeforeClass
//    public static void logging() {
//        PostGISTests.logging();
//    }

    @BeforeClass
    public static void connect()  {
        PostGISTests.connect();
    }

    @BeforeClass
    public static void setUpData() throws Exception {
        PostGISTests.setupStatesData();
    }

    @Before
    public void rollback() throws Exception {
        PGPoolingDataSource ds = PostGISWorkspace.createDataSource(PostGISTests.OPTS); 
        try (Connection cx = ds.getConnection()) {
            try (Statement st = cx.createStatement()) {
                st.executeUpdate(
                    "DELETE FROM states WHERE \"STATE_NAME\" = 'JEOLAND' OR \"STATE_NAME\" is null");
                st.executeUpdate("UPDATE states set \"STATE_ABBR\" = upper(\"STATE_ABBR\")");
                st.executeUpdate("DROP TABLE IF EXISTS widgets");
            }
        }
        ds.close();
    }

    @Before
    public void setUp() throws Exception {
        pg = new PostGISWorkspace(PostGISTests.OPTS);
    }

    @After
    public void tearDown() throws Exception {
        pg.close();
    }
    @Test
    public void testLayers() throws Exception {
        Iterables.find(pg.list(), refFor("states"));
        try {
            Iterables.find(pg.list(), refFor("geometry_columns"));
            fail();
        }
        catch(NoSuchElementException e) {}
        try {
            Iterables.find(pg.list(), refFor("geography_columns"));
            fail();
        }
        catch(NoSuchElementException e) {}
        try {
            Iterables.find(pg.list(), refFor("raster_columns"));
            fail();
        }
        catch(NoSuchElementException e) {}
        try {
            Iterables.find(pg.list(), refFor("raster_overviews"));
            fail();
        }
        catch(NoSuchElementException e) {}
        try {
            Iterables.find(pg.list(), refFor("topology"));
            fail();
        }
        catch(NoSuchElementException e) {}
    }

    Predicate<Handle<Dataset>> refFor(final String name) {
        return new Predicate<Handle<Dataset>>() {
            @Override
            public boolean apply(Handle<Dataset> input) {
                return name.equals(input.name());
            }
        };
    }

    @Test
    public void testSchema() throws Exception {
        VectorDataset states = pg.get("states");
        assertNotNull(states);
        
        Schema schema = states.schema();

        assertNotNull(schema.field("STATE_NAME"));
        assertNotNull(schema.geometry());
        assertEquals(MultiPolygon.class, schema.geometry().type());

        assertNotNull(schema.crs());
        assertEquals("EPSG:4326", schema.crs().getName());
    }

    @Test
    public void testBounds() throws Exception {
        VectorDataset states = pg.get("states");

        Envelope bounds = states.bounds();
        assertNotNull(bounds);

        assertEquals(-124.7, bounds.getMinX(), 0.1);
        assertEquals(25.0, bounds.getMinY(), 0.1);
        assertEquals(-67.0, bounds.getMaxX(), 0.1);
        assertEquals(49.3, bounds.getMaxY(), 0.1);
    }

    @Test
    public void testCount() throws Exception {
        VectorDataset states = pg.get("states");
        assertEquals(49, states.count(new VectorQuery()));
    }
    
    @Test
    public void testCountWithBounds() throws Exception {
        VectorDataset states = pg.get("states");
        Set<String> abbrs = Sets.newHashSet("MO", "OK", "TX", "NM", "AR", "LA"); 

        Envelope bbox = new Envelope(-106.649513, -93.507217, 25.845198, 36.493877);
        assertEquals(abbrs.size(), states.count(new VectorQuery().bounds(bbox)));
    }

    @Test
    public void testCursorRead() throws Exception {
        VectorDataset states = pg.get("states");

        Cursor<Feature> c = states.read(new VectorQuery());
        
        assertNotNull(c);
        for (int i = 0; i < 49; i++) {
            assertTrue(c.hasNext());

            Feature f = c.next();
            //assertEquals(new Integer(i+1), Integer.valueOf(f.getId()));
            assertNotNull(f);

            assertTrue(f.geometry() instanceof MultiPolygon);
            assertNotNull(f.get("STATE_NAME"));
        }

        assertFalse(c.hasNext());
        assertNull(c.next());
        c.close();
    }

    @Test
    public void testCursorFilter() throws Exception {
        VectorDataset states = pg.get("states");
        assertEquals(1, states.read(new VectorQuery().filter("STATE_NAME = 'Texas'")).count());
    }

    @Test
    public void testCursorUpdate() throws Exception {
        VectorDataset states = pg.get("states");
        
        FeatureWriteCursor c = states.update(new VectorQuery());
        while(c.hasNext()) {
            Feature f = c.next();

            String abbr = f.get("STATE_ABBR").toString();
            assertEquals(abbr, abbr.toUpperCase(Locale.ROOT));

            f.put("STATE_ABBR", f.get("STATE_ABBR").toString().toLowerCase(Locale.ROOT));
            c.write();
        }
        c.close();

        for (Feature f : states.read(new VectorQuery())) {
            String abbr = f.get("STATE_ABBR").toString();

            assertEquals(abbr, abbr.toLowerCase(Locale.ROOT));
        }

        c.close();
    }

    @Test
    public void testCursorInsert() throws Exception {
        VectorDataset states = pg.get("states");
        Schema schema = states.schema();

        FeatureWriteCursor c = states.append(new VectorQuery());
        Feature f = c.next();

        GeomBuilder gb = new GeomBuilder();
        Geometry g = gb.point(0,0).point().buffer(1).toMultiPolygon();
        f.put(schema.geometry().name(), g);
        f.put("STATE_NAME", "JEOLAND");
        c.write();
        c.close();

        assertEquals(50, states.count(new VectorQuery()));

        FeatureCursor d = states.read(new VectorQuery().bounds(g.getEnvelopeInternal()));
        assertTrue(d.hasNext());

        assertEquals("JEOLAND", d.next().get("STATE_NAME"));
        d.close();
    }

    @Test
    public void testCreate() throws Exception {
        Schema widgets = new SchemaBuilder("widgets").field("shape", Polygon.class)
            .field("name", String.class).field("cost", Double.class).schema();

        PostGISDataset data = pg.create(widgets);
        assertEquals(0, data.count(new VectorQuery()));

        GeomBuilder gb = new GeomBuilder();
        FeatureWriteCursor c = data.append(new VectorQuery());

        Feature f = c.next();
        f.put("shape", gb.point(0,0).point().buffer(10).get());
        f.put("name", "bomb");
        f.put("cost", 1.99);
        c.write();

        f = c.next();
        f.put("shape", gb.points(0,0,1,1).lineString().buffer(1).get());
        f.put("name", "dynamite");
        f.put("cost", 2.99);
        c.write();

        f = c.next();
        f.put("shape", gb.points(-5,5, 5,5, 2,-2, 3,-5, -3,-5, -2,-2, -5,5).ring().toPolygon());
        f.put("name", "anvil");
        f.put("cost", 3.99);

        c.write();
        c.close();

        data = pg.get("widgets");
        assertEquals(3, data.count(new VectorQuery()));

        FeatureCursor d = data.read(new VectorQuery().filter("name = 'bomb'"));
        assertTrue(d.hasNext());
        assertEquals(1.99, d.next().get("cost"));
        d.close();
    }

}
