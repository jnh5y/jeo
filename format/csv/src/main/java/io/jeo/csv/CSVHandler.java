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
package io.jeo.csv;

import java.io.IOException;
import java.util.List;

import com.csvreader.CsvReader;
import org.locationtech.jts.geom.Geometry;

/**
 * Strategy dealing with structure of CSV file. 
 */
public abstract class CSVHandler {

    public abstract void header(CsvReader r) throws IOException;

    public abstract Geometry geom(CsvReader r) throws IOException;
}
