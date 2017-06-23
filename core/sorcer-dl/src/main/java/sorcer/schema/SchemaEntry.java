/*
 * Copyright 2013 Sorcersoft.com S.A.
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

package sorcer.schema;

import sorcer.service.Signature.Direction;

import java.io.Serializable;

/**
 * @author Rafał Krupiński
 */
public class SchemaEntry implements Serializable {
    private static final long serialVersionUID = -809999449914128883L;
    public String path;
    public Direction direction;
    public boolean required;
    public Class type;
    public String tag;
    public String description;
    public boolean result;
    public String[] returnArgPaths;

    protected SchemaEntry() {
    }

    public SchemaEntry(Class type, String path, Direction direction, boolean required, String tag, String description, boolean result, String[] returnArgPaths) {
        if (type == null) throw new IllegalArgumentException("fiType is null");
        if (path == null && tag == null) throw new IllegalArgumentException("path is null and no tags");
        if (direction == null) throw new IllegalArgumentException("direction is null");
        this.type = type;
        this.path = path;
        this.direction = direction;
        this.required = required;
        this.tag = tag;
        this.description = description;
        this.result = result;
        this.returnArgPaths = returnArgPaths;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj != null && obj == this) || (obj instanceof SchemaEntry && equals((SchemaEntry) obj));
    }

    protected boolean equals(SchemaEntry entry) {
        return compare(path, entry.path) && equals(tag, entry.tag);
    }

    @Override
    public int hashCode() {
        return 13 * (path == null ? 0 : path.hashCode()) + 37 * (tag == null ? 0 : tag.hashCode());
    }

    @Override
    public String toString() {
        return (required ? "required" : "optional") + " " + type.getName() + " " + path + " " + tag + " " + direction;
    }

    private static boolean compare(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    private static boolean equals(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }
}
