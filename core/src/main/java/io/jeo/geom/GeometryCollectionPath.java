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
package io.jeo.geom;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;

public class GeometryCollectionPath extends CoordinatePath {

    GeometryCollection gcol;

    CoordinatePath it;
    int i;

    GeometryCollectionPath(GeometryCollection gcol) {
        this.gcol = gcol;
        doReset();
    }

    @Override
    public Geometry geometry() {
        return gcol;
    }

    @Override
    protected PathStep doNext(Coordinate c) {
        while(!it.hasNext()) {
            if (++i == gcol.getNumGeometries()) {
                return PathStep.STOP;
            }

            it = create(gcol.getGeometryN(i));
        }

        return next(c);
    }

    PathStep next(Coordinate c) {
        Coordinate d = it.next();
        c.x = d.x;
        c.y = d.y;

        return it.step();
    }

    @Override
    protected void doReset() {
        i = 0;
        it = create(gcol.getGeometryN(i));
    }

}
