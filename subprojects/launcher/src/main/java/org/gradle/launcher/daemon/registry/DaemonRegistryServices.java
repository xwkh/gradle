/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.launcher.daemon.registry;

import org.gradle.api.internal.cache.Cache;
import org.gradle.api.internal.cache.CacheAccessSerializer;
import org.gradle.api.internal.cache.MapBackedCache;
import org.gradle.cache.internal.DefaultFileLockManager;
import org.gradle.cache.internal.DefaultProcessMetaDataProvider;
import org.gradle.cache.internal.FileLockManager;
import org.gradle.internal.Factory;
import org.gradle.internal.nativeplatform.ProcessEnvironment;
import org.gradle.internal.nativeplatform.services.NativeServices;
import org.gradle.internal.service.DefaultServiceRegistry;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Takes care of instantiating and wiring together the services required for a daemon registry.
 */
public class DaemonRegistryServices extends DefaultServiceRegistry {
    private final File daemonBaseDir;
    private final Cache<File, DaemonRegistry> daemonRegistryCache;

    private static final Map<File, DaemonRegistry> REGISTRY_STORAGE = new HashMap<File, DaemonRegistry>();
    private static final Cache<File, DaemonRegistry> REGISTRY_CACHE = new CacheAccessSerializer<File, DaemonRegistry>(
            new MapBackedCache<File, DaemonRegistry>(REGISTRY_STORAGE)
    );

    public DaemonRegistryServices(File daemonBaseDir) {
        this(daemonBaseDir, REGISTRY_CACHE);
        add(new NativeServices());
    }

    protected DaemonRegistryServices(File daemonBaseDir, Cache<File, DaemonRegistry> daemonRegistryCache) {
        this.daemonBaseDir = daemonBaseDir;
        this.daemonRegistryCache = daemonRegistryCache;
    }

    protected DaemonDir createDaemonDir() {
        return new DaemonDir(daemonBaseDir);
    }

    protected FileLockManager createFileLockManager() {
        return new DefaultFileLockManager(new DefaultProcessMetaDataProvider(get(ProcessEnvironment.class)));
    }

    protected DaemonRegistry createDaemonRegistry() {
        final File daemonRegistryFile = get(DaemonDir.class).getRegistry();
        return daemonRegistryCache.get(daemonRegistryFile, new Factory<DaemonRegistry>() {
            public DaemonRegistry create() {
                return new PersistentDaemonRegistry(daemonRegistryFile, get(FileLockManager.class));
            }
        });
    }
    
    protected Properties createProperties() {
        return System.getProperties();
    }
}
