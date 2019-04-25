/*
 * Copyright 2016 the original author or authors.
 * Copyright 2016 SorcerSoft.org.
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

package sorcer.service;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Mike Sobolewski on 5/15/16.
 */
public interface MetaFi extends Fi<Fi>, Serializable {

    public String getName();

    public String getPath();

    public Fi getSelect();

    public Fi selectSelect(String name) throws ConfigurationException;

    public void addSelect(Fi fidelity);

    public List<Fi> getSelects();

}
