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
package io.jeo.util;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import io.jeo.geojson.GeoJSONReader;
import io.jeo.geom.Bounds;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Conversion utility class.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class Convert {

    public static <T> Optional<T> to(Object obj, Class<T> clazz) {
        return to(obj, clazz, true);
    }
    
    public static <T> Optional<T> to(Object obj, Class<T> clazz, boolean safe) {
        if (clazz.isInstance(obj)) {
            return Optional.of((T) obj);
        }

        if (clazz == String.class) {
            return (Optional<T>) toString(obj);
        }
        else if (Number.class.isAssignableFrom(clazz)) {
            return toNumber(obj, (Class)clazz);
        }
        else if (Boolean.class.isAssignableFrom(clazz)) {
            return (Optional<T>) toBoolean(obj);
        }
        else if (File.class.isAssignableFrom(clazz)) {
            return (Optional<T>) toFile(obj);
        }
        else if (Path.class.isAssignableFrom(clazz)) {
            return (Optional<T>) toPath(obj);
        }
        else if (URL.class.isAssignableFrom(clazz)) {
            return (Optional<T>) toURL(obj);
        }
        else if (URI.class.isAssignableFrom(clazz)) {
            return (Optional<T>) toURI(obj);
        }
        else if (!safe && obj != null) {
            //constructor trick
            T converted = null;
            try {
                try {
                    converted = clazz.getConstructor(obj.getClass()).newInstance(obj);
                } 
                catch(NoSuchMethodException e) {
                    //try with string
                    converted = clazz.getConstructor(String.class).newInstance(obj.toString());
                }
            }
            catch(Exception e) {
            }
            return Optional.of(converted);
        }

        return Optional.empty();
    }

    public static Optional<String> toString(Object obj) {
        if (obj != null) {
            return Optional.of(obj.toString());
        }
        return Optional.empty();
    }

    public static Optional<Boolean> toBoolean(Object obj) {
        if (obj instanceof Boolean) {
            return Optional.of((Boolean) obj);
        }

        if (obj instanceof String) {
            return Optional.of(Boolean.parseBoolean((String) obj));
        }

        return Optional.empty();
    }

    public static Optional<Path> toPath(Object obj) {
        return toPath(obj, false);
    }

    static Optional<Path> toPath(Object obj, boolean checkFile) {
        if (obj instanceof Path) {
            return Optional.of(((Path) obj));
        }

        if (obj instanceof String) {
            return Optional.of(Paths.get((String)obj));
        }

        if (checkFile) {
            Optional<File> file = toFile(obj, false);
            return file.map(new Function<File, Path>() {
                @Override
                public Path apply(File f) {
                    return f.toPath();
                }
            });
        }

        return Optional.empty();
    }

    public static Optional<File> toFile(Object obj) {
        return toFile(obj, true);
    }

    static Optional<File> toFile(Object obj, boolean checkPath) {
        if (obj instanceof File) {
            return Optional.of((File) obj);
        }

        if (obj instanceof Path) {
            return Optional.of(((Path)obj).toFile());
        }

        if (obj instanceof String) {
            return Optional.of(new File((String)obj));
        }

        if (checkPath) {
            return toPath(obj, false).map(new Function<Path, File>() {
                @Override
                public File apply(Path p) {
                    return p.toFile();
                }
            });
        }

        return Optional.empty();
    }

    public static Optional<URL> toURL(Object obj) {
        if (obj instanceof String) {
            try {
                return Optional.of(new URL(obj.toString()));
            } catch (MalformedURLException e) {
            }
        }

        return Optional.empty();
    }

    public static Optional<URI> toURI(Object obj) {
        if (obj instanceof String) {
            try {
                return Optional.of(new URI(obj.toString()));
            } catch (URISyntaxException e) {
            }
        }

        return Optional.empty();
    }

    public static <T extends Number> Optional<T> toNumber(Object obj, Class<T> clazz) {
        Optional<Number> n = toNumber(obj);
        if (!n.isPresent()) {
            return Optional.empty();
        }

        if (clazz == Byte.class) {
            return Optional.of(clazz.cast(new Byte(n.get().byteValue())));
        }
        if (clazz == Short.class) {
            return Optional.of(clazz.cast(new Short(n.get().shortValue())));
        }
        if (clazz == Integer.class) {
            return Optional.of(clazz.cast(new Integer(n.get().intValue())));
        }
        if (clazz == Long.class) {
            return Optional.of(clazz.cast(new Long(n.get().longValue())));
        }
        if (clazz == Float.class) {
            return Optional.of(clazz.cast(new Float(n.get().floatValue())));
        }
        if (clazz == Double.class) {
            return Optional.of(clazz.cast(new Double(n.get().doubleValue())));
        }
        
        return Optional.empty();
    }

    public static <T extends Number> Optional<List<T>> toNumbers(Object obj, Class<T> clazz) {
        Collection<Object> l = null;
        if (obj instanceof Collection) {
            l = (Collection<Object>) obj;
        }
        else if (obj.getClass().isArray()) {
            l = new ArrayList<Object>();
            for (int i = 0; i < Array.getLength(obj); i++) {
                l.add(Array.get(obj, i));
            }
        }
        else if (obj instanceof String) {
            l = (List) Arrays.asList(obj.toString().split(" "));
        }

        if (l != null) {
            List<T> converted = new ArrayList<T>();
            for (Object o : l) {
                Optional<T> num = toNumber(o, clazz);
                if (!num.isPresent()) {
                    return (Optional) Optional.empty();
                }
                converted.add(num.get());
            }
            return Optional.of(converted);
        }

        return (Optional) Optional.empty();
    }

    public static Optional<Number> toNumber(Object obj) {
        if (obj instanceof Number) {
            return Optional.of((Number) obj);
        }
        if (obj instanceof String) {
            String str = (String) obj;
            try {
                return Optional.of((Number)Long.parseLong(str));
            }
            catch(NumberFormatException e) {
                try {
                    return Optional.of((Number)Double.parseDouble(str));
                }
                catch(NumberFormatException e1) {
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<Reader> toReader(Object obj) throws IOException {
        if (obj instanceof Reader) {
            return Optional.of((Reader) obj);
        }

        if (obj instanceof InputStream) {
            return Optional.of((Reader)new BufferedReader(new InputStreamReader((InputStream)obj, Util.UTF_8)));
        }

        if (obj instanceof File) {
            return Optional.of((Reader)Files.newBufferedReader(((File)obj).toPath(), Util.UTF_8));
        }
        if (obj instanceof Path) {
            return Optional.of((Reader)Files.newBufferedReader(((Path)obj), Util.UTF_8));
        }
        if (obj instanceof String) {
            return Optional.of((Reader) new StringReader((String) obj));
        }

        return null;
    }

    public static Optional<Geometry> toGeometry(Object obj) {
        if (obj instanceof Geometry) {
            return Optional.of((Geometry)obj);
        }
        if (obj instanceof Envelope) {
            Envelope env = (Envelope) obj;
            return Optional.of((Geometry) Bounds.toPolygon(env));
        }
        if (obj instanceof String) {
            String str = (String) obj;
            Geometry g = null;
            // wkt
            try {
                g = new WKTReader().read(str);
            } catch (ParseException e1) {
                // try geojson
                try {
                    g = new GeoJSONReader().geometry(str);
                }
                catch(Exception e2) {
                }
            }

            return Optional.of(g);
        }

        return Optional.empty();
    }

    public static Optional<Envelope> toEnvelope(Object obj) {
        if(obj instanceof Envelope) {
            return Optional.of((Envelope)obj);
        }
        if (obj instanceof String) {
            Envelope env = Bounds.parse(obj.toString());
            if (env != null) {
                return Optional.of(env);
            }
        }
        return toGeometry(obj).map(new Function<Geometry, Envelope>() {
            @Override
            public Envelope apply(Geometry value) {
                return value.getEnvelopeInternal();
            }
        });
    }
}
