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

/**
 * Annotations {@link Path} and {@link Schema} are markers for adding schema to service contexts.
 * There is the <code>service interface</code> and the <code>schema interface</code>.
 * <code>Service interface</code> is the interface implemented by the service provider. All its methods return a {@link Context} and take one as a parameter.
 * <code>Schema interface</code> is an interface that describes required and optional context paths along with their direction and a 'required' attribute.
 */
package sorcer.schema;
