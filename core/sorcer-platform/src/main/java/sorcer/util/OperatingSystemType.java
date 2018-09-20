/*
 * Copyright to the original author or authors.
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
package sorcer.util;

/**
 * A simple utility to get the operating system type
 *
 * @author Dennis Reedy
 */
public class OperatingSystemType {
    static {
        new OperatingSystemType();
    }
    private static String opSysType;

    private OperatingSystemType() {
        if(System.getProperty("os.name").startsWith("Windows")) {
            opSysType = "win";
        } else if(System.getProperty("os.name").startsWith("Linux")) {
            opSysType = "linux";
        } else {
            opSysType = "mac";
        }
    }

    public static String get() {
        return opSysType;
    }

    public static boolean isWindows() {
        return opSysType.equals("win");
    }

    public static boolean isLinux() {
        return opSysType.equals("linux");
    }

    public static boolean isMac() {
        return opSysType.equals("mac");
    }

}
