/*
 * Copyright 2019 Aitu Software Limited.
 *
 * https://aitusoftware.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aitusoftware.aether.net;

import java.util.Optional;

public final class Context
{
    private Mode mode = Configuration.mode();

    public Mode mode()
    {
        return mode;
    }

    public Context mode(final Mode mode)
    {
        this.mode = mode;
        return this;
    }

    public static final class Configuration
    {
        public static final String MODE_PROPERTY_NAME = "aether.net.mode";

        public static Mode mode()
        {
            return Optional.ofNullable(System.getProperty(MODE_PROPERTY_NAME))
                .map(Mode::valueOf).orElse(Mode.NETWORK);
        }
    }
}
