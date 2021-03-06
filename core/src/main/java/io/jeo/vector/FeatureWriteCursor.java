/* Copyright 2015 The jeo project. All rights reserved.
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

import io.jeo.data.WriteCursor;

import java.io.IOException;

/**
 * Writable extension to {@link FeatureCursor}.
 * <p>
 * Usage:
 * <pre>
 *     FeatureCursor c = dataset.update(...);
 *     while (c.hasNext()) {
 *         MutableFeature f = c.next();
 *         f.put("foo", "bar");
 *         c.write();
 *     }
 *     c.close();
 * </pre>
 * </p>
 */
public abstract class FeatureWriteCursor extends FeatureCursor implements WriteCursor {

    /**
     * Returns an empty cursor.
     */
    public static FeatureWriteCursor empty() {
        return new FeatureWriteCursor() {
            @Override
            public boolean hasNext() throws IOException {
                return false;
            }

            @Override
            public Feature next() throws IOException {
                return null;
            }

            @Override
            public FeatureWriteCursor write() throws IOException {
                return null;
            }

            @Override
            public FeatureWriteCursor remove() throws IOException {
                return null;
            }

            @Override
            public void close() throws IOException {

            }
        };
    }

    @Override
    public abstract FeatureWriteCursor write() throws IOException;

    @Override
    public abstract FeatureWriteCursor remove() throws IOException;
}
