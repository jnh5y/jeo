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
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

public class PolygonPath extends CoordinatePath {

    /** the polygon */
    final Polygon poly;

    /** coordinate index */
    int i;

    /** inner ring index */
    int r;

    /** current ring coordinates */
    CoordinateSequence coords;

    /** current number of coordinates */
    int numCoords;

    PolygonPath(Polygon poly) {
        this.poly = poly;
        doReset();
    }

    @Override
    public Geometry geometry() {
        return poly;
    }

    @Override
    protected PathStep doNext(Coordinate c) {
        if (i < numCoords) {
            coords.getCoordinate(i, c);

            return i++ == 0 ? PathStep.MOVE_TO :
                i == numCoords ? PathStep.CLOSE : PathStep.LINE_TO;
        }
        else {
            //new path?
            if (poly.getNumInteriorRing() > 0 && r < poly.getNumInteriorRing()) {
                i=0;

                coords = poly.getInteriorRingN(r++).getCoordinateSequence();
                numCoords = coords.size();

                coords.getCoordinate(i++, c);
                return PathStep.MOVE_TO;
            }
            else {
                return PathStep.STOP;
            }
        }
    }

    protected void doReset() {
        i = 0;
        r = 0;
        coords = poly.getExteriorRing().getCoordinateSequence();
        numCoords = coords.size();
    }
}
