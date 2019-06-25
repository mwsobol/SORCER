package sorcer.core.service;
/*
 * Copyright 2013, 2014 Sorcersoft.com S.A.
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

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.EmptyConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import sorcer.config.Component;
import sorcer.config.ConfigEntry;
import sorcer.config.Configurable;
import sorcer.config.convert.TypeConverter;
import sorcer.core.provider.ServiceExerter;
import sorcer.util.reflect.Fields;
import sorcer.util.reflect.Methods;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Configure object of which class is annotated with @Component and methods or fields annotated with ConfigEntry
 *
 * @author Rafał Krupiński
 */
public class Configurer {
    final private static Logger log = LoggerFactory.getLogger(Configurer.class);

    public void preProcess(ServiceExerter provider, Object serviceBean) {
        try {
            process(serviceBean, provider.getProviderConfiguration());
        } catch (IllegalArgumentException x) {
            log.error("Error while processing {}", serviceBean, x);
            throw x;
        } catch (ConfigurationException e) {
            String message = MessageFormatter.format("Error while processing {}", serviceBean).getMessage();
            log.error(message, e);
            throw new IllegalArgumentException(message, e);
        }
    }

    public void process(Object object, Configuration config) throws ConfigurationException {
        if (config instanceof EmptyConfiguration)
            return;
        log.debug("Processing {} with {}", object, config);
        if (object instanceof Configurable) {
            ((Configurable) object).configure(config);
        }
        Class<?> targetClass = object.getClass();
        Component configurable = targetClass.getAnnotation(Component.class);
        if (configurable == null) return;

        String component = configurable.value();

        for (Field field : Fields.findAll(targetClass, ConfigEntry.class)) {
            updateField(object, field, config, component, field.getAnnotation(ConfigEntry.class));
        }

        for (Method method : Methods.findAll(targetClass, ConfigEntry.class)) {
            updateProperty(object, method, config, component, method.getAnnotation(ConfigEntry.class));
        }
    }

    public static <T> T getConfig(Class<T> type, Configuration config) {
        try {
            T result = type.newInstance();
            new Configurer().process(result, config);
            return result;
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        } catch (ConfigurationException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private void updateProperty(Object object, Method method, Configuration config, String defComponent, ConfigEntry configEntry) {
        Class<?>[] ptypes = method.getParameterTypes();
        if (ptypes.length != 1) return;
        Class<?> type = ptypes[0];

        Object defaultValue = Configuration.NO_DEFAULT;
        Object value;
        String component = ConfigEntry.DEFAULT_COMPONENT.equals(configEntry.component()) ? defComponent : configEntry.component();
        String entryKey = getEntryKey(getPropertyName(method), configEntry);
        boolean required = configEntry.required();
        try {
            Class<?> entryType = getEntryType(type, configEntry);
            value = config.getEntry(component, entryKey, entryType, defaultValue);
            value = convert(value, type, configEntry);
        } catch (ConfigurationException e) {
            if (required)
                throw new IllegalArgumentException("Could not configure " + method + " with " + entryKey, e);
            else
                log.debug("Could not configure {} with {} {}", method, entryKey, e.getMessage());
            return;
        } catch (IllegalArgumentException e) {
            if (required)
                throw e;
            else {
                log.debug("Could not configure {} with {} {}", method, entryKey, e.getMessage());
                return;
            }
        }

        if (type.isPrimitive() && value == null) {
            log.debug("Value for a primitive property is null");
            return;
        }

        try {
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
        } catch (SecurityException ignored) {
            log.warn("Could not set eval of {} because of access restriction", method);
            return;
        }

        try {
            log.debug("Configure {} to {}", method.getName(), value);
            method.invoke(object, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private String getPropertyName(Method m) {
        String name = m.getName();
        if (name.length() > 3 && name.startsWith("set") && Character.isUpperCase(name.charAt(3))) {
            return "" + Character.toLowerCase(name.charAt(3)) + name.substring(4);
        }
        return name;
    }

    private void updateField(Object target, Field field, Configuration config, String defComponent, ConfigEntry configEntry) {
        try {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
        } catch (SecurityException x) {
            log.warn("Could not set eval of {} because of access restriction", field, x);
            return;
        }

        String entryKey = getEntryKey(field.getName(), configEntry);
        String component = ConfigEntry.DEFAULT_COMPONENT.equals(configEntry.component()) ? defComponent : configEntry.component();
        try {
            Object defaultValue = field.get(target);
            Class<?> targetType = field.getType();
            Class<?> entryType = getEntryType(targetType, configEntry);

            Object value = config.getEntry(component, entryKey, entryType, defaultValue);
            value = convert(value, targetType, configEntry);
            if (configEntry.required() && value == null)
                throw new IllegalArgumentException("Null eval for required " + field);
            log.debug("Configure {} to {}", field.getName(), value);
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Error while writing config entry " + entryKey + " to " + field, e);
        } catch (ConfigurationException e) {
            throw new IllegalArgumentException("Error while writing config entry " + entryKey + " to " + field, e);
        }
    }

    private Class<?> getEntryType(Class targetType, ConfigEntry configEntry) {
        Class userType = configEntry.type();
        return userType == Void.class ? targetType : userType;
    }

    private <F, T> T convert(F value, Class<T> targetType, ConfigEntry configEntry) {
        Class sourceType = configEntry.type();
        if (sourceType == Void.class)
            return (T) value;
        Class<? extends TypeConverter> converterType = configEntry.converter();
        if (converterType == TypeConverter.class)
            throw new IllegalArgumentException("converter is required if multitype is provided");

        if (targetType.isInstance(value))
            return (T) value;

        try {
            TypeConverter<F, T> typeConverter = converterType.newInstance();
            return (T) typeConverter.convert(value, targetType);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String getEntryKey(String propertyName, ConfigEntry entry) {
        if (ConfigEntry.DEFAULT_KEY.equals(entry.value())) {
            return propertyName;
        } else {
            return entry.value();
        }
    }
}
