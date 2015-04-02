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
package io.jeo.filter;

import io.jeo.util.Convert;
import io.jeo.util.Optional;

import java.util.Objects;

/**
 * Filter that applies a binary comparison operator to two expression operands.  
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class Comparison<T> extends Filter<T> {

    /**
     * Comparison operator type.  
     */
    public static enum Type {
        EQUAL("="), NOT_EQUAL("!="),
        LESS("<") {
            @Override
            public Type invert() {
                return GREATER_OR_EQUAL;
            }
        },
        LESS_OR_EQUAL("<=") {
            @Override
            public Type invert() {
                return GREATER;
            }
        },
        GREATER(">") {
            @Override
            public Type invert() {
                return LESS_OR_EQUAL;
            }
        },
        GREATER_OR_EQUAL(">=") {
            @Override
            public Type invert() {
                return LESS;
            }
        };

        String op;

        Type(String op) {
            this.op = op;
        }

        public Type invert() {
          return this;
        }

        @Override
        public String toString() {
            return op;
        }
    }

    Type type;
    Expression left, right;

    public Comparison(Type type, Expression left, Expression right) {
        this.type = type;
        this.left = left;
        this.right = right;

        Objects.requireNonNull(left, "operands must not be null");
        Objects.requireNonNull(right, "right operand must not be null");
    }

    public Type type() {
        return type;
    }

    public Expression left() {
        return left;
    }

    public Expression right() {
        return right;
    }

    /**
     * Normalizes the filter.
     * <p>
     * A "normal" filter is one where a {@link Property} is compared to another expression.
     * This method returns <code>null</code> if the filter is not normal.
     * </p>
     */
    public Comparison<T> normalize() {
        if (left instanceof Property) {
            return this;
        }
        else if (right instanceof Property) {
            return new Comparison<>(type.invert(), right, left);
        }
        else {
            return null;
        }
    }

    @Override
    public boolean test(T obj) {
        Object o1 = left.evaluate(obj);
        Object o2 = right.evaluate(obj);

        return compare(o1, o2);
    }

    protected boolean compare(Object o1, Object o2) {
        if (o1 != null && !o1.getClass().isInstance(o2)) {
            //attempt to convert 
            Optional<?> converted = Convert.to(o2, o1.getClass());
            if (converted.isPresent()) {
                o2 = converted.get();
            }
        }

        // if either is NaN, shortcut here as Double.compareTo has different
        // behavior than expected (adheres to Object.equals ordering)
        if (isNaN(o1) || isNaN(o2)) {
            return false;
        }

        if (type == Type.EQUAL) {
            return o1 != null ? o1.equals(o2) : o2 == null;
        }
        if (type == Type.NOT_EQUAL) {
            return o1 != null ? !o1.equals(o2) : o2 != null;
        }

        if (o1 == null || o2 == null) {
            return false;
        }

        Comparable<Object> c1 = toComparable(o1);
        Comparable<Object> c2 = toComparable(o2);
        int compare = c1.compareTo(c2);

        switch(type) {
        case LESS:
            return compare < 0;
        case LESS_OR_EQUAL:
            return compare <= 0;
        case GREATER:
            return compare > 0;
        case GREATER_OR_EQUAL:
            return compare >= 0;
        default:
            throw new IllegalStateException();
        }
    }

    static boolean isNaN(Object o) {
        return o instanceof Number && Double.isNaN(((Number)o).doubleValue());
    }

    @SuppressWarnings("unchecked")
    protected Comparable<Object> toComparable(Object o) {
        if (o instanceof Comparable) {
            return (Comparable<Object>) o;
        }
        throw new IllegalArgumentException("Unable to convert " + o + " to comparable");
    }

    @Override
    public <R> R accept(FilterVisitor<R> v, Object obj) {
        return v.visit(this, obj);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((left == null) ? 0 : left.hashCode());
        result = prime * result + ((right == null) ? 0 : right.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Comparison<?> other = (Comparison<?>) obj;
        if (left == null) {
            if (other.left != null)
                return false;
        } else if (!left.equals(other.left))
            return false;
        if (right == null) {
            if (other.right != null)
                return false;
        } else if (!right.equals(other.right))
            return false;
        if (type != other.type)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return new StringBuilder().append(left).append(" ").append(type).append(" ").append(right)
            .toString();
    }
}