#
# Copyright to the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#!/bin/sh
path="$1"
class="$2"

for i in $(find $path -name "*.jar");
    # The grep options are:
	# H  Always print filename headers with output lines
	# s  Nonexistent and unreadable files are ignored
	# i  Perform case insensitive matching
    do echo $i; jar -tvf $i | grep -Hsi $class;
done	
