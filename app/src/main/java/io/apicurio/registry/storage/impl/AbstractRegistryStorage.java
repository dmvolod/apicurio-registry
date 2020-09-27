/*
 * Copyright 2019 Red Hat
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

package io.apicurio.registry.storage.impl;

import io.apicurio.registry.content.ContentHandle;
import io.apicurio.registry.storage.*;
import io.apicurio.registry.types.ArtifactType;
import io.apicurio.registry.types.RuleType;
import io.apicurio.registry.utils.ConcurrentUtil;
import io.vertx.mutiny.core.eventbus.EventBus;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;

/**
 * @author Ales Justin
 */
public abstract class AbstractRegistryStorage implements RegistryStorage {
    @Inject
    EventBus bus;

    // workaround for Quarkus issue #9887

    @Override
    public boolean isReady() {
        return getGlobalRules() != null;
    }

    @Override
    public boolean isAlive() {
        return true;
    }

    @Override
    public void createArtifactRule(String artifactId, RuleType rule, RuleConfigurationDto config) throws ArtifactNotFoundException, RuleAlreadyExistsException, RegistryStorageException {
        ConcurrentUtil.result(createArtifactRuleAsync(artifactId, rule, config));
    }

    public CompletionStage<ArtifactMetaDataDto> createArtifact(String artifactId, ArtifactType artifactType, ContentHandle content) throws ArtifactAlreadyExistsException, RegistryStorageException {
        return createArtifactWithEvent(artifactId, artifactType, content).whenComplete((res, th) -> {
            System.out.println("atrifactId " + res.getId());
            bus.publish("createArtifact", res.getId());
        });
    }
}
