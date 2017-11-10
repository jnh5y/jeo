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

import org.locationtech.jts.io.WKBReader;
import io.jeo.sql.PrimaryKey;
import io.jeo.sql.PrimaryKeyColumn;
import io.jeo.vector.Feature;
import io.jeo.vector.FeatureCursor;
import io.jeo.vector.ListFeature;
import io.jeo.vector.MapFeature;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PostGISCursor extends FeatureCursor {

    ResultSet rs;
    Connection cx;
    PostGISDataset dataset;
    Boolean hasNext;
    Feature next;

    PostGISCursor(ResultSet rs, Connection cx, PostGISDataset dataset) {
        this.rs = rs;
        this.cx = cx;
        this.dataset = dataset;
    }

    @Override
    public boolean hasNext() throws IOException {
        if (hasNext == null) {
            try {
                hasNext = rs.next();
            } catch (SQLException e) {
                handle(e);
            }
        }
        return hasNext;
    }

    @Override
    public Feature next() throws IOException {
        if (hasNext != null && hasNext.booleanValue()) {
            try {
                ResultSetMetaData md = rs.getMetaData();
                Map<String,Object> map = new LinkedHashMap<>();

                for (int i = 0; i < md.getColumnCount(); i++) {
                    Object obj = rs.getObject(i+1);
                    String col = md.getColumnName(i+1);

                    if (dataset.schema().field(col).geometry()) {
                        obj = new WKBReader().read(rs.getBytes(i+1));
                    }

                    map.put(col, obj);
                }

                PrimaryKey key = dataset.getTable().primaryKey();
                StringBuilder sb = new StringBuilder();
                for (PrimaryKeyColumn pkcol : key.getColumns()) {
                    sb.append(rs.getObject(pkcol.getName())).append(".");
                }
                if (!key.getColumns().isEmpty()) {
                    sb.setLength(sb.length()-1);
                }

                next = new MapFeature(sb.toString(), map);
                return next;
            }
            catch(Exception e) {
                handle(e);
            }
            finally {
                hasNext = null;
            }
        }

        return null;
    }

    @Override
    public void close() throws IOException {
        if (rs != null) {
            Statement st = null;
            try {
                st = rs.getStatement();
            } catch (SQLException e) {}

            try {
                rs.close();
            } catch (SQLException e) {}

            if (st != null) {
                try {
                    st.close();
                } catch (SQLException e) {}
            }
            if (cx != null) {
                try {
                    cx.close();
                } catch (SQLException e) {}
            }
        }
    }

    void handle(Exception e) throws IOException {
        close();
        throw new IOException(e);
    }
}
